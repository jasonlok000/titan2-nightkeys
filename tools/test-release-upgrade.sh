#!/usr/bin/env bash
# Disposable emulator only. Install real release APKs, not debug lookalikes.
set -euo pipefail
mkdir -p build/evidence
trap 'timeout 10s adb logcat -d -t 1500 > build/evidence/upgrade-logcat.txt 2>&1 || true' EXIT
adb wait-for-device
gh release download v1.0.0-beta.1 --repo "$GITHUB_REPOSITORY" -p NightKeys-1.0.0-beta.1.apk -D build/previous
adb install -r build/previous/NightKeys-1.0.0-beta.1.apk | tee build/evidence/upgrade.txt
adb shell dumpsys package com.everyonesniche.nightkeys > build/evidence/before-package.txt
adb install -r deliverables/NightKeys-1.0.0-beta.2.apk | tee -a build/evidence/upgrade.txt
adb shell dumpsys package com.everyonesniche.nightkeys > build/evidence/after-package.txt
python3 - <<'PY'
from pathlib import Path
import re
p=Path('build/evidence')
before=(p/'before-package.txt').read_text();after=(p/'after-package.txt').read_text()
assert re.search(r'\bversionCode=1000001\b',before)
assert re.search(r'\bversionCode=1000002\b',after)
assert 'versionName=1.0.0-beta.2' in after
# Android versions label this field appId or userId. Never skip the identity check.
def app_id(s):
    match=re.search(r'^\s*(?:appId|userId)=(\d+)\s*$',s,re.M)
    assert match, 'Package dump did not provide an application identity'
    return int(match.group(1))
assert app_id(before)==app_id(after), 'Upgrade changed the application identity'
def installed(s):
    match=re.search(r'firstInstallTime=([^\r\n]+)',s)
    assert match, 'First install time missing'
    return match.group(1)
assert installed(before)==installed(after), 'Update unexpectedly became a fresh install'
with (p/'upgrade.txt').open('a') as out:
    out.write('PASS: app ID '+str(app_id(after))+' and first-install time preserved across upgrade.\n')
PY
adb shell am start -W -n com.everyonesniche.nightkeys/com.everyonesniche.nightkeys.ui.MainActivity | tee -a build/evidence/upgrade.txt
sleep 2
adb shell pidof com.everyonesniche.nightkeys | grep -E '[0-9]'
adb exec-out screencap -p > build/evidence/release-app-top.png
# Use the emulator's actual dimensions rather than assume a 1080-pixel screen.
SIZE="$(adb shell wm size | tr -d '\r' | sed -n 's/Physical size: //p')"
WIDTH="${SIZE%x*}"; HEIGHT="${SIZE#*x}"
[[ "$WIDTH" =~ ^[0-9]+$ && "$HEIGHT" =~ ^[0-9]+$ ]]
adb shell input swipe "$((WIDTH/2))" "$((HEIGHT*4/5))" "$((WIDTH/2))" "$((HEIGHT/4))" 500
sleep 1
adb exec-out screencap -p > build/evidence/release-app-setup.png
printf '%s\n' 'PASS: official beta.1 installed and upgraded in place to beta.2.' 'PASS: permanent release signature accepted by Android 16, versionCode advanced 1000001 -> 1000002.' 'PASS: new release activity launched and remained alive.' 'No Titan vendor hardware or physical LEDs are present in this emulator.' >> build/evidence/upgrade.txt
