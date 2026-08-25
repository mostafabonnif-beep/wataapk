# FIRESTORE_SECURITY_TASKS.md — مهام إصلاح أمن Firestore

> **هذا الملف موجّه لمساعد AI (أو مطوّر) لتنفيذ إصلاحات أمنية محددة.**
> كل مهمة تحتوي: الملف المستهدف، الكود الحالي، الكود المُصحّح، خطوات التحقق، وخطوات النشر.
> نفّذ المهام بالترتيب (TASK-001 أولاً). لا تغيّر أي شيء خارج نطاق المهمة الموصوفة.

> ## ⚠️ تحديث 2026-08-08 (v7.7.0)
> القواعد النهائية المعتمدة هي `admin/firestore.rules`. أظهر Firebase Console إصدار Rules منشوراً بتاريخ 2026-08-08، لكن محتوى الإصدار لم يكن قابلاً للقراءة عبر جلسة المتصفح، لذلك لا نعتبر التطابق مع الملف المحلي مثبتاً حتى تُجرى مقارنة مباشرة أو اختبار Emulator.
> - Anonymous Auth أصبح مفعّلاً في مشروع `elwataniatvapp` بتاريخ 2026-08-08، وهو مطلوب لمسارات التطبيق.
> - القراءة العامة متاحة، بينما كتابة عدادات `live/reactions` و`likes` والتفاعلات التجميعية متوقفة للمستخدمين حتى نشر جامع server-side موثوق؛ Anonymous Auth وحده لا يكفي لحماية عداد مشترك.
> - إنشاء التعليقات متاح للمستخدم الموثق مع `hasOnly` وربط `userUid`؛ تحديث النص والتفاعلات محصور في `admin` حتى نشر جامع server-side موثوق.
> - `push_tokens/{userUid}` يربط معرّف الوثيقة بالمستخدم الموثق، مع حفظ `deviceId`/معرّف التثبيت داخل الحقول للتتبع.
> - أصبح مسار `live/reactions/user_reactions/{userUid}` مربوطاً مباشرةً بهوية Firebase؛ ويُحفظ معرّف التثبيت داخل الوثيقة فقط.
> - اختبارات Rules السابقة تحتاج إعادة تشغيل بعد هذه التغييرات؛ ظهور إصدار منشور في Console لا يثبت وحده أنه يطابق الملف المحلي.
> - Cloud Functions غير منشورة لأن مشروع Firebase على خطة Spark؛ App Check Enforcement أيضاً مؤجل إلى ما بعد إصدار AAB موقّع واختبار Play Integrity.

**تاريخ المراجعة:** 2026-08-08
**النطاق الحالي:** `admin/firestore.rules` + كود Android في `app/src/main/java/` + `functions/`
**النتيجة الحالية:** قواعد admin وcatch-all deny و`hasOnly` الأساسية موجودة، وتم تشديد push_tokens وربط مسارات الملكية بـFirebase UID؛ عدادات التفاعلات العامة مغلقة مؤقتاً حتى نشر جامع server-side. تبقى إعادة اختبار Emulator/Playground والنشر إلى Firebase production مطلوبين.

---

## 📜 سجل التنفيذ (ما تم إنجازه)

### ✅ 2026-07-29 — إصلاح لوحة تحكم الويب (commit `52acc2a`)

أُجريت مراجعة كاملة للوحة الويب (`admin/index.html`) واكتُشف وأُصلح 9 مشاكل:

