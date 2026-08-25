const test = require("node:test");
const assert = require("node:assert/strict");

const {
  FCM_BATCH_LIMIT,
  chunkItems,
  isStaleExpoTicket,
  isStaleFcmResponse,
  filterPushTokensByCategory,
  normalizeCategories,
  partitionPushTokens,
} = require("../lib/push-helpers");

test("push token partition preserves document IDs and ignores empty tokens", () => {
  const result = partitionPushTokens([
    { docId: "fcm-doc", token: "fcm-token" },
    { docId: "expo-doc", token: "ExponentPushToken[abc]" },
    { docId: "empty-doc", token: "" },
    { docId: "null-doc", token: null },
  ]);

  assert.deepEqual(result.fcmTokens, [{ docId: "fcm-doc", token: "fcm-token" }]);
  assert.deepEqual(result.expoTokens, [{ docId: "expo-doc", token: "ExponentPushToken[abc]" }]);
  assert.deepEqual(result.validTokens, [
    { docId: "fcm-doc", token: "fcm-token" },
    { docId: "expo-doc", token: "ExponentPushToken[abc]" },
  ]);
});

test("push category filtering targets explicit subscriptions and keeps legacy records safe", () => {
  const records = [
    { docId: "all", token: "token-all", categories: ["all"] },
    { docId: "sports", token: "token-sports", categories: ["sports"] },
    { docId: "news", token: "token-news", categories: ["news"] },
    { docId: "legacy", token: "token-legacy" },
    { docId: "empty", token: "token-empty", categories: [] },
  ];

  assert.deepEqual(normalizeCategories([" Sports ", "NEWS"]), ["sports", "news"]);
  assert.deepEqual(
    filterPushTokensByCategory(records, "sports").map((record) => record.docId),
    ["all", "sports", "legacy", "empty"],
  );
  assert.deepEqual(
    filterPushTokensByCategory(records, "all").map((record) => record.docId),
    records.map((record) => record.docId),
  );
});

test("FCM chunks never exceed the 500-token multicast limit", () => {
  const chunks = chunkItems(Array.from({ length: FCM_BATCH_LIMIT + 1 }, (_, i) => i));
  assert.deepEqual(chunks.map((chunk) => chunk.length), [500, 1]);
  assert.equal(Math.max(...chunks.map((chunk) => chunk.length)), FCM_BATCH_LIMIT);
});

test("stale FCM responses and Expo tickets are classified explicitly", () => {
  assert.equal(isStaleFcmResponse({
    success: false,
    error: { code: "messaging/registration-token-not-registered" },
  }), true);
  assert.equal(isStaleFcmResponse({
    success: false,
    error: { code: "messaging/invalid-registration-token" },
  }), true);
  assert.equal(isStaleFcmResponse({
    success: false,
    error: { code: "messaging/server-unavailable" },
  }), false);
  assert.equal(isStaleExpoTicket({ status: "error", details: { error: "DeviceNotRegistered" } }), true);
  assert.equal(isStaleExpoTicket({ status: "error", details: { error: "MessageTooBig" } }), false);
  assert.equal(isStaleExpoTicket({ status: "ok" }), false);
});
