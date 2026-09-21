package com.elwataniatv.app.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Multi-locale date formatting for Elwatania TV: supports Arabic (Algerian month names),
 * French, and English with relative day rules (Today / Yesterday / X days ago / X weeks ago).
 *
 * Implemented with Calendar/SimpleDateFormat (API 24-safe, no java.time).
 */
object DateFmt {

    private val ALGERIAN_MONTHS = arrayOf(
        "جانفي", "فيفري", "مارس", "أفريل", "ماي", "جوان",
        "جويلية", "أوت", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )

    private val FRENCH_MONTHS = arrayOf(
        "janvier", "février", "mars", "avril", "mai", "juin",
        "juillet", "août", "septembre", "octobre", "novembre", "décembre"
    )

    private val ENGLISH_MONTHS = arrayOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    private val ALGIERS = TimeZone.getTimeZone("Africa/Algiers")

    private val ISO_DATE = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = ALGIERS
        isLenient = false
    }
    private val ISO_PATTERNS = arrayOf(
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSX",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd"
    )

    /** Parses an ISO date, ISO datetime, or epoch-millis string; null when unparseable. */
    fun parse(raw: String): Calendar? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        trimmed.toLongOrNull()?.let { epoch ->
            return Calendar.getInstance(ALGIERS).apply { timeInMillis = epoch }
        }
        for (pattern in ISO_PATTERNS) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = ALGIERS
                    isLenient = false
                }
                val parsed: Date = sdf.parse(trimmed) ?: continue
                return Calendar.getInstance(ALGIERS).apply { time = parsed }
            } catch (_: Exception) {
                // try the next pattern
            }
        }
        return null
    }

    /** Relative or short date according to the active locale. */
    fun smartDate(raw: String, today: Calendar = Calendar.getInstance(ALGIERS), locale: Locale = Locale.getDefault()): String {
        val date = parse(raw) ?: return raw
        val dayOfDate = floorDivDay(date)
        val dayOfToday = floorDivDay(today)
        val days = ((dayOfToday - dayOfDate) / DAY_MS)
        val lang = locale.language.lowercase()

        return when {
            days <= -1L -> formatDayMonthYear(date, lang)
            days == 0L -> when (lang) {
                "fr" -> "Aujourd'hui"
                "en" -> "Today"
                else -> "اليوم"
            }
            days == 1L -> when (lang) {
                "fr" -> "Hier"
                "en" -> "Yesterday"
                else -> "أمس"
            }
            days == 2L -> when (lang) {
                "fr" -> "Il y a 2 jours"
                "en" -> "2 days ago"
                else -> "منذ يومين"
            }
            days < 7L -> when (lang) {
                "fr" -> "Il y a $days jours"
                "en" -> "$days days ago"
                else -> "منذ $days أيام"
            }
            days <= 30L -> {
                val weeks = days / 7
                when (lang) {
                    "fr" -> if (weeks == 1L) "Il y a 1 semaine" else "Il y a $weeks semaines"
                    "en" -> if (weeks == 1L) "1 week ago" else "$weeks weeks ago"
                    else -> if (weeks == 1L) "منذ أسبوع" else if (weeks == 2L) "منذ أسبوعين" else "منذ $weeks أسابيع"
                }
            }
            date.get(Calendar.YEAR) == today.get(Calendar.YEAR) -> formatDayMonth(date, lang)
            else -> formatDayMonthYear(date, lang)
        }
    }

    /** Formatted date with month and year based on active locale. */
    fun fullDate(raw: String, locale: Locale = Locale.getDefault()): String {
        val date = parse(raw) ?: return raw
        return formatDayMonthYear(date, locale.language.lowercase())
    }

    private fun floorDivDay(cal: Calendar): Long {
        val copy = cal.clone() as Calendar
        copy.set(Calendar.HOUR_OF_DAY, 0)
        copy.set(Calendar.MINUTE, 0)
        copy.set(Calendar.SECOND, 0)
        copy.set(Calendar.MILLISECOND, 0)
        return copy.timeInMillis
    }

    private const val DAY_MS = 86_400_000L

    private fun formatDayMonth(date: Calendar, lang: String): String {
        val monthIdx = date.get(Calendar.MONTH)
        val day = date.get(Calendar.DAY_OF_MONTH)
        return when (lang) {
            "fr" -> "$day ${FRENCH_MONTHS[monthIdx]}"
            "en" -> "${ENGLISH_MONTHS[monthIdx]} $day"
            else -> "$day ${ALGERIAN_MONTHS[monthIdx]}"
        }
    }

    private fun formatDayMonthYear(date: Calendar, lang: String): String {
        val monthIdx = date.get(Calendar.MONTH)
        val day = date.get(Calendar.DAY_OF_MONTH)
        val year = date.get(Calendar.YEAR)
        return when (lang) {
            "fr" -> "$day ${FRENCH_MONTHS[monthIdx]} $year"
            "en" -> "${ENGLISH_MONTHS[monthIdx]} $day, $year"
            else -> "$day ${ALGERIAN_MONTHS[monthIdx]} $year"
        }
    }
}
