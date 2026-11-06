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
    P["Award 3 points once after eligible attendance is verified"]
    A1["Review event: Approved, Changes requested or Rejected - console"]
    A2["Revoke invalid attendance - console"]
  end
 
  Student --- S1 & S2 & S3 & S4 & S5 & S6 & S7 & S8 & S9
  Committee --- S1 & C1 & C2 & C3 & C4 & C5 & C6
  Admin --- A1 & A2
  S6 -.->|includes| V
  V -.->|Verified and eligible| P
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
  ED -->|Place available and no clash| RB["Review booking"]
  ED -->|No places available| EF["Event full"]
  ED -->|Overlapping booking| CL["Schedule clash"]
  EF -->|Browse alternatives| EX
  CL -->|View existing booking| ME
  RB -->|Confirm| BP["Booking in progress"]
  BP -->|Server confirms| BK["Booking confirmed"]
  BP -->|Capacity changed| EF
  BP -->|Clash found| CL
  BP -->|Request failed or status uncertain| BF["Booking failed"]
  BF -->|Check booking status before retrying| RB
  BK --> ME
 
  ME -->|View booking| VB["Your booking"]
  VB -->|Cancel| CB["Confirm booking cancellation"]
  CB -->|Confirm| CX["Cancellation in progress"]
  CX -->|Server confirms| BC["Booking cancelled"]
  CX -->|Retryable failure| CF["Cancellation failed"]
  CF -->|Retry| CX
  CB -->|Keep booking| VB
  BC --> ME
  ME -->|Organiser cancelled| EC["Event cancelled by organiser"]
 
  ME -->|Open attendance| PH{"Attendance stage for this booking"}
  PF -->|Open attendance| PH
  PH -->|Arrival open| SQ["Scan arrival QR"]
  PH -->|Checkpoint open| CQ["Scan checkpoint QR"]
  PH -->|Checkpoint not open| CW["Wait for checkpoint"]
  PH -->|Already verified| AC["Already checked in"]
  PH -->|Review pending| AR["Under review"]
  PH -->|One stage verified, other required stage missing after deadline| IA["Incomplete attendance"]
  PH -->|Deadline passed without a received claim| NSH["No-show"]
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
  PS -->|Reconnect| UP["Sync in progress"]
  UP -->|Retryable or uncertain result| UR["Retry sync"]
  UR -->|Retry same request| UP
  UP -->|Stage accepted| V
  UP -->|Claim flagged| AR
  UP -->|Claim rejected| RA["Rejected attendance"]
  QE & WE --> PH
  V -->|All required stages verified| AV["Attendance verified"]
  V -->|Checkpoint not yet open| CW
  V -->|Checkpoint open| CQ
  V -->|Another saved scan awaits upload| PS
  AR -->|Office verifies attendance| AV
  AR -->|Office rejects with reason| RA
 
  PG --> AH["Attendance history"]
  PG --> FS["Your first stamp"]
  PG --> PP["Progress points removed"]
  AH --> RA
  AH --> IA
 
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
  SUB -->|Create or edit draft| CE
  CE -->|Submit for review| SP["Submitting"]
  SP -->|Server confirms| SR["Submission received"]
  SP -->|Retryable failure| SF["Submission retry"]
  SF -->|Retry| SP
  SR --> PR["Pending review"]
  DASH -->|Pending review card| PR
  DASH -->|Changes requested card| NC["Submission needs changes"]
  NC -->|Edit submission, resubmit| SP
  SUB -->|Open pending review| PR
  SUB -->|Open changes requested| NC
  SUB -->|View final decision and reason| RJ["Rejected submission (final)"]
  DASH -->|Approved card| AE["Approved event"]
  SUB -->|Open approved event| AE
  AE -->|Cancel event| CA["Confirm event cancellation"]
  CA -->|Confirm| CI["Cancelling event"]
  CI -->|Server confirms| CD["Cancellation confirmed"]
  CI -->|Retryable failure| CR["Cancellation retry"]
  CR -->|Retry| CI
  CD --> EV
  PR -->|Withdraw submission| WC["Confirm withdrawal"]
  WC -->|Confirm| WI["Withdrawing"]
  WI -->|Server confirms| WD["Withdrawn, returned to Draft"]
  WI -->|Retryable failure| WR["Withdrawal retry"]
  WR -->|Retry| WI
  WD -->|Edit draft| CE
  WD -->|Return| SUB
 
  EV -->|Upcoming: View bookings| BL["Event bookings list"]
  EV -->|Upcoming: View submission, Review feedback| PR
  EV -->|Past: View attendance| PV{"Sync deadline passed?"}
  PV -->|No| SY["Awaiting sync"]
  PV -->|Yes| AS["Attendance summary"]
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
  CP -->|Close checkpoint| PC["Confirm checkpoint closure"]
  PC --> CC["Check-in closed"]
  EC --> CC
  CC -->|View attendance| PV
  SY -->|Deadline reached| AS
  AS -->|Flagged claims remain visible| AL
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
| Student scan queue | Pending sync, Sync deadline passed | **Pending sync** describes a scan saved on the student’s device. A queue item can expire without a server rejection. Reconcile an uncertain upload by request ID. |
| Attendance record | Awaiting sync, Under review, Incomplete attendance, Verified, Rejected | **Awaiting sync** means the server may still receive missing claims. **Under review** stays unresolved until an authorised office decision. After the deadline, one verified stage with the other required stage missing is **Incomplete attendance**. A verified arrival at an event where the organiser never opened the required checkpoint goes to office review. **Verified** requires all stages or an authorised office decision. **Rejected** includes the reason. |
| Points | +3 per eligible Verified attendance, 0 otherwise | Award points once after attendance is verified. If an authorised correction invalidates a credited record, reverse its award once and recalculate progress. |
 
