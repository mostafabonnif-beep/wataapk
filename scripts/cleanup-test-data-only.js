#!/usr/bin/env node
/*
 * Remove only the known test records listed in ADMIN_HANDOFF_REPORT_AR.md.
 * This script is intentionally opt-in: it refuses to run unless
 * CONFIRM_TEST_DATA_CLEANUP=YES is provided.
 *
 * Prerequisite:
 *   npm install --prefix functions
 *   CONFIRM_TEST_DATA_CLEANUP=YES node scripts/cleanup-test-data-only.js
 *
 * Credentials are read from GOOGLE_APPLICATION_CREDENTIALS, or from the
 * ignored scripts/service-account.json path. Never commit either file.
 */
const fs = require("fs");
const path = require("path");

const rootDir = path.resolve(__dirname, "..");
const serviceAccountPath = process.env.GOOGLE_APPLICATION_CREDENTIALS
  || path.join(__dirname, "service-account.json");

if (process.env.CONFIRM_TEST_DATA_CLEANUP !== "YES") {
  console.error("Refusing to modify Firestore.");
  console.error("Set CONFIRM_TEST_DATA_CLEANUP=YES after reviewing the exact document IDs.");
  process.exit(1);
}

if (!fs.existsSync(serviceAccountPath)) {
  console.error(`Service account file not found: ${serviceAccountPath}`);
  console.error("Set GOOGLE_APPLICATION_CREDENTIALS to a protected Firebase service-account JSON file.");
  process.exit(1);
}

let admin;
try {
  admin = require("firebase-admin");
} catch (firstError) {
  try {
    admin = require(path.join(rootDir, "functions", "node_modules", "firebase-admin"));
  } catch (secondError) {
    console.error("firebase-admin is not installed. Run: npm install --prefix functions");
    process.exit(1);
  }
}

const serviceAccount = JSON.parse(fs.readFileSync(serviceAccountPath, "utf8"));
const app = admin.apps.length
  ? admin.app()
  : admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
const db = admin.firestore(app);

const epgTestDocuments = [
  "epg/epg_7",
  "epg/wm6BWBmK1KxZjW38DOOL",
];
const notificationTestDocument = "notifications/RdQSsaqf8jNPEtOKKUny";
const breakingDocument = "config/breaking";

async function deleteIfPresent(documentPath) {
  const reference = db.doc(documentPath);
  const snapshot = await reference.get();
  if (!snapshot.exists) {
    console.log(`SKIP missing: ${documentPath}`);
    return;
  }
  await reference.delete();
  console.log(`DELETED: ${documentPath}`);
}

async function disableBreakingNews() {
  const reference = db.doc(breakingDocument);
  const snapshot = await reference.get();
  if (!snapshot.exists) {
    console.log(`SKIP missing: ${breakingDocument}`);
    return;
  }
  await reference.set({ enabled: false, text: "" }, { merge: true });
  console.log(`DISABLED and cleared text: ${breakingDocument}`);
}

async function main() {
  for (const documentPath of epgTestDocuments) await deleteIfPresent(documentPath);
  await disableBreakingNews();
  await deleteIfPresent(notificationTestDocument);
  console.log("Test-data cleanup completed. No other documents were targeted.");
}

main()
  .catch((error) => {
    console.error("Cleanup failed:", error.message);
    process.exitCode = 1;
  })
  .finally(async () => {
    if (admin.apps.length) await app.delete();
  });
