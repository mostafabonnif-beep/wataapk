/**
 * ProfanityFilter.kt
 * ─────────────────────────────────────────────────────────────────
 * Client-side comment moderation filter, ported 1:1 from
 * `functions/profanity.js` (same algorithm, same thresholds, same
 * reasons) so the app blocks obvious spam before any network call.
 *
 * ⚠️ CLIENT-SIDE ONLY — this check is trivially bypassable by a
 * determined user (anyone can craft a raw Firestore write). It exists
 * to keep the chat clean and give instant feedback. The authoritative
 * line of defense on the free tier is the Firestore Security Rules
 * (`admin/firestore.rules`), which validate payload shape, text
 * length, timestamp freshness, per-user posting interval, and reject
 * comments containing more than one link.
 *
 * Keep this file in sync with `functions/profanity.js`: if you add a
 * banned word here, add it there too (and vice versa).
 * ─────────────────────────────────────────────────────────────────
 */
package com.elwataniatv.app.util

import java.text.Normalizer
import java.util.Locale

object ProfanityFilter {

    // ── Reasons (mirror REASONS in functions/profanity.js) ──────
    const val REASON_PROFANE = "profane"
    const val REASON_TOO_LONG = "too_long"
    const val REASON_TOO_SHORT = "too_short"
    const val REASON_SHOUTING = "shouting"
    const val REASON_SPAM_REPEAT = "spam_repeat"
    const val REASON_TOO_MANY_LINKS = "too_many_links"

    // ── Banned words (mirror SEED_BANNED in functions/profanity.js)
    private val SEED_BANNED = listOf(
        // English (kept short on purpose)
        "fuck", "shit", "bitch", "asshole", "bastard", "cunt",
        // French
        "merde", "putain", "salope", "connard", "connasse", "enculé", "nique",
        // Arabic — common slurs
        "كس", "قحب", "زنا", "عاهر", "شرموطة", "زب", "يلعن", "حمار",
    )

    private val banned: Set<String> = SEED_BANNED.map(::normalise).toSet()

    /**
     * Normalise a word before adding it to the banned set.
     * Lowercase + strip Latin accents + collapse Arabic alef forms +
     * strip whitespace (same as functions/profanity.js).
     */
    fun normalise(word: String): String = word
        .lowercase(Locale.ROOT)
        .let { Normalizer.normalize(it, Normalizer.Form.NFD) }
        .replace(Regex("[\\u0300-\\u036f]"), "") // strip combining marks
        .replace(Regex("[\\u0622\\u0623\\u0625]"), "\u0627") // آ/أ/إ → ا
        .replace(Regex("[\\u064a]"), "\u0649") // ي → ى
        .replace(Regex("\\s+"), "")
        .trim()

    /**
     * True when `text` contains a banned word. Same algorithm as
     * `functions/profanity.js` isProfane().
     */
    fun isProfane(text: String): Boolean {
        if (text.isEmpty()) return false
        val collapsed = text
            .replace(Regex("\\s+"), " ")
            .lowercase(Locale.ROOT)
            .let { Normalizer.normalize(it, Normalizer.Form.NFD) }
            .replace(Regex("[\\u0300-\\u036f]"), "")
            .replace(Regex("(.)\\1{2,}"), "$1$1") // collapse repeated chars

        val tokens = collapsed.split(Regex("[^a-z\\u0600-\\u06FF]+")).filter { it.isNotEmpty() }
        for (token in tokens) {
            if (banned.contains(token)) return true
            for (w in banned) {
                if (w.length >= 4 && token.contains(w)) return true
            }
        }
        return false
    }

    data class ModerationResult(val ok: Boolean, val reason: String? = null)

    /**
     * Run all moderation checks. Returns ok=true if acceptable,
     * otherwise ok=false with a reason. Same thresholds as
     * functions/profanity.js moderate().
     */
    fun check(text: String, maxLength: Int = 500, minLength: Int = 2): ModerationResult {
        val trimmed = text.trim()

        if (trimmed.length < minLength) return ModerationResult(false, REASON_TOO_SHORT)
        if (trimmed.length > maxLength) return ModerationResult(false, REASON_TOO_LONG)

        val links = Regex("https?://", RegexOption.IGNORE_CASE).findAll(trimmed).count()
        if (links >= 3) return ModerationResult(false, REASON_TOO_MANY_LINKS)

        val letters = trimmed.replace(Regex("[^A-Za-z\\u0600-\\u06FF]"), "")
        if (letters.length > 20) {
            val upper = letters.count { it in 'A'..'Z' }
            if (upper.toFloat() / letters.length > 0.7f) {
                return ModerationResult(false, REASON_SHOUTING)
            }
        }

        if (Regex("(.)\\1{7,}").containsMatchIn(trimmed)) {
            return ModerationResult(false, REASON_SPAM_REPEAT)
        }

        if (isProfane(trimmed)) return ModerationResult(false, REASON_PROFANE)

        return ModerationResult(true)
    }

    /** Arabic user-facing message for a moderation reason. */
    fun userMessage(reason: String?): String = when (reason) {
        REASON_PROFANE -> "التعليق يحتوي على كلمات ممنوعة ولا يمكن نشره."
        REASON_TOO_LONG -> "التعليق طويل جداً (الحد الأقصى 500 حرف)."
        REASON_TOO_SHORT -> "التعليق قصير جداً."
        REASON_SHOUTING -> "يرجى الكتابة بأحرف عادية وليس كلها كبيرة."
        REASON_SPAM_REPEAT -> "يبدو أن التعليق مكرر أو غير واضح."
        REASON_TOO_MANY_LINKS -> "التعليق يحتوي على روابط كثيرة."
        else -> "لا يمكن نشر هذا التعليق."
    }
}
