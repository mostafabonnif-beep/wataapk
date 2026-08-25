# ✅ دليل تفعيل Firebase — خطوة بخطوة (نسخة v8)

> هذا كل ما عليك فعله في [Firebase Console](https://console.firebase.google.com) بالترتيب.
> مدة الإنجاز: **30–45 دقيقة** (مرة واحدة فقط).

---

## 🟢 المرحلة 1 — إنشاء المشروع

1. افتح [console.firebase.google.com](https://console.firebase.google.com) → **Add project**
2. اسم المشروع: **`elwataniatvapp`** ⚠️ مهم — ملف `.firebaserc` عندك مضبوط على هذا الاسم مسبقاً
   - (إن كان الاسم محجوزاً، اختر اسماً آخر وعدّل `admin/.firebaserc` لاحقاً)
3. **عطّل** Google Analytics (اختياري — يسرّع الإعداد)

---

## 🟢 المرحلة 2 — أضف تطبيق Android

1. من صفحة المشروع → أيقونة **🤖 Android**
2. **Android package name**: `com.elwataniatv.app` (مطابق لـ `app/build.gradle.kts`)
3. **App nickname**: الوطنية TV
4. **Register app** → **حمّل `google-services.json`**
5. ضع الملف في مجلد التطبيق القياسي `app/`:
   ```
   elwataniatv-Channel/
   ├── settings.gradle.kts
   ├── app/
   │   └── google-services.json   ← هنا (المسار المفضّل)
   └── ...
   ```
   > يدعم إعداد Gradle الحالي أيضاً وضع الملف في جذر المشروع، ويقوم CI بنسخه إلى `app/` قبل البناء. الملف مستثنى في `.gitignore` — لن يُرفع على GitHub.

---

## 🟢 المرحلة 3 — تفعيل الخدمات

| الخدمة | المسار | الإعداد |
|--------|--------|---------|
| **Firestore** | Build → Firestore Database | **Create database** → وضع **production** → سجّل الموقع الفعلي في Console؛ المشروع الحالي موثّق على `eur3`. لا تغيّر الموقع بعد الإنشاء. **قرار المنطقة طُبّق في الكود**: الدوال مضبوطة على `europe-west1` (ضمن eur3) — إذا أظهرت Console موقعاً مختلفاً غيّر `REGION` في `functions/index.js` قبل النشر |
| **Authentication** | Build → Authentication → Sign-in method | فعّل **Email/Password** ✅ + **Anonymous** ✅ (كلاهما إلزامي — القواعد تطلبهما) |
| **Cloud Functions / Storage** | **القرار الحالي: لا تُنشر** (خطة Spark مجانية) | الفلترة مجانية عبر فلتر العميل + القواعد (server timestamp + 30 ثانية + رابط واحد + 500 حرف) — راجع `admin/DEPLOY.md`؛ Storage غير مستخدمة |
| **Gemini API** | [aistudio.google.com](https://aistudio.google.com) | إنشاء **API Key** → سنضعه كـ Secret في المرحلة 6 |

---

## 🟢 المرحلة 4 — انشر قواعد الأمان (الأهم!)

1. Console → **Firestore Database → Rules** tab
2. **امسح كل الموجود** → الصق محتوى الملف `admin/firestore.rules` كاملاً
3. **Publish**

> ⚠️ لا تنسخ قواعد من أي مكان آخر. القواعد المرفقة تمنع أي مستخدم غير المسؤول من الكتابة،
> وتقيّد العدّادات بخطوة ±1، وتمنع تهريب الحقول (hasOnly).

**للتحقق السريع** — افتح تبويب Rules وابحث عن:
- `function isAdmin() { return isSignedIn() && request.auth.token.admin == true; }` ✅
- `match /likes/{programId}` مع `counterStep` ✅
- `match /{document=**} { allow read, write: if false; }` في الأسفل ✅

---

## 🟢 المرحلة 5 — أنشئ حساب المسؤول

1. Console → **Authentication → Users** → **Add user**
2. البريد: `admin@elwataniatv.dz` (أو ما تريد) + كلمة مرور قوية
3. هذا الحساب سيُستخدم للدخول إلى **لوحة التحكم** فقط

---

## 🟢 المرحلة 6 — استخدم النسخة المجانية فقط

لا تنشر Cloud Functions ولا تفعّل Cloud Storage. التطبيق يحفظ المحتوى والتنبيهات في Firestore، ويستخدم روابط HTTPS عامة للصور. الإشعارات الخارجية Push غير متاحة في هذا الوضع؛ التنبيهات المنبثقة داخل التطبيق تعمل بدون خدمة مدفوعة.

## 🟢 المرحلة 7 — اضبط المسؤول + املأ البيانات الأولية

### أ. نزّل Service Account
1. Console → ⚙️ Project Settings → **Service accounts**
2. **Generate new private key** → احفظ `service-account.json`
3. ضعه في `scripts/` (في `.gitignore` — لا يُرفع)

### ب. اضبط صلاحية المسؤول (admin claim)
```bash
cd scripts
npm install --no-save firebase-admin
node set-admin-claim.js admin@elwataniatv.dz
# ✅ يعرض claims الحالية بعد إضافة admin:true
# ⚠️ حدّث ID token بتسجيل الخروج/الدخول قبل اختبار الصلاحيات
```

### ج. املأ البيانات الأولية (قنوات، مواقع، برامج...)
```bash
node seed-firestore.js
```

للتنظيف الاختياري لبيانات الاختبار القديمة فقط، استخدم الخيار الصريح:
```bash
node seed-firestore.js --cleanup-test-data
```

---

## 🟢 المرحلة 8 — انشر لوحة التحكم (اختياري لكن موصى به)

```bash
cd admin
firebase use elwataniatvapp    # إن لم يكن .firebaserc مضبوطاً
firebase deploy --only hosting
```

بعد النشر قد يظهر رابط من نمط **`https://<project-id>.web.app`**؛ تحقّق من الرابط الفعلي الذي يعرضه Firebase CLI ولا تفترض أن النشر تم مسبقاً.

> ملاحظة: `admin/firebase.json` يحتوي إعداد Hosting بالفعل. نفّذ النشر ثم تحقّق من الرابط الذي يعرضه Firebase CLI؛ وجود الإعداد المحلي لا يثبت أن الموقع منشور.

---

## 🟢 المرحلة 9 — حذف بيانات likes القديمة (إن وُجدت)

إن كان عندك بيانات likes بالشكل القديم (مستند لكل مستخدم) من نسخة سابقة:
- Console → Firestore → مجموعة `likes`
- احذف أي مستندات بصيغة `{userId: ...}` القديمة
- القواعد الجديدة تستخدم **مستند واحد لكل programId** بعدّادات `{likes, dislikes, views}`

---

## ✅ قائمة التحقق النهائية

- [ ] مشروع `elwataniatvapp` في Firebase
- [ ] `google-services.json` في `app/` (أو في الجذر وفق دعم Gradle الحالي)
- [ ] Firestore مفعّل والموقع المؤكد في Console موثّق؛ الحالة الحالية المسجلة هي `eur3` والدوال مضبوطة على `europe-west1` — إن اختلف الموقع، عدّل `REGION` قبل النشر
- [ ] Authentication: Email/Password + Anonymous
- [ ] القواعد = `admin/firestore.rules` منشورة
- [ ] حساب `admin@elwataniatv.dz` في Authentication
- [ ] `GEMINI_API_KEY` secret مضبوط
- [ ] **خط الدفاع المجاني**: فلتر العميل (`ProfanityFilter.kt`) + القواعد المشددة منشورة (`firebase deploy --only firestore:rules`) — اختبارات القواعد 21/21 محلياً وفي CI
- [ ] (اختياري مستقبلاً) Functions تتطلب ترقية **Blaze** ثم `firebase deploy --only functions` و`firebase deploy --only firestore:indexes`
- [ ] `node set-admin-claim.js admin@elwataniatv.dz` نجح
- [ ] `node seed-firestore.js` نجح
- [ ] **App Check**: فعّل Play Integrity API → سجّل التطبيق في App Check → (بعد نشر AAB موقّع) Monitor ثم Enforce على Firestore وAuth
- [ ] **تقييد مفاتيح API**: إعداد Web مخصص للوحة + تقييد مفتاح الويب بالـreferrers وتقييد مفتاح Android بالحزمة+SHA-1 (راجع `admin/DEPLOY.md`)
- [ ] نشر لوحة التحكم عبر Firebase Hosting والتحقق من رابطها الفعلي وتسجيل الدخول

---

## 🧪 اختبار سريع بعد كل شيء

1. افتح لوحة التحكم → سجّل دخول بحساب المسؤول
2. غيّر الخبر العاجل → **يظهر في التطبيق فوراً** (بعد ربط Firestore في التطبيق)
3. أرسل إشعار تجريبي → يصل للجهاز
4. نفّذ على جهازك:
   ```bash
   ./gradlew assembleDebug
   ```
   ثم ثبّت `app/build/outputs/apk/debug/app-debug.apk`

---

> ⚠️ **تذكير مهم**: التطبيق يقرأ المحتوى من Firestore عبر `FirebaseSync`. بعد وضع `google-services.json` الصحيح والتحقق من إعداد Firebase، تظهر تعديلات لوحة التحكم وفق صلاحيات Firestore والبيانات المنشورة. التفاعلات العميلية للقراءة فقط، ويجب أن يكون رابط البث الرسمي منشوراً في Firestore من مسؤول القناة. لا ينشئ التطبيق رابطاً احتياطياً أو مصدراً خارجياً غير معتمد.
