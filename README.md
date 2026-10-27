**COMP3040 Mobile Device Programming — CW1 Part 1: Requirements and Analysis**

| Group member | Student ID | Main responsibility |
|---|---|---|
| Moiz Israr Malik | 20705922 | Requirements and user stories: R1–R6 |
| [Name] | xxxxxxxx | Technical design and diagrams |
| Wong Cheuk Kei | 20602511 | UI/UX design and wireframes |

> **Target:** approximately 1,500 words, excluding UI components and diagrams.

## 1. Project overview

Campus events can be difficult for students to discover, while clubs need a clear way to manage event submissions, bookings and attendance. The proposed s-CPD app is a native Android app for University of Nottingham Malaysia students and club committees. Students can find events through search and personalised recommendations, book places, check in with an event’s rotating QR code or short-lived backup code, and track verified attendance. Club committees can submit events, manage bookings and run check-in. The s-CPD office reviews submissions through the backend; approved event details are locked. QR scans made offline remain pending until the server verifies them. Only eligible verified attendance earns 3 points and contributes to the weekly streak. The two added features are personalised recommendations and attendance streaks.

### Actors

- **Student:** [Main actions]
- **Club representative:** [Main actions]
- **Admin / s-CPD office:** [Main actions and whether handled in the backend]

## 2. Requirements analysis

### Functional requirements

| Ref | Area | Expected behaviour |
|:---:|---|---|
| **R1** | Event review | Club Committee submits events; Admin approves them or requests changes. **Approved** details are locked. |
| **R2** | Discovery and booking | Students browse approved events, reserve available places and manage bookings through **My Events**. |
| **R3** | Check-in | Committee displays a rotating QR and backup code. Student check-ins are validated against bookings and event times. |
| **R4** | Offline use | QR scans can be saved as **Pending sync** and verified later. Manual code entry requires internet. |
| **R5** | Points and progress | **Verified** attendance earns **3 points**. Invalid awards can be reversed. |
| **R6** | Access | Student and Club Committee use the Android app. Admin approvals and corrections are backend-only. |

### Non-functional requirements

| Area | Proposed test target |
|---|---|
| **Security** | All tested cross-role, cross-club and cross-student unauthorised requests are denied by the backend. |
| **Reliability** | Offline scans survive app restarts; resubmitting one scan never creates extra points. |
| **Booking integrity** | Of 20 simultaneous attempts for the final place, at most one booking succeeds. |
| **Responsiveness** | On the required emulator, a scan is saved offline and **Pending sync** appears within 2 seconds. |
| **Usability** | Booking and attendance failures show clear messages; main touch targets are at least 48 dp. |

### User stories and acceptance criteria

**Event review and booking**

| Story | Actor / priority | User need and acceptance criteria | Screen in Section 5 |
|---|---|---|---|
| **US-01 · R1** | Club Committee · Must-have | I want to create and submit events from **Dashboard**. Required details are checked; submitting produces **Pending review**. **Changes requested** shows feedback and allows editing and resubmission; **Approved** details cannot be edited. | Dashboard / Submission |
| **US-02 · R1** | Admin · Must-have | I want to approve, reject or request changes to event submissions. Only authorised admins can make these decisions in the backend. Only **Approved** events can be booked. | Backend only |
| **US-03 · R1, R2** | Club Committee · Must-have | I want to cancel an approved event. Cancellation closes bookings and informs booked students without unlocking approved details. | Submission approved |
| **US-04 · R2** | Student · Must-have | I want to find suitable activities. **Home** and **Explore** show approved events, search, filters, recommendations and useful empty/offline states. | Home / Explore |
| **US-05 · R2** | Student · Must-have | I want to book an available event. **Event details** explains full capacity and clashes; **Review booking** rechecks them on the server. Concurrent requests cannot overbook, and only success opens **Booking confirmed**. | Event details / Review booking |
| **US-06 · R2** | Student · Must-have | I want to check or cancel my booking. **My Events** opens **Your booking**; cancelling releases the place and opens **Booking cancelled**. **Booked** applies only before the event. | My Events / Your booking |
| **US-07 · R2, R5** | Club Committee · Must-have | I want to review participation. **Events > Upcoming > Bookings** shows reservations; **Events > Past > Attendance summary** shows attendance outcomes. | Events / Attendance summary |

**Check-in and offline attendance**