| # | المشكلة | الإصلاح |
|---|---|---|
| 1 | **قالب مهروس (`\``) في `mountActivityPane` كان يقتل الـ module script كله** — نسخة الريبو من اللوحة لا تعمل إطلاقاً | تصحيح القالب؛ السكريبت يُقرأ الآن بلا أخطاء |
| 2 | **اللوحة لا تفحص `admin` claim** — أي حساب Firebase مسجّل يفتح الواجهة كاملة | أُضيفت `hasAdminClaim()` (مطابقة لـ `lib/admin-auth.ts`) في نموذج الدخول و`onAuthStateChanged` — غير الأدمن يُرفض ويُسجَّل خروجه |
| 3 | زر "إشعار تجريبي" لا يرسل فعلياً — يكتب `sent: true` فتتخطاه Cloud Function | حُذف `sent: true` — يمر الآن عبر `sendPushNotification` |
| 4 | ميزة `admin_users` ميتة (محظورة بالقواعد + تعمل قبل bootstrap + claims حلّت محلها) | حُذفت (HTML + JS) — إدارة المسؤولين عبر `scripts/set-admin-claim.js` فقط |
| 5 | 4 دوال تستدعي `tryGetFirebase()` غير المعرّفة (exportCollection, batchDelete, testNotification, logActivity) | أُضيفت `tryGetDb()` وأُصلحت جميعها |
| 6 | `import("firebase/firestore")` الديناميكي — bare specifier يرفضه المتصفح | استُبدل بالاستيرادات العلوية (`getDocs, query, where`) |
| 7 | ترقيع `onSnapshot` بإعادة ربط الـ import — TypeError في ES modules | حُذف |
| 8 | `deleteApp` مستعملة في معالج الإعداد وغير مستوردة | أُضيفت لاستيراد firebase-app |
| 9 | تبويب "النشاط" محظور بالقواعد | أُضيفت قاعدة `activity_log` (admin فقط) في `admin/firestore.rules` |

**⚠️ خطوات نشر يدوية متبقية على المالك:**
- [ ] نشر القواعد المحدّثة في Firebase Console (Publish) — ضروري لتبويب النشاط
- [ ] إعادة نشر `admin/index.html` على الاستضافة (GitHub Pages أو `firebase deploy --only hosting`) — النسخة الحية ما زالت قديمة

---

## ⚠️ قواعد عامة لمن ينفّذ (مهم — اقرأ قبل أي تعديل)

1. **القواعد لا تُنشر تلقائياً من GitHub.** التعديل على `admin/firestore.rules` في الريبو
   لا يكفي — يجب نشرها يدوياً:
   - انسخ محتوى الملف → Firebase Console → Firestore → Rules → الصق → **Publish**
   - أو: `firebase deploy --only firestore:rules` من مجلد `admin/` (يتطلب firebase-tools + تسجيل دخول)
2. **لا تُضعّف أي قاعدة موجودة** (مثلاً: لا تفتح `write: if true` أبداً، لا تحذف الـ catch-all deny).
3. **لا تغيّر نمط `isAdmin()`** المعتمد على custom claim — هو صحيح ومدروس (راجع `FIRESTORE_RULES.md`).
4. **لا تعدّل `functions/` أو `lib/` إلا إذا كانت المهمة تنص على ذلك صراحة.**
5. بعد كل تعديل على القواعد: اختبر في **Firestore Rules Playground** (داخل Firebase Console)
   السيناريوهات الموثّقة في قسم "التحقق" لكل مهمة قبل النشر.

---

## 🔴 TASK-001 — حرجة: تقييد تحديث التعليقات إلى reactions أو admin

**الملف:** `admin/firestore.rules`
**الموقع:** داخل `match /programs/{programId}` ← `match /comments/{commentId}` ← قاعدة `allow update`

### المشكلة

كانت قاعدة `update` للتعليقات أوسع من المطلوب. في النسخة الحالية لا يوجد تعديل نص من التطبيق، لذلك يجب أن يبقى التحديث محصوراً في admin أو عدادات reactions.

### المشكلة السابقة

كانت قاعدة `create` للتعليقات فيها تحقق صارم (`hasOnly`، حجم النص ≤ 500، ومصادقة Firebase مطلوبة).
أما قاعدة `update` الحالية فلا تسمح إلا للمسؤول أو لتغيير عدادات `reactions` بخطوات ±1.
لا يملك تطبيق Android الحالي ميزة تعديل نص التعليق؛ لذلك لا يوجد مسار كتابة عام للنص بعد النشر.
وكان الخطر السابق أن تكون قاعدة update أوسع من ذلك، ما كان سيسمح بـ:

- يضخّم عدّادات `reactions` على تعليقه لأي رقم (مثلاً `reactions.like = 999999`) — متجاوزاً قاعدة ±1
- يكتب نصاً أطول من 500 حرف أو سبام/روابط بلا حدود — متجاوزاً فلتر الشتائم (الذي يفحص النص الأصلي فقط)
- يعدّل/يزيل حقل `moderation` الذي كتبته الـ Cloud Function
- يضيف حقولاً عشوائية للوثيقة

