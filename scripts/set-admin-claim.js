#!/usr/bin/env node
/* eslint-env node */
/**
 * scripts/set-admin-claim.js
 *
 * Sets the `admin: true` custom claim on a Firebase Auth user so that
 * the Firestore rules (admin/firestore.rules) recognise them as admin.
 *
 * Usage:
 *   1. Install: npm install --no-save firebase-admin
 *   2. Get a service account key:
 *      Firebase Console → Project Settings → Service Accounts →
 *      "Generate new private key" → save as scripts/service-account.json
 *      (DO NOT commit this file — it's in .gitignore)
 *   3. Run:
  *   node scripts/set-admin-claim.js <user-email-or-uid> [admin|editor|moderator]
 *
 * Example:
 *      node scripts/set-admin-claim.js admin@elwataniatv.com admin
 *      node scripts/set-admin-claim.js editor@elwataniatv.com editor
 *
 * The script is idempotent — running it twice is safe.
 */

const path = require("path");
const fs = require("fs");

async function main() {
  const target = process.argv[2];
  const role = process.argv[3] || "admin";
  const allowedRoles = new Set(["admin", "editor", "moderator"]);
  if (!target || !allowedRoles.has(role)) {
    console.error("Usage: node scripts/set-admin-claim.js <email-or-uid> [admin|editor|moderator]");
    console.error("Example: node scripts/set-admin-claim.js admin@elwataniatv.com admin");
    console.error("Allowed roles: admin, editor, moderator");
    process.exit(1);
  }

  const keyPath = path.resolve(__dirname, "service-account.json");
  if (!fs.existsSync(keyPath)) {
    console.error(`Service account key not found at: ${keyPath}`);
    console.error("Generate one in Firebase Console → Project Settings → Service Accounts.");
    process.exit(1);
  }

  // Lazy-load firebase-admin so the script doesn't crash on import
  // when the package isn't installed.
  let admin;
  try {
    admin = require("firebase-admin");
  } catch {
    console.error("firebase-admin is not installed. Run:");
    console.error("  npm install --no-save firebase-admin");
    process.exit(1);
  }

  const serviceAccount = require(keyPath);
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
  });

  const auth = admin.auth();

  // Resolve email to UID if needed
  let uid = target;
  if (target.includes("@")) {
    console.log(`Looking up user by email: ${target}`);
    const userRecord = await auth.getUserByEmail(target);
    uid = userRecord.uid;
    console.log(`Resolved to UID: ${uid}`);
  }

  const currentUser = await auth.getUser(uid);
  const currentClaims = currentUser.customClaims || {};

  console.log(`Setting ${role} claim on user: ${uid}`);
  await auth.setCustomUserClaims(uid, {
    ...currentClaims,
    admin: role === "admin",
    role,
  });

  // Verify
  const updated = await auth.getUser(uid);
  console.log("\n✓ Done.");
  console.log(`  email:  ${updated.email ?? "(no email)"}`);
  console.log(`  uid:    ${updated.uid}`);
  console.log(`  claims: ${JSON.stringify(updated.customClaims)}`);
  console.log("\nThe user must sign out and back in (or refresh their ID token)");
  console.log("before the Firestore rules will recognise the new role.");

  process.exit(0);
}

main().catch((err) => {
  console.error("Failed:", err.message ?? err);
  process.exit(1);
});
