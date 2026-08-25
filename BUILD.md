# دليل بناء تطبيق الوطنية TV

هذا المشروع هو تطبيق Android أصلي مكتوب بـ Kotlin وJetpack Compose Material 3، ويستخدم Firebase Firestore/Auth/App Check وRoom وMedia3. لم يتم إنشاء مشروع جديد أو استبدال أي تقنية، ولا يحتوي هذا التغيير على أسرار أو `google-services.json`.

## ملفات التغيير ومساراتها

| الغرض | المسار |
|---|---|
| تهيئة Hilt | `app/src/main/java/com/elwataniatv/app/ElWataniaApp.kt` |
| مزودات Room وRepository | `app/src/main/java/com/elwataniatv/app/di/RepositoryModule.kt` |
| حالة البث المباشر | `app/src/main/java/com/elwataniatv/app/ui/viewmodel/LiveViewModel.kt` |
| حالة الأرشيف والبحث والمفضلة | `app/src/main/java/com/elwataniatv/app/ui/viewmodel/ArchiveViewModel.kt` |
| دليل البرامج والتذكيرات | `app/src/main/java/com/elwataniatv/app/ui/viewmodel/EpgViewModel.kt` |
| سجل المشاهدة | `app/src/main/java/com/elwataniatv/app/ui/viewmodel/HistoryViewModel.kt` |
| الإعدادات والملاحظات | `app/src/main/java/com/elwataniatv/app/ui/viewmodel/SettingsViewModel.kt` |
| شاشة المزيد | `app/src/main/java/com/elwataniatv/app/ui/viewmodel/MoreViewModel.kt` |
| الإشعارات | `app/src/main/java/com/elwataniatv/app/ui/viewmodel/NotificationsViewModel.kt` |
| المنسق المتوافق مرحلياً | `app/src/main/java/com/elwataniatv/app/ui/viewmodel/MainViewModel.kt` |
| ربط Hilt وإنشاء ViewModels | `app/src/main/java/com/elwataniatv/app/MainActivity.kt` |
| مكونات Compose القابلة لإعادة الاستخدام | `app/src/main/java/com/elwataniatv/app/ui/components/AppTopBar.kt`, `AppBottomBar.kt`, `AppNavHost.kt`, `UpdateDialog.kt`, `PopupAlertDialog.kt` |
| Dynamic Color وTheme | `app/src/main/java/com/elwataniatv/app/ui/theme/Theme.kt` |
| نماذج Compose غير المتغيرة | ملفات مستقلة داخل `app/src/main/java/com/elwataniatv/app/data/model/` مثل `RemoteStream.kt` و`ArchiveProgram.kt` و`EpgItem.kt` |
| اختبارات Compose | `app/src/androidTest/java/com/elwataniatv/app/LiveScreenTest.kt`, `ArchiveScreenTest.kt`, `NavigationTest.kt` |
| موارد النصوص | `app/src/main/res/values/strings.xml`, `app/src/main/res/values-ar/strings.xml` |
| الاعتماديات والإصدار | `gradle/libs.versions.toml`, `build.gradle.kts`, `app/build.gradle.kts` |
| قواعد R8 | `app/proguard-rules.pro` |
| مكونات المشغل وحالات الشبكة | `app/src/main/java/com/elwataniatv/app/ui/components/VideoPlayerView.kt`, `GlobalErrorBoundary.kt`, `WebBrowserView.kt` |
| EPG والتنبيهات الزمنية | `app/src/main/java/com/elwataniatv/app/ui/components/EpgStrip.kt` |
| بطاقات الأرشيف وإتاحة الوصول | `app/src/main/java/com/elwataniatv/app/ui/screens/archive/ArchiveScreen.kt`, `ArchiveProgramCard.kt` |
| مكونات الإعدادات والتوطين | `app/src/main/java/com/elwataniatv/app/ui/screens/settings/` |
| لوحة التحكم Firebase والتنظيف التشغيلي | `admin/index.html`, `scripts/seed-firestore.js` |
| قواعد Firestore الآمنة | `admin/firestore.rules` |
| إعداد Firebase Hosting | `admin/firebase.json`, `admin/.firebaserc` |
| دليل نشر لوحة التحكم | `admin/DEPLOY.md` |
| دليل التشغيل والتسليم المؤسسي | `ops/PRODUCTION_RUNBOOK_AR.md` |
| التحقق من APK وSHA-256 والتوقيع | `scripts/verify-release.sh` |

## المتطلبات

استخدم Android Studio حديثاً مع JDK 17 وAndroid SDK 36. يبقى إعداد Firebase خارج Git. عند البناء المحلي، ضع ملف `google-services.json` الذي يملكه صاحب المشروع في المسار القياسي محلياً فقط، ولا تضعه في Kotlin أو XML أو KTS ولا ترفعه إلى المستودع.