| Story | Actor / priority | User need and acceptance criteria | Screen in Section 5 |
|---|---|---|---|
| **US-08 · R3** | Club Committee · Must-have | I want to manage check-in using **Scan**. The QR and six-digit backup code refresh every 30 seconds during an active session. Lost connectivity pauses new code display; ending check-in closes the session, not pending claims. | Scan / End check-in |
| **US-09 · R3** | Student · Must-have | I want to scan the displayed event QR. The server checks my account, booking, event window and duplicate attendance; altered, replayed or expired online codes are rejected. | Scan |
| **US-10 · R3** | Student · Must-have | I want a fallback when the camera fails. **Enter code manually** checks the current backup code online; an incorrect or expired code is rejected. | Enter code manually |
| **US-11 · R4** | Student · Must-have | I want to keep an offline scan. It survives app restarts as **Pending sync**, without awarding points or a streak. | Pending sync |
| **US-12 · R3, R4** | Student · Must-have | I want a reliable verification result. Upload retries after reconnection; valid records become **Verified**, while invalid or duplicate claims become **Rejected** with a reason. Suspicious offline timestamps are flagged for review. | Attendance verification result |
| **US-13 · R3, R4** | Club Committee · Must-have | I want an accurate attendee list. **Attendee list** and **Attendance summary** distinguish **Verified**, **Pending sync**, **Rejected** and **No-show**. A pending scan is not treated as a no-show. | Attendee list / Attendance summary |

**Points and access**

| Story | Actor / priority | User need and acceptance criteria | Screen in Section 5 |
|---|---|---|---|
| **US-14 · R5** | Student · Must-have | I want to track my points. **Progress** and **Attendance history** award **3 points** once per **Verified** event. **Pending sync**, **Rejected** and **No-show** earn zero. | Progress / Attendance history |
| **US-15 · R5** | Admin · Must-have | I want to correct incorrect attendance. An authorised backend reversal removes 3 points, records the correction and updates the streak and stamps. | Backend only |
| **US-16 · R6** | Student / Club Committee · Must-have | I want to sign in securely. **Welcome to s-CPD** offers the two app roles; **Sign in to s-CPD** checks university credentials and actual permissions. Invalid sign-in or unauthorised committee access displays an error. | Student entry / Committee entry |
| **US-17 · R6** | Student · Must-have | I want my records kept private. Backend checks prevent access to another student's bookings, points and attendance; **Profile** supports interests, notifications and sign-out. | Profile / Privacy & settings |
| **US-18 · R6** | Club Committee · Must-have | I want access limited to my own society. Backend checks prevent editing other clubs' events or approving submissions. **Account** shows verified access and confirms sign-out. | Account |

### Role permissions

| Action | Student | Club Committee | Admin (backend only) |
|---|:---:|:---:|:---:|
| Browse events and manage own bookings | Yes | No | No |
| Scan QR and view own points | Yes | No | No |
| Create, revise or cancel club events | No | Own club | No |
| Run check-in, view club bookings/attendance | No | Own club | No |
| Approve events or request changes | No | No | Yes |
| Reverse invalid attendance and points | No | No | Yes |

### Original features

| Feature | Value and scope | Screen in Section 5 |
|---|---|---|
| **1. Personalised recommendations** | Interests in Leadership, Sports, Tech, Volunteering, Arts and Career improve discovery. **Recommended for you** uses saved interests and verified attendance history; similar-student data is optional. | Make it yours / Home / Explore |
| **2. Weekly attendance streak** | **Progress** counts consecutive Monday-to-Sunday weeks with verified attendance. Corrections recalculate the streak. Interest stamps are visual rewards, not a third feature. | Progress |

| Story | Actor / priority | User need and acceptance criteria | Screen in Section 5 |
|---|---|---|---|
| **US-19 · Feature 1** | Student · Must-have | I want to choose and edit interests. **Make it yours** saves selected categories; **Interests** changes them later and updates recommendations. | Make it yours / Interests |
| **US-20 · Feature 1** | Student · Must-have | I want relevant recommendations. **Home** explains why suggested approved events match my interests; new students receive interest-based results. | Home / Explore |
| **US-21 · Feature 1** | Student · Nice-to-have | I want suggestions informed by similar students. Aggregated patterns may improve ranking without exposing individual attendance histories. | Home / Explore |
| **US-22 · Feature 2** | Student · Must-have | I want to track my weekly streak and stamps. Only **Verified** attendance counts. Late verification or reversal updates the relevant week and visual rewards. | Progress |

