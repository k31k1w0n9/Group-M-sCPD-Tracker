[s-CPD overview](../README.md) › **4. Technical design** › Status terms and data model

## 4. Technical design

*Part 3 of 4: Status terms and data model. The other parts are [architecture](4-1-architecture.md), [screen flows](4-2-screen-flows.md), and [attendance security and offline sync](4-4-attendance-security-and-sync.md).*

### Status terms

| Area | Terms | Notes |
|---|---|---|
| Event review | Draft, Pending review, Approved, Changes requested, Rejected | Draft is not a review status. Admin reviews submissions in the backend: **Approved** locks the event details, **Changes requested** allows edits and resubmission, and **Rejected** is final and cannot be booked. |
| Booking / participation | Booked, Cancelled, No-show | **Booked** reserves a place and **Cancelled** releases it. **No-show** is assigned after the sync deadline when the server has received no attendance claim. A claim still under review stays unresolved. |
| Student scan queue | Pending sync, Sync deadline passed | **Pending sync** is a scan saved on the student's device. A queued item can expire without a server rejection. An uncertain upload is reconciled by request ID. |
| Attendance record | Awaiting sync, Under review, Incomplete attendance, Verified, Rejected | **Awaiting sync** means the server may still receive missing claims. **Under review** stays unresolved until an authorised office decision. After the deadline, one verified stage with the other required stage missing is **Incomplete attendance**. If the organiser never opened a required checkpoint, a verified arrival goes to office review. **Verified** needs every stage or an authorised office decision. **Rejected** always includes a reason. |
| Points | +3 per eligible Verified attendance, 0 otherwise | Points are awarded once, after attendance is verified. If an authorised correction invalidates a credited record, the award is reversed once and progress is recalculated. |

### Data model

**DG-05 Event lifecycle (R1)**

A club creates an event as a Draft and submits it for Pending review. Admin approves it, requests changes or rejects it. The Committee can edit and resubmit an event marked Changes requested. Withdrawing a pending submission returns the same event to Draft, where it stays unpublished and can be edited and resubmitted. Rejected events are final and cannot be booked. Backend rules lock approved event details. After approval, the Committee can cancel the event, which closes bookings and notifies booked students.

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

Before the sync deadline, a booking without a received claim stays unresolved. **Pending sync** and **Sync deadline passed** describe the device queue and do not change the server outcome. While a claim is under review, an office decision takes priority over automatic finalisation. The attendance result and any points ledger entry are saved together, so a retry cannot add a second award.

**DG-07 Entity-relationship diagram (R1–R5)**

Each queued scan has a client request ID for retries. The server keeps one attendance outcome per student and event, linked to the booking, and one evidence record per required stage. A ledger records every points award or reversal. Streaks use verified attendance by event week. Interest stamps and milestones come from attendance, event tags and points. Recommendations use saved interests and verified attendance history. Capacity and schedule clashes are checked in the same transaction as the booking.

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

`LOCAL_SCAN_QUEUE` lives in Room on the device. The backend holds the QR authentication key. The token and verification rules are on the [attendance security and offline sync](4-4-attendance-security-and-sync.md) page.

---

[← Previous: Screen flows](4-2-screen-flows.md) · [Next: Attendance security and offline sync →](4-4-attendance-security-and-sync.md)
