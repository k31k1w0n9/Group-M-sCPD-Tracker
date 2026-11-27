# s-CPD Firebase backend (free Spark plan)

No Cloud Functions, no Cloud Storage, nothing that needs a card. The server-side checks are in
[`firestore.rules`](firestore.rules). Why and how: [docs/part2/10-free-tier-backend.md](../docs/part2/10-free-tier-backend.md).

| File | What it is |
|---|---|
| `firestore.rules` | Security rules: the backend logic (bookings, signed QR codes, backup-code limit, one phone per account, points) |
| `test/rules.test.mjs` | Rules tests, run against the Firestore emulator |
| `test/codes.mjs` | QR and backup-code maths, shared with the tests (the app's copy is `SessionCodes.kt`) |
| `scripts/seed.mjs` | Loads the demo accounts and events into the emulators |

## Setup (once)

- Node.js 20 or newer
- Java 21 or newer for the emulators. Android Studio's bundled JBR works: `C:\Program Files\Android\Android Studio\jbr`
- `cd firebase` and `npm install`

## Run the rules tests

```
npm test
```

On Windows, if the emulator stops with `Unable to establish loopback connection`, Java can't create its local socket
under a user folder whose name has a space. `run-tests.sh` already works around it. For other commands, set
`JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:\Users\Public\scpd-jsock` (create that folder first).

## Run the app on the emulators

1. `npm run emulators` and leave it running. The Emulator UI is at http://localhost:4000. The s-CPD office uses
   it to approve events, clear flags and reverse awards.
2. In a second terminal: `npm run seed`. It empties both emulators and loads the demo data, so run it again
   whenever you want a fresh start. Every account's password is `scpd-demo`:
   `student1@scpd.example`, `student2@scpd.example`, `committee.tech@scpd.example`, `committee.arts@scpd.example`,
   `office@scpd.example` (admin).
3. Connect the phone's ports to the laptop (once per emulator boot or USB connection):
   `adb reverse tcp:9099 tcp:9099` and `adb reverse tcp:8080 tcp:8080`.
   The app looks for the emulators at `127.0.0.1` on the phone. We don't use `10.0.2.2`: on the Pixel emulator's
   virtual Wi-Fi, the app's connections to it time out.
4. Build the app against Firebase, from the project root:
   `./gradlew installDebug -Pscpd.backend=firebase`
   Without `-Pscpd.backend=firebase` the app uses the in-memory demo backend, as before.

The end-to-end check (`app/src/androidTest/.../FirebaseBackendSmokeTest.kt`) runs the real app code against the
emulators. It covers committee codes, booking, a live check-in, an offline check-in uploaded from Room, the +3
award, and a second account on the same phone. Run the seed again first, then:

```
./gradlew connectedDebugAndroidTest -Pscpd.backend=firebase -Pandroid.testInstrumentationRunnerArguments.class=com.example.scpd_tracker.FirebaseBackendSmokeTest
```

## The build for the markers: a real Spark project (still free)

The markers run the APK on their own emulated Pixel 2. That emulator can't reach anyone's laptop, so the final
signed APK must talk to a real Firebase project. A release build never uses the local emulators, and it refuses
plain HTTP. The person who owns the project does these steps once (no card needed):

Our project is **`scpd-tracker`**, and steps 1–3 and 5 are done (26 Nov). Firestore runs in `asia-southeast1`
(Singapore), because Firestore has no Malaysia region and Singapore is the closest. Delete protection is on.

1. In the [Firebase console](https://console.firebase.google.com), create a project and stay on the **Spark** plan.
   Add an **Android app** with package `com.example.scpd_tracker`. You don't need `google-services.json` in the
   app, only three values from it (step 5). Don't commit it.
2. Turn on **Authentication > Sign-in method > Email/Password**, and create **Cloud Firestore** (production mode,
   `asia-southeast1`).
3. Deploy the rules and indexes from this folder: `npx firebase login`, then
   `npx firebase deploy --only firestore --project scpd-tracker`. Do this again after every change to
   `firestore.rules`.
4. Load the demo data. In **Project settings > Service accounts**, generate a private key and save it outside the
   repo. Then:
   ```
   GOOGLE_APPLICATION_CREDENTIALS=C:\path\to\key.json node scripts/seed.mjs --live --project <project-id>
   ```
   `--live` creates or resets the demo accounts and overwrites the demo documents. It deletes nothing unless you add
   `--reset`, which empties the whole project first. **Only Keiki runs `--reset`, once, before a release.** It also
   needs `--confirm-wipe scpd-tracker`, so it can't run by accident. Event dates are relative to the moment you run
   it, so run it again shortly before the final submission.
5. Put the real values in `~/.gradle/gradle.properties` (your user folder, not the repo). Each teammate who builds
   the release needs these three lines:
   ```
   scpd.firebase.live.projectId=scpd-tracker
   scpd.firebase.live.appId=<client[0].client_info.mobilesdk_app_id>
   scpd.firebase.live.apiKey=<client[0].api_key[0].current_key>
   ```
   The API key isn't a secret, because the rules protect the data, but keeping it out of git avoids noise.
6. Build the signed APK (`./gradlew assembleRelease` with your signing config), install it on a Pixel 2 / API 31
   emulator, and sign in with the accounts below.

**Which build talks to what:**

| Build | Backend |
|---|---|
| `./gradlew installDebug` | In-memory fake (all tests use this) |
| `./gradlew installDebug -Pscpd.backend=firebase` | Local emulators, project `demo-scpd` |
| `./gradlew installDebug -Pscpd.firebase.target=live` | The real `scpd-tracker` project |
| `./gradlew assembleRelease` | Always the real `scpd-tracker` project, never the emulators |

### Demo accounts

Every account's password is `scpd-demo`.

| Email | Role in the app | What it shows |
|---|---|---|
| `student1@scpd.example` | Student (Aisha) | History, points, a streak, a rejected record, a no-show, an event cancelled by the organiser, booked on the live event |
| `student2@scpd.example` | Student (Daniel) | First-time student, booked on the full Garden event and the live event |
| `committee.tech@scpd.example` | Committee, CS Society **owner** | Runs events: drafts, submissions, check-in |
| `committee2.tech@scpd.example` | Committee, CS Society **committee** | A second personal account for the same club (no shared club login) |
| `scanner.tech@scpd.example` | Committee, CS Society **scanner** | Can run check-in only. The rules refuse creating, editing or cancelling events |
| `committee.arts@scpd.example` | Committee, Arts Club owner | A different club: can't see CS Society drafts or attendees |
| `office@scpd.example` | s-CPD office (admin) | No Android screens. Works in the Firebase console |
