# تسليم Elwatania TV v8.5.0

**الإصدار:** `8.5.0` — `versionCode 36`

**الحزمة:** `com.elwataniatv.app`

**المستودع:** `https://github.com/mostafabonnif-beep/wataapk`

## ما تم تنفيذه

تمت إزالة رابط البث غير الموثق من fallback المحلي ومن الوثائق ولوحة التحكم. أصبح التطبيق لا ينشئ قناة أو رابط بث تلقائياً عند غياب Firestore؛ المصدر الرسمي يجب أن يحدده مسؤول القناة داخل Firestore أو لوحة التحكم.

تم تعديل `seed-firestore.js` حتى لا ينشئ برامج أو مواقع أو إعلانات أو ترددات تحريرية مصطنعة. هذه البيانات أصبحت مسؤولية لوحة التحكم بعد المراجعة. كما تم توسيع `ContentSanitizer` لحجب القيم التجريبية المعروفة والعلامات العامة مثل demo وplaceholder وtest والعناوين العربية التجريبية.

تم التحقق من مسار Firebase في Android: ملف `google-services.json` المحلي يطابق المشروع `elwataniatvapp` والحزمة `com.elwataniatv.app`، وتظهر موارد `google_app_id` وFCM داخل مخرجات release. لم يُضمّن ملف الإعداد في Git.

## التحقق من الجودة

| الفحص | النتيجة |
|---|---|
| `compileDebugKotlin` | ناجح |
| `testDebugUnitTest` | ناجح |
| `lintDebug` | ناجح |
| `assembleDebug` مع Firebase | ناجح |
| `assembleRelease` مع Firebase وkeystore محلي | ناجح |
| `bundleRelease` مع Firebase وkeystore محلي | ناجح |
| APK Signature Scheme v2 | متحقق |
| AAB JAR signature | متحقق |
| GitHub Actions للنسخة السابقة | ناجح بالكامل |

## الملفات الناتجة

| الملف | الاستخدام | SHA-256 |
|---|---|---|
| `ElwataniaTV-v8.5.0-FIREBASE-release-signed.apk` | تثبيت مباشر على هاتف Android | `ff5ce4b49042ce7933570e2ca0887475cb4c4f4ee1b28a77f414b1b7e189a5ba` |
| `ElwataniaTV-v8.5.0-FIREBASE-release-signed.aab` | حزمة Google Play | `2797f583bc2ef00b108042012ad8f16c4f0b6e9d974fccce77e52206bc020dc9` |

حجم APK الموقّع يقارب `9 MB`، وحجم AAB يقارب `15 MB`.

## ملاحظة مهمة حول Firestore

لم يتم حذف أي مستند مباشر من قاعدة Firestore تلقائياً، لأن الحذف عملية لا رجعة فيها وتتطلب تأكيد صاحب المشروع. يوجد سكريبت محمي باسم `scripts/cleanup-test-data-only.js` يستهدف السجلات التجريبية المحددة فقط، ولا يعمل إلا بعد وضع حساب خدمة محلي وتعيين `CONFIRM_TEST_DATA_CLEANUP=YES`.

## إجراءات الاختبار التالية

ثبّت APK على هاتف Android حقيقي، سجّل اتصالاً بالإنترنت، وتحقق من ظهور القنوات والبرامج التي نشرها المسؤول في Firestore. إذا كانت collections التحريرية فارغة، فهذا مقصود بعد إزالة البيانات المصطنعة؛ أضف المحتوى الرسمي من لوحة التحكم بدلاً من استعمال بيانات تجريبية.

لا يزال تفعيل GitHub Pages يحتاج صلاحية مالك المستودع من `Settings → Pages → Source: GitHub Actions`. كما يجب عدم نشر أو مشاركة أي تقرير يحتوي كلمات مرور أو مفاتيح توقيع.
