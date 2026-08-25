# دليل نشر تطبيق الوطنية TV على Google Play Store

دليل عملي خطوة بخطوة لنشر تطبيق Android الحالي على متجر Google Play. هذا الدليل لا يفترض وجود AAB جاهز؛ يجب بناء إصدار `7.7.0` موقّع والتحقق منه قبل الرفع.

---

## ١. ما تحتاجه قبل البدء

> **حالة هذا المستودع:** رابط البث الرسمي الحقيقي وFirebase production والتوقيع النهائي ما زالت متطلبات خارجية. لا يوجد keystore أو سر توقيع داخل المستودع؛ إعداد release signing يقرأ فقط `RELEASE_STORE_FILE` و`RELEASE_STORE_PASSWORD` و`RELEASE_KEY_ALIAS` و`RELEASE_KEY_PASSWORD` من Gradle properties أو متغيرات بيئية محمية.

| البند | التفاصيل |
|------|---------|
| 💳 **حساب Google Play Developer** | $25 رسم تسجيل لمرة واحدة، مدى الحياة |
| 📧 **حساب Google** | يفضّل حساب مخصّص للقناة (مثلاً `dev@elwataniatv.dz`) |
| 🆔 **بطاقة هوية شخصية** | لتوثيق الحساب (للأفراد) |
| 🏢 **سجل تجاري** | للحسابات التجارية (يفضّل) |
| 📦 **AAB إصدار Android** | يجب بناؤه وتوقيعه للإصدار `7.7.0` (`versionCode 8`)؛ لا يوجد artifact جاهز في المستودع |
| 🖼️ **أيقونة التطبيق** | `store-assets/icon-512.png` موجودة؛ راجعها قبل الرفع |
| 🖼️ **Feature Graphic** | `store-assets/feature-graphic.png` موجودة (1024×500) |
| 📸 **Screenshots** | لا توجد لقطات شاشة محفوظة في المستودع؛ يجب التقاطها من التطبيق الفعلي (1080×1920 لـ Android) |
| 📝 **سياسة الخصوصية** | الرابط التشغيلي: `https://elwataniatv-channel.vercel.app/privacy` |

---

## ٢. تسجيل حساب Play Console

