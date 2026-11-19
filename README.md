**COMP3040 Mobile Device Programming — s-CPD Tracker (Group M)**

| Group member | Student ID | Main responsibility |
|---|---|---|
| Moiz Israr Malik | 20705922 | Requirements and user stories: R1–R6 |
| Tham Wing Lok | 20611657 | Technical design and diagrams |
| Wong Cheuk Kei | 20602511 | UI/UX design and wireframes |


## 1. Project overview

s-CPD is a native Android app for University of Nottingham Malaysia students and club committees. Students discover and book events, check in with a rotating QR code, and track verified attendance. Events that need a later checkpoint show the two-scan requirement before booking. Committees submit events and run check-in. The s-CPD office reviews submissions and corrects attendance in the backend. The app adds two original features: personalised event recommendations and weekly attendance streaks.

The documentation is split into short pages so each one can be read on its own. Sections 2 to 5 are the Part 1 proposal. Sections 6 to 8 record what changed while we built the app.

| Section | Page | What it covers |
|:---:|---|---|
| **2** | [Requirements analysis](docs/2-requirements-analysis.md) | Functional and non-functional requirements, user stories, role permissions and original features |
| **3** | [Challenges, risks, and assumptions](docs/3-challenges-risks-assumptions.md) | Attendance, booking and access risks, and the assumptions behind the design |
| **4** | [Architecture](docs/4-1-architecture.md) | MVVM app structure, Firebase backend, roles and sign-in |
| | [Screen flows](docs/4-2-screen-flows.md) | Student and Committee navigation |
| | [Status terms and data model](docs/4-3-status-and-data-model.md) | Status definitions, lifecycles and the entity-relationship diagram |
| | [Attendance security and offline sync](docs/4-4-attendance-security-and-sync.md) | Arrival and checkpoint verification, saved QR scans and attendance outcomes |
| **5** | [Student wireframes](docs/5-1-student-wireframes.md) | Entry, discovery, booking, My Events, check-in, progress and profile |
| | [Committee wireframes](docs/5-2-committee-wireframes.md) | Entry, dashboard, submissions, events, check-in, attendance and account |
| **6** | [Changes since Part 1](docs/part2/6-changes-since-part1.md) | Design, security and code changes made while building the app, and why |
| **7** | [User stories v2](docs/part2/7-user-stories-v2.md) | Re-ranked stories, new stories and abuse stories, each linked to its test |
| **8** | [Threat model](docs/part2/8-threat-model.md) | Twelve ways to cheat, the defence for each, its test and what remains weak |


## 2. Requirements analysis

The app meets six requirements: event review (R1), discovery and booking (R2), check-in (R3), offline use (R4), points and progress (R5) and access (R6). Twenty-two user stories cover all three actors, including full events, attendance without a connection and attempts to cheat. [Read Section 2](docs/2-requirements-analysis.md).


## 3. Challenges, risks, and assumptions

The main risks are proxy check-in, relayed codes, late or altered offline scans, overbooking and cross-club access. Each risk has a response and the limitation that remains. [Read Section 3](docs/3-challenges-risks-assumptions.md).


## 4. Technical design

The Kotlin app uses MVVM with repositories, Room for offline data and Firebase for shared data and trusted operations. Section 4 is split into four pages: [architecture](docs/4-1-architecture.md), [screen flows](docs/4-2-screen-flows.md), [status terms and data model](docs/4-3-status-and-data-model.md), and [attendance security and offline sync](docs/4-4-attendance-security-and-sync.md).


## 5. UI design and wireframes

The wireframes are exported from the [Figma file](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=0-1) and cover every state in the Student and Committee journeys. [Student wireframes](docs/5-1-student-wireframes.md) · [Committee wireframes](docs/5-2-committee-wireframes.md).


## 6–8. Part 2 changes

The front end is complete for both roles. It runs on an in-memory backend that plays the server's part, so every journey can be tried today. The Firebase implementation replaces it behind the same repository interfaces. [Changes since Part 1](docs/part2/6-changes-since-part1.md) · [User stories v2](docs/part2/7-user-stories-v2.md) · [Threat model](docs/part2/8-threat-model.md).


