# نسخ Firestore الاحتياطية

## الحالة في 2026-09-22

تعذّر إنشاء حاوية `gs://elwataniatvapp-backups` وإنشاء النسخة الأولى من هذه البيئة. واجهة Google Cloud Storage أعادت الخطأ `403 accountDisabled`: حساب الفوترة للمشروع `elwataniatvapp` غير مفعّل/غير موجود. لم تُنشأ نسخة احتياطية وهمية ولم تُعد المحاولة.

## التكرار بعد تفعيل الفوترة

بعد تفعيل حساب الفوترة وإنشاء الحاوية، نفّذ تصديراً كاملاً لكل مجموعات Firestore:

```bash
export GOOGLE_APPLICATION_CREDENTIALS="/path/to/elwataniatvapp-firebase-adminsdk.json"
gcloud firestore export gs://elwataniatvapp-backups/firestore/$(date -u +%Y-%m-%dT%H-%M-%SZ) \
  --project=elwataniatvapp
```

أو أنشئ الحاوية أولاً إن لم تكن موجودة:

```bash
gcloud storage buckets create gs://elwataniatvapp-backups --project=elwataniatvapp --location=US
```

تحقق من نجاح العملية:

```bash
gcloud firestore operations list --project=elwataniatvapp
```

يجب أن تظهر العملية بحالة `done: true` قبل اعتبار النسخة صالحة. لا تضع ملف حساب الخدمة أو أي كلمة مرور داخل المستودع.
