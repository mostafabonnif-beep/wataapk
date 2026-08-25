/**
 * Pure push-token helpers.
 *
 * These helpers only normalize, filter, classify, and partition values.
 * Firebase Admin, Expo, batching, and document writes stay inside the trigger implementation.
 */
const FCM_BATCH_LIMIT = 500;
const EXPO_TOKEN_PREFIX = "ExponentPushToken[";
const STALE_FCM_ERROR_CODES = new Set([
  "messaging/registration-token-not-registered",
  "messaging/invalid-registration-token",
]);

function normalizeCategories(categories) {
  return Array.isArray(categories)
    ? categories.map((category) => String(category).trim().toLowerCase()).filter(Boolean)
    : null;
}

/**
 * Filters explicit category subscriptions without breaking legacy token docs.
 * A missing/invalid/empty categories field means the record predates category
 * subscriptions, so it remains eligible for delivery until the app refreshes it.
 */
function filterPushTokensByCategory(records, category) {
  const target = String(category ?? "all").trim().toLowerCase() || "all";
  if (target === "all") return records;

  return records.filter((record) => {
    const categories = normalizeCategories(record?.categories);
    return categories === null || categories.length === 0 ||
      categories.includes("all") || categories.includes(target);
  });
}

function partitionPushTokens(records) {
  const fcmTokens = [];
  const expoTokens = [];
  const validTokens = [];

  for (const record of records) {
    const token = (record?.token ?? "").toString();
    if (!token) continue;

    const validToken = { token, docId: record.docId };
    const categories = normalizeCategories(record.categories);
    if (categories !== null) validToken.categories = categories;
    validTokens.push(validToken);
    if (token.startsWith(EXPO_TOKEN_PREFIX)) {
      expoTokens.push(validToken);
    } else {
      fcmTokens.push(validToken);
    }
  }

  return { fcmTokens, expoTokens, validTokens };
}

function chunkItems(items, size = FCM_BATCH_LIMIT) {
  if (!Number.isInteger(size) || size <= 0) {
    throw new RangeError("chunk size must be a positive integer");
  }

  const chunks = [];
  for (let i = 0; i < items.length; i += size) {
    chunks.push(items.slice(i, i + size));
  }
  return chunks;
}

function isStaleFcmResponse(response) {
  return Boolean(
    response &&
    !response.success &&
    STALE_FCM_ERROR_CODES.has(response.error?.code),
  );
}

function isStaleExpoTicket(ticket) {
  return ticket?.status !== "ok" && ticket?.details?.error === "DeviceNotRegistered";
}

module.exports = {
  EXPO_TOKEN_PREFIX,
  FCM_BATCH_LIMIT,
  STALE_FCM_ERROR_CODES,
  chunkItems,
  isStaleExpoTicket,
  isStaleFcmResponse,
  filterPushTokensByCategory,
  normalizeCategories,
  partitionPushTokens,
};
