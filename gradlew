#!/usr/bin/env sh
# Transparent source bootstrap; see tools/GradleBootstrap.java.
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
if [ -n "${JAVA_HOME:-}" ]; then JAVA="$JAVA_HOME/bin/java"; else JAVA=java; fi
"$JAVA" "$ROOT/tools/GradleBootstrap.java" "$ROOT"
exec "$ROOT/.local-tools/gradle-8.13/bin/gradle" "$@"
