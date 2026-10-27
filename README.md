**COMP3040 Mobile Device Programming — CW1 Part 1: Requirements and Analysis**

| Group member | Student ID | Main responsibility |
|---|---|---|
| [Name] | xxxxxxxx | Requirements and user stories: R1–R6 |
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
| **R1** | Approval | Clubs submit events for review. Returned requests can be edited; approved event details are locked. |
| **R2** | Booking | Students discover and reserve approved events, subject to capacity and timetable checks. |
| **R3** | Attendance | Students scan a rotating event QR code. The server checks the code, booking and check-in window. |
| **R4** | Offline use | Scans can be saved without internet and submitted later, with clear verification statuses. |
| **R5** | Points | Verified attendance earns points. Students can view their progress, and invalid awards can be reversed. |
| **R6** | Access | University sign-in and backend permissions restrict users to their authorised roles and records. |

### Non-functional requirements

| Area | Expected standard |
|---|---|
| **Security** | Role permissions and attendance checks are enforced in the backend, not only in the interface. |
| **Reliability** | Offline records survive app restarts and retries cannot create duplicate attendance or points. |
| **Data integrity** | Concurrent bookings cannot exceed event capacity; point corrections remain traceable. |
| **Usability** | Booking errors and attendance statuses have clear messages, especially while offline. |
| **Responsiveness** | Scanning and local saving remain usable without waiting for a network response. |

### User stories and acceptance criteria

**Event approval and booking**

| Story | Actor / priority | User need | Acceptance criteria |
|---|---|---|---|
| **US01 · R1** | Club Committee · Must-have | As a committee member, I want to submit an event for approval. | Title, description, category, date, time, venue, capacity and requested points are required. Invalid submissions are blocked. |
| **US02 · R1** | Club Committee · Must-have | As a committee member, I want to see decisions and respond to feedback. | Pending, Approved, Rejected and Returned statuses are shown. Returned requests can be revised; approved details cannot. |
| **US03 · R1** | Admin · Must-have | As an admin, I want to approve, reject or return requests. | Only authorised admins can change decisions in the backend. Only approved events open for booking. |
| **US04 · R2** | Student · Must-have | As a student, I want to find events that suit my interests. | Approved events show their date, venue, points and availability. Search, filters and empty results work correctly. |
| **US05 · R2** | Student · Must-have | As a student, I want to reserve a place without double-booking. | Full, duplicate and clashing bookings are refused. The server prevents overbooking, even for simultaneous requests. |
| **US06 · R2** | Club Committee · Must-have | As a committee member, I want to manage an event's booking availability. | Events can become Full, Closed or Cancelled without changing approved details. Cancelled bookings are clearly shown to students. |

**Check-in and offline attendance**

| Story | Actor / priority | User need | Acceptance criteria |
|---|---|---|---|
| **US07 · R3** | Club Committee · Must-have | As a committee member, I want to display a QR code for check-in. | The code identifies the event, rotates every 30 seconds and is available only during the check-in window. |
| **US08 · R3** | Student · Must-have | As a student, I want to scan the event QR code to record attendance. | The booking must match the signed-in account and event. Unbooked students, duplicate check-ins, altered codes and online scans outside the valid QR or check-in window are rejected. Offline scans are verified after upload. |
| **US09 · R4** | Student · Must-have | As a student, I want to save a scan when I have no internet. | The scan survives an app restart and shows **Pending Sync**. No points or streak credit are awarded yet. |
| **US10 · R4** | Student · Must-have | As a student, I want to know if my saved scan was accepted. | Uploads retry after reconnection. Status becomes **Verified** or **Rejected**, with a reason. Invalid codes and repeated attendance IDs cannot earn credit; suspicious offline timestamps are flagged for review. |

**Points and role permissions**

| Story | Actor / priority | User need | Acceptance criteria |
|---|---|---|---|
| **US11 · R5** | Student · Must-have | As a student, I want to see my points and attendance history. | Only verified attendance adds points. Pending and rejected claims do not affect the total. |
| **US12 · R5** | Admin · Must-have | As an admin, I want to correct wrongly awarded points. | Authorised reversals are recorded and the student's total and affected streak are recalculated. |
| **US13 · R6** | All roles · Must-have | As a university user, I want to sign in with my account. | Selecting Student, Club Committee or Admin at launch does not grant access; the backend verifies the role. |
| **US14 · R6** | Student · Must-have | As a student, I want my records kept private. | Students cannot access another student's bookings, attendance or points, including through direct backend requests. |
| **US15 · R6** | Club Committee · Must-have | As a committee member, I want access to my club's events only. | Club membership is checked. Members cannot manage other clubs' events or approve requests. |

### Original features

