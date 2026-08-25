#!/usr/bin/env node
/**
 * scripts/cleanup-test-data-only.js
 *
 * يحذف ONLY بيانات الاختبار التجريبية من Firestore:
 *   - برامج EPG التجريبية («oussama» و«200»)
 *   - الإشعارات التجريبية (test: true أو category: test)
 *   - تعطيل الخبر العاجل التجريبي (config/breaking)
 *
 * لا يزرع ولا يعدّل أي بيانات أخرى — تنظيف فقط.
 *
 * الاستعمال:
 *   1. ضع service-account.json في هذا المجلد (من Firebase Console →
 *      Project Settings → Service Accounts → Generate new private key)
 *   2. node scripts/cleanup-test-data-only.js
 */
const path = require("path");
const fs = require("fs");

async function main() {
  const keyPath = path.resolve(__dirname, "service-account.json");
  if (!fs.existsSync(keyPath)) {
    console.error("❌ service-account.json غير موجود في:", keyPath);
    console.error("   نزّله من Firebase Console → Project Settings → Service Accounts");
    process.exit(1);
  }

  let admin;
  try {
    admin = require("firebase-admin");
  } catch {
    console.error("❌ firebase-admin غير منصّب. شغّل: npm install firebase-admin");
    process.exit(1);
  }

  const { initializeApp, cert, getApps } = require("firebase-admin/app");
  const { getFirestore } = require("firebase-admin/firestore");

  const serviceAccount = require(keyPath);
  const app = getApps().length ? getApps()[0] : initializeApp({ credential: cert(serviceAccount) });
  const db = getFirestore(app);

  console.log("🧹 بدء تنظيف بيانات الاختبار...\n");

  // ─── 1. حذف برامج EPG التجريبية ─────────────────────────
  const epgSnap = await db.collection("epg").get();
  let epgDeleted = 0;
  for (const d of epgSnap.docs) {
    const title = String(d.data().title || "").trim();
    if (title === "200" || title.toLowerCase() === "oussama") {
      await d.ref.delete();
      epgDeleted++;
      console.log(`  🗑️ حُذف EPG تجريبي: «${title}» (${d.id})`);
    }
  }
  if (epgDeleted === 0) console.log("  ℹ️ لا توجد برامج EPG تجريبية (oussama/200)");

  // ─── 2. حذف الإشعارات التجريبية ──────────────────────────
  const notifSnap = await db.collection("notifications").get();
  let notifDeleted = 0;
  for (const d of notifSnap.docs) {
    const data = d.data() || {};
    const textFields = [data.title, data.body, data.message, data.author, data.createdBy]
      .map((v) => String(v || "").trim().toLowerCase())
      .filter(Boolean);
    const isTest = data.test === true || data.category === "test" ||
      textFields.includes("oussama") || textFields.includes("200");
    if (isTest) {
      await d.ref.delete();
      notifDeleted++;
      console.log(`  🗑️ حُذف إشعار تجريبي: ${d.id}`);
    }
  }
  if (notifDeleted === 0) console.log("  ℹ️ لا توجد إشعارات تجريبية");

  // ─── 3. تعطيل الخبر العاجل التجريبي ──────────────────────
  const brRef = db.doc("config/breaking");
  const brSnap = await brRef.get();
  if (brSnap.exists) {
    const text = String(brSnap.data().text || "").trim().toLowerCase();
    if (text === "oussama" || text === "200" || text === "") {
      await brRef.update({ enabled: false, text: "" });
      console.log("  🚫 عُطّل الخبر العاجل التجريبي (config/breaking)");
    } else {
      console.log("  ℹ️ الخبر العاجل الحالي ليس تجريبياً — لم يُمَس");
    }
  }

  console.log("\n✅ انتهى التنظيف بنجاح.");
}

main().catch((err) => {
  console.error("❌ خطأ:", err.message);
  process.exit(1);
});