## Build and run

1. Open the project in Android Studio (a version that supports AGP 9.4 and compile SDK 37) and let Gradle sync. Gradle downloads the JDK it needs automatically.
2. Create an emulator matching the marking device: **Pixel 2, Android 12 (API 31) or newer**.
3. Run the `app` configuration.

From a terminal: `./gradlew assembleDebug` builds the app, `./gradlew testDebugUnitTest` runs the unit tests, and `./gradlew connectedDebugAndroidTest` runs the instrumented tests on a running emulator.

### Demo accounts

Every account uses the password `scpd-demo`.

| Email | Role | Notes |
|---|---|---|
| `student1@scpd.example` | Student | Aisha. Booked on today's live Leadership Workshop, with 3 weeks of history and one reversed award. |
| `student2@scpd.example` | Student | Daniel. Has not chosen interests yet, so the first-run Interests screen appears. |
| `committee.tech@scpd.example` | Club Committee | Computer Science Society. Runs the live workshop. |
| `committee.arts@scpd.example` | Club Committee | Arts Club. |

Event dates are generated relative to today, so there is always an event happening now, a full event and a clashing pair. The in-memory data resets when the app process ends.

### Acting as the s-CPD office

The brief lets the admin work in the backend only. Until the Firebase console is connected, debug builds have a **Developer menu** (Profile or Account → Developer menu). It can:

- approve, reject or request changes to submitted events
- reverse the latest award for the signed-in student
- simulate being offline
- move the clock forward one hour, to show deadlines

### Demo walkthrough

1. Sign in as `committee.tech`, open **Check-in → Leadership Workshop**, and leave the live QR on screen.
2. Sign out, then sign in as `student1`. Tap **Check in** on Home and use **Debug: read the organiser's live QR** (the emulator has no camera to point at another phone), or type the 6-digit backup code.
3. Turn on **Simulate offline** in the developer menu and scan again. The stage shows **Saved on phone**. Turn it off and the scan uploads and verifies by itself.
4. As the committee, tap **Open checkpoint**. As the student, scan the checkpoint. Both stages are verified and 3 points are added.
5. In the developer menu, reverse the award. Progress shows the −3 line with the office's reason.


## Tests

| Kind | Where | What it covers |
|---|---|---|
| Domain (JUnit) | `app/src/test/.../domain` | Booking rules, streak weeks, recommendation ranking, event form validation and attendance outcomes |
| Data (coroutines-test) | `app/src/test/.../data/fake` | Booking transaction and waitlist, offline queue and sync, signed codes, rate limiting and device flags |
| ViewModel (coroutines-test) | `app/src/test/.../ui` | Every screen's state, including offline, double taps and state that survives recreation |
| Theme (JUnit) | `app/src/test/...` | WCAG AA contrast for every text and status colour pair |
| Compose UI and end-to-end (instrumented) | `app/src/androidTest` | Screens at the Pixel 2 size (411 × 731 dp), sign-in for both roles, and rotation |


## Project structure

```
app/src/main/java/com/example/scpd_tracker/
  model/        Plain data: events, bookings, attendance, points, users
  domain/       Pure Kotlin rules: Policy, BookingRules, StreakCalculator, RecommendationRanker, EventFormValidator
  data/         Repository interfaces, the in-memory backend (fake/) and device sources (system/)
  di/           AppContainer: builds every repository in one place
  ui/           One package per feature, each with its screen and ViewModel; navigation/ chooses the graph by role
```


## Credits and licences

- [Inter](https://rsms.me/inter/) typeface: SIL Open Font License 1.1 (`app/src/main/assets/licenses/inter-ofl.txt`).
- [ZXing](https://github.com/zxing/zxing) for drawing QR codes: Apache License 2.0.
- [CameraX](https://developer.android.com/jetpack/androidx/releases/camera), Jetpack Compose, Navigation, Lifecycle, DataStore and Biometric: Apache License 2.0.
- [ML Kit Barcode Scanning](https://developers.google.com/ml-kit/vision/barcode-scanning): ML Kit Terms of Service.
- Material Symbols icons: Apache License 2.0.
