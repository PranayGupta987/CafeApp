#!/bin/bash
set -e
export TEMP="C:\\tmp" TMP="C:\\tmp"
echo "=== Android build ==="
cd "$(dirname "$0")/.."
./gradlew :app:assembleDebug --no-daemon -q 2>&1 | tail -10
echo "=== Backend ==="
cd backend
npm run build 2>&1 | tail -5
