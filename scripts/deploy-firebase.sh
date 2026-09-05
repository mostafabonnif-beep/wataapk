#!/bin/bash
# ============================================================
# deploy-firebase.sh — نشر Firestore Rules + Indexes لمشروع الوطنية TV
# ────────────────────────────────────────────────────────────
# مجاني بالكامل على خطة Spark. يتحقق من Firebase CLI والتسجيل
# ثم ينشر:
#   1) firestore:rules   (القواعد المشددة — خط الدفاع الأساسي)
#   2) firestore:indexes (فهرس rate-limit للتعليقات)
# ثم يعرض خطوات التحقق.
#
# الاستخدام (من جذر المستودع — يوجد firebase.json هناك):
#   bash scripts/deploy-firebase.sh
#   bash scripts/deploy-firebase.sh --with-functions   # يتطلب خطة Blaze
# ============================================================
set -euo pipefail

PROJECT="elwataniatvapp"
WITH_FUNCTIONS="${1:-}"

echo "🔥 نشر Firebase — مشروع: $PROJECT"
echo "────────────────────────────────────────────"

# 1) Firebase CLI
if ! command -v firebase >/dev/null 2>&1; then
  echo "⚠️  Firebase CLI غير مثبت. أثبّته ثم أعد التشغيل:"
  echo "    npm install -g firebase-tools"
  exit 1
fi
echo "✔ Firebase CLI: $(firebase --version 2>/dev/null | head -1 || echo '?')"

# 2) تسجيل الدخول
if ! firebase projects:list >/dev/null 2>&1; then
  echo "🔑 يلزم تسجيل الدخول بحساب يملك المشروع (سيفتح المتصفح):"
  firebase login
fi
echo "✔ تم التحقق من تسجيل الدخول"

# 3) نشر القواعد + الفهارس (مجاني)
echo ""
echo "📤 نشر firestore:rules + firestore:indexes ..."
firebase deploy --only firestore:rules,firestore:indexes --project "$PROJECT"

# 4) Functions (اختياري — يتطلب Blaze)
if [[ "$WITH_FUNCTIONS" == "--with-functions" ]]; then
  echo ""
  echo "📤 نشر Cloud Functions (يتطلب خطة Blaze — ستُرفض على Spark):"
  firebase deploy --only functions --project "$PROJECT"
  echo "ℹ️  فهرس الـrate limit يُنشر مع الفهارس أعلاه."
fi

# 5) التحقق
cat <<'EOF'

✅ اكتمل النشر. تحقق الآن:
  1) Firebase Console → Firestore → Rules: تأكد أن النسخة المنشورة تحتوي
     commentCreatedAtIsServerTimestamp / withinCommentInterval / hasTooManyLinks
  2) علّق من التطبيق على أي برنامج → افتح المستند في Console →
     يجب أن يُنشأ بنجاح (createdAt تلقائي من السيرفر).
  3) أرسل تعليقاً يحوي رابطين → يجب رفضه.
  4) أرسل تعليقين خلال 30 ثانية من نفس الحساب → الثاني يُرفض.
  5) (App Check لاحقاً) Console → App Check → سجّل التطبيق بـ Play Integrity.

ملاحظة: نشر القواعد مجاني حتى على Spark. Cloud Functions (مع --with-functions)
تتطلب ترقية Blaze — قرار فوترة بيدك.
EOF
