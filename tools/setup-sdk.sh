#!/usr/bin/env bash
set -euo pipefail
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/usr/local/lib/android/sdk}}"
MANAGER="$(find "$SDK/cmdline-tools" -type f -path '*/bin/sdkmanager' | sort -V | tail -1)"
test -x "$MANAGER"
if [ -n "${GITHUB_ENV:-}" ]; then
  echo "ANDROID_HOME=$SDK" >> "$GITHUB_ENV"
  echo "ANDROID_SDK_ROOT=$SDK" >> "$GITHUB_ENV"
  echo "$SDK/platform-tools" >> "$GITHUB_PATH"
fi
yes | "$MANAGER" --licenses >/dev/null || true
"$MANAGER" 'platforms;android-36' 'build-tools;35.0.0' 'platform-tools'