1. اذهب إلى [play.google.com/console](https://play.google.com/console)
2. سجّل دخول بحساب Google
3. اقبل **Google Play Developer Distribution Agreement**
4. ادفع $25 (Visa/Mastercard) — ⚠️ **لا تردّ بعد الدفع**
5. أكمل بياناتك الشخصية:
   - الاسم القانوني
   - البريد + رقم الهاتف
   - العنوان

⏱️ **التحقّق من الهوية**: قد يطلب Google إثبات هوية (صورة بطاقة + selfie). يستغرق 24-72 ساعة.

---

## ٣. إنشاء التطبيق في Play Console

1. اضغط **"Create app"**
2. املأ:
   - **App name**: `الوطنية TV`
   - **Default language**: `Arabic (العربية)`
   - **App or game**: `App`
   - **Free or paid**: `Free`
   - وافق على شروط: التطبيق متوافق مع سياسات Play، ولن يستهدف الأطفال (إلا إذا كان كذلك)
3. اضغط **Create app**

---

## ٤. إعداد صفحة المتجر (Store Listing)

### نص عربي (للجمهور الجزائري)

**App name** (30 حرف max):
```
الوطنية TV
```

**Short description** (80 حرف max):
```
الوطنية TV: الأرشيف، الأخبار، المواقع الرسمية، والبث عند توفره
```

**Full description** (4000 حرف max):
```
📺 الوطنية TV — تطبيق Android الرسمي للقناة الوطنية الجزائرية

يوفّر التطبيق الوصول إلى أرشيف البرامج، الأخبار، المواقع والصفحات الرسمية، وإعدادات المستخدم. يستخدم البث المباشر رابط HLS الرسمي الأساسي عند توفر المصدر، مع عدم ادعاء وجود رابط احتياطي خارجي غير معتمد.

✨ المميزات الحالية:
📂 أرشيف البرامج والبحث والفلترة
🔔 تنبيهات ومحتوى إخباري عند توفره
🌐 تصفح المواقع الإخبارية الرسمية
📱 صفحات الوسائط الاجتماعية الرسمية للقناة
🎥 قناة YouTube الرسمية
⚙️ وضع ليلي وإعدادات قابلة للتخصيص
📤 مشاركة المحتوى بسهولة

ملاحظة مهمة: لا يضمن التطبيق بثاً HD أو بثاً متواصلاً 24/7. جودة البث وتوفره يعتمدان على المصدر الرسمي، ولا يوجد رابط احتياطي خارجي معتمد حالياً.

📞 للتواصل: contact@elwataniatv.dz
🌐 الموقع: https://elwataniatv.dz
```

### نص إنجليزي (للوصول الدولي)

**Short description**:
```
Algerian National TV app: archive, news, official links, and live when enabled
```

**Full description**:
```
📺 Elwataniatv TV — The official Android app for Algeria's National TV channel

Access the channel's program archive, news, official websites and social pages, plus user settings. The live screen uses the official primary HLS stream when available; no unapproved external backup stream is advertised.

✨ Current features:
📂 Program archive with search and filtering
🔔 Notifications and news content when available
🌐 Official news websites
📱 Official social media pages
🎥 Official YouTube channel
⚙️ Dark mode and customizable settings
📤 Easy content sharing

Important: the app does not promise HD or continuous 24/7 streaming. Stream availability and quality depend on the official source, and no unapproved external backup stream is advertised.

📞 Contact: contact@elwataniatv.dz
🌐 Website: https://elwataniatv.dz
```

### الأيقونة (Icon)
- الأيقونة الرسمية موجودة في `store-assets/icon-512.png` بمقاس 512×512، والنسخة الكبيرة في `store-assets/icon-1024.png`. راجع الشعار قبل الرفع ولا تستبدله بملف placeholder.

### Feature Graphic
- `store-assets/feature-graphic.png` موجودة بمقاس 1024×500.
- راجعها قبل الرفع وتأكد من أنها تمثل التطبيق الفعلي ولا توحي ببث HD أو بث دائم.

### Screenshots (مطلوبة قبل الإرسال)
- لا توجد screenshots محفوظة حالياً في المستودع.
- التقط 2-8 لقطات من النسخة Android الفعلية بعد بناء وتشغيل الإصدار، بدقة 1080×1920 (portrait) أو 1920×1080 (landscape).
- لا تستخدم لقطات وهمية ولا تعرض بثاً غير متاح على أنه يعمل.
- اقتراحات للقطات: الأرشيف، شاشة المواقع، صفحات التواصل الاجتماعي، والإعدادات.

### فيديو ترويجي (اختياري)
- رفع رابط YouTube بطول 30s-2min
- يزيد التحويلات بنسبة 30%

---

## ٥. تصنيفات التطبيق

### Category
- **Application category**: `News & Magazines`
- **Tags**: `News`, `Live TV`, `Streaming`

### Content Rating (مهم — يجب إكماله للنشر)
1. اضغط **Content rating → Start questionnaire**
2. أجب على الأسئلة بصدق:
   - **Email**: contact@elwataniatv.dz
   - **Category**: News
   - **Violence**: لا (إلا إذا كانت الأخبار تحوي عنفاً صريحاً → اختر مناسب)
   - **Sexual content**: لا
   - **Profanity**: لا
   - **Drugs**: لا
   - **Gambling**: لا
   - **User-generated content**: لا (أو نعم إذا كنت تستعمل WebView لمواقع تسمح بتعليقات)
   - **Location sharing**: لا
3. توقّع التصنيف: `Everyone` أو `Teen`

### Target audience
- **Age groups**: `13+` (لا تختر "للأطفال" إلا إذا كنت تتبع COPPA)

- **Privacy policy**:
- استخدم رابط سياسة الخصوصية التشغيلي: `https://elwataniatv-channel.vercel.app/privacy`
- المصدر النصي الموحد هو `privacy.html`، وتلخصه النسخة العربية `PRIVACY_POLICY_AR.md`.

### Data safety
أكمل النموذج بصدق وفق إعدادات الإصدار المنشور فعلياً:
- **Data collected**: معرّف Firebase مجهول، معرّف تثبيت الجهاز، توكن FCM، التعليقات/الملاحظات التي يرسلها المستخدم، وبيانات تقنية لازمة للتشغيل؛ لا يطلب التطبيق اسماً أو بريداً أو كلمة مرور.
- **Data shared with third parties**: تُعالج بعض البيانات عبر Google Firebase لتوفير المصادقة وFirestore وFCM؛ راجع سياسة الخصوصية قبل الإقرار النهائي.
- **Data is encrypted in transit**: ✅ نعم حيث يدعم المصدر HTTPS/TLS.
- **Users can request data deletion**: ✅ نعم عبر contact@elwataniatv.dz، مع توضيح نطاق الحذف في نموذج Play.

---

## ٦. بناء ورفع AAB Android

هذا المستودع تطبيق Android أصلي مبني بـ Kotlin وJetpack Compose؛ شغّل Gradle من جذر المستودع:

```bash
./gradlew bundleRelease
# ينتج: app/build/outputs/bundle/release/app-release.aab
```

الإصدار الحالي هو `7.7.0` (`versionCode 8`). لا يوجد AAB أو APK إصدار جاهز داخل المستودع. لا يُبنى AAB موقّعاً إلا بعد توفير متغيرات التوقيع الأربعة عبر Gradle properties أو البيئة المحمية. أمر `bundleRelease` في CI فحص مشروط ويتخطى نفسه عند غيابها؛ لا تضع keystore أو كلمات المرور في GitHub أو المستودع.

### رفع الـ AAB
1. **Play Console → Release → Production → Create new release**
2. اضغط **Upload** وارفع `app-release.aab`
3. **Release name**: `7.7.0 (8)` أو الاسم الذي يعرضه Play Console
4. **Release notes** (يظهر للمستخدمين عند التحديث):
   ```
   ✨ تحديث الوطنية TV:
   - أرشيف وبرامج مع بحث وفلترة
   - تحسينات في الإشعارات والإعدادات
   - شاشة بث تُفعّل عند توفر رابط رسمي
   ```

### أول رفع — App Signing
عند أول رفع، Google سيطلب:
- **Use Play App Signing** (موصى به).
- استخدم keystore الذي تديره خارج المستودع، أو دع Google ينشئ مفتاح التطبيق وفق إعدادات Play Console.

⚠️ لا تُنشئ أو ترفع keystore داخل هذا المستودع، ولا تضع كلمة المرور في ملفات المشروع. خزّن مواد التوقيع في مدير أسرار/بيئة محمية، واحتفظ بنسخة احتياطية آمنة وفق سياسة فريقك.

---

## ٧. تجربة داخلية (Internal Testing)

قبل النشر للجمهور، جرّب على دائرتك:

1. **Play Console → Internal testing → Create new release**
2. ارفع AAB
3. أضف **testers** (إيميلاتهم)
4. شارك رابط الاختبار معهم
5. اطلب تقييمات وملاحظات
6. صحّح الأخطاء قبل النشر العام

---

## ٨. النشر للجمهور

1. **Production → Create new release**
2. ارفع AAB (نفس النسخة من Internal testing إذا سار جيداً)
3. اضغط **Review release**
4. تأكّد من أن جميع الأقسام مكتملة (✅ خضراء)
5. اضغط **Start rollout to production**

⏱️ **مدة المراجعة**: عادة 1-7 أيام (أحياناً أقصر للتطبيقات الموثقة)

---

## ٩. بعد النشر

### المراقبة
- **Statistics**: تثبيتات، إلغاءات، تقييمات
- **Crashes & ANRs**: أخطاء التشغيل (إذا تجاوزت 1.09% Google ينخفض ترتيبك)
- **Reviews**: ردّ على التقييمات (يحسّن ترتيبك)

### التحديثات
كل تحديث:
1. ارفع `versionCode` و`versionName` في `app/build.gradle.kts`.
2. أعد البناء: `./gradlew bundleRelease` بعد توفير توقيع release خارج المستودع.
3. ارفع AAB الجديد عبر **Production → Create new release**

### الترويج
- ✅ **رابط مباشر**: `https://play.google.com/store/apps/details?id=com.elwataniatv.app`
- ✅ **Badge HTML** للموقع: [play.google.com/intl/en_us/badges](https://play.google.com/intl/en_us/badges/)
- ✅ **شارك على وسائل التواصل** (Facebook، Instagram، Twitter)
- ✅ **اعرض في القناة** (banner أو شاشة OBT)

---

## ١٠. ASO (App Store Optimization)

لزيادة تنزيلات التطبيق:

### كلمات مفتاحية (في الوصف العربي)
- البث المباشر، قناة الوطنية، تلفزيون الجزائر، أخبار عاجلة، الجزيرة، الجزائر، تلفزيون مباشر، live tv

### تشجيع التقييمات
- اطلب من المشاهدين تقييم التطبيق بعد فتحه 5 مرات (يمكن إضافة هذه الميزة لاحقاً)
- ردّ على كل تقييم — حتى السلبية (يظهر للمستخدمين الجدد)

### تحديثات منتظمة
- Google يفضّل التطبيقات التي يتم تحديثها كل شهرين على الأقل
- حتى لو كان تحديث صغير (إصلاح خطأ بسيط)، اعمل releases منتظمة

---

## ١١. مشاكل شائعة وحلولها

| المشكلة | الحل |
|--------|------|
| **مصادقة Firebase لا تعمل** | فعّل Anonymous Auth في Firebase production وتأكد من وجود `google-services.json` الصحيح؛ التطبيق لا يستخدم Google Sign-In. |
| **رفض ملف الإصدار** | ابنِ `bundleRelease` من جذر المستودع وارفع AAB موقّعاً من الإصدار الحالي |
| **"App is not optimized for tablets"** | راجع التحذير على أجهزة فعلية؛ هذا المستودع يخص تطبيق Android فقط |
| **"Privacy policy required"** | استخدم `https://elwataniatv-channel.vercel.app/privacy` بعد التحقق من HTTP 200 |
| **"Target SDK 35 required"** | في `app/build.gradle.kts`: `targetSdk = 35` |
| **"Use Play App Signing"** | اقبل — Google يحفظ المفتاح بأمان |
| **رفض المراجعة بسبب "spammy content"** | راجع وصف المتجر، أزل التكرار، استعمل وصفاً طبيعياً |
| **"Misleading icon"** | الأيقونة يجب أن تعكس محتوى التطبيق فعلياً |

---

## ١٢. ملحق: تحديثات لاحقة

### إضافة Firebase Analytics (اختياري، بعد قرار المنتج)
1. اتبع `FIREBASE_SETUP.md`.
2. ضع `google-services.json` الصحيح في مسار Android المخصص له، ولا ترفعه إذا كان يحتوي أسرار بيئة غير مناسبة.
3. أضف اعتماد Android/Firebase الرسمي فقط إلى Gradle بعد مراجعة سياسة الخصوصية.
4. أعد تشغيل اختبارات Gradle ثم ابنِ الإصدار.

### إضافة AdMob (اختياري، خارج نطاق الإصدار الحالي)
لا تضف SDK أو إعلانات إلى هذا الإصدار دون قرار منتج ومراجعة Data safety وسياسة الخصوصية.

### إضافة Push Notifications (Firebase Cloud Messaging)
- جاهز في الكود! فقط أكمل إعداد Firebase
- ادفع إشعارات من Firebase Console أو من لوحة admin

---

## ١٣. روابط مفيدة

- 📖 [Google Play Console Help](https://support.google.com/googleplay/android-developer)
- 📖 [Android App Bundle](https://developer.android.com/guide/app-bundle)
- 📖 [Android Developer Distribution Agreement](https://play.google.com/about/developer-distribution-agreement.html)
- 🛠️ [Play Console Best Practices](https://developer.android.com/distribute/best-practices)
- 📊 [Firebase Console](https://console.firebase.google.com)
- 💰 [AdMob](https://admob.google.com)

---

## ١٤. خلاصة الـ checklist السريع

قبل ضغط "Start rollout":

- [ ] حساب Play Console مفعّل (دفعت $25 ووثّقت هويتك)
- [ ] أنشأت تطبيق في Play Console
- [ ] أكملت Store listing (نص عربي + إنجليزي)
- [ ] رفعت أيقونة 512×512
- [ ] رفعت Feature Graphic 1024×500
- [ ] رفعت 4-8 screenshots
- [ ] أكملت Content rating
- [ ] أكملت Target audience
- [ ] رفعت Privacy Policy URL
- [ ] أكملت Data safety form
- [ ] رفعت AAB في Production track
- [ ] كتبت Release notes
- [ ] خزّنت مواد التوقيع ونسخة keystore خارج المستودع في مكان آمن
- [ ] تحققت من رابط البث الرسمي الحقيقي على جهاز فعلي
- [ ] تحققت من Firebase production ونشرت القواعد والـ Functions
- [ ] جرّبت Internal testing مع 5+ أشخاص
- [ ] ضغطت "Start rollout to production"
- [ ] انتظرت 1-7 أيام للموافقة

🎉 **مبروك — تطبيقك أصبح متاحاً لكل الجزائر!**
