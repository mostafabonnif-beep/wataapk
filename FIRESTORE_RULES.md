# قواعد أمان Firestore — الوطنية TV

> المصدر الفعلي للقواعد هو `admin/firestore.rules`. هذا الملف يشرح السلوك الحالي للقواعد المحلية، ولا يثبت أن النسخة المنشورة في Firebase Console مطابقة لها حتى تتم المقارنة أو الاختبار والنشر يدوياً.

## ملخص سريع

- القراءة العامة متاحة للمحتوى المعلن وبعض العدادات والإشعارات.
- كتابة المحتوى التحريري متاحة لحساب `admin: true` أو دور `editor`، بينما تبقى الإعدادات الحساسة للمسؤول فقط.
- أدوار Firebase Auth المدعومة هي `admin` و`editor` و`moderator`، مع استمرار دعم claim القديم `admin: true`.
- مستخدم التطبيق يسجّل دخولاً مجهولاً عبر Firebase Authentication قبل الكتابات المسموحة.
- التفاعلات التجميعية `live/reactions` و`likes` مغلقة للعميل حالياً؛ الكتابة فيها للمسؤول فقط إلى أن يُنشر جامع server-side موثوق.
- لا تُرفع أسرار أو service-account keys إلى المستودع.

## نماذج الهوية

| النوع | طريقة الدخول | الصلاحيات الحالية |
|---|---|---|
| المسؤول | حساب Firebase حقيقي مع `role: admin` أو `admin: true` | كامل صلاحيات الإدارة والمحتوى |
| المحرر | حساب Firebase حقيقي مع `role: editor` | إدارة `streams` و`archive` و`websites` و`social` و`epg` فقط |
| المشرف | حساب Firebase حقيقي مع `role: moderator` | مخصص لمسارات الإشراف عند تفعيلها؛ لا يكتب المحتوى التحريري |
| مستخدم التطبيق | Firebase Anonymous Authentication | قراءة عامة، وإرسال تعليق أو feedback وتسجيل heartbeat/FCM token ضمن ملكيته |
| زائر غير موثق | بلا جلسة Firebase | قراءة المسارات التي تسمح بها القواعد فقط؛ لا توجد كتابة |

تعيين الدور يتم خارج التطبيق عبر `scripts/set-admin-claim.js <email-or-uid> [admin|editor|moderator]`. بعد تغييره يجب تسجيل الخروج والدخول مجدداً أو تجديد ID token. لا تُحفظ service-account keys في Git.

## مصفوفة الصلاحيات الحالية

| المسار | القراءة | الكتابة | الملاحظات |
|---|---|---|---|
| `streams/{id}` | عامة | admin أو editor | محتوى البث يأتي من Firestore؛ لا يوجد fallback بث مخترع في التطبيق |
| `archive/{id}` | عامة | admin أو editor | — |
| `websites/{id}` | عامة | admin أو editor | — |
| `social/{id}` | عامة | admin أو editor | — |
| `epg/{id}` | عامة | admin أو editor | — |
| `config/{id}` | عامة | admin فقط | — |
| `ad_banners/{id}` | عامة | admin فقط | — |
| `notifications/{id}` | عامة | admin فقط | الإشعارات داخل التطبيق تُعرض للمستخدمين |
| `live/reactions` | عامة | admin فقط | العدّاد التجميعي مغلق للعميل حتى نشر جامع موثوق |
| `live/reactions/user_reactions/{userUid}` | المستخدم نفسه قراءة؛ admin قراءة ضمن صلاحياته | admin فقط | المسار محجوز للجامع/الخادم حالياً |
| `likes/{programId}` | عامة | admin فقط | لا يوجد voter ledger وجامع server-side في الإصدار الحالي |
| `devices/{userUid}` | admin فقط | المستخدم نفسه إنشاء/تحديث؛ admin حذف | المفتاح هو Firebase Auth UID، والتحديث مقيد بفاصل 5 دقائق |
| `feedback/{id}` | admin فقط | المستخدم الموثق إنشاء؛ admin تحديث/حذف | الحقول والحجم مقيدان بـ `hasOnly` والتحقق النوعي |
| `push_tokens/{userUid}` | admin فقط | المستخدم نفسه إنشاء/تحديث؛ admin حذف | مفتاح الوثيقة هو UID، وليس FCM token أو installation ID |
| `programs/{programId}` | عامة | admin فقط | — |
| `programs/{programId}/comments/{commentId}` | عامة | المستخدم الموثق إنشاء؛ admin حذف | لا يوجد تعديل نص عام في الإصدار الحالي |
| `programs/{programId}/comments/{commentId}/reactions/{userUid}` | عامة | admin فقط | تفاعلات التعليقات مغلقة للعميل حتى نشر جامع موثوق |
| `users/{uid}` و`users/{uid}/preferences/{id}` | المالك فقط | المالك فقط | `request.auth.uid == uid` |
| `activity_log/{id}` | admin فقط | admin فقط | سجل تدقيق لوحة الإدارة |
| `admin-media/{path}` في Storage | عامة | admin فقط | صور/فيديو/صوت حتى 10MB؛ الأنواع تُفحص في `storage.rules` |
| أي مسار آخر | مرفوض | مرفوض | catch-all deny |

