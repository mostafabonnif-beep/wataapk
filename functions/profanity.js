/**
 * functions/profanity.js
 * ─────────────────────────────────────────────────────────────────
 * Server-side profanity / spam filter for the Cloud Function in
 * `functions/index.js`. Ported from `lib/profanity.ts` (the client-
 * side check) so the same algorithm runs in both places. Pure JS,
 * zero dependencies.
 *
 * The client-side filter in `lib/profanity.ts` is still useful — it
 * blocks the obvious spam before it hits the network — but it is
 * trivially bypassable via DevTools. The Cloud Function is the
 * authoritative gate: the Firestore rules already validate the
 * payload shape, this function decides whether the content is
 * acceptable.
 *
 * Update rule: if you add a banned word here, add it to the client
 * list too so the user gets instant feedback rather than waiting for
 * a server-side flag.
 * ─────────────────────────────────────────────────────────────────
 */

const SEED_BANNED = [
  // English (kept short on purpose)
  "fuck", "shit", "bitch", "asshole", "bastard", "cunt",
  // French
  "merde", "putain", "salope", "connard", "connasse", "enculé", "nique",
  // Arabic — common slurs
  "كس", "قحب", "زنا", "عاهر", "شرموطة", "زب", "يلعن", "حمار",
];

const banned = new Set(SEED_BANNED.map(normalise));

/** Reasons a comment was rejected, used to give the user feedback. */
const REASONS = {
  PROFANE:        "profane",
  TOO_LONG:       "too_long",
  TOO_SHORT:      "too_short",
  SHOUTING:       "shouting",
  SPAM_REPEAT:    "spam_repeat",
  TOO_MANY_LINKS: "too_many_links",
};

/**
 * Normalise a word before adding it to the banned set.
 * Lowercase + strip Latin accents + collapse Arabic alef forms +
 * strip whitespace.
 */
function normalise(word) {
  return word
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[\u0622\u0623\u0625]/g, "\u0627")   // آ/أ/إ → ا
    .replace(/[\u064a]/g, "\u0649")                // ي → ى
    .replace(/\s+/g, "")
    .trim();
}

/**
 * Returns true if `text` contains a banned word. Cheap and
 * language-agnostic — same algorithm as `lib/profanity.ts`.
 */
function isProfane(text) {
  if (!text || typeof text !== "string") return false;
  const collapsed = text
    .replace(/\s+/g, " ")
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/(.)\1{2,}/g, "$1$1"); // collapse repeated chars

  const tokens = collapsed.split(/[^a-z\u0600-\u06FF]+/).filter(Boolean);
  for (const t of tokens) {
    if (banned.has(t)) return true;
    for (const w of banned) {
      if (w.length >= 4 && t.includes(w)) return true;
    }
  }
  return false;
}

/**
 * Run all moderation checks. Returns { ok: true } if acceptable,
 * otherwise { ok: false, reason: one of REASONS }.
 */
function moderate(text, opts = {}) {
  const { maxLength = 500, minLength = 2 } = opts;
  const trimmed = (text ?? "").trim();

  if (trimmed.length < minLength) return { ok: false, reason: REASONS.TOO_SHORT };
  if (trimmed.length > maxLength) return { ok: false, reason: REASONS.TOO_LONG };

  const links = trimmed.match(/https?:\/\//gi);
  if (links && links.length >= 3) return { ok: false, reason: REASONS.TOO_MANY_LINKS };

  const letters = trimmed.replace(/[^A-Za-z\u0600-\u06FF]/g, "");
  if (letters.length > 20) {
    const upper = letters.replace(/[^A-Z]/g, "").length;
    if (upper / letters.length > 0.7) return { ok: false, reason: REASONS.SHOUTING };
  }

  if (/(.)\1{7,}/.test(trimmed)) return { ok: false, reason: REASONS.SPAM_REPEAT };

  if (isProfane(trimmed)) return { ok: false, reason: REASONS.PROFANE };

  return { ok: true };
}

module.exports = { moderate, isProfane, REASONS };

// PR #21 — this commit forces CI re-run after PR #22 merged the type fixes
// to main. Cloud Function behavior unchanged.
