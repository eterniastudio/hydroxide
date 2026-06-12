#!/usr/bin/env bash
set -euo pipefail
REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO"

if [ -f ./gradlew.bat ]; then
  ./gradlew.bat test
  ./gradlew.bat clean build
  ./gradlew.bat build --warning-mode all
else
  ./gradlew test
  ./gradlew clean build
  ./gradlew build --warning-mode all
fi

git diff --check
