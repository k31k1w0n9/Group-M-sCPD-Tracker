[s-CPD overview](../README.md) › **4. Technical design** › Architecture

## 4. Technical design

*Part 1 of 4: Architecture. The other parts are [screen flows](4-2-screen-flows.md), [status terms and data model](4-3-status-and-data-model.md), and [attendance security and offline sync](4-4-attendance-security-and-sync.md).*

### Architecture

The Kotlin Android app uses MVVM. ViewModels expose screen state, and repositories read from Room or the Firebase backend. Room caches events and bookings and stores queued QR scans. Firebase Authentication handles sign-in, Firestore stores shared data, Cloud Functions handle bookings and attendance, and Cloud Messaging sends reminders and cancellation notices. WorkManager uploads queued scans when the device reconnects.

Students use Home, Explore, My Events, Progress and Profile. Committees use Dashboard, Events, Scan and Submissions. The s-CPD office reviews submissions and corrects attendance in the backend console, so Admin has no Android screens.

**Roles and authentication.** The role picker only chooses which sign-in screen opens; it grants nothing. After sign-in, the backend checks the account, its assigned role and its club membership on every request. The prototype uses pre-approved test accounts, and a production build would use university single sign-on, subject to access. The profile badge reads **University account verified** after university sign-in and **Account verified** for prototype accounts.

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

---

[← Previous: 3. Challenges, risks, and assumptions](3-challenges-risks-assumptions.md) · [Next: Screen flows →](4-2-screen-flows.md)
