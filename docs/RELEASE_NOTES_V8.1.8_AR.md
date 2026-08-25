# ملاحظات الإصدار — Elwatania TV v8.1.8

**الإصدار:** 8.1.8

**رقم البناء:** 31

**نوع الإصدار:** Release موقّع

**سبب الإصدار:** إعادة فحص مستقلة بعد v8.1.7

## ملخص

أُعيد فحص النسخة السابقة من المصدر والبناء ومسارات الواجهة. لم يظهر عيب Kotlin أو اختبار أو lint جديد. الملاحظة البصرية المؤكدة الوحيدة المتبقية كانت وجود ثلاثة عناوين عند 20sp في سجل المشاهدة، وشاشة البداية، وحالة عدم الاتصال. خُفّضت هذه العناوين إلى 18sp لتتطابق مع نظام Typography المركزي وتخفف الثقل على الهواتف الصغيرة.

## التعديلات

| الملف أو المجال | التعديل |
|---|---|
| `HistoryScreen.kt` | خفض عنوان سجل المشاهدة من 20sp إلى 18sp. |
| `OnboardingScreen.kt` | خفض اسم التطبيق في شاشة البداية من 20sp إلى 18sp. |
| `GlobalErrorBoundary.kt` | خفض عنوان عدم الاتصال من 20sp إلى 18sp. |
| `app/build.gradle.kts` | رفع النسخة إلى `versionName 8.1.8` و`versionCode 31`. |
| لوحة التحكم | تحديث الافتراضات النصية إلى v8.1.8 دون تغيير إعدادات Firebase أو الأدوار. |
| صفحة التحميل | تحديث اسم الملف والإصدار والبصمة إلى APK v8.1.8. |
| قائمة القبول | إضافة فحص صريح للشاشات الثلاث وتحديث build إلى 31. |

## نتائج الاختبارات

| الاختبار | النتيجة |
|---|---|
| `git diff --check` | ناجح |
| `compileDebugKotlin` | ناجح |
| `testDebugUnitTest` | ناجح |
| `lintDebug` | ناجح |
| `assembleDebug` | ناجح |
| `assembleRelease` | ناجح |
| `bundleRelease` | ناجح |
| APK Signature Scheme v2 | ناجح |
| Metadata | `com.elwataniatv.app`, versionCode 31, versionName 8.1.8 |

## أصول الإصدار

| الملف | SHA-256 |
|---|---|
| `ElwataniaTV-v8.1.8-professional-signed.apk` | `8814fd29efbde8d2c36903f3bfbe5636468cd02815c10f1d35db6484c9f7672c` |
| `ElwataniaTV-v8.1.8.aab` | `d6a0da600ffc633caf9be581239ddf1d421cb20366fac8d19314a2dda6dea006` |

## فحص المحاكي

أُعيد تشغيل AVD `elwatania-lite35` أكثر من مرة. نجح تشغيل TCG software emulation والوصول إلى حالة `device` وخدمة Package جزئية، ثم جرى `wipe-data` وإعادة المحاولة. فشل تثبيت APK قبل تشغيل التطبيق مرتين: أولاً بسبب `StorageManager.getVolumes()` الذي أعاد `NullPointerException` داخل Android PackageManager، وثانياً بسبب `Cannot access system provider: settings before system providers are installed`. ظهرت أيضاً حالة توقف خدمات `settings` و`package` أثناء الانتظار.

هذه الأخطاء تقع داخل خدمات Android Emulator قبل وصول التطبيق إلى MainActivity، ولذلك لا تُنسب إلى كود Elwatania TV. يبقى اختبار جهاز حقيقي أو محاكي يعمل مع KVM إلزامياً لاعتماد الواجهة بصرياً ووظيفياً.
