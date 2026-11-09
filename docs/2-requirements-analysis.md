[s-CPD overview](../README.md) › **2. Requirements analysis**

## 2. Requirements analysis

### Functional requirements

| Ref | Area | Expected behaviour |
|:---:|---|---|
| **R1** | Event review | A Club Committee submits an event for **Pending review**. Admin approves it, requests changes or rejects it. A **Changes requested** submission can be edited and resubmitted. **Rejected** is final, and the event cannot be booked. **Approved** event details are locked. |
| **R2** | Discovery and booking | Students browse approved events, book available places and manage their bookings in **My Events**. |
| **R3** | Check-in | The Committee shows a rotating QR and backup code. Each check-in is checked against the student's booking and the event times. |
| **R4** | Offline use | A QR scan can be saved as **Pending sync** and verified later. Manual code entry needs internet. |
| **R5** | Points and progress | Eligible **Verified** attendance earns **3 points** once. Points are reversed if the record is later found invalid. |
| **R6** | Access | Students and Club Committees use the Android app. Admin approvals and corrections happen in the backend only. |

### Non-functional requirements

| Area | Proposed test target |
|---|---|
| **Security** | The backend denies every tested cross-role, cross-club and cross-student request. |
| **Reliability** | Offline scans survive an app restart, and resubmitting a scan never adds extra points. |
| **Booking integrity** | Of 20 simultaneous attempts for the last place, at most one booking succeeds. |
| **Responsiveness** | On the required emulator, an offline scan is saved and **Pending sync** appears within 2 seconds. |
| **Usability** | Booking and attendance failures show a clear message, and main touch targets are at least 48 dp. |

### User stories and acceptance criteria

The **Screen in Section 5** column names screens on the [Student wireframes](5-1-student-wireframes.md) and [Committee wireframes](5-2-committee-wireframes.md) pages.

**Event review and booking**

| Story | Actor / priority | User need and acceptance criteria | Screen in Section 5 |
|---|---|---|---|
| **US-01 · R1** | Club Committee · Must-have | I want to create and submit events from **Dashboard**. Submitting creates a **Pending review** submission. A **Changes requested** submission shows the feedback and can be edited and resubmitted. **Approved** details are locked. A **Rejected** submission is closed and cannot be booked. | Dashboard / Submissions |
| **US-02 · R1** | Admin · Must-have | I want to approve, reject or request changes to event submissions in the backend. **Changes requested** allows editing and resubmission, **Rejected** is final, and **Approved** locks the details and opens the event for booking. The Committee sees the decision and reason in Submissions. | Backend only |
| **US-03 · R1, R2** | Club Committee · Must-have | I want to cancel an approved event. Cancelling closes bookings and tells booked students, without unlocking the approved details. | Approved event |
| **US-04 · R2** | Student · Must-have | I want to find activities that suit me. **Home** and **Explore** show approved events, search, filters, recommendations, and clear empty and offline states. | Home / Explore |
| **US-05 · R2** | Student · Must-have | I want to book an available event. **Event details** explains when an event is full or clashes with a booking, and **Review booking** checks both again on the server. Simultaneous requests cannot overbook, and only a successful request opens **Booking confirmed**. | Event details / Review booking |
| **US-06 · R2** | Student · Must-have | I want to check or cancel my booking. **My Events** opens the booking, and cancelling releases the place and opens **Booking cancelled**. **Booked** applies only before the event. | My Events |
| **US-07 · R2, R5** | Club Committee · Must-have | I want to see who is taking part. Before the event, **Events › Upcoming › Bookings** lists reservations. During it, **Scan › Attendee list** shows live check-ins. Afterwards, **Events › Past › Attendance summary** shows the final outcomes. | Events / Bookings / Attendance summary |

**Check-in and offline attendance**

| Story | Actor / priority | User need and acceptance criteria | Screen in Section 5 |
|---|---|---|---|
| **US-08 · R3** | Club Committee · Must-have | I want to run check-in from **Scan**. The QR and six-digit backup code refresh every 30 seconds while a session is active. Losing the connection pauses new codes. Ending check-in closes the session but not claims that are still pending. | Active arrival QR / Checkpoint open |
| **US-09 · R3** | Student · Must-have | I want to scan the event QR. The server ties the scan to my own booking and rejects a mismatch. A relayed live QR or a shared account could still let someone check in for a friend; this remains a known limitation. | Scan arrival QR |
| **US-10 · R3** | Student · Must-have | I want a fallback when the camera fails. **Enter code** checks the current backup code online and rejects a wrong or expired code. If I am offline, nothing is saved or queued, so I reconnect and enter the current code. | Enter arrival code online |
| **US-11 · R4** | Student · Must-have | I want my offline QR scan kept. It survives an app restart as **Pending sync** and earns no points or streak until it is verified. | Arrival saved — pending sync |
| **US-12 · R3, R4** | Student · Must-have | I want a clear result. Uploads retry after reconnecting. Valid records become **Verified**, and invalid or duplicate claims become **Rejected** with a reason. Suspicious offline timestamps are flagged for review. | Sync in progress / Rejected attendance |
| **US-13 · R3, R4** | Club Committee · Must-have | I want an accurate attendee list. **Attendee list** and **Attendance summary** separate **Verified**, **Pending sync**, **Rejected** and **No-show**. A pending scan is never counted as a no-show. | Live attendee list / Attendance summary |