| Feature | Benefit | Scope |
|---|---|---|
| **Personalised recommendations** | Helps students find events without browsing the entire marketplace. | Editable interests: Leadership, Sports, Tech, Volunteering, Arts and Career. **Recommended for You** ranks approved upcoming events using interests and past verified attendance. Similar-student activity is an optional extension. |
| **Weekly attendance streak** | Makes regular participation visible in the progress screen. | Consecutive Monday-to-Sunday weeks count when they contain verified attendance. Late verification and reversals update the streak. |

| Story | Actor / priority | User need | Acceptance criteria |
|---|---|---|---|
| **US16 · Feature 1** | Student · Must-have | As a student, I want suggestions based on my interests. | Tags can be edited; recommended events reflect interests and verified attendance. New users receive tag-based suggestions. |
| **US17 · Feature 1** | Student · Nice-to-have | As a student, I want suggestions informed by similar students' activity. | Aggregated activity may improve rankings without exposing individual attendance histories. |
| **US18 · Feature 2** | Student · Must-have | As a student, I want to track consecutive weeks of participation. | A week counts once after verification. Pending scans do not count; later corrections update the relevant week. |

**Attendance limitation:** A live QR code could be forwarded to someone at home, and shared account credentials could allow impersonation. The rotating code alone cannot prove physical presence. Offline phone timestamps can also be manipulated, so questionable records may need review rather than automatic approval.

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

The app is a native Kotlin Android app using MVVM with repositories. Screens only display state, ViewModels hold presentation logic, and repositories are the only layer that decides whether data comes from the device or the backend. A Room database stores cached events, bookings and every attendance record scanned on the device, so students can keep working offline. Firebase is the shared backend: Authentication for sign-in, Firestore for shared data, and Cloud Functions for trusted operations (booking, attendance verification, points). A WorkManager job uploads pending records when a connection returns.

**Roles and authentication.** Choosing Student, Club or Admin on launch only selects the sign-in screen and grants nothing. After university sign-in, the backend reads the role stored on the account (set by the backend, never by the app). Security rules and Cloud Functions check it on every request, so a student cannot call club operations, and the UI shows only the screens for the verified role. Admin approval is done in the backend console.

```mermaid
flowchart LR
  subgraph Device["Android app (Kotlin, MVVM)"]
    UI["UI screens (Student, Club)"]
    VM["ViewModels"]
    Repo["Repositories"]
    Room[("Room database: cached events, bookings, pending attendance")]
    Sync["WorkManager sync worker"]
    UI --> VM --> Repo
    Repo <--> Room
    Sync <--> Room
  end
  subgraph Backend["Firebase backend"]
    Auth["Firebase Authentication"]
    FS[("Firestore")]
    CF["Cloud Functions: bookEvent, verifyAttendance, awardPoints"]
    Rules["Security rules (role checks)"]
  end
  Admin["Admin (backend console)"]
  Repo -->|sign in| Auth
  Repo -->|read events, own bookings| FS
  Repo -->|book, check in| CF
  Sync -->|upload pending records| CF
  CF --> FS
  Rules -.protects.- FS
  Admin -->|approve or return events| FS
```

### Data model

All entities use stable IDs (`userId`, `clubId`, `eventId`, `bookingId`, `attendanceId`). `attendanceId` is a UUID created on the device, so a retried upload cannot create a duplicate. The event's QR secret is a server-only field. Points are stored as a ledger (one row per award or reversal) so they can be revoked with an audit trail, and the weekly streak counts calendar weeks containing at least one verified attendance. Booking capacity and clash checks run in one Cloud Function transaction, so two students cannot take the last place or hold overlapping bookings.

```mermaid
erDiagram
  USER {
    string userId PK
    string email
    string name
    string role "student, club, admin"
    string studentId
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
    string title
    datetime startTime
    datetime endTime
    string location
    int capacity
    int pointsRequested
    string status
    bool featured "set by admin"
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
    string status "confirmed, cancelled"
    datetime createdAt
  }
  ATTENDANCE_RECORD {
    string attendanceId PK
    string eventId FK
    string userId FK
    string bookingId FK
    datetime scannedAt
    datetime receivedAt
    string codeWindow
    string nonce
    string deviceId
    string syncStatus "pending, synced"
    string verifyStatus "unverified, verified, rejected"
    string rejectReason
  }
  POINTS_LEDGER {
    string entryId PK
    string userId FK
    string eventId FK
    string attendanceId FK
    int points
    string status "credited, revoked"
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

### Event approval workflow

A club creates an event as a Draft and submits it. The admin approves it or returns it with comments, and a returned event is editable and can be resubmitted. Once Approved, details are locked because security rules reject edits to an approved event, so a club cannot change an event after approval (R1). Approved events open for booking, then become Full, Closed or Cancelled. Cancelling is the club's only action after approval.

```mermaid
stateDiagram-v2
  [*] --> Draft
  Draft --> Submitted: club submits
  Submitted --> Returned: admin returns with comments
  Submitted --> Approved: admin approves
  Returned --> Submitted: club edits and resubmits
  Approved --> Open: booking opens
  Open --> Full: capacity reached
  Full --> Open: a booking is cancelled
  Open --> Closed: booking closes or event ends
  Full --> Closed: event ends
  Approved --> Cancelled: club cancels
  Open --> Cancelled: club cancels
  Full --> Cancelled: club cancels
  Closed --> [*]
  Cancelled --> [*]
  note right of Approved
    Details locked from here on
  end note
