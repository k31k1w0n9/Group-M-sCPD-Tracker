# 4. Technical design
 
### Architecture
 
The Kotlin Android app uses MVVM. ViewModels expose screen state, and repositories read from Room or the Firebase backend. Room caches events and bookings and stores queued QR scans. Firebase Authentication handles sign-in, Firestore stores shared data, Cloud Functions handle bookings and attendance, and Cloud Messaging sends reminders and cancellation notices. WorkManager uploads queued scans when the device reconnects.
 
Students use Home, Explore, My Events, Progress and Profile. Committees use Dashboard, Events, Scan and Submissions. The s-CPD office reviews submissions and corrects attendance in the backend console.
 
**Roles and authentication.** The role picker opens the relevant sign-in flow. The backend checks the authenticated account, assigned role and club membership. The prototype uses pre-approved test accounts. A production build would use university single sign-on, subject to access. The profile badge refers to account verification. Use **University account verified** after university sign-in and **Account verified** for prototype accounts.
 
**DG-01 Use-case overview (R1–R6)**
 
```mermaid
flowchart LR
  Student(["Student"])
  Committee(["Club Committee"])
  Admin(["Admin (s-CPD office)"])
 
  subgraph App["Android app"]
    direction TB
    S1["Sign in and verify account role"]
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
    P["Award 3 points after eligible attendance is verified"]
    A1["Review event: Approved, Changes requested or Rejected - console"]
    A2["Revoke invalid attendance - console"]
  end
 
  Student --- S1 & S2 & S3 & S4 & S5 & S6 & S7 & S8 & S9
  Committee --- S1 & C1 & C2 & C3 & C4 & C5 & C6
  Admin --- A1 & A2
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
  Admin -->|record decisions and corrections| CF
```
 
### Screen flows
 
The flows use the Figma screen names where applicable. An event’s attendance requirement determines whether students need an arrival scan only or both an arrival scan and a later checkpoint. The booking review explains the requirement before confirmation.
 
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
 
  ME -->|Open attendance| PH{"Attendance stage for this booking"}
  PF -->|Open attendance| PH
  PH -->|Arrival open| SQ["Scan arrival QR"]
  PH -->|Checkpoint open| CQ["Scan checkpoint QR"]
  PH -->|Checkpoint not open| CW["Wait for checkpoint"]
  PH -->|Already verified| AC["Already checked in"]
  CW -->|Checkpoint opens| CQ
  SQ -->|Online QR accepted| V{"Which required stages are verified?"}
  CQ -->|Online QR accepted| V
  SQ & CQ -->|Offline QR captured| PS["Attendance pending sync"]
  SQ & CQ -->|Expired code| QE["Event QR expired"]
  SQ & CQ -->|Other event| WE["Wrong event"]
  SQ & CQ -->|Offline, no scan| NI["No connection"]
  SQ & CQ --> MC["Enter stage code manually"]
  MC -->|Offline, no record saved| NI
  MC --> IC["Invalid code"]
  MC --> CE["Code expired"]
  MC -->|Online code accepted| V
  PS -->|Accepted after sync| V
  QE & WE --> PH
  V -->|All required stages| AV["Attendance verified"]
  V -->|Checkpoint still required| CW
  V -->|Another saved scan awaits upload| PS
 
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
  SI -->|committee access verified| DASH
 
  subgraph Tabs["Committee tab bar"]
    DASH["Committee dashboard"]
    EV["Committee Events"]
    SC["Scan"]
    SUB["Submissions"]
  end
 
  DASH -->|Create event| CE["Create event"]
  CE -->|Submit for review| SR["Submission received"]
  SR --> PR["Pending review"]
  DASH -->|Pending review card| PR
  DASH -->|Changes requested card| NC["Submission needs changes"]
  NC -->|Edit submission, resubmit| SR
  SUB -->|Open pending review| PR
  SUB -->|Open changes requested| NC
  SUB -->|View final decision and reason| RJ["Rejected submission (final)"]
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
  ST -->|View live attendees| LA["Live attendee list"]
  LA -->|Back to active scan| SC
  ST -->|Open required checkpoint| CP["Checkpoint open"]
  CP -->|View live attendees| LA
  ST & CP -->|Offline| PA["Check-in paused"]
  ST -->|End check-in| EC["Confirm end check-in"]
  CP -->|Close checkpoint| EC
  EC --> CC["Check-in closed"]
  CC -->|Uploads still accepted| SY["Awaiting sync"]
  SY -->|View results as decisions are recorded| AS
  CC -->|Back to Events| EV
 
  DASH --> CACC["Committee account"]
  CACC --> SOC["Sign out confirmation"]
  SOC --> RS
