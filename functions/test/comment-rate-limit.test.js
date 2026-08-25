const test   = require("node:test");
const assert = require("node:assert/strict");

const {
  COMMENT_RATE_WINDOW_MS,
  COMMENT_RATE_LIMIT,
  RATE_LIMIT_REASON,
  cutoffTimestampMs,
  isRateLimited,
} = require("../lib/comment-rate-limit");

test("rate-limit constants are sane", () => {
  assert.equal(COMMENT_RATE_WINDOW_MS, 60_000);
  assert.equal(COMMENT_RATE_LIMIT, 5);
  assert.equal(RATE_LIMIT_REASON, "rate_limit");
});

test("cutoffTimestampMs subtracts the window from now", () => {
  const now = 1_000_000;
  assert.equal(cutoffTimestampMs(now), now - COMMENT_RATE_WINDOW_MS);
});

test("isRateLimited allows up to the limit and flags the next comment", () => {
  assert.equal(isRateLimited(0), false);
  assert.equal(isRateLimited(COMMENT_RATE_LIMIT), false);
  assert.equal(isRateLimited(COMMENT_RATE_LIMIT + 1), true);
  assert.equal(isRateLimited(100), true);
});
