#!/usr/bin/env bash
# Disposable emulator only. Install real release APKs, not debug lookalikes.
set -euo pipefail
mkdir -p build/evidence
trap 'timeout 10s adb logcat -d -t 1500 > build/evidence/upgrade-logcat.txt 2>&1 || true' EXIT
adb wait-for-device
gh release download v1.0.0-beta.1 --repo "$GITHUB_REPOSITORY" -p NightKeys-1.0.0-beta.1.apk -D build/previous
adb install -r build/previous/NightKeys-1.0.0-beta.1.apk | tee build/evidence/upgrade.txt
adb shell dumpsys package com.everyonesniche.nightkeys > build/evidence/before-package.txt
grep -q 'versionCode=1000001' build/evidence/before-package.txt
adb install -r deliverables/NightKeys-1.0.0-beta.2.apk | tee -a build/evidence/upgrade.txt
adb shell dumpsys package com.everyonesniche.nightkeys > build/evidence/after-package.txt
grep -q 'versionCode=1000002' build/evidence/after-package.txt
grep -q 'versionName=1.0.0-beta.2' build/evidence/after-package.txt
BEFORE="$(grep -m1 'userId=' build/evidence/before-package.txt | tr -d '\r\n ')"
AFTER="$(grep -m1 'userId=' build/evidence/after-package.txt | tr -d '\r\n ')"
test -n "$BEFORE" && test "$BEFORE" = "$AFTER"
adb shell am start -W -n com.everyonesniche.nightkeys/com.everyonesniche.nightkeys.ui.MainActivity | tee -a build/evidence/upgrade.txt
sleep 2
adb shell pidof com.everyonesniche.nightkeys | grep -E '[0-9]'
adb exec-out screencap -p > build/evidence/release-app-top.png
adb shell input swipe 500 1600 500 400 500
sleep 1
adb exec-out screencap -p > build/evidence/release-app-setup.png
printf '%s\n' 'PASS: official beta.1 installed and upgraded in place to beta.2, same Android UID.' 'PASS: permanent release signature accepted by Android 16, versionCode advanced 1000001 -> 1000002.' 'PASS: new release activity launched and remained alive.' 'No Titan vendor hardware or physical LEDs are present in this emulator.' >> build/evidence/upgrade.txt
