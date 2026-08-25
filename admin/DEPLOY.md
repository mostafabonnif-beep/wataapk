# 🚀 نشر لوحة التحكم على Firebase Hosting

تنشر لوحة الإدارة على نطاق `https://<projectId>.web.app` مجاناً وبسرعة عالية، باستعمال نفس مشروع Firebase الذي تستعمله للتطبيق.

## ⚙️ إعداد لمرة واحدة (5 دقائق)

### 1. ثبّت Firebase CLI
```bash
npm install -g firebase-tools
```

### 2. سجّل دخول
```bash
firebase login
```
سيفتح المتصفح لتسجيل الدخول بحساب Google الذي يمتلك مشروع Firebase.

### 3. اربط مشروعك
من داخل مجلد `admin/`، راجع `.firebaserc` الموجود وعدّل قيمة `projects.default` فقط إذا كنت تستخدم مشروع Firebase آخر. الملف الحالي مضبوط على `elwataniatvapp`.
مثال:
```json
{
  "projects": {
    "default": "elwataniatvapp"
  }
}
```

### 4. تهيئة Firebase Web

`admin/index.html` يستخدم تهيئة Web العامة للمشروع الرسمي تلقائياً، بينما تم حذف زر وشاشة إعداد Firebase من واجهة المسؤول حتى لا تتم إعادة تغيير المشروع بالخطأ. هذه التهيئة ليست service-account credential ولا تمنح صلاحية إدارة وحدها؛ الوصول الفعلي محكوم بـFirebase Authentication وFirestore Rules وcustom claim `admin: true`.

تظهر للمسؤول حالة اتصال مختصرة فقط، ولا تُعرض حقول API Key أو Project ID أو إعدادات Firebase في شاشة الدخول. لا تضع service-account JSON أو كلمة مرور أو private key في المستودع أو داخل `index.html`.

## 📤 النشر

من داخل مجلد `admin/` (سيبني Firebase CLI تلقائياً شجرة `hosting-dist` قبل النشر):
```bash
firebase deploy --only hosting
```

أو لنشر القواعد + الموقع معاً:
```bash
firebase deploy --only hosting,firestore:rules
```

سيظهر لك رابط مثل: `https://<project-id>.web.app`.

## 🔄 التحديث لاحقاً

بعد أيّ تعديل على `index.html`:
```bash
firebase deploy --only hosting
```

## 🌐 نطاق مخصّص (اختياري)

من Firebase Console → Hosting → "Add custom domain" → أضف نطاقك (مثلاً `admin.elwataniatv.dz`) واتبع تعليمات تحديث DNS.

## 🛡️ أمان

- اللوحة تتطلب تسجيل دخول قبل أيّ تعديل (Firebase Auth)
- أنشئ حساب admin في Firebase Console → Authentication → Add user
- لا تنشئ حسابات لغير المسؤولين
- لا تمنح صلاحية الإدارة اعتماداً على البريد وحده؛ استخدم custom claim `admin: true` بعد التحقق من UID.

## ⚙️ فلترة التعليقات — خط الدفاع المجاني (بدون Cloud Functions)

القرار الحالي: **البقاء على خطة Spark المجانية**، لذلك لا تُنشر Cloud Functions. الحماية تتم عبر طبقتين مجانيتين بالكامل:

1. **فلترة عميلية فورية** — `app/src/main/java/com/elwataniatv/app/util/ProfanityFilter.kt`:
   نفس خوارزمية `functions/profanity.js` (قائمة سوداء عربي/فرنسي/إنجليزي، normalise، كشف الشتم/السبام/الروابط الكثيرة/الكتابة الكبيرة). تُستدعى قبل أي إرسال؛ الرفض يعرض رسالة للمستخدم **بدون أي استدعاء شبكي**. هذا الفحص قابل للتجاوز تقنياً — الخط الحقيقي هو القواعد.

2. **Firestore Security Rules (خط الدفاع الأساسي)** — `admin/firestore.rules` تفرض على إنشاء التعليق:
   - طول النص 1–500 حرف.
   - `createdAt` يجب أن يكون **server timestamp** (`== request.time`) — العميل لا يستطيع تزوير/تحويل التاريخ.
   - **Rate limit**: تعليق واحد لكل مستخدم كل **30 ثانية** عبر `users/{uid}.lastCommentAt` (يكتبه التطبيق في نفس الـbatch مع التعليق، والقواعد تمنع كتابته إلا كـserver timestamp).
   - رفض أي تعليق يحوي أكثر من رابط واحد.
   - التحقق من الحقول (hasOnly) وربط `userUid == request.auth.uid` وعدّادات التفاعلات.

### نشر القواعد (مجاني على Spark)

```bash
firebase login
firebase deploy --only firestore:rules
```

> أو أمر واحد جاهز من جذر المستودع: `bash scripts/deploy-firebase.sh` (يفحص CLI والتسجيل، ينشر القواعد + الفهارس، ويعرض خطوات التحقق).

