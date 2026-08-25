#!/bin/bash
# ============================================================
# إنشاء Keystore لتوقيع تطبيق الوطنية TV
# ============================================================
# هذا السكريبت ينشئ ملف keystore لتوقيع APK/AAB.
# التوقيع مطلوب لنشر التطبيق على Google Play Store
# وللتخلص من تحذير Play Protect.
#
# التشغيل:
#   chmod +x scripts/generate-keystore.sh
#   ./scripts/generate-keystore.sh
# ============================================================

KEYSTORE_DIR="keystore"
KEYSTORE_FILE="$KEYSTORE_DIR/release.keystore"
ALIAS="watania_release"
VALIDITY=10000  # 27 سنة

if [ -f "$KEYSTORE_FILE" ]; then
    echo "⚠️  الملف $KEYSTORE_FILE موجود مسبقاً."
    echo "   إذا تريد إنشاء واحد جديد، احذف القديم أولاً:"
    echo "   rm $KEYSTORE_FILE"
    exit 1
fi

mkdir -p "$KEYSTORE_DIR"

echo "📦 إنشاء Keystore لتطبيق الوطنية TV..."
echo ""
echo "أدخل كلمة مرور الـ keystore (احفظها جيداً):"
read -s STORE_PASS
echo ""
echo "أدخل كلمة مرور المفتاح (أو نفس كلمة المرور السابقة):"
read -s KEY_PASS
echo ""

keytool -genkey -v \
    -keystore "$KEYSTORE_FILE" \
    -alias "$ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity "$VALIDITY" \
    -dname "CN=الوطنية TV, OU=Development, O=Elwatania TV, L=Algiers, ST=Algiers, C=DZ" \
    -storepass "$STORE_PASS" \
    -keypass "$KEY_PASS"

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ تم إنشاء keystore بنجاح!"
    echo "   الملف: $KEYSTORE_FILE"
    echo ""
    echo "📋 لبناء نسخة موقعة، مرّر القيم عبر مدير أسرار أو بيئة محمية فقط:"
    echo "   RELEASE_STORE_FILE='$KEYSTORE_FILE' RELEASE_STORE_PASSWORD='[محمي]' RELEASE_KEY_ALIAS='$ALIAS' RELEASE_KEY_PASSWORD='[محمي]' ./gradlew bundleRelease"
    echo ""
    echo "لا تُدخل كلمات المرور مباشرة في سجل CI أو ترفع ملف keystore إلى Git."
    echo ""
    echo "⚠️  احفظ كلمة المرور في مكان آمن! فقدانها = فقدان القدرة على تحديث التطبيق!"
else
    echo "❌ فشل إنشاء keystore"
    exit 1
fi