**Attendance limitation:** A live QR or backup code can be relayed to somebody outside the venue. Offline phone timestamps can be changed, so an offline scan alone cannot prove physical presence or scan time. Such claims remain **Pending sync** until server validation, with suspicious claims flagged for review.

## 3. Challenges, risks, and assumptions

### Challenges and risks

| Risk / challenge | Response | Remaining limitation |
|---|---|---|
| [Example: checking in for a friend] | [Defence] | [Weakness] |
| [Example: poor connection at an event] | [Offline handling] | [Limitation] |
| [Example: incorrect role access] | [Permission checks] | [Limitation] |

### Assumptions

- [Assumption about accounts, roles, or Club membership]
- [Assumption about devices, connectivity, or event information]

## 4. Technical design
 
### Architecture
 
The app is a native Kotlin Android app using MVVM with repositories. Screens only display state, ViewModels hold presentation logic, and repositories are the only layer that decides whether data comes from the device or the backend. A Room database stores cached events, bookings and every attendance record scanned on the device, so students can view saved bookings and capture a QR scan while offline. Firebase is the shared backend: Authentication for sign-in, Firestore for shared data, Cloud Functions for trusted operations (booking, cancelling, issuing check-in codes, attendance verification, points) and Cloud Messaging for reminders and cancellation notices. A WorkManager job uploads Pending sync records when a connection returns.
 
The app has two sets of screens: **Student** (Home, Explore, My Events, Progress, Profile) and **Committee** (Dashboard, Events, Scan, Submissions). **Admin is not an app user.** The s-CPD office reviews events in the backend console, so no Admin Android screen is required. Admin appears in the diagrams only as an actor or as a backend review step.
 
**Roles and authentication.** The University sign-in (role selection) screen offers Student or Club Committee. Choosing one only selects the sign-in screen and grants nothing. After university sign-in, the backend reads the role stored on the account (set by the backend, never by the app). Security rules and Cloud Functions check it on every request, so a student cannot call committee operations, and the UI shows only the screens for the verified role. If the role does not match, the Committee access not authorised screen offers Continue as Student or Return to Sign in.
 
**DG-01 Use-case diagram (R1–R6)**
 
```mermaid
flowchart LR
  Student(["Student"])
  Committee(["Club Committee"])
  Admin(["Admin (s-CPD office)"])
 
  subgraph App["Android app"]
    direction TB
    S1["Sign in with university account"]
    S2["Choose interests"]
    S3["Browse, search and view events"]
    S4["Book event"]
    S5["View booking details and cancel booking"]
    S6["Scan event QR or enter backup code"]
    S7["View attendance result and history"]
    S8["View progress, points and streak"]
    S9["Manage profile and settings"]
    C1["Create event, save draft, submit for review"]
    C2["Edit and resubmit after Changes requested"]
    C3["Cancel approved event"]
    C4["View bookings - Events, Upcoming"]
    C5["View attendance summary - Events, Past"]
    C6["Start and end check-in - Scan"]
  end
 
  subgraph Backend["Firebase backend"]
    V["Verify attendance on server"]
    P["Award 3 points after verification"]
    A1["Review event: Approved or Changes requested - console"]
    A2["Revoke invalid attendance - console"]
    A3["Mark event as featured - console"]
  end
 
  Student --- S1 & S2 & S3 & S4 & S5 & S6 & S7 & S8 & S9
  Committee --- C1 & C2 & C3 & C4 & C5 & C6
  Admin --- A1 & A2 & A3
  S6 -.->|includes| V
  V -.->|includes| P
```
 
**DG-02 Architecture diagram (R4, R6)**
 
```mermaid
flowchart LR
  subgraph Device["Android app (Kotlin, MVVM)"]
    UIS["Student screens"]
    UIC["Committee screens"]
    VM["ViewModels"]
    Repo["Repositories"]
    Room[("Room: cached events, bookings, attendance records")]
    Sync["WorkManager sync worker"]
    UIS --> VM
    UIC --> VM
    VM --> Repo
    Repo <--> Room
    Sync <--> Room
  end
  subgraph Backend["Firebase backend"]
    Auth["Authentication"]
    FS[("Firestore")]
    CF["Cloud Functions: bookEvent, cancelBooking, cancelEvent, getCheckInCode, submitAttendance, closeCheckIn"]
    Rules["Security rules (role checks, approved events locked)"]
    FCM["Cloud Messaging"]
  end
  Admin(["Admin (backend console)"])
  Repo -->|sign in| Auth
  Repo -->|read events, own bookings| FS
  Repo -->|book, cancel, check in| CF
  Sync -->|upload Pending sync records| CF
  CF --> FS
  CF -->|reminders, cancellation notices| FCM
  Rules -.protects.- FS
  Admin -->|approve or request changes| FS
```
 
