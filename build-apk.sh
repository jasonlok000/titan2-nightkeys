#!/usr/bin/env sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$ROOT"
sh tools/test-core.sh
./gradlew --no-daemon :app:lintDebug :app:assembleDebug
cp app/build/outputs/apk/debug/app-debug.apk NightKeys-0.9.5-rc6-test.apk
printf '\nCreated %s/NightKeys-0.9.5-rc6-test.apk\nComplete the physical hardware check before daily use.\n' "$ROOT"