### الكود الحالي (ابحث عنه)

```text
        // Android client has no text-edit path. Updates are admin-only,
        // except signed-in reaction-counter changes constrained to ±1.
        allow update: if isAdmin()
          || (isSignedIn() && onlyReactionCountersMove(resource.data, request.resource.data));
```

### الكود المُصحّح (موجود فعلياً في `admin/firestore.rules`)

```text
        allow update: if isAdmin()
          || (isSignedIn() && onlyReactionCountersMove(resource.data, request.resource.data));
```

> **ملاحظة توافقية:** تطبيق Android الحالي (راجع `app/src/main/java/.../FirebaseSync.kt`) لا يملك
ميزة تعديل التعليق؛ لديه `postComment` و`toggleCommentReaction` فقط. لذلك قصر update على
`reactions` أو `admin` لا يكسر مساراً موجوداً.

### التحقق (Firestore Rules Playground قبل النشر)

| # | العملية | المصادقة | المتوقع |
|---|---|---|---|
| 1 | تحديث `text` فقط | مستخدم موثق | ❌ مرفوض (لا يوجد مسار تعديل نص) |
| 2 | تحديث `reactions.like = 999` | مستخدم موثق | ❌ مرفوض |
| 3 | تحديث `text` + حقل إضافي | مستخدم موثق | ❌ مرفوض |
| 4 | تحديث reactions بخطوة +1 فقط | مستخدم موثق | ✅ مسموح |
| 5 | تحديث reactions بخطوة +2 | مستخدم موثق | ❌ مرفوض |
| 6 | تحديث أي حقل غير reactions | مستخدم غير موثق | ❌ مرفوض |

---

## 🟡 TASK-002 — متوسطة: `push_tokens` تقبل حقولاً عشوائية (hasAll بدل hasOnly)

**الملف:** `admin/firestore.rules`
**الموقع:** `match /push_tokens/{userUid}`

### المشكلة

القاعدة تتحقق من **وجود** الحقول المطلوبة (`hasAll`) لكنها لا تمنع **إضافة حقول أخرى**.
أي مستخدم مجهول يقدر يخزّن بيانات عشوائية إضافية في وثيقة التوكن.

الحقول الفعلية التي يكتبها تطبيق Android (`FirebaseSync.registerFcmToken`):
`deviceId`, `userUid`, `token`, `platform`, `savedAt` — وتمنع القاعدة أي حقول إضافية.

### الشكل السابق (للتوثيق فقط)

كانت القاعدة القديمة تفتقد ربط `deviceId` بمعرّف الوثيقة، كما لم تكن تفرض `hasOnly` بشكل كامل. لا تستخدم هذا المثال للنشر.

### الكود المُصحّح (موجود فعلياً في `admin/firestore.rules`)

```text
    match /push_tokens/{userUid} {
      allow create: if isSignedIn()
        && userUid == request.auth.uid
        && request.resource.data.userUid == request.auth.uid
        && request.resource.data.keys().hasOnly(['deviceId', 'userUid', 'token', 'platform', 'savedAt']);
      allow update: if isSignedIn()
        && userUid == request.auth.uid
        && (!resource.data.keys().hasAny(['userUid']) || resource.data.userUid == request.auth.uid)
        && request.resource.data.userUid == request.auth.uid;
      allow read, delete: if isAdmin();
    }
```

> ⚠️ **تحذير:** يجب أن تتطابق القائمة مع الحقول الخمسة التي يكتبها Android: `deviceId` و`userUid` و`token` و`platform` و`savedAt`. حذف أي حقل منها من `hasOnly` سيمنع تسجيل التوكنات ويكسر إشعارات Push.

### التحقق (Playground)

| # | العملية | المتوقع |
|---|---|---|
| 1 | create بالحقول الأربعة + userUid صحيح | ✅ مسموح |
| 2 | create مع حقل إضافي `role: "admin"` | ❌ مرفوض |
| 3 | create بـ userUid ≠ auth.uid | ❌ مرفوض |