### Screen flows
 
Screen names below are the Figma frame names.
 
**DG-03 Student screen flow (R2–R6)**
 
```mermaid
flowchart TD
  RS["University sign-in (role selection)"] --> SI["University sign-in"]
  SI -->|first sign-in| MY["Make it yours"]
  SI -->|returning| HOME
  MY --> HOME
 
  subgraph Tabs["Student tab bar"]
    HOME["Hello, User (Home)"]
    EX["Explore"]
    ME["My Events"]
    PG["Your progress"]
    PF["Your profile"]
  end
 
  HOME --> ED["Event details points"]
  EX --> ED
  ED -->|places left| RB["Review booking"]
  ED -->|0 places or overlap| EF["Event full"]
  EF -->|Browse alternatives| EX
  EF -->|View existing booking| ME
  RB --> BK["You’re booked"]
  BK --> ME
 
  ME -->|View booking| CB["Cancel booking confirmation - Your booking"]
  CB -->|Cancel booking| BC["Booking cancelled"]
  CB -->|Keep booking| ME
  BC --> ME
 
  ME -->|Scan event QR| SQ["Scan event QR"]
  SQ -->|server verified| AC["Already checked in"]
  SQ -->|offline after capture| PS["Attendance pending sync"]
  SQ -->|expired code| QE["Event QR expired"]
  SQ -->|other event| WE["Wrong event"]
  SQ -->|offline, no scan| NI["No internet student"]
  SQ --> MC["Enter code manually"]
  QE --> SQ
  WE --> SQ
  MC --> IC["Manual entry → Invalid code"]
  MC --> CE["Manual entry → Code expired"]
  MC --> MV["Manual entry → Attendance verified"]
  PS --> ME
 
  PG --> AH["Attendance history"]
  PG --> FS["Your first stamp"]
  PG --> PP["Progress points removed"]
  AH --> RA["Rejected attendance"]
 
  PF --> PSET["Privacy & settings"]
  PSET --> NS["Notification settings"]
  PSET --> IN["Interests"]
  PF --> AO["Account"]
  AO --> SO["Sign out confirmation"]
  SO --> RS
```
 
**DG-04 Committee screen flow (R1, R3, R6)**
 
```mermaid
flowchart TD
  RS["University sign-in (role selection)"] --> SI["University sign-in"]
  SI -->|role not committee| NA["Committee access not authorised"]
  NA -->|Return to Sign in| SI
  SI --> DASH
 
  subgraph Tabs["Committee tab bar"]
    DASH["Committee dashboard"]
    EV["Committee Events"]
    SC["Scan"]
  end
 
  DASH -->|Create event| CE["Create event"]
  CE -->|Submit for review| SR["Submission received"]
  SR --> PR["Pending review"]
  DASH -->|Pending review card| PR
  DASH -->|Changes requested card| NC["Submission needs changes"]
  NC -->|Edit submission, resubmit| SR
  DASH -->|Approved card| AE["Approved event"]
  AE -->|Cancel event| CA["Cancel approved event"]
  PR -->|Withdraw submission| DASH
 
  EV -->|Upcoming: View bookings| BL["Event bookings list"]
  EV -->|Upcoming: View submission, Review feedback| PR
  EV -->|Past: View attendance summary| AS["Attendance summary → Attendee list"]
  AS --> AL["Attendee list / all registered attendees"]
  EV --> EE["Committee Events Empty"]
 
  AE -->|Start check-in| ST["Start check-in"]
  SC -->|Active| ST
  SC -->|Upcoming, not yet open| NO["Scan - Check in not yet open"]
  ST --> OG["Attendance verified (ongoing)"]
  ST -->|offline| PA["Check-in paused"]
  ST -->|End check-in| EC["Confirm end → Check-in closed"]
  EC --> CC["Check-in closed"]
  CC -->|View attendance summary| AS
  CC -->|Back to Events| EV
 
  DASH --> CACC["Committee account"]
  CACC --> SOC["Sign out confirmation"]
  SOC --> RS
```
 
