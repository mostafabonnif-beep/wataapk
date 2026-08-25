#!/usr/bin/env node
/* eslint-env node */
/**
 * scripts/seed-firestore.js
 *
 * يزرع (seeds) البيانات الأولية في Firestore:
 * - config/app ← إعدادات التطبيق
 * - config/breaking ← الخبر العاجل
 * - streams ← قنوات البث
 * - archive ← فيديوهات الأرشيف
 * - streams/live_main ← رابط بث رسمي يمرر عبر OFFICIAL_STREAM_URL
 *
 * لا يزرع السكريبت أي برامج أو مواقع أو إعلانات أو ترددات افتراضية؛
 * هذه البيانات تحريرية ويجب أن تأتي من لوحة التحكم بعد مراجعتها.
 *
 * استعمال:
 *   1. npm install firebase-admin
 *   2. نزّل service-account.json من Firebase Console →
 *      Project Settings → Service Accounts → "Generate new private key"
 *      → احفظه كـ scripts/service-account.json (في .gitignore)
 *   3. node scripts/seed-firestore.js [--cleanup-test-data]
 *      أضف --cleanup-test-data فقط إذا أردت حذف بيانات الاختبار القديمة.
 *
 * السكريبت آمن (idempotent) — تشغيله مرتين ما يسببش مشاكل.
 */

const path = require("path");
const fs = require("fs");