---

## 🔴 TASK-003 — حرجة (بنية تحتية): تفعيل Firebase App Check

**النوع:** إعداد Firebase + تعديل كود — **ليست تعديل قواعد.**
**لماذا هي حرجة:** القواعد الحالية تسمح بالكتابة لأي "مستخدم موقّع"، والتوقيع المجهول
(`signInAnonymously`) مجاني وغير محدود — أي سكريبت خارج التطبيق يقدر ينشئ آلاف الجلسات
المجهولة ويكتب في `likes`, `comments`, `feedback`, `push_tokens` مباشرة عبر Firestore REST API،
متجاوزاً كل حماية العميل (rate-limit المحلي، فلتر الشتائم client-side، منطق المفضلة المحلي).

**App Check** يربط كل طلب Firestore بتوقيع جهاز حقيقي يشغّل التطبيق فعلاً
(Play Integrity على Android / App Attest على iOS)، فيسقط طلبات السكريبتات الخارجية
حتى لو كانت "موقّعة" بجلسة مجهولة صالحة.

### خطوات التنفيذ

1. **Firebase Console → App Check:**
   - سجّل تطبيق Android بمزوّد **Play Integrity**
   - (لاحقاً لـ iOS: **App Attest** أو DeviceCheck)
   - فعّل **Enforcement** على Firestore **بعد** التأكد أن التطبيق المحدّث انتشر
     (ابدأ بـ Monitor mode، راقب المقاييس أسبوعاً، ثم Enforce)
2. **في كود تطبيق Android الأصلي (مكتمل في المستودع):**
   - يستخدم التطبيق Firebase App Check مع **Play Integrity** في نسخ release، و**Debug provider** في نسخ التطوير (`MainActivity`، اعتماديات `firebase-appcheck`/`playintegrity`/`debug` عبر BOM).
   - لا يُفعّل Enforcement قبل إصدار AAB موقّع واختبار Play Integrity على جهاز أو مسار توزيع حقيقي.
   - للتطوير: سجّل Debug token الظاهر في logcat (`D DebugAppCheckProvider: Enter this debug secret...`) في Console → App Check → Apps → Debug tokens.
3. **حدّث `.env.example` والتوثيق** بأي متغيرات جديدة مطلوبة.

> **ترتيب مهم:** لا تفعّل Enforcement قبل نشر نسخة التطبيق التي تحتوي App Check SDK —
> وإلا ستُحجب طلبات المستخدمين الحاليين على النسخة القديمة.

### التحقق

- في App Check Console: ظهور طلبات موقّعة (verified) قادمة من التطبيق.
- بعد Enforcement: طلب Firestore REST يدوي بجلسة مجهولة من curl/Postman → يجب أن يُرفض.

---

## 🔴 TASK-003A — تقييد مفتاح Firebase Web API للوحة التحكم

**النوع:** إعداد Google Cloud Console فقط (مجاني).
**لماذا:** لوحة التحكم لا تحتوي الآن على إعداد Firebase أو API key داخل المستودع؛ يدخل المسؤول إعداد Web من معالج الإعداد وتُحفظ القيم محلياً في متصفحه. يجب استخدام إعداد Web منفصل وتقييد مفتحه بنطاقات الاستضافة الرسمية لمنع استغلال حصص المشروع من دومينات أخرى.

### خطوات التنفيذ (Google Cloud Console → APIs & Services → Credentials)

1. **حدد المفتاح الصحيح**: افتح مفتاح Web الخاص بلوحة التحكم في Google Cloud Console وتأكد من هويته.
   ⚠️ **ملاحظة حرجة**: لا تضع إعداد Android أو `google-services.json` داخل لوحة التحكم. يجب أن يكون إعداد اللوحة من نوع **Web** مستقلاً عن إعداد تطبيق Android؛ تقييد مفتاح Android بـ HTTP referrers **يكسر التطبيق الأصلي** لأنه لا يرسل referrer.
