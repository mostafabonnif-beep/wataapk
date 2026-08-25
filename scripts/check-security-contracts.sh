#!/usr/bin/env bash
set -euo pipefail

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
rules="$root_dir/admin/firestore.rules"
gradle="$root_dir/app/build.gradle.kts"
ci="$root_dir/.github/workflows/ci.yml"
hosting_build="$root_dir/scripts/build-hosting.js"
hosting_config="$root_dir/admin/firebase.json"

require() {
  local pattern="$1"
  local file="$2"
  if ! grep -Fq "$pattern" "$file"; then
    echo "Missing expected contract in $file: $pattern" >&2
    exit 1
  fi
}

require "allow create: if isSignedIn()" "$rules"
require "match /likes/{programId}" "$rules"
require "match /push_tokens/{userUid}" "$rules"
require "request.resource.data.userUid == request.auth.uid" "$rules"
require "allow write: if isAdmin()" "$rules"
require "firebaseSyncScope?.isActive == true" "$root_dir/app/src/main/java/com/elwataniatv/app/data/repository/WataniaRepository.kt"
require "while (isActive)" "$root_dir/app/src/main/java/com/elwataniatv/app/ui/viewmodel/MainViewModel.kt"
require "RELEASE_STORE_PASSWORD" "$gradle"
require "bundleRelease" "$ci"

privacy_default="https://elwataniatvapp.web.app/privacy.html"
privacy_official="https://elwataniatv.dz/privacy"
privacy_default_files=(
  "$root_dir/app/src/main/java/com/elwataniatv/app/MainActivity.kt"
  "$root_dir/app/src/main/java/com/elwataniatv/app/data/model/RemoteAppConfig.kt"
  "$root_dir/app/src/main/java/com/elwataniatv/app/data/remote/FirestoreContentSync.kt"
  "$root_dir/app/src/main/java/com/elwataniatv/app/data/repository/WataniaRepository.kt"
  "$root_dir/scripts/seed-firestore.js"
  "$root_dir/admin/index.html"
)
for privacy_file in "${privacy_default_files[@]}"; do
  require "$privacy_default" "$privacy_file"
  if grep -Fq "$privacy_official" "$privacy_file"; then
    echo "Unavailable official privacy URL returned in runtime defaults: $privacy_file" >&2
    exit 1
  fi
done
runtime_privacy_files=(
  "$root_dir/app"
  "$root_dir/admin/index.html"
  "$root_dir/scripts"
)
if grep -RInF --exclude='check-security-contracts.sh' "$privacy_official" "${runtime_privacy_files[@]}" >/dev/null; then
  echo "Unavailable official privacy URL returned in runtime, admin UI, or seed paths" >&2
  exit 1
fi


require '"public": "hosting-dist"' "$hosting_config"
require '"predeploy": [' "$hosting_config"
require 'node ../scripts/build-hosting.js' "$hosting_config"
require '"source": "/"' "$hosting_config"
require '"source": "/admin"' "$hosting_config"
require '"source": "/admin/"' "$hosting_config"
require '"source": "/privacy"' "$hosting_config"
require '"source": "/privacy/"' "$hosting_config"
if grep -Fq '"source": "**"' "$hosting_config"; then
  echo "Catch-all Hosting rewrite must not be present" >&2
  exit 1
fi
require 'fs.cpSync' "$hosting_build"
require 'admin/hosting-dist/' "$root_dir/.gitignore"

if git -C "$root_dir" ls-files | grep -E '(^|/)([^/]+\.(jks|keystore|p12|p8|key)|google-services\.json)$' >/dev/null; then
  echo "Tracked signing/Firebase secret file detected" >&2
  exit 1
fi

echo "Security and release contract checks passed."