### Status terms
 
| Area | Terms | Notes |
|---|---|---|
| Event review | Pending review, Approved, Changes requested | Admin sets Approved or Changes requested in the backend console. A Draft is an unsubmitted event, not a review status. |
| Booking | Booked, Cancelled, No-show | Booked applies only before the event. After check-in closes, a booking without an attendance record becomes No-show. |
| Attendance | Verified, Pending sync, Rejected, No-show | Verified and Rejected come only from the server. Pending sync is a saved scan awaiting the server. |
| Points | 3 points per Verified attendance | Never awarded for a booking, a scan, Pending sync or Rejected. |
 
### Data model
 
**DG-05 Event lifecycle (R1)**
 
A club creates an event as a Draft and submits it for review. Admin approves it or returns it as Changes requested, and the committee can edit and resubmit. Once Approved, details are locked because security rules reject edits to an approved event, so a club cannot change an event after approval (R1). The only committee action after approval is Cancel event, which notifies all booked students and cancels their bookings.
 
```mermaid
stateDiagram-v2
  [*] --> Draft
  Draft --> PendingReview: club submits for review
  PendingReview --> Draft: club withdraws submission
  PendingReview --> ChangesRequested: admin requests changes
  PendingReview --> Approved: admin approves
  ChangesRequested --> PendingReview: club edits and resubmits
  state "Pending review" as PendingReview
  state "Changes requested" as ChangesRequested
  state Approved {
    [*] --> BookingOpen
    BookingOpen --> Full: capacity reached
    Full --> BookingOpen: a booking is cancelled
    BookingOpen --> CheckInOpen: committee starts check-in on event day
    Full --> CheckInOpen: committee starts check-in on event day
    CheckInOpen --> CheckInClosed: committee ends check-in or event ends
    CheckInClosed --> [*]
  }
  Approved --> Cancelled: committee cancels event
  Cancelled --> [*]
  note right of Approved
    Details locked from here on (R1)
  end note
```
 
**DG-06 Booking and attendance lifecycle (R2–R5)**
 
```mermaid
stateDiagram-v2
  [*] --> Booked: student confirms Review booking
  Booked --> Cancelled: student cancels or event cancelled
  Booked --> PendingSync: QR scan saved while offline
  Booked --> Verified: server verifies scan or backup code
  Booked --> Rejected: server rejects scan
  Booked --> NoShow: check-in closes with no record
  PendingSync --> Verified: sync accepted, 3 points credited
  PendingSync --> Rejected: sync refused with reason
  NoShow --> Verified: late upload accepted within delay cap
  Verified --> Rejected: record later found invalid, points reversed
  state "Pending sync" as PendingSync
  state "No-show" as NoShow
  Cancelled --> [*]
  Verified --> [*]
  Rejected --> [*]
  NoShow --> [*]
```
 
**DG-07 Entity-relationship diagram (R1–R5)**
 
All entities use stable IDs (`userId`, `clubId`, `eventId`, `bookingId`, `attendanceId`). `attendanceId` is a UUID created on the device, so a retried upload cannot create a duplicate. The event's QR secret is a server-only field. Each Verified attendance earns a fixed 3 points. Points are stored as a ledger (one row per award or reversal) so they can be revoked with an audit trail, and the weekly streak counts Monday-to-Sunday weeks containing at least one Verified attendance. Recommended for you is ranked from saved interests (USER_INTEREST, EVENT_TAG) and Verified attendance history, and any similar-student signal would use aggregated counts only. Interest stamps and the points milestone on the Your progress screen are calculated from the ledger and event tags, so they are not stored. Booking capacity and schedule-clash checks run in one Cloud Function transaction, so two students cannot take the last place or hold overlapping bookings.
 