```
 
### Status terms
 
| Area | Terms | Notes |
|---|---|---|
| Event review | Draft, Pending review, Approved, Changes requested, Rejected | Draft is not a review status. Admin reviews submissions in the backend: **Approved** locks event details; **Changes requested** allows edits and resubmission; **Rejected** is final and cannot be booked. |
| Booking / participation | Booked, Cancelled, No-show | **Booked** reserves a place. **Cancelled** releases it. Assign **No-show** after the sync deadline if the server has received no attendance claim. A received claim still under review stays unresolved. |
| Attendance record | Pending sync, Awaiting sync, Under review, Incomplete attendance, Verified, Rejected | **Pending sync** describes a scan saved on the student’s device. The committee sees server-received claims and uses **Awaiting sync** while it can still receive missing claims. **Under review** means a received claim is awaiting an office decision. A verified arrival with a missing required checkpoint becomes **Incomplete attendance** after the deadline. **Verified** requires all stages. **Rejected** includes the reason. |
| Points | +3 per eligible Verified attendance, 0 otherwise | Award points once after the server verifies all required stages. If verified attendance is later invalidated, reverse the 3 points and recalculate progress. |
 
### Data model
 
**DG-05 Event lifecycle (R1)**
 
A club creates an event as a Draft and submits it for Pending review. Admin can approve it, request changes, or reject it. The Committee can edit and resubmit events marked Changes requested; Rejected events are final and cannot be booked. Approved event details are locked by backend rules. After approval, the Committee can cancel the event, which closes bookings and notifies booked students.
 
```mermaid
stateDiagram-v2
  [*] --> Draft
  Draft --> PendingReview: club submits for review
  PendingReview --> Draft: club withdraws submission
  PendingReview --> ChangesRequested: admin requests changes
  PendingReview --> Approved: admin approves
  PendingReview --> Rejected: admin rejects
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
  Rejected --> [*]
  note right of Approved
    Details locked from here on (R1)
  end note
```
 
**DG-06 Booking and attendance lifecycle (R2–R5)**
 
```mermaid
flowchart LR
  subgraph Booking["Booking status"]
    BOOKED["Booked (before event)"]
    CANCELLED["Cancelled"]
    NOSHOW["No-show"]
    BOOKED -->|Student cancels or event is cancelled| CANCELLED
    BOOKED -->|Sync deadline passes with no claim received| NOSHOW
  end

  subgraph Attendance["Attendance record status"]
    NONE["No attendance record"]
    PENDING["Pending sync"]
    REVIEW["Under review"]
    STAGE["Stage verified"]
    VERIFIED["Verified"]
    INCOMPLETE["Incomplete attendance"]
    REJECTED["Rejected"]
    NONE -->|Offline QR scan captured| PENDING
    NONE -->|Online QR or backup code accepted| STAGE
    NONE -->|Server rejects attempt| REJECTED
    PENDING -->|Sync accepted| STAGE
    PENDING -->|Suspicious claim received| REVIEW
    REVIEW -->|Claim accepted| STAGE
    REVIEW -->|Review rejects claim| REJECTED
    PENDING -->|Sync rejected or upload window expires| REJECTED
    STAGE -->|All required stages verified, award 3 points if eligible| VERIFIED
    STAGE -->|Verified arrival but required checkpoint missing after deadline| INCOMPLETE
    VERIFIED -->|Later invalidated, reverse 3 points| REJECTED
  end
```

Before the sync deadline, a booking without a received claim remains unresolved. An offline scan can appear as **Pending sync** on the student’s device while the server is still waiting for it.

**DG-07 Entity-relationship diagram (R1–R5)**
 
Each queued scan has a client request ID for retries. The server keeps one attendance outcome per student and event, linked to the booking, and one evidence record per required stage. A ledger records each points award or reversal. Streaks use verified attendance by event week. Interest stamps and milestones are derived from attendance, event tags and points. Recommendations use saved interests and verified attendance history. Booking capacity and schedule clashes are checked in the same transaction.
 
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
    string reviewStatus "draft, pending_review, changes_requested, approved, rejected"
    string reviewComment "admin feedback"
    string eventState "booking_open, full, check_in_open, check_in_closed, cancelled"
    string checkInSessionId
    string attendanceMode "arrival_only, arrival_and_checkpoint"
    datetime checkInOpenedAt
    datetime checkInClosedAt
    datetime checkpointOpenedAt
    datetime checkpointClosedAt
    datetime syncDeadline
    bool featured "shown in Home Featured section"
    string posterUrl
    string venueApprovalUrl
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
    string status "awaiting_sync, under_review, incomplete, verified, rejected"
    string rejectReason
  }
  ATTENDANCE_STAGE {
    string attendanceId PK, FK
    string stage PK "arrival, checkpoint"
    string entryMethod "qr, backup_code"
    string clientRequestId
    datetime clientCapturedAt
    datetime receivedAt
    string codeWindow
    string tokenId
    string status "under_review, verified, rejected"
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
  LOCAL_SCAN_QUEUE {
    string requestId PK
    string userId
    string eventId
    string capturedQrToken
    string stage "arrival, checkpoint"
    datetime clientCapturedAt
    string localStatus
  }
  USER ||--o{ CLUB_MEMBER : "belongs to"
  CLUB ||--o{ CLUB_MEMBER : has
  CLUB ||--o{ EVENT : runs
  EVENT ||--o{ BOOKING : receives
  USER ||--o{ BOOKING : makes
  BOOKING ||--o| ATTENDANCE_RECORD : "leads to"
  EVENT ||--o{ ATTENDANCE_RECORD : has
  USER ||--o{ ATTENDANCE_RECORD : submits
  ATTENDANCE_RECORD ||--o{ ATTENDANCE_STAGE : "holds stage evidence"
  ATTENDANCE_RECORD ||--o{ POINTS_LEDGER : has award or reversal entries
  USER ||--o{ POINTS_LEDGER : owns
  USER ||--o| STREAK : has
  USER ||--o{ USER_INTEREST : selects
  INTEREST_TAG ||--o{ USER_INTEREST : chosen
  EVENT ||--o{ EVENT_TAG : tagged
  INTEREST_TAG ||--o{ EVENT_TAG : labels
```

`LOCAL_SCAN_QUEUE` is stored in Room. The QR authentication key is held by the backend. See [attendance security and offline sync](attendance-security-and-sync.md) for the token and verification rules.

[Back to project overview](../README.md)
