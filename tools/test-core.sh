#!/usr/bin/env sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
OUT="$ROOT/build/core-tests"
mkdir -p "$OUT"
javac --release 17 -d "$OUT" "$ROOT"/app/src/main/java/com/everyonesniche/nightkeys/core/*.java "$ROOT/core-tests/CoreTests.java"
java -cp "$OUT" CoreTests
