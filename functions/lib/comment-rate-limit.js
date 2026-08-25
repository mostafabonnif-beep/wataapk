/**
 * functions/lib/comment-rate-limit.js
 * ─────────────────────────────────────────────────────────────────
 * Pure rate-limit constants and decision helpers for the
 * `moderateComment` Cloud Function. Kept dependency-free so it can
 * be unit-tested without the Firestore emulator.
 *
 * Policy: an authenticated userUid may post at most COMMENT_RATE_LIMIT comments per
 * program within a rolling COMMENT_RATE_WINDOW_MS window. The window
 * is measured against the comment's `createdAt` timestamp (enforced
 * by Firestore rules as a timestamp on create). This is a
 * best-effort server-side gate — the Firestore composite index
  * `comments(userUid ASC, createdAt DESC)` must be deployed for
 the
 * trigger's query to run (see firestore.indexes.json).
 * ─────────────────────────────────────────────────────────────────
 */

/** Rolling window length for the per-user comment rate limit (60 s). */
const COMMENT_RATE_WINDOW_MS = 60 * 1000;

/** Maximum comments an authenticated user may post per program inside the window. */
const COMMENT_RATE_LIMIT = 5;

/** `moderation.reason` written when a comment trips the rate limit. */
const RATE_LIMIT_REASON = "rate_limit";

/**
 * Epoch-ms cutoff for "recent" comments. `nowMs` is injectable for tests.
 */
function cutoffTimestampMs(nowMs = Date.now()) {
  return nowMs - COMMENT_RATE_WINDOW_MS;
}

/**
 * True when the number of comments already posted inside the window
 * exceeds the allowed limit. The caller passes a count that includes
 * the triggering comment, so the limit itself is still allowed and
 * only the (limit + 1)-th comment is flagged.
 */
function isRateLimited(recentCount) {
  return recentCount > COMMENT_RATE_LIMIT;
}

module.exports = {
  COMMENT_RATE_WINDOW_MS,
  COMMENT_RATE_LIMIT,
  RATE_LIMIT_REASON,
  cutoffTimestampMs,
  isRateLimited,
};
