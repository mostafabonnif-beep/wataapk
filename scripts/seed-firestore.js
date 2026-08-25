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
 * - websites ← المواقع
 * - social ← التواصل الاجتماعي
 * - satellite_frequencies ← ترددات الأقمار الصناعية
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
  const DEFAULT_OFFICIAL_STREAM_URL = "https://livesstream.work.gd:5443/WebRTCApp/streams/stream.m3u8";
  const officialStreamUrl = String(process.env.OFFICIAL_STREAM_URL || DEFAULT_OFFICIAL_STREAM_URL).trim();
  if (officialStreamUrl && !/^https?:\/\/[^\s]+\.m3u8(?:[?#].*)?$/i.test(officialStreamUrl)) {
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
    latestVersion:        String(process.env.LATEST_VERSION || "8.1.1"),
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

  // ─── 4. websites — المواقع الإخبارية ─────────────────────
  const existingWebsites = await db.collection("websites").get();
  if (existingWebsites.empty) {
    const websites = [
      {
        name: "الوطنية TV",
        url: "https://www.youtube.com/@ElwataniaTV",
        description: "القناة الوطنية الجزائرية",
        emoji: "📺",
        color: "#0a7ea4",
        order: 0,
        isActive: true,
      },
      {
        name: "الجزائر الجديدة",
        url: "https://www.eldjazaireldjadida.dz/",
        description: "موقع إخباري جزائري شامل",
        emoji: "📰",
        color: "#16a34a",
        order: 1,
        isActive: true,
      },
      {
        name: "الحدث",
        url: "https://elhadath-dz.com/",
        description: "أخبار وتحليلات سياسية",
        emoji: "⚡",
        color: "#dc2626",
        order: 2,
        isActive: true,
      },
    ];
    for (const w of websites) {
      await db.collection("websites").add(w);
    }
    console.log(`✅ websites — ${websites.length} موقع إخباري`);
  } else {
    console.log(`⏭️ websites — موجودة قبل (${existingWebsites.size})`);
  }

  // ─── 5. social — منصات التواصل الاجتماعي ─────────────────
  const existingSocial = await db.collection("social").get();
  if (existingSocial.empty) {
    const socials = [
      {
        platform: "Facebook",
        name: "الوطنية TV",
        url: "https://www.facebook.com/elwataniatvweb",
        description: "تابع آخر الأخبار والبرامج",
        emoji: "📺",
        color: "#1877F2",
        order: 0,
        isActive: true,
      },
      {
        platform: "Facebook",
        name: "الوطنية سبورت",
        url: "https://www.facebook.com/ElwataniaSport",
        description: "أخبار الرياضة الجزائرية",
        emoji: "⚽",
        color: "#1877F2",
        order: 1,
        isActive: true,
      },
      {
        platform: "Facebook",
        name: "الجزائر الجديدة",
        url: "https://www.facebook.com/Eldjazair.Eldjadida.News",
        description: "آخر الأخبار والتحليلات",
        emoji: "📰",
        color: "#1877F2",
        order: 2,
        isActive: true,
      },
    ];
    for (const s of socials) {
      await db.collection("social").add(s);
    }
    console.log(`✅ social — ${socials.length} منصة تواصل`);
  } else {
    console.log(`⏭️ social — موجودة قبل (${existingSocial.size})`);
  }

  // ─── 6. epg — جدول برامج اليوم ───────────────────────────
  const existingEpg = await db.collection("epg").get();
  if (existingEpg.empty) {
    const schedule = [
      { time: "06:00", title: "نشرة أخبار الصباح" },
      { time: "09:00", title: "برنامج صباح الخير" },
      { time: "12:00", title: "نشرة أخبار الظهيرة" },
      { time: "15:00", title: "برنامج رياضي" },
      { time: "18:00", title: "برنامج ثقافي" },
      { time: "20:00", title: "نشرة أخبار المساء" },
      { time: "22:00", title: "برنامج منوعات" },
    ];
    await db.collection("epg").add({
      title: "جدول اليوم",
      startTime: "06:00",
      category: "برامج يومية",
      duration: "يوم كامل",
      description: "البرنامج اليومي لقناة الوطنية TV",
      day: new Date().toLocaleDateString("ar-DZ", { weekday: "long" }),
      programs: schedule,
    });
    console.log(`✅ epg — جدول اليوم (${schedule.length} برنامج)`);
  } else {
    console.log(`⏭️ epg — موجودة قبل (${existingEpg.size})`);
  }

  // ─── 7. ad_banners — رعاة البث ───────────────────────────
  const existingAds = await db.collection("ad_banners").get();
  if (existingAds.empty) {
    const banners = [
      { title: "تابعوا البث المباشر HD لقناة الوطنية TV", imageUrl: "", targetUrl: "https://elwataniatv.dz", order: 1, isEnabled: true },
      { title: "برنامج حوار الساعة — كل أسبوع", imageUrl: "", targetUrl: "https://www.youtube.com/@ElwataniaTV", order: 2, isEnabled: true },
    ];
    for (const b of banners) {
      await db.collection("ad_banners").add(b);
    }
    console.log(`✅ ad_banners — ${banners.length} بنر رعاية`);
  } else {
    console.log(`⏭️ ad_banners — موجودة قبل (${existingAds.size})`);
  }

  // ─── 8. satellite_frequencies — ترددات الأقمار الصناعية ────
  const satelliteFrequencies = [
    {
      id: "sat_nilesat",
      satelliteName: "نايل سات",
      orbitalPosition: "Nilesat 7.0°W",
      frequencyMhz: 10922,
      polarization: "V (عمودي)",
      symbolRate: 27500,
      fec: "7/8",
      notes: "التردد الرسمي المعلن للقناة",
      isActive: true,
      order: 1,
    },
    {
      id: "sat_badr",
      satelliteName: "عرب سات / بدر",
      orbitalPosition: "Badr 26.0°E",
      frequencyMhz: 12303,
      polarization: "H (أفقي)",
      symbolRate: 27500,
      fec: "5/6",
      notes: "التردد الرسمي المعلن للقناة",
      isActive: true,
      order: 2,
    },
  ];
  for (const frequency of satelliteFrequencies) {
    const { id, ...data } = frequency;
    await db.collection("satellite_frequencies").doc(id).set(data, { merge: true });
  }
  console.log(`✅ satellite_frequencies — ${satelliteFrequencies.length} ترددات قابلة للتحكم من اللوحة`);

  // ─── 9. archive — أرشيف الفيديوهات ───────────────────────
  const existingArchive = await db.collection("archive").get();
  if (existingArchive.empty) {
    const archives = [
      {
        title: "نشرة الأخبار الرئيسية",
        description: "نشرة الأخبار الرئيسية — متابعة كاملة لأهم الأحداث الوطنية والدولية",
        category: "أخبار",
        youtubeUrl: "https://www.youtube.com/@ElwataniaTV",
        date: new Date().toISOString().split("T")[0],
        duration: "30 دقيقة",
      },
      {
        title: "ملعب الجزائر",
        description: "أبرز الأحداث الرياضية والتحليلات الفنية للمباريات",
        category: "رياضة",
        youtubeUrl: "https://www.youtube.com/@ElwataniaSport",
        date: new Date().toISOString().split("T")[0],
        duration: "45 دقيقة",
      },
      {
        title: "الجزائر الجديدة",
        description: "برنامج يستعرض مسيرة التطوير والإنجازات الوطنية",
        category: "وثائقي",
        youtubeUrl: "https://www.youtube.com/@eldjazaireldjadida",
        date: new Date().toISOString().split("T")[0],
        duration: "60 دقيقة",
      },
    ];
    for (const a of archives) {
      await db.collection("archive").add(a);
    }
    console.log(`✅ archive — ${archives.length} فيديو في الأرشيف`);
  } else {
    console.log(`⏭️ archive — موجودة قبل (${existingArchive.size})`);
  }

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
