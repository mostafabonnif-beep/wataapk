# تقرير فحص روابط Firestore

تاريخ الفحص: 2026-09-22 الساعة 14:56 UTC

تمت قراءة Firestore في مشروع `elwataniatvapp` بحساب خدمة خاص، من دون تعديل أي مستند، ثم فحص الروابط في المجموعات المطلوبة: `streams` و`archive` و`social` و`websites`.

## النتيجة

- المستندات المفحوصة: 8
- الروابط المفحوصة: 16
- أخطاء HTTP 404: لا يوجد
- أخطاء اتصال: لا يوجد
- روابط HLS: استجابت HTTP 200 وتبدأ بـ `#EXTM3U`
- صور الشعارات: استجابت HTTP 206، وهو رد صحيح لتحميل جزئي/مدى bytes

## روابط البث والمحتوى

| المجموعة | المستند | الحقل | الحالة |
|---|---|---|---|
| `streams` | `live_main` | `url` | HTTP 200، وملف HLS صالح |
| `streams` | `live_main` | `logoUrl` | HTTP 206 |
| `streams` | `yt_djazair` | `url` | HTTP 200 |
| `streams` | `yt_djazair` | `logoUrl` | HTTP 206 |
| `streams` | `yt_sport` | `url` | HTTP 200 |
| `streams` | `yt_sport` | `logoUrl` | HTTP 206 |
| `streams` | `yt_watania` | `url` | HTTP 200 |
| `streams` | `yt_watania` | `logoUrl` | HTTP 206 |
| `archive` | `program_0` | `youtubeUrl` | HTTP 200 |
| `archive` | `program_0` | `thumbnailUrl` | HTTP 200 |

## الروابط الاجتماعية والمواقع

| المجموعة | المستندات | الحالة |
|---|---|---|
| `social` | `s1`, `s2`, `s3` | HTTP 200 |
| `websites` | `w1`, `w2`, `w3` | HTTP 200 |

رابط Facebook يعيد تحويل المتصفح إلى صفحة تسجيل دخول Facebook، وهذا سلوك المنصة وليس رابطاً ميتاً. جميع الروابط الأخرى أعادت الوجهة المتوقعة أو استجابة ناجحة.

## الخلاصة

لا توجد حالياً روابط Firestore ميتة أو 404 في هذه المجموعات. مصدر البث المباشر قابل للوصول ويعيد playlist HLS حقيقية. يبقى اختبار التشغيل على جهاز Android حقيقي مهماً لأن صلاحية HTTP لا تضمن أن كل مزود خارجي يسمح بالتشغيل داخل كل شبكة أو جهاز.
