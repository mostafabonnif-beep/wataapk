const test = require("node:test");
const assert = require("node:assert/strict");

const { moderate, isProfane, REASONS } = require("../profanity");

test("isProfane detects banned words and ignores normal text", () => {
  assert.equal(isProfane("This is a fuck sentence"), true);
  assert.equal(isProfane("هذا تعليق مفيد عن البرنامج"), false);
  assert.equal(isProfane(""), false);
  assert.equal(isProfane(null), false);
});

test("moderate accepts a normal comment", () => {
  assert.deepEqual(moderate("شكراً على هذا البرنامج"), { ok: true });
});

test("moderate reports the main rejection reasons", () => {
  assert.deepEqual(moderate(""), { ok: false, reason: REASONS.TOO_SHORT });
  assert.deepEqual(moderate("fuck"), { ok: false, reason: REASONS.PROFANE });
  assert.deepEqual(moderate("https://a.test https://b.test https://c.test"), {
    ok: false,
    reason: REASONS.TOO_MANY_LINKS,
  });
  assert.deepEqual(moderate("A".repeat(21)), {
    ok: false,
    reason: REASONS.SHOUTING,
  });
  assert.deepEqual(moderate("x".repeat(8)), {
    ok: false,
    reason: REASONS.SPAM_REPEAT,
  });
  assert.deepEqual(moderate("a".repeat(501)), {
    ok: false,
    reason: REASONS.TOO_LONG,
  });
});