```mermaid
erDiagram
  USER {
    string userId PK
    string email
    string name
    string role "student, committee, admin"
    string studentId
    string department
    string language
  }
  CLUB {
    string clubId PK
    string name
  }
  CLUB_MEMBER {
    string clubId FK
    string userId FK
  }
  EVENT {
    string eventId PK
    string clubId FK
    string reference
    string title
    string description
    datetime startTime
    datetime endTime
    string location
    int capacity
    bool eligible "counts for points"
    int pointsAward "3"
    string reviewStatus "draft, pending_review, changes_requested, approved"
    string reviewComment "admin feedback"
    string eventState "booking_open, full, check_in_open, check_in_closed, cancelled"
    datetime checkInClosedAt
    bool featured "set by admin"
    string posterUrl
    string venueApprovalUrl
    string qrSecret "server only"
  }
  INTEREST_TAG {
    string tagId PK
    string name
  }
  USER_INTEREST {
    string userId FK
    string tagId FK
  }
  EVENT_TAG {
    string eventId FK
    string tagId FK
  }
  BOOKING {
    string bookingId PK
    string eventId FK
    string userId FK
    string reference
    string status "booked, cancelled, no_show"
    datetime createdAt
    datetime cancelledAt
  }
  ATTENDANCE_RECORD {
    string attendanceId PK
    string eventId FK
    string userId FK
    string bookingId FK
    string entryMethod "qr, backup_code"
    datetime scannedAt
    datetime receivedAt
    string codeWindow
    string nonce
    string deviceId
    string status "pending_sync, verified, rejected"
    string rejectReason
  }
  POINTS_LEDGER {
    string entryId PK
    string userId FK
    string eventId FK
    string attendanceId FK
    int points "3 or -3"
    string status "credited, revoked"
    string reason "verified attendance, invalid record adjustment"
    datetime createdAt
  }
  STREAK {
    string userId PK
    int currentWeeks
    int longestWeeks
    string lastQualifyingWeek
  }
  USER ||--o{ CLUB_MEMBER : "belongs to"
  CLUB ||--o{ CLUB_MEMBER : has
  CLUB ||--o{ EVENT : runs
  EVENT ||--o{ BOOKING : receives
  USER ||--o{ BOOKING : makes
  BOOKING ||--o| ATTENDANCE_RECORD : "leads to"
  EVENT ||--o{ ATTENDANCE_RECORD : has
  USER ||--o{ ATTENDANCE_RECORD : submits
  ATTENDANCE_RECORD ||--o| POINTS_LEDGER : earns
  USER ||--o{ POINTS_LEDGER : owns
  USER ||--o| STREAK : has
  USER ||--o{ USER_INTEREST : selects
  INTEREST_TAG ||--o{ USER_INTEREST : chosen
  EVENT ||--o{ EVENT_TAG : tagged
  INTEREST_TAG ||--o{ EVENT_TAG : labels
```
 
### Attendance security
 
Check-in is started from Committee Scan. The committee device asks the server for the current code, which is `HMAC(eventSecret, eventId + 30-second window)` shown as a rotating QR code with a 6-digit backup code derived from the same value. The event secret never leaves the server, so neither the committee device nor the student device can validate a code itself. Code refresh pauses while the committee device is offline (Check-in paused). The student scans the QR with the app (or types the backup code while online), and the app saves a record (code, scan time, nonce, device ID). The app can reject a QR for a different event locally (Wrong event), but only the server decides whether a record is Verified. The server checks that:
 
1. the record belongs to the signed-in student,
2. the code is valid for the claimed scan time (one window either side),
3. the event is Approved and eligible, and the student has a Booked booking for it,
4. the scan time is inside the check-in window (event day, before the committee ended check-in or the event ended),
5. no attendance exists for this student and event, and the nonce is unused.
Failed records are Rejected with a reason the student can see (for example, scanned after the QR expired). Only after all checks pass does the server award the 3 points and update the weekly streak. Students scan the committee's code, and there is no personal code to share. For emulator testing, the scan screen also accepts the 6-digit backup code typed in by hand, which goes through the same server checks and needs a connection.
 
| Cheating attempt | Defence | Remaining weakness |
|---|---|---|
| Checking in for a friend | Record tied to the signed-in account and its Booked booking, one per student per event, and the server flags a device used by several accounts | A friend who shares their login |
| Checking in from home | Code exists only on the committee screen and expires after 30 seconds | Live relay or a photo sent within seconds |
| Checking in after the event | Server checks scan time against the check-in window and caps upload delay | Changed device clock while offline |
| Faked offline record | Server recomputes the code for the claimed time, the app stores a monotonic clock, and the server flags odd gaps and lone scans | Photographing a real code, setting the clock back and scanning later can pass the code check. Flagged records can be revoked |
| Replay or duplicate | Idempotent `attendanceId`, unique student-event key, nonce | None significant |
| Rooted device | Records signed with a Keystore key bound to the account's device | Root can bypass this, accepted risk |
| Dishonest committee member | Admin can audit counts and revoke | Detected, not prevented |
 
