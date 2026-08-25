# ملاحظات الإصدار — Elwatania TV v8.1.5

## ملخص الجولة

تأتي هذه النسخة بعد فحص فعلي لـAPK على AVD Android 35 داخل الخادم. ركزت الجولة على إزالة العوائق التي تظهر قبل وصول المستخدم إلى الواجهة، وتحسين موثوقية الإقلاع، وتنظيف الهوية البصرية للشعار.

## الإصلاحات المنفذة

| المجال | التغيير | النتيجة |
|---|---|---|
| الإقلاع الأول | إيقاف طلب `POST_NOTIFICATIONS` التلقائي داخل `onCreate` | لا تظهر نافذة صلاحية فوق أول تجربة؛ يمكن إضافة طلب اختياري من إعدادات المستخدم لاحقاً |
| Firebase | إزالة بدء مستمعي Firebase من constructor الخاص بـ`MainViewModel` وتأجيله إلى أول composition على `Dispatchers.Default` | تقليل احتمال حبس Splash أثناء تهيئة الشبكة أو Firebase |
| الأيقونة وSplash | استبدال adaptive-icon foreground الذي كان يحتوي checkerboard مطبوعاً بمورد الشعار الرسمي النظيف | إزالة أثر بصري غير مؤسسي من شاشة البداية |
| التوثيق | تحديث README وقائمة القبول إلى build 28 وإضافة شرط فحص startup | ربط الأدلة الميدانية بالنسخة الصحيحة |

## التحقق

نجح `assembleDebug` بعد التعديلات. تم تثبيت Debug APK v8.1.5 / build 28 على المحاكي وبدأت `MainActivity` عبر Monkey. لم يظهر crash خاص بـ`com.elwataniatv.app`، ولم يظهر طلب `POST_NOTIFICATIONS` في التشغيل الجديد.

## قيد بيئة الخادم

المحاكي يعمل بدون KVM وبـTCG software emulation. أثناء الفحص ظهرت رسائل `System UI isn't responding` و`Permission controller isn't responding`، كما فشل DNS داخل المحاكي في حل نطاقات Firebase. هذه النتائج تخص بيئة الفحص ولا تكفي للحكم على جهاز Android فعلي. يلزم تنفيذ `ops/ACCEPTANCE_TEST_AR.md` على هاتف أو AVD مدعوم بتسريع عتادي قبل اعتماد النسخة للإنتاج.

## الإصدار

- **Version name:** `8.1.5`
- **Version code:** `28`
- **Application ID:** `com.elwataniatv.app`
