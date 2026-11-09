[s-CPD overview](../README.md) › **4. Technical design** › Screen flows

## 4. Technical design

*Part 2 of 4: Screen flows. The other parts are [architecture](4-1-architecture.md), [status terms and data model](4-3-status-and-data-model.md), and [attendance security and offline sync](4-4-attendance-security-and-sync.md).*

### Screen flows

The flows use the screen names from the Figma file, shown on the [Student](5-1-student-wireframes.md) and [Committee](5-2-committee-wireframes.md) wireframe pages. Each event's attendance requirement decides whether students need an arrival scan only, or an arrival scan and a later checkpoint. **Review booking** explains the requirement before the student confirms.

**DG-03 Student screen flow (R2–R6)**

```mermaid
flowchart TD
  RS["Choose a role"] --> SI["Student sign-in"]
  SI -->|first sign-in| MY["Make it yours (choose interests)"]
  SI -->|returning| HOME
  MY --> HOME

  subgraph Tabs["Student tab bar"]
    HOME["Home"]
    EX["Explore"]
    ME["My Events"]
    PG["Your progress"]
    PF["Profile"]
  end

  HOME --> ED["Event details"]
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

  ME -->|No bookings| MEE["My Events empty"]
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
  PH -->|Checkpoint not open| CW["Waiting for checkpoint"]
  PH -->|Already verified| AC["Already checked in"]
  PH -->|Review pending| AR["Uploaded — under review"]
  PH -->|One stage verified, other required stage missing after deadline| IA["Incomplete attendance"]
  PH -->|Deadline passed without a received claim| NSH["No-show"]
  CW -->|Checkpoint opens| CQ
  SQ -->|Online QR accepted| V{"Which required stages are verified?"}
  CQ -->|Online QR accepted| V
  SQ & CQ -->|Offline QR captured| PS["Saved — pending sync"]
  SQ & CQ -->|Expired code| QE["QR expired"]
  SQ & CQ -->|Unreadable code| QU["QR unreadable"]
  SQ & CQ -->|Other event| WE["Wrong event"]
  SQ & CQ -->|Offline, no scan| NI["No connection"]
  SQ & CQ -->|No camera permission| CP["Camera permission required"]
  SQ & CQ --> MC["Enter stage code online"]
  MC -->|Offline, no record saved| NI
  MC -->|Invalid or expired code| MCE["Code rejected"]
  MC -->|Online code accepted| V
  PS -->|Reconnect| UP["Sync in progress"]
  UP -->|Retryable or uncertain result| UR["Retry failed sync"]
  UR -->|Retry same request| UP
  UP -->|Stage accepted| V
  UP -->|Claim flagged| AR
  UP -->|Claim rejected| RA["Rejected attendance"]
  QE & QU & WE --> PH
  V -->|All required stages verified| AV["Attendance verified"]
  V -->|Checkpoint not yet open| CW
  V -->|Checkpoint open| CQ
  V -->|Another saved scan awaits upload| PS
  AR -->|Office verifies attendance| AV
  AR -->|Office rejects with reason| RA

  PG --> AH["Attendance history"]
  PG --> FS["Your first stamp"]
  PG --> PP["Points correction"]
  AH --> RA
  AH --> IA

  PF --> PSET["Privacy and settings"]
  PSET --> NS["Notifications"]
  PSET --> IN["Edit interests"]
  PF --> AO["Account"]
  AO --> SO["Sign-out confirmation"]
  SO --> RS
```

**DG-04 Committee screen flow (R1, R3, R6)**

```mermaid
flowchart TD
  RS["Choose a role"] --> SI["Committee sign-in"]
  SI -->|role not committee| NA["Access not authorised"]
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
  SP -->|Server confirms| SR["Submission success"]
  SP -->|Retryable failure| SF["Submission retry"]
  SF -->|Retry| SP
  SR --> PR["Pending review"]
  DASH -->|Pending review card| PR
  DASH -->|Changes requested card| NC["Changes requested"]
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
  WI -->|Server confirms| WD["Returned to draft"]
  WI -->|Retryable failure| WR["Withdrawal retry"]
  WR -->|Retry| WI
  WD -->|Edit draft| CE
  WD -->|Return| SUB

  EV -->|Upcoming: View bookings| BL["Event bookings"]
  EV -->|Upcoming: View submission or feedback| PR
  EV -->|Past: View attendance| PV{"Sync deadline passed?"}
  PV -->|No| SY["Awaiting sync"]
  PV -->|Yes| AS["Attendance summary"]
  AS --> AL["Final attendee list"]
  EV -->|No events| EE["Committee Events empty"]

  AE -->|Start check-in| ST["Active arrival QR"]
  SC -->|Active session| ST
  ST -->|View live attendees| LA["Live attendee list"]
  LA -->|Back to active QR| SC
  ST -->|Open required checkpoint| CP["Checkpoint open"]
  CP -->|View live attendees| LA
  ST & CP -->|Offline| PA["Offline — checkpoint QR paused"]
  ST -->|End check-in| EC["Confirm end check-in"]
  CP -->|Close checkpoint| PC["Confirm checkpoint closure"]
  PC --> CC["Checkpoint closed"]
  EC --> CC
  CC -->|View attendance| PV
  SY -->|Deadline reached| AS
  AS -->|Flagged claims remain visible| AL
  CC -->|Back to Events| EV

  DASH --> CACC["Committee account"]
  CACC --> SOC["Sign-out confirmation"]
  SOC --> RS
```

---

[← Previous: Architecture](4-1-architecture.md) · [Next: Status terms and data model →](4-3-status-and-data-model.md)
