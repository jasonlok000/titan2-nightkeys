#!/usr/bin/env bash
# Sign the exact unsigned APK built by this workflow's CI; never use a debug key.
set -euo pipefail
umask 077
MODE="${1:-release}"
case "$MODE" in bootstrap|release) ;; *) echo 'Invalid release mode'; exit 1;; esac
: "${NIGHTKEYS_KEYSTORE:?Missing permanent keystore}"
: "${NIGHTKEYS_STORE_PASSWORD:?Missing signing password}"
: "${NIGHTKEYS_KEY_PASSWORD:?Missing key password}"
: "${NIGHTKEYS_KEY_ALIAS:?Missing key alias}"
VERSION="$(sed -n 's/^versionName=//p' version.properties)"
CODE="$(sed -n 's/^versionCode=//p' version.properties)"
[[ "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[a-z]+\.[0-9]+)?$ ]]
[[ "$CODE" =~ ^[0-9]+$ ]]
if [[ "${GITHUB_REF:-}" == refs/tags/* ]]; then test "$GITHUB_REF" = "refs/tags/v$VERSION"; fi
if [ "$MODE" = bootstrap ]; then
  test "$VERSION" = '1.0.0-beta.1'
  test ! -f .signing/certificate.sha256
else
  test -s .signing/certificate.sha256
fi
TOOLS="${ANDROID_HOME:-/usr/local/lib/android/sdk}/build-tools/35.0.0"
UNSIGNED='app/build/outputs/apk/release/app-release-unsigned.apk'
test -s "$UNSIGNED"
mkdir -p deliverables
APK="deliverables/NightKeys-$VERSION.apk"
"$TOOLS/zipalign" -f 4 "$UNSIGNED" "$RUNNER_TEMP/nightkeys-aligned.apk"
"$TOOLS/apksigner" sign --ks "$NIGHTKEYS_KEYSTORE" --ks-key-alias "$NIGHTKEYS_KEY_ALIAS" \
  --ks-pass env:NIGHTKEYS_STORE_PASSWORD --key-pass env:NIGHTKEYS_KEY_PASSWORD \
  --out "$APK" "$RUNNER_TEMP/nightkeys-aligned.apk"
"$TOOLS/apksigner" verify --verbose --print-certs "$APK" > deliverables/signature-verification.txt
"$TOOLS/zipalign" -c -v 4 "$APK" > deliverables/alignment-verification.txt
"$TOOLS/aapt" dump badging "$APK" > "$RUNNER_TEMP/badging.txt"
grep -q "name='com.everyonesniche.nightkeys' versionCode='$CODE' versionName='$VERSION'" "$RUNNER_TEMP/badging.txt"
! grep -q application-debuggable "$RUNNER_TEMP/badging.txt"
keytool -exportcert -keystore "$NIGHTKEYS_KEYSTORE" -storepass:env NIGHTKEYS_STORE_PASSWORD \
  -alias "$NIGHTKEYS_KEY_ALIAS" -file deliverables/nightkeys-signing-certificate.der >/dev/null
CERT="$(sha256sum deliverables/nightkeys-signing-certificate.der | cut -d' ' -f1)"
grep -q "Signer #1 certificate SHA-256 digest: $CERT" deliverables/signature-verification.txt
if [ "$MODE" = release ]; then test "$(tr -d '\r\n ' < .signing/certificate.sha256)" = "$CERT"; fi
printf '%s\n' "$CERT" > deliverables/signing-certificate.sha256
cp docs/SETUP.md deliverables/SETUP.md
cp docs/SAFETY.md deliverables/SAFETY.md
git archive --format=zip --prefix="NightKeys-$VERSION/" -o "deliverables/NightKeys-$VERSION-source.zip" "$GITHUB_SHA"
export VERSION CODE CERT
python3 - <<'PY'
import hashlib,json,os,pathlib
p=pathlib.Path('deliverables')
a=p/f"NightKeys-{os.environ['VERSION']}.apk"
meta={'version':os.environ['VERSION'],'versionCode':int(os.environ['CODE']),
      'applicationId':'com.everyonesniche.nightkeys','debuggable':False,
      'commit':os.environ['GITHUB_SHA'],'workflowRun':os.environ['GITHUB_RUN_ID'],
      'apkSha256':hashlib.sha256(a.read_bytes()).hexdigest(),'certificateSha256':os.environ['CERT'],
      'validation':'JVM, Android build/lint, real non-root Shizuku emulator tests and website browser tests passed before signing.',
      'hardwareStatus':'0.9.7 baseline owner-verified on Elite V02.00.04. This newly signed community build still needs its physical setup check.',
      'limitations':'Persistent large timeout; no independent watchdog; restore before removal.'}
(p/'BUILD-INFO.json').write_text(json.dumps(meta,indent=2)+'\n')
PY
(cd deliverables && sha256sum ./* > SHA256SUMS.txt)
printf 'version=%s\ntag=v%s\n' "$VERSION" "$VERSION" >> "$GITHUB_OUTPUT"
