#!/usr/bin/env bash
# Runs the rules tests on the Firestore emulator. Needs Java 21+ (Android Studio's bundled JBR works).
# On Windows the emulator's Java can't open its local socket under a user folder with a space in it, so point it at a plain folder.
set -e
cd "$(dirname "$0")"
if [ -z "$JAVA_HOME" ] && [ -d "/c/Program Files/Android/Android Studio/jbr" ]; then export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"; fi
[ -n "$JAVA_HOME" ] && export PATH="$JAVA_HOME/bin:$PATH"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) mkdir -p /c/Users/Public/scpd-jsock; export JAVA_TOOL_OPTIONS="-Djdk.net.unixdomain.tmpdir=C:\Users\Public\scpd-jsock";; esac
npx firebase emulators:exec --only firestore --project demo-scpd "node --test --test-reporter=spec test/rules.test.mjs"
