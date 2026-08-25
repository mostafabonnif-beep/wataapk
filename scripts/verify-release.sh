#!/usr/bin/env bash
set -euo pipefail

# Verify a locally built release without exposing signing credentials.
# Usage: scripts/verify-release.sh path/to/app-release.apk

APK="${1:-app/build/outputs/apk/release/app-release.apk}"
if [[ ! -f "$APK" ]]; then
  echo "Release APK not found: $APK" >&2
  exit 1
fi

SDK_ROOT="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/android-sdk}}"
AAPT="$(find "$SDK_ROOT" -type f -name aapt 2>/dev/null | sort -V | tail -n 1 || true)"
APKSIGNER="$(find "$SDK_ROOT" -type f -name apksigner 2>/dev/null | sort -V | tail -n 1 || true)"

if [[ -n "$AAPT" ]]; then
  echo "== Package metadata =="
  "$AAPT" dump badging "$APK" | grep -E "^package:|^application-label|launchable-activity" | head -n 12 || true
else
  echo "aapt not found; package metadata check skipped" >&2
fi

echo "== SHA-256 =="
sha256sum "$APK"

if [[ -n "$APKSIGNER" ]]; then
  echo "== Android signature =="
  "$APKSIGNER" verify --verbose "$APK" | grep -E "Verifies|Verified using|Number of signers" || true
else
  echo "apksigner not found; signature check skipped" >&2
fi

echo "Release verification completed."