2. **الحل الآمن**: Console → Project settings → **Add app → Web** لإنشاء إعداد Web مخصص (مفتاح Web جديد + `appId: 1:...:web:...`) ثم أدخل القيم عبر معالج إعداد Firebase في لوحة التحكم، ولا تحفظها في Git. ثم:
   - قيّد **مفتاح الويب** بـ **HTTP referrers** → اسمح فقط بـ `https://elwataniatv-channel.vercel.app/*` ودومين Hosting (`*.web.app`) ودومين الإدارة الرسمي عند توفره.
   - قيّد **مفتاح Android** في `google-services.json` بـ **Package name + SHA-1** للتوقيع الموقّع (وليس referrers).
3. أعد إدخال القيم في معالج الإعداد المحلي للوحة عند الحاجة، ولا تضعها في `admin/index.html` أو Git.

### التحقق
- اللوحة تعمل من الدومين المسموح فقط.
- طلب مباشر لـ Firebase Auth/API من أي دومين آخر → `403`/`REJECTED`.
- التطبيق Android يعمل بعد تقييد مفتاحه بالحزمة (جرّب تسجيل الدخول والتعليق).

---

## 🟢 TASK-004 — اختيارية (طويلة المدى): عدّادات likes بصوت واحد لكل مستخدم

**الملفات:** `admin/firestore.rules` + `lib/likes.ts` + Cloud Function جديدة (اختياري)

### المشكلة

`likes/{programId}` عدّاد عام بلا سجل تصويت شخصي — بخلاف reactions التعليقات التي لها
`reactions/{userUid}` (وثيقة لكل مستخدم تمنع التكرار). القاعدة تقيّد كل write بخطوة ±1،
لكنها لا تمنع تكرار الخطوات. **App Check (TASK-003) يغطي أغلب هذا الخطر عملياً** —
نفّذ هذه المهمة فقط إذا أردت دقة إحصائية كاملة (منع نفس المستخدم الشرعي من التصويت المتكرر).

### التصميم المقترح (نفس نمط reactions الموجود — حافظ على الاتساق)

```
match /likes/{programId} {
  allow read: if true;
  allow write: if false;  // العدّاد الإجمالي يحرّكه السيرفر فقط (Cloud Function / Admin SDK)

  match /voters/{uid} {
    allow read: if true;
    allow create, update: if isSignedIn() && request.auth.uid == uid
      && request.resource.data.keys().hasOnly(['kind'])
      && request.resource.data.kind in ['like', 'dislike', 'none'];
    allow delete: if isSignedIn() && request.auth.uid == uid;
  }
}
```

- `lib/likes.ts` ← `syncCounters()`: بدل كتابة `increment()` على العدّاد مباشرة،
  تكتب وثيقة `voters/{uid}`.
- Cloud Function (trigger على `likes/{programId}/voters/{uid}` onWrite):
  تحسب الفرق بين القيمة القديمة والجديدة وتطبّقه على العدّاد الإجمالي عبر Admin SDK.
- وثيقة views: إن أردت الإبقاء على عدّاد المشاهدات بدون سجل، افصله لوثيقة مستقلة
  (مثلاً `view_counters/{programId}`) بقاعدة مشابهة للحالية، لأن views بطبيعتها تتكرر.

### التحقق

- نفس المستخدم يصوّت مرتين على نفس البرنامج → العدّاد يتحرك مرة واحدة فقط.
- تغيير التصويت like→dislike → likes −1 و dislikes +1.

---

## 🟡 TASK-005 — متوسطة: تقوية فلتر الشتائم + rate-limit السيرفري للـ feedback

### 5أ — قائمة الكلمات (`functions/profanity.js` ومرشح العميل Android إن وُجد)