**DG-08 Attendance verification sequence (R3, R5)**
 
```mermaid
sequenceDiagram
  participant C as Committee device (Scan)
  participant S as Student app
  participant F as Cloud Function
  participant DB as Firestore
  C->>F: getCheckInCode(eventId)
  F-->>C: QR + 6-digit backup code for current 30 s window
  S->>C: Scan QR code (Scan event QR)
  S->>S: Check QR is for the booked event, create record (attendanceId, nonce, scannedAt, deviceId)
  S->>F: submitAttendance(record)
  F->>F: Check account, code for window, Booked booking, check-in window, duplicate, nonce
  alt all checks pass
    F->>DB: Save attendance as Verified
    F->>DB: Add points ledger entry (+3), update weekly streak
    F-->>S: Verified, 3 points earned
  else a check fails
    F->>DB: Save attendance as Rejected with reason
    F-->>S: Rejected, show reason, no points
  end
  Note over S,F: Offline scans follow DG-09. Backup code entry skips the camera and needs a connection.
```
 
### Offline sync
 
Without a connection a QR scan still works. The record is saved in Room as Pending sync and shown in the attendance list, with no points or streak credit yet. If nothing was scanned while offline, there is no record and nothing to sync (No internet student). WorkManager uploads Pending sync records when the network returns and sets each one to Verified or Rejected from the server's answer. Failed uploads stay Pending sync and retry with backoff, which is safe because uploads are idempotent. Points and streak credit come only from the server after verification. If a Verified record is later found invalid, it becomes Rejected, the ledger gets a reversing entry, totals and streak are recalculated, and the student sees it on Progress points removed.
 
**DG-09 Offline sync sequence (R4, R5)**
 
```mermaid
sequenceDiagram
  participant S as Student app
  participant R as Room database
  participant W as WorkManager
  participant F as Cloud Function
  participant DB as Firestore
  S->>R: Save record with status Pending sync
  S->>S: Show Attendance pending sync, no points yet
  Note over S,W: Device is offline
  W->>W: Network connection returns
  W->>R: Load all Pending sync records
  W->>F: submitAttendance(records, signed with device key)
  F->>F: Recompute valid code for scannedAt, check Booked booking, check-in window, nonce, duplicate, upload delay
  alt record is valid
    F->>DB: Mark Verified, add points ledger entry (+3), update streak
    F-->>W: Verified
    W->>R: Set status Verified
  else record is invalid
    F->>DB: Mark Rejected with reason
    F-->>W: Rejected
    W->>R: Set status Rejected
  end
  W->>S: Refresh attendance list and points
  Note over W,F: On network error the record stays Pending sync and is retried with backoff
```
 
## 5. UI design and wireframes

These wireframes show the main Student and Club Committee journeys and their key states. Admin event review is handled in the backend, so Admin mobile screens are not included.

**Header behaviour:** The main dashboard and landing screens use an expanded header with the campus background, blue overlay and university branding. As the user scrolls, it collapses into a compact dark-blue app bar. Secondary screens use the compact header. These are states of one header design, not separate designs.

### Student entry and interests — R6

Student - entry and interests
<img width="1936" height="1133" alt="STUDENT · Entry" src="https://github.com/user-attachments/assets/5089640b-e6f5-4c04-b6f0-de759e6683ff" />

Users choose Student or Club Committee before signing in. University sign-in verifies their actual permissions. Students can select interests to personalise event suggestions.

### Event discovery — R2, Feature 1

Student - explore events
<img width="2952" height="2433" alt="STUDENT · Explore" src="https://github.com/user-attachments/assets/a851cde4-a013-4d5e-beda-ffb61c8c5ab6" />


Students browse featured and recommended events, search and filter the listings, and see helpful empty-result or offline states.

### Event details and booking — R2

Student - booking flow
<img width="3460" height="1764" alt="STUDENT · Booking" src="https://github.com/user-attachments/assets/e3332479-1304-4c51-b495-c442de9b0d16" />


**Navigation note:** Students can reach an event from either Home or Explore. On Home, they open a Featured or Recommended event; recommendation cards show why the event matches them. In Explore, they open an event from search results or filtered listings. Both routes lead to the same event details and booking flow. The repeated event screens show different entry points and recommendation context, not separate versions of the event.

Event details show availability and relevant information. The flow covers full events, timetable clashes, booking review and confirmation.

### My Events and cancellations — R2