async function main() {
  const cleanupTestData = process.argv.includes("--cleanup-test-data");
  const keyPath = path.resolve(__dirname, "service-account.json");
  if (!fs.existsSync(keyPath)) {
    console.error(`❌ service-account.json موش موجود في: ${keyPath}`);
    console.error("   نزّله من Firebase Console → Project Settings → Service Accounts");
    return process.exit(1);
  }

  let admin;
  try {
    admin = require("firebase-admin");
  } catch {
    console.error("❌ firebase-admin منصبش. شغّل:");
    console.error("   npm install firebase-admin");
    return process.exit(1);
  }

  const { initializeApp, cert, getApps } = require("firebase-admin/app");
  const { getFirestore } = require("firebase-admin/firestore");

  const serviceAccount = require(keyPath);
  const app = getApps().length
    ? getApps()[0]
    : initializeApp({ credential: cert(serviceAccount) });
  const db = getFirestore(app);

  console.log("🚀 بدء زرع البيانات الأولية في Firestore...\n");

  // ─── 1. config/app — إعدادات التطبيق ─────────────────────
  const DEFAULT_PRIVACY_URL = "https://elwataniatvapp.web.app/privacy.html";
  const officialStreamUrl = String(process.env.OFFICIAL_STREAM_URL || "").trim();
  if (!officialStreamUrl) {
    console.error("OFFICIAL_STREAM_URL is required; no fallback stream is created.");
    return process.exit(1);
  }
  if (!/^https?:\/\/[^\s]+\.m3u8(?:[?#].*)?$/i.test(officialStreamUrl)) {
    console.error("OFFICIAL_STREAM_URL must be a valid http(s) HLS .m3u8 URL.");
    return process.exit(1);
  }

  const appRef = db.collection("config").doc("app");
  const existingAppSnap = await appRef.get();
  const existingAppData = existingAppSnap.exists ? (existingAppSnap.data() || {}) : {};
  const hasExistingPrivacyUrl = Object.prototype.hasOwnProperty.call(existingAppData, "privacyUrl");

  const appConfig = {
    appName:              "الوطنية TV",
    appSlogan:            "نبض الجزائر في قلبك",
    logoUrl:              "",
    onboardingBannerUrl:  "",
    primaryColor:         "#1E88E5",
    secondaryColor:       "#16a34a",
    accentColor:          "",
    contactEmail:         "contact@elwataniatv.dz",
    officialWebsite:      "https://elwataniatv.dz",
    appStoreUrl:          "",
    iosStoreUrl:          "",
    facebookUrl:          "https://www.facebook.com/elwataniatvweb",
    youtubeUrl:           "https://www.youtube.com/@ElwataniaTV",
    telegramUrl:          "",
    tiktokUrl:            "",
    instagramUrl:         "",
    twitterUrl:           "",
    whatsappUrl:          "",
    // Never invent a production stream URL. Provide OFFICIAL_STREAM_URL explicitly.
    defaultStreamUrl:     officialStreamUrl,
    maintenanceMode:      false,
    maintenanceMessage:   "",
    minVersion:           String(process.env.MIN_VERSION || "8.0.0"),
    latestVersion:        String(process.env.LATEST_VERSION || "8.5.0"),
    updateUrl:            "",
    updateMessage:        "",
    enableOnboarding:     true,
    enableEpg:            true,
    showArchivePreview:   true,
    showPromotionalBanners: true,
    enableArchive:        true,
    enableSocial:         true,
    enableWebsites:       true,
    enableComments:       true,
    enablePush:           true,
    enableDarkMode:      true,
    enableDynamicColor:  false,
  };
  if (!hasExistingPrivacyUrl) appConfig.privacyUrl = DEFAULT_PRIVACY_URL;
  await appRef.set(appConfig, { merge: true });
  console.log(`✅ config/app — إعدادات التطبيق${hasExistingPrivacyUrl ? " (privacyUrl الموجود محفوظ)" : " (رابط الخصوصية الرسمي الافتراضي)"}`);

  // ─── 2. config/breaking — الخبر العاجل ───────────────────
  await db.collection("config").doc("breaking").set(
    { enabled: false, text: "" },
    { merge: true },
  );
  console.log("✅ config/breaking — الخبر العاجل (معطل)");

  // ─── 2bis. تنظيف بيانات الاختبار القديمة (اختياري) ────────
  // لا تحذف أي بيانات اختبار إلا عند تمرير --cleanup-test-data.
  if (cleanupTestData) {
    const epgSnap = await db.collection("epg").get();
    for (const d of epgSnap.docs) {
      const title = String(d.data().title || "").trim();
      if (title === "200" || title.toLowerCase() === "oussama") {
        await d.ref.delete();
        console.log(`🧹 حُذف EPG تجريبي: «${title}»`);
      }
    }

    const notificationsSnap = await db.collection("notifications").get();
    for (const d of notificationsSnap.docs) {
      const data = d.data() || {};
      const textFields = [data.title, data.body, data.message, data.author, data.createdBy]
        .map((value) => String(value || "").trim().toLowerCase())
        .filter(Boolean);
      const isTestNotification = data.test === true || data.category === "test" ||
        textFields.includes("oussama") || textFields.includes("200");
      if (isTestNotification) {
        await d.ref.delete();
        console.log(`🧹 حُذف إشعار تجريبي: ${d.id}`);
      }
    }

    const brSnap = await db.doc("config/breaking").get();
    const breakingText = String(brSnap.exists ? brSnap.data().text || "" : "").trim().toLowerCase();
    if (brSnap.exists && (breakingText === "oussama" || breakingText === "200")) {
      await db.doc("config/breaking").set({ enabled: false, text: "" }, { merge: true });
      console.log("🧹 تصفير الخبر العاجل التجريبي");
    }
  }

  // ─── 3. streams — قنوات البث المباشر ─────────────────────
  const existingStreams = await db.collection("streams").get();
  const liveMainRef = db.collection("streams").doc("live_main");
  await liveMainRef.set({
    title: "الوطنية TV — البث المباشر",
    url: officialStreamUrl,
    type: "m3u8",
    order: 0,
    isActive: true,
  }, { merge: true });
  console.log(`✅ streams/live_main — تم تثبيت رابط البث الأساسي الرسمي${existingStreams.empty ? "" : " (تحديث الوثيقة الموجودة)"}`);

  // ─── 4. المحتوى التحريري ─────────────────────────────────
  // لا ننشئ سجلات مصطنعة. يضيف المسؤول البرامج والمواقع والإعلانات
  // والترددات من لوحة التحكم بعد مراجعة الروابط والمعلومات الرسمية.
  console.log("⏭️ المحتوى التحريري — لم تُنشأ بيانات افتراضية؛ المصدر هو Firestore/لوحة التحكم.");

  console.log(
    cleanupTestData
      ? "\n🎉 تم زرع البيانات الأولية وتنظيف بيانات الاختبار بنجاح!"
      : "\n🎉 تم زرع البيانات الأولية بنجاح!",
  );
  console.log("   🔗 روح للوحة التحكم وسجل دخولك باش تشوف البيانات.");
  console.log("   📱 التطبيق يقرا البيانات مباشرة من Firestore.");
}

main().catch((err) => {
  console.error("\n❌ فشل:", err.message ?? err);
  process.exit(1);
});
