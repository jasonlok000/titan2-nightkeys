#!/usr/bin/env bash
set -euo pipefail
mkdir -p build/evidence
trap 'timeout 10s adb logcat -d -t 1500 > build/evidence/emulator-logcat.log 2>&1 || true' EXIT
adb wait-for-device
test "$(adb shell id -u | tr -d '\r')" = 2000
adb install -r -t app/build/outputs/apk/debug/app-debug.apk
adb install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
curl --fail --location --retry 3 -o /tmp/shizuku.apk 'https://github.com/RikkaApps/Shizuku/releases/download/v13.6.0/shizuku-v13.6.0.r1086.2650830c-release.apk'
adb install -r /tmp/shizuku.apk
unzip -p /tmp/shizuku.apk lib/x86_64/libshizuku.so > /tmp/nightkeys-shizuku-start
adb push /tmp/nightkeys-shizuku-start /data/local/tmp/nightkeys-shizuku-start
adb shell chmod 755 /data/local/tmp/nightkeys-shizuku-start
adb shell monkey -p moe.shizuku.privileged.api -c android.intent.category.LAUNCHER 1 >/dev/null
APK="$(adb shell pm path moe.shizuku.privileged.api | tr -d '\r' | sed -n 's/^package://p' | head -1)"
test -n "$APK"
adb shell /data/local/tmp/nightkeys-shizuku-start --apk="$APK" | tee build/evidence/shizuku-start.log
COMPONENT='com.everyonesniche.nightkeys.debug.test/com.everyonesniche.nightkeys.bridge.PermissionTests'
adb shell am instrument -w -r "$COMPONENT" | tee build/evidence/runtime-fresh.log
grep -q 'NIGHTKEYS_SHELL_TESTS_PASSED count=7 scenario=fresh' build/evidence/runtime-fresh.log
! grep -q 'NIGHTKEYS_SHELL_TESTS_FAILED' build/evidence/runtime-fresh.log
adb shell am force-stop com.everyonesniche.nightkeys.debug
adb shell am instrument -w -r -e scenario reconnect "$COMPONENT" | tee build/evidence/runtime-reconnect.log
grep -q 'NIGHTKEYS_SHELL_TESTS_PASSED count=2 scenario=reconnect' build/evidence/runtime-reconnect.log
! grep -q 'NIGHTKEYS_SHELL_TESTS_FAILED' build/evidence/runtime-reconnect.log
adb shell am start -n com.everyonesniche.nightkeys.debug/com.everyonesniche.nightkeys.ui.MainActivity
sleep 2
adb exec-out screencap -p > build/evidence/app-emulator.png