## التحقق الموجود في الكتابات

القواعد تتحقق من المصادقة والملكية وشكل البيانات، ومن ذلك:

- `userUid` يساوي `request.auth.uid` في المسارات المرتبطة بالمستخدم.
- `deviceId`/`installationId` نص بطول محدود.
- نص التعليق لا يتجاوز 500 حرف، والحقول المسموحة محددة صراحةً.
- توكن FCM نص بطول محدود، والمنصة الحالية `android`.
- heartbeat لا يُحدّث أكثر من مرة كل خمس دقائق وفق `lastSeen` السابق.
- الكتابة الإدارية تتطلب claim صالحاً؛ `admin` يدير كل شيء، و`editor` يدير المحتوى التحريري فقط، مع catch-all deny للمسارات غير المعرّفة.
- رفع الوسائط يتطلب admin claim، وحجم الملف أقل من 10MB ونوع MIME صورة أو فيديو أو صوت.

> ملاحظة مهمة: وجود فحص client-side للكلمات (`ProfanityFilter.kt`) لا يساوي حماية server-side — لكن القواعد الآن تفرض خادمياً: `createdAt` كـ server timestamp (`== request.time`)، تعليق واحد كل 30 ثانية عبر `users/{uid}.lastCommentAt`، رابط واحد كحد أقصى، و500 حرف. هذه القواعد مختبرة عبر Emulator (28/28 في آخر تشغيل). Cloud Functions غير منشورة (خطة Spark) — الحماية الدلالية الكاملة تبقى اختيارية عند الترقية إلى Blaze.

## النشر والتحقق

1. راجع `admin/firestore.rules` باعتباره المصدر الوحيد للقواعد.
2. قارن محتواه مع النسخة الظاهرة في Firebase Console أو اختبره عبر Firestore Emulator/Playground.
3. انشر القواعد يدوياً من مجلد `admin` عند توفر Firebase CLI وتسجيل الدخول:
   ```bash
   firebase use elwataniatvapp
   firebase deploy --only firestore:rules
   ```
4. اختبر بعد النشر على الأقل:
   - مستخدم مجهول يقرأ المحتوى ويُرفض عند الكتابة في `likes` و`live/reactions`.
   - مستخدم موثق ينشئ تعليقاً بالحقول الصحيحة ويُرفض عند إضافة حقل غير مسموح.
   - مستخدم يحاول الكتابة في `devices/{uid}` أو `push_tokens/{uid}` بمعرّف UID مختلف ويُرفض.
   - حساب admin يقرأ ويكتب المسارات الإدارية.
   - أي مسار غير معروف يُرفض.

## حدود هذه الوثيقة

- لا تثبت هذه الوثيقة حالة Rules المنشورة أو App Check Enforcement في Firebase Console.
- لا تثبت نجاح Play Integrity أو وجود custom claim لحساب إداري بعينه.
- لا تعني أن Cloud Functions منشورة.
- عند تعديل `admin/firestore.rules` يجب تحديث هذا الملف ومراجعة اختبارات القواعد قبل الإطلاق.