**Points and access**

| Story | Actor / priority | User need and acceptance criteria | Screen in Section 5 |
|---|---|---|---|
| **US-14 · R5** | Student · Must-have | I want to track my points. **Progress** and **Attendance history** award **3 points** once for each eligible **Verified** attendance. **Pending sync**, **Rejected** and **No-show** earn nothing. | Your progress / Attendance history |
| **US-15 · R5** | Admin · Must-have | I want to correct invalid attendance. An authorised backend reversal removes the 3 points, records the correction, and updates the streak and stamps. | Backend only (student sees Points correction) |
| **US-16 · R6** | Student / Club Committee · Must-have | I want to sign in securely. **Welcome to s-CPD** offers the two app roles, and **Sign in to s-CPD** checks university credentials and actual permissions. A failed sign-in or unauthorised committee access shows an error. | Choose a role / Sign-in |
| **US-17 · R6** | Student · Must-have | I want my records kept private. The backend blocks access to other students' bookings, points and attendance. **Profile** lets me change interests and notifications and sign out. | Profile / Privacy and settings |
| **US-18 · R6** | Club Committee · Must-have | I want access limited to my own society. The backend blocks editing other clubs' events and approving submissions. **Account** shows verified access and confirms sign-out. | Committee account |

**Supplementary acceptance criteria for staged attendance (US-08–US-15)**

These criteria extend the stories above for implementation. Check-in is either arrival only, or arrival plus one later checkpoint.

| Ref | Acceptance criteria |
|---|---|
| **R3** | Show the required stages before booking. Validate the signed-in account, booking, event, stage and code window. Each stage has its own code, so an arrival code cannot satisfy a checkpoint. Manual entry needs a connection. |
| **R4** | Save each offline QR separately with a request ID. A pending arrival upload does not block the checkpoint scanner when it opens. The queue survives an app restart and retries with the same request ID. Keep the raw token until a definitive server decision is stored on the device. A student's **Pending sync** is local, while the Committee's **Awaiting sync** means the server is still waiting for evidence. |
| **R3, R4** | Missing stages stay provisional until the sync deadline. At the deadline, one verified stage with the other required stage missing becomes **Incomplete attendance**, and no claim at all becomes **No-show**. A received claim under review stays unresolved. If the organiser never opens a required checkpoint, a verified arrival goes to office review. |
| **R5** | Award 3 points once for an eligible event after every required stage is verified, or after an authorised office decision verifies attendance. Pending, incomplete and rejected outcomes earn nothing. An authorised correction reverses an award once and recalculates progress. |

### Role permissions

| Action | Student | Club Committee | Admin (backend only) |
|---|:---:|:---:|:---:|
| Browse events and manage own bookings | Yes | No | No |
| Scan QR and view own points | Yes | No | No |
| Create drafts, edit drafts or Changes requested submissions, cancel approved events | No | Own club | No |
| Run check-in and view club bookings and attendance | No | Own club | No |
| Approve, request changes to or reject event submissions | No | No | Yes |
| Reverse invalid attendance and points | No | No | Yes |

### Original features

| Feature | Value and scope | Screen in Section 5 |
|---|---|---|
| **1. Personalised recommendations** | Interests in Leadership, Sports, Tech, Volunteering, Arts and Career improve discovery. **Recommended for you** uses saved interests and verified attendance history; similar-student data is optional. | Choose interests / Home / Explore |
| **2. Weekly attendance streak** | **Progress** counts consecutive Monday-to-Sunday weeks with verified attendance, and corrections recalculate the streak. Interest stamps are visual rewards, not a third feature. | Your progress |

| Story | Actor / priority | User need and acceptance criteria | Screen in Section 5 |
|---|---|---|---|
| **US-19 · Feature 1** | Student · Must-have | I want to choose and edit my interests. **Make it yours** saves the selected categories, and **Interests** changes them later and updates recommendations. | Choose interests / Edit interests |
| **US-20 · Feature 1** | Student · Must-have | I want relevant recommendations. **Home** explains why each suggested event matches my interests, and new students get interest-based results. | Home / Explore |
| **US-21 · Feature 1** | Student · Nice-to-have | I want suggestions informed by similar students. Aggregated patterns may improve the ranking without exposing anyone's attendance history. | Home / Explore |
| **US-22 · Feature 2** | Student · Must-have | I want to track my weekly streak and stamps. Only **Verified** attendance counts, and late verification or a reversal updates the right week and rewards. | Your progress / Your first stamp |

---

[Next: 3. Challenges, risks, and assumptions →](3-challenges-risks-assumptions.md)