الاختبارات المحلية (تتطلب Java 11+):
```bash
npm ci --prefix rules-tests
npm test --prefix rules-tests        # 21/21 — تُشغَّل أيضاً في CI
```
> ملاحظة: `rules-tests/firebase.json` يستخدم منفذ المحاكي **8081** (الساندبوكس يشغل 8080).

### التحقق بعد النشر
1. علّق من التطبيق → يُنشر التعليق العادي فوراً (الفلترة العميلية تمرره والقواعد تقبله).
2. أرسل تعليقاً يحوي رابطين → يُرفض من القواعد.
3. أرسل تعليقين خلال 30 ثانية من نفس الحساب → يُرفض الثاني.

---

## ⚙️ Cloud Functions (اختيارية — تتطلب خطة Blaze)

الدوال `moderateComment` و`sendPushNotification` موجودة في `../functions` لكنها **غير منشورة** عمداً (الخطة المجانية لا تدعم Cloud Functions v2). إن قررت لاحقاً الترقية إلى **Blaze** (قرار فوترة بيد صاحب الحساب):

- المنطقة: كل الدوال على `europe-west1` (مطابقة Firestore `eur3`) — إن أظهرت Console موقعاً مختلفاً غيّر `REGION` في `functions/index.js`.
- النشر: `firebase deploy --only functions` ثم `firebase deploy --only firestore:indexes` (فهرس الـrate limit في `firestore.indexes.json`).
- بدون Functions: الإشعارات تظهر داخل التطبيق من مستندات `notifications` (in-app)؛ إرسال FCM خارجي يتطلب نشر `sendPushNotification`.

### حدود خط الدفاع المجاني (بصراحة)

- أي فلتر على جهاز العميل قابل للتجاوز؛ وحدود القواعد (رابط واحد، 30 ثانية، length) تُقيّد الإساءة لكنها لا تحلل المحتوى دلالياً.
- الحماية الدلالية الكاملة (كلمات ممنوعة لا تُطابق القائمة، إساءة مموّهة) تحتاج Cloud Functions — اختيارية عند الترقية إلى Blaze.
- بدون Functions، الإشعارات الخارجية (FCM) لا تُرسَل؛ الإشعارات تظهر داخل التطبيق فقط.

### كيف يقرأ التطبيق النتيجة؟

يقرأ عميل Android حقل `moderation` على كلّ تعليق عند توفره:
- `{ ok: true }` -> التعليق يظهر عادي.
- `{ ok: false, reason: "profane" }` -> يظهر خلف بطاقة "تم إخفاء هذا التعليق من قبل المشرف التلقائي".


## 🛡️ Firebase App Check (مجاني على Spark) + تقييد مفتاح API

### App Check (Play Integrity)
الكود جاهز في المستودع: نسخ release تستخدم `PlayIntegrityAppCheckProviderFactory` ونسخ debug تستخدم
Debug provider (`MainActivity` + اعتماديات `firebase-appcheck`/`playintegrity`/`debug`). الخطوات المتبقية
إعداد Console فقط (كلها مجانية):

1. **Google Cloud Console** → APIs & Services → Library → فعّل **Google Play Integrity API** (مجاني).
2. **Firebase Console → App Check → Apps** → سجّل تطبيق Android بمزوّد **Play Integrity**.
3. فعّل **Enforcement** لاحقاً على Firestore وAuth (ابدأ بـ **Monitor** أسبوعاً ثم **Enforce**) —
   ⚠️ **بعد** نشر نسخة تحتوي الـSDK (الـAAB الموقّع من البند 3) حتى لا تُحجب النسخ القديمة.
4. للتطوير: انسخ Debug token من logcat (`D DebugAppCheckProvider`) → Console → App Check → Debug tokens.
   بدون تسجيله، نسخ debug ستُحجب بعد تفعيل Enforcement.

### تقييد مفتاح Firebase Web API (اللوحة)
تهيئة Web العامة للمشروع الرسمي موجودة في كود اللوحة لتعمل تلقائياً. مفتاح Firebase Web API ليس service-account secret، لكن يجب تقييده من Google Cloud Console بـHTTP referrers مثل `https://elwataniatvapp.web.app/*` وأي نطاق Hosting رسمي، مع عدم وضع أي service-account key في الواجهة.

يجب تسجيل تطبيق Web في Firebase App Check قبل تفعيل enforcement للويب. أما تطبيق Android فيستخدم Play Integrity كما هو مسجل في Console. ابدأ بوضع Monitor، اختبر تسجيل الدخول والقراءة والكتابة، ثم فعّل Enforce بعد التأكد من أن النسخة المنشورة تستخدم provider الصحيح.

## Firebase Hosting

من مجلد المشروع الجذر نفّذ:

```bash
cd admin
firebase use elwataniatvapp
firebase deploy --only hosting
```

الملف `firebase.json` يعرّف `hosting-dist` كمجلد Hosting، ويولّده من allowlist فقط: لوحة الإدارة، سياسة الخصوصية، و`assets`. رابط التشغيل المنشور حالياً هو `https://elwataniatvapp.web.app/admin/`، والجذر يحوّل تلقائياً إلى لوحة الإدارة. صفحة الخصوصية متاحة على `https://elwataniatvapp.web.app/privacy.html`.
