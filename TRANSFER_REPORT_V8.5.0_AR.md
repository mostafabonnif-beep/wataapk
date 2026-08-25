# تقرير نقل مشروع Elwatania TV إلى wataapk

## معلومات النقل

تم نقل نسخة المشروع المرفقة `ElwataniaTV-v8.5.0-COMPLETE.zip` إلى المستودع:

`https://github.com/mostafabonnif-beep/wataapk`

النسخة المنقولة هي Android `v8.5.0` برقم بناء `36`، وتستخدم:

- Kotlin وJetpack Compose وMaterial 3.
- Firebase Auth وFirestore وApp Check وFCM وCrashlytics وAnalytics.
- Room للتخزين المحلي.
- Media3/ExoPlayer لبث HLS، مع دعم روابط YouTube.
- Hilt وMVVM وStateFlow.
- لوحة تحكم Web داخل مجلد `admin/`، وقواعد Firestore داخل `admin/firestore.rules`.

## ما تم نقله

تم نقل مصدر Android، ملفات Gradle، ViewModels المنفصلة، Repository، طبقة Firebase، الشاشات، موارد اللغتين، لوحة التحكم، API/Functions، قواعد Firestore، قواعد Storage، اختبارات القواعد، مستندات التشغيل، صفحة التحميل، وصفحة الخصوصية.

يحتوي المشروع على تدفق المزامنة للمجموعات التالية: `streams` و`config/app` و`config/breaking` و`epg` و`archive` و`websites` و`social` و`satellite_frequencies` و`notifications` و`ad_banners` و`config/popup` و`live/reactions` و`comments` و`feedback` و`devices` و`push_tokens` و`users/{uid}/preferences/app`.

## الأمان

لم يتم نقل `debug.keystore` أو أي keystore أو ملف service account أو `google-services.json` أو `.env` فعلي إلى المستودع العام. بقي فقط `.env.example` ووثائق الإعداد. هذا مقصود حتى لا يتم نشر اعتمادات أو مفاتيح تشغيلية داخل مستودع عام.

لإعادة بناء Android محلياً، يجب وضع ملف Firebase الخاص بالتطبيق في:

`app/google-services.json`

وهو ملف محلي لا ينبغي رفعه إلى مستودع عام إلا بعد اعتماد سياسة المؤسسة لذلك. يمكن مراجعة `FIREBASE_SETUP.md` و`FIREBASE_ACTION_GUIDE.md` لمعرفة الإعدادات المطلوبة.

للوحة التحكم، تُحقن إعدادات Firebase Web وقت بناء Hosting باستخدام المتغيرات المعرفة في سكربت `scripts/build-hosting.js`. لا ينبغي وضع service-account أو كلمة مرور داخل `admin/index.html`.

## التحقق

نجح في مجلد النقل تشغيل:

`compileDebugKotlin`

`testDebugUnitTest`

`lintDebug`

`assembleDebug`

تم تشغيل البناء باستخدام Android SDK محلي مؤقتاً عبر `local.properties` ثم حذف الملف قبل الدفع. لا يوجد `local.properties` في المستودع.

## ملاحظات Firebase

المشروع المنقول يحتوي على كود الربط وقواعد Firestore ولوحة التحكم، لكنه لا يستطيع افتراض أن مستودع GitHub الجديد يملك تلقائياً صلاحيات Firebase أو إعداد Hosting الخاص بالمشروع القديم. يجب ربط المشروع الجديد بمشروع Firebase المقصود عبر Firebase CLI أو إضافة الإعدادات العامة المناسبة وقت النشر، مع إبقاء الاعتمادات الخاصة خارج Git.

إذا كان المطلوب استمرار استخدام مشروع Firebase الحالي، يجب استخدام نفس `projectId` في إعدادات Hosting وملف `google-services.json` المحلي. وإذا كان المطلوب إنشاء مشروع Firebase جديد، يجب نشر القواعد والـindexes وإعادة تعيين Custom Claims للمسؤول قبل اختبار لوحة التحكم.

## حالة المستودع

المستودع الجديد كان فارغاً عند بدء النقل. بعد النقل أصبح يحتوي على نسخة المشروع الكاملة، مع حذف ملفات الاعتماد الحساسة. يجب إجراء `git status` قبل كل دفع، ومراجعة أي ملف جديد قبل النشر، خصوصاً ملفات `.env` وkeystore وservice account.

## الاختبار الميداني المطلوب

يلزم اختبار APK على هاتف Android حقيقي أو محاكي KVM، لأن خادم الفحص لا يوفر `/dev/kvm` بصورة تسمح بتشغيل Android كاملاً. ينبغي اختبار تسجيل الدخول المجهول، قراءة Firestore، تبديل القنوات، HLS وYouTube، EPG، Archive، RTL، الإشعارات، App Check، والعمل عند انقطاع الشبكة.

## نقطة البداية لمراجع خارجي

ابدأ من `README.md` و`BUILD.md`، ثم راجع `app/src/main/java/com/elwataniatv/app/data/remote/FirestoreContentSync.kt` لمعرفة مسارات Firestore، و`admin/index.html` لمعرفة واجهة الإدارة، و`admin/firestore.rules` لمعرفة الأدوار والصلاحيات.