الإصدار الموحد هو `versionCode = 26` و`versionName = "8.1.3"` داخل `app/build.gradle.kts`. يتضمن الإصدار أصول شعار القناة الموحد في `assets/images/watania-channel-logo-unified.png` و`app/src/main/res/drawable-nodpi/watania_channel_logo.png` و`app/src/main/res/drawable-nodpi/watania_channel_logo_foreground.png`. يستخدم `AndroidManifest.xml` الاسم الصريح `@mipmap/ic_launcher_watania` لتجنب بقاء أيقونة launcher قديمة في cache. كما تم تحسين RTL، توسيع مساحة شريط التنقل للعربية، حجب القيم التجريبية في المصدر والواجهة، وربط زر الأخبار بوجهة YouTube.

## البناء والاختبارات

```bash
./gradlew assembleDebug
./gradlew test
./gradlew connectedDebugAndroidTest
./gradlew lint
```

تحتاج اختبارات Compose إلى محاكي أو جهاز Android متصل. تغطي الاختبارات الحالية مبدّل القنوات، إخفاء عبارات الحالة الداخلية، مسارات AppBottomBar، شاشة Live، شاشة Archive، واختبارات اللغة وRTL. إذا ظهر خطأ بأن Android SDK غير موجود، اضبط `ANDROID_HOME` أو أنشئ `local.properties` محلياً فقط بمسار SDK، ولا تلتزم بهذا الملف في Git. في بيئة CI أو محلياً استخدم JDK 17؛ يمكن التحقق من إعداد Kotlin عبر `./gradlew :app:checkKotlinGradlePluginConfigurationErrors --no-daemon`.

## التحقق من إصدار Release

بعد بناء APK Release شغّل:

```bash
scripts/verify-release.sh app/build/outputs/apk/release/app-release.apk
```

يتحقق السكربت من package name وversionCode وversionName وSHA-256 وتوقيع Android دون طباعة أي بيانات اعتماد. راجع `ops/PRODUCTION_RUNBOOK_AR.md` لخطوات النسخ الاحتياطي، الأدوار، المراقبة، وقبول المؤسسة.

## لوحة التحكم وربط Firebase

لوحة التحكم موجودة مسبقاً في `admin/index.html` وتستخدم Firebase Authentication وFirestore مباشرة من المتصفح بعد إدخال إعدادات **Web App** في معالج الإعداد. لا تستخدم إعداد Android داخل المتصفح، ولا تضع مفتاحاً أو كلمة مرور في ملفات Kotlin أو HTML أو Git. تحفظ إعدادات اللوحة محلياً في متصفح المسؤول، وتتحقق من custom claim باسم `admin` قبل فتح عمليات الإدارة.

تدعم اللوحة حالياً إدارة البث، الأرشيف، دليل البرامج، المواقع، الوسائط الاجتماعية، الإشعارات، الملاحظات، الإعلانات، الإعدادات، سجل النشاط والإحصائيات. وتتحكم إعدادات الهوية في اسم القناة، الشعار النصي، رابط الشعار، صورة الترحيب، ألوان Material، Dynamic Color، ظهور معاينة الأرشيف والبنرات وأقسام الأرشيف والتواصل والمواقع في شريط التنقل، وضع الصيانة، والإصدار المطلوب. أضيف زر آمن لتنظيف سجلات الاختبار المعروفة (`oussama` و`200`) من `epg` و`notifications` و`config/breaking` بعد تأكيد المسؤول. يتم الحفاظ على قواعد القراءة والكتابة في `admin/firestore.rules`، وتبقى كل عمليات الكتابة الإدارية محمية بصلاحية المسؤول.

للنشر، راجع `admin/DEPLOY.md` ثم نفّذ من مجلد `admin` بعد تسجيل الدخول إلى Firebase CLI:

```bash
firebase deploy --only hosting,firestore:rules
```

فعّل Firestore وAuthentication وApp Check في مشروع Firebase الصحيح، واختبر قواعد Firestore قبل نشرها. لتنظيف بيانات الاختبار من جهاز يملك صلاحية المسؤول، افتح `/admin/` ثم قسم دليل التشغيل واضغط زر التنظيف، أو استخدم `node scripts/seed-firestore.js --cleanup-test-data` بعد وضع `scripts/service-account.json` محلياً فقط. لا يحتوي المستودع على مفاتيح أو كلمات مرور. يبقى توقيع release اختيارياً من خلال متغيرات `RELEASE_STORE_FILE` و`RELEASE_STORE_PASSWORD` و`RELEASE_KEY_ALIAS` و`RELEASE_KEY_PASSWORD` أو خصائص Gradle خارج المستودع.
