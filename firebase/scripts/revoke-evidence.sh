#!/usr/bin/env bash
# Evidence for "permission switched off in Settings while the app is closed" (lecturer, 24 Nov).
# Needs: the Firebase emulators running (npm run emulators), one Android emulator or phone on adb.
# Prints each step, so the output can go straight into the report.
set -euo pipefail
cd "$(dirname "$0")/../.."
ADB="${ADB:-adb}"
PKG=com.example.scpd_tracker
RUNNER="$PKG.test/androidx.test.runner.AndroidJUnitRunner"
TEST="$PKG.PermissionRevokedTest"

step() { echo; echo "== $*"; }

step "Fresh demo data and tunnels to the laptop's emulators"
(cd firebase && node scripts/seed.mjs)
"$ADB" reverse tcp:9099 tcp:9099 >/dev/null
"$ADB" reverse tcp:8080 tcp:8080 >/dev/null

step "Install the Firebase build and its tests"
./gradlew -q installDebug installDebugAndroidTest -Pscpd.backend=firebase
"$ADB" shell pm grant $PKG android.permission.CAMERA

step "Phase 1: sign in as student1, go offline, scan the arrival QR (saved on the phone)"
"$ADB" shell am instrument -w -e class "$TEST#phase1_scanSavedOffline" "$RUNNER" | tail -3

step "Room queue on the phone"
"$ADB" shell run-as $PKG ls -l databases/ | grep scpd.db

step "Open the app, send it to the background, then switch the camera off"
"$ADB" shell am start -W -n $PKG/.MainActivity >/dev/null
"$ADB" shell input keyevent KEYCODE_HOME
echo "pid before revoke: $("$ADB" shell pidof $PKG || echo none)"
"$ADB" shell pm revoke $PKG android.permission.CAMERA
sleep 2
echo "pid after revoke:  $("$ADB" shell pidof $PKG || echo none)   (Android killed the process)"

step "Phase 2: new process, still signed in, scan still queued, camera denied; online again it uploads"
"$ADB" shell am instrument -w -e class "$TEST#phase2_afterRevoke_signedInWithQueueIntact" "$RUNNER" | tail -3