القائمة الحالية صغيرة (~19 كلمة) ويسهل تجاوزها (تشكيل، فراغات بين الحروف، حروف لاتينية
بدل عربية). الحد الأدنى المقبول: توسيع القائمة المبدئية للعربية والفرنسية، والحفاظ على
**مزامنة القائمتين** (الملف يعلّق على ذلك صراحة: "if you add a banned word here, add it
to the client list too"). للحل الاحترافي لاحقاً: Google Perspective API أو خدمة moderation
مشابهة — قرار يعود لصاحب المشروع (تكلفة + خصوصية).

### 5ب — rate-limit للـ feedback من جهة السيرفر

حالياً الحد (5 رسائل / 10 دقائق) client-side فقط عبر AsyncStorage — مسح بيانات التطبيق
يتجاوزه. App Check (TASK-003) يخفف هذا كثيراً. إن أردت حداً صارماً: Cloud Function
(trigger على إنشاء `feedback/{docId}`) تقرأ آخر رسائل نفس `uid` وتحذف/تعلّم الزائد —
لكن لاحظ أن قاعدة `feedback` الحالية لا تخزّن `uid` في الحمولة (حماية خصوصية مقصودة)،
فإضافة حد سيرفري تتطلب إضافة حقل `uid` أولاً وتحديث `hasOnly` في القاعدة و`lib/feedback.ts`
معاً — نفّذ الثلاثة كوحدة واحدة أو لا شيء.

---

## ✅ ما تمت مراجعته ولا يحتاج تعديل (لا تلمسه)

| العنصر | الحالة |
|---|---|
| `isAdmin()` عبر custom claim | ✅ صحيح ومدروس — لا تغيّره لنمط collection-lookup |
| Catch-all deny في نهاية الملف | ✅ ممتاز — إبقاؤه إلزامي |
| `lib/admin-auth.ts` (fail closed + force token refresh) | ✅ سليم |
| قاعدة `create` للتعليقات (hasOnly + مصادقة + userUid/deviceId) | ✅ صحيحة |
| عدادات reactions العامة | ⏸️ مغلقة حتى نشر جامع server-side موثوق |
| `onlyReactionCountersMove` | ⏸️ محفوظ كمرجع تصميمي، غير مستخدم في قاعدة عامة حالياً |
| قواعد `analytics_events` و `feedback` (hasOnly) | ✅ سليمة |
| `notifications` و `users/{uid}` | ✅ سليمة |
| Collections المحتوى (streams/archive/websites/social/epg/config) | ✅ قراءة عامة + كتابة admin — مقصود وصحيح لتطبيق محتوى |

---

## 📋 قائمة النشر النهائية (Deployment Checklist)

- [x] TASK-001: إنشاء التعليقات محصور في جلسة Firebase، وتحديثها إداري حتى نشر جامع reactions موثوق
- [x] TASK-002: `push_tokens` تستخدم `hasOnly` وتربط الوثيقة بـ `userUid` مع حفظ `deviceId` كحقل
- [ ] إعادة تشغيل اختبارات Playground/Emulator للسيناريوهات الجديدة
- [ ] نشر القواعد في Firebase production عبر Firebase Console أو `firebase deploy --only firestore:rules`
  — ملاحظة: قاعدة `activity_log` أُضيفت للملف في 2026-07-29 (commit `52acc2a`) وستُنشر معها تلقائياً
- [ ] تجربة يدوية من التطبيق: نشر تعليق وتسجيل توكن إشعارات؛ تفاعلات العدادات تبقى مؤجلة حتى نشر الجامع server-side
- [ ] TASK-003: App Check مفعّل (Monitor أولاً ثم Enforce)
- [ ] تحديث `FIRESTORE_RULES.md` إذا تغيّر أي سلوك موثّق فيه (جدول "ماذا يحمي كل rule")
- [x] ~~`FIRESTORE_RULES.md`: توثيق `activity_log`~~ — تم في commit `52acc2a`

---

## 🔗 ملفات مرجعية

- `admin/firestore.rules` — القواعد الفعلية المنشورة
- `FIRESTORE_RULES.md` — شرح نموذج الأمن الحالي وكيفية النشر
- `app/src/main/java/com/elwataniatv/app/data/remote/FirebaseSync.kt` — حمولات التعليقات/reactions/push_tokens
- `functions/index.js` + `functions/profanity.js` — المعالجة السحابية والمراجعة

## حالة 2026-08-09

- تم تشديد قواعد التعليقات (server timestamp + 30 ثانية/مستخدم + رابط واحد) واختبارها 21/21 في المحاكي.
- فلتر العميل `ProfanityFilter.kt` مضاف ويُستدعى قبل الإرسال.
- Cloud Functions غير منشورة (خطة Spark) — اختيارية مستقبلاً مع ترقية Blaze.