```

### Attendance security

The club's device shows a rotating QR code: `HMAC(eventSecret, eventId + 30-second window)`. The club device can generate it offline because it receives the secret when the committee member starts the attendance session. The student scans it and the app saves a record (code, scan time, nonce, device ID). The student device never holds the secret, so it never validates the code itself. The server checks that:

1. the record belongs to the signed-in student,
2. the code is valid for the claimed scan time (one window either side),
3. the student has a confirmed booking,
4. the scan is inside the check-in window (15 minutes before the start to 15 minutes after the end),
5. no attendance exists for this student and event, and the nonce is unused.

Failed records are Rejected with a reason the student can see. Students scan the club's code, and the profile ID card has no barcode, so there is no personal code to share. For emulator testing, the scan screen also accepts the short code typed in by hand, which goes through the same checks.

| Cheating attempt | Defence | Remaining weakness |
|---|---|---|
| Checking in for a friend | Record tied to the signed-in account and its booking, one per student per event, and the server flags a device used by several accounts | A friend who shares their login |
| Checking in from home | Code exists only on the club screen and expires after 30 seconds | Live relay or a photo sent within seconds |
| Checking in after the event | Server checks scan time against the event window and caps upload delay | Changed device clock while offline |
| Faked offline record | Server recomputes the code for the claimed time, the app stores a monotonic clock, and the server flags odd gaps and lone scans | Photographing a real code, setting the clock back and scanning later can pass the code check. Flagged records can be revoked |
| Replay or duplicate | Idempotent `attendanceId`, unique student-event key, nonce | None significant |
| Rooted device | Records signed with a Keystore key bound to the account's device | Root can bypass this, accepted risk |
| Dishonest committee member | Admin can audit counts and revoke | Detected, not prevented |

```mermaid
sequenceDiagram
  participant C as Club device
  participant S as Student app
  participant F as Cloud Function
  participant DB as Firestore
  C->>C: Show code = HMAC(secret, eventId + 30 s window)
  S->>C: Scan QR code
  S->>S: Create record (attendanceId, nonce, scannedAt, deviceId)
  S->>F: submitAttendance(record)
  F->>F: Check account, code for window, booking, time window, duplicate, nonce
  alt all checks pass
    F->>DB: Save attendance as VERIFIED
    F->>DB: Add points ledger entry, update weekly streak
    F-->>S: Verified, points earned
  else a check fails
    F->>DB: Save attendance as REJECTED with reason
    F-->>S: Rejected, show reason
  end
```

### Offline sync

Without a connection the scan still works. The record is saved in Room as Pending Sync and shown in the attendance list, with no points or streak credit yet. WorkManager uploads pending records when the network returns and sets each one to Verified or Rejected from the server's answer. Failed uploads stay Pending and retry with backoff, which is safe because uploads are idempotent. Points and streak credit come only from the server after verification. If a verified record is later invalidated, the ledger gets a reversing entry, totals and streak are recalculated, and the student sees it marked as revoked.

```mermaid
sequenceDiagram
  participant S as Student app
  participant R as Room database
  participant W as WorkManager
  participant F as Cloud Function
  participant DB as Firestore
  S->>R: Save record with status PENDING_SYNC
  S->>S: Show "Pending Sync", no points yet
  Note over S,W: Device is offline
  W->>W: Network connection returns
  W->>R: Load all PENDING_SYNC records
  W->>F: Upload records (signed with device key)
  F->>F: Recompute valid code for scannedAt, check booking, time window, nonce, duplicate, upload delay
  alt record is valid
    F->>DB: Mark VERIFIED, add points, update streak
    F-->>W: verified
    W->>R: Set status VERIFIED
  else record is invalid
    F->>DB: Mark REJECTED with reason
    F-->>W: rejected
    W->>R: Set status REJECTED
  end
  W->>S: Refresh attendance list and points
  Note over W,F: On network error the record stays PENDING and is retried with backoff
```
[If Admin review is handled in the backend, explain that no Admin Android screen is required.]

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