### Data model
 
**DG-05 Event lifecycle (R1)**
 
A club creates an event as a Draft and submits it for Pending review. Admin can approve it, request changes, or reject it. The Committee can edit and resubmit events marked Changes requested. Withdrawing a pending submission returns the same event to Draft, where it stays unpublished and can be edited and resubmitted. Rejected events are final and cannot be booked. Approved event details are locked by backend rules. After approval, the Committee can cancel the event, which closes bookings and notifies booked students.
 
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

  subgraph Device["Student scan queue"]
    PENDING["Pending sync"]
    ACK["Server result saved locally"]
    EXPIRED["Sync deadline passed"]
    PENDING -->|Definitive response saved| ACK
    PENDING -->|Confirmed expired without accepted receipt| EXPIRED
  end

  subgraph Attendance["Server attendance outcome"]
    NONE["No claim received"]
    REVIEW["Under review"]
    STAGE["Stage verified"]
    VERIFIED["Verified"]
    INCOMPLETE["Incomplete attendance"]
    REJECTED["Rejected"]
    NONE -->|Online QR or backup code accepted| STAGE
    NONE -->|Uploaded QR accepted| STAGE
    NONE -->|Received evidence flagged| REVIEW
    NONE -->|Received evidence rejected| REJECTED
    STAGE -->|All required stages verified| VERIFIED
    STAGE -->|Other required stage opened but evidence missing at deadline| INCOMPLETE
    STAGE -->|Arrival verified and required checkpoint never opened| REVIEW
    REVIEW -->|Office verifies an individual stage| STAGE
    REVIEW -->|Office verifies attendance with recorded reason| VERIFIED
    REVIEW -->|Office rejects with reason| REJECTED
    VERIFIED -->|Authorised correction with reason| REJECTED
  end

  VERIFIED -->|Eligible and no award recorded| CREDIT["Credit 3 points once"]
  REJECTED -->|Earlier award exists and was not reversed| REV["Reverse the earlier award once"]
```

Before the sync deadline, a booking without a received claim remains unresolved. **Pending sync** and **Sync deadline passed** describe the device queue and do not change the server outcome. An office decision takes precedence over automatic finalisation while a claim is under review. The attendance result and any points ledger entry are saved together so a retry cannot add a second award.

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
    string reviewReason
    string reviewedBy "office account ID, if reviewed"
    datetime reviewedAt
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