Student - My Events
<img width="1936" height="1896" alt="STUDENT · My Events" src="https://github.com/user-attachments/assets/fbc3e433-122a-4b5b-90da-b67a8d63858f" />


Students view upcoming and past bookings, open booking details, and cancel a booking. The confirmation and cancelled states show the outcome.

### Attendance and check-in — R3, R4

Student - attendance and check-in
<img width="7524" height="1941" alt="STUDENT · Attendance" src="https://github.com/user-attachments/assets/57184104-4fa9-4309-b11d-b7edc2f2533b" />


Students scan the Committee’s rotating QR or enter its current backup code. Invalid and expired codes show an error. Offline scans show Pending sync; manual code entry requires an online server check. Attendance is Verified only after server confirmation.

### Points and progress — R5, Feature 2

Student - progress and attendance history
<img width="1936" height="1628" alt="STUDENT · Progress" src="https://github.com/user-attachments/assets/dda466bc-6d65-4241-b677-984224fc7cd8" />


Students view verified points, attendance history, interest stamps and their weekly streak. Pending records do not earn points or streak credit; invalid attendance can have its points removed.

### Profile and settings — R6

Student - profile and account settings
<img width="3460" height="1406" alt="STUDENT · Account" src="https://github.com/user-attachments/assets/a74ee55b-092f-4e6c-8eff-cfa1fc43b9f1" />


Students view their profile, manage interests and notification preferences, adjust privacy settings and sign out.

### Committee entry and access — R6

Committee - entry and sign-in
<img width="1936" height="1089" alt="COMMITTEE · Entry" src="https://github.com/user-attachments/assets/453d9c01-b26a-4c9c-bc9b-c4d30dc15b02" />


Committee members choose their role and sign in with their university account. Selecting a role does not grant access; the app checks permissions after sign-in. The screens also show sign-in and unauthorised-access errors.

### Committee dashboard — R1, R3, R6

Committee - dashboard
<img width="920" height="1931" alt="COMMITTEE · Dashboard" src="https://github.com/user-attachments/assets/53a18d9b-592f-4d95-baae-ad7c783e1fee" />


The dashboard summarises event and submission statuses, links to event creation and check-in, and shows the next public event. The Account button provides access to the committee profile.

### Event submissions — R1

Committee event submissions
<img width="2952" height="1862" alt="COMMITTEE · Submissions" src="https://github.com/user-attachments/assets/27eb2d00-39f8-48e9-84d8-c9816f1f05af" />


Committee members create drafts and submit events for review. They can view **Pending review** and **Changes requested** states, edit and resubmit returned events, and view approved details as locked. Approved events can be cancelled.

### Committee Events — R1, R2, R3

Committee - Events
<img width="1428" height="1711" alt="COMMITTEE · Committee Events" src="https://github.com/user-attachments/assets/c42f9d64-c1c3-42a9-988b-c38518721569" />


The Upcoming tab shows events and links to their booking lists. The Past tab links to attendance summaries. An empty state lets the committee create an event.

### Check-in — R3, R4, R5

Committee - check-in
<img width="4476" height="1580" alt="COMMITTEE · Check-in" src="https://github.com/user-attachments/assets/7e67621e-a690-4107-9ffd-318be6f34ad2" />


The committee displays a rotating event QR and its short-lived backup code. Check-in can be paused when offline; a stale code cannot be used. Ending check-in closes the session, while saved scans still need server verification.

### Bookings and attendance — R2, R3, R4, R5

Committee - bookings and attendance
<img width="2444" height="1400" alt="COMMITTEE · Attendees" src="https://github.com/user-attachments/assets/8ec9d0c6-5f36-41e7-856a-3eae5653a0a9" />


**Navigation note:** Committee Events manages scheduled events: before an event starts, Upcoming opens its bookings; after it ends, Past opens its attendance summary. During an event, Scan is used to display the rotating QR and backup code and open the live attendee list. Ending check-in leads to the attendance summary.

The booking list and attendance summary show **Verified**, **Pending sync**, **Rejected** and **No-show** records. Only server-verified attendance earns 3 points; closing check-in does not verify pending scans.

### Committee account — R6

Committee - account and sign-out
<img width="920" height="1062" alt="COMMITTEE · Account" src="https://github.com/user-attachments/assets/483af387-fca3-4742-97cb-cbfb77e126b2" />


Committee members can view their verified club access and sign out, with a confirmation step.

