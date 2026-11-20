# 2. Requirements analysis

### Functional requirements

| Ref | Area | Expected behaviour |
|:---:|---|---|
| **R1** | Event review | Club Committee submits an event for **Pending review**. Admin can approve it, request changes, or reject it. **Changes requested** submissions can be edited and resubmitted; **Rejected** is final and cannot be booked. **Approved** event details are locked. |
| **R2** | Discovery and booking | Students browse approved events, reserve available places and manage bookings through **My Events**. |
| **R3** | Check-in | Committee displays a rotating QR and backup code. Student check-ins are validated against bookings and event times. |
| **R4** | Offline use | QR scans can be saved as **Pending sync** and verified later. Manual code entry requires internet. |
| **R5** | Points and progress | Eligible **Verified** attendance earns **3 points** once. Points can be reversed if a record is later invalidated. |
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

The screen references in the story tables point to [Section 5: UI design and wireframes](ui-and-wireframes.md).

**Event review and booking**

| Story | Actor / priority | User story | Acceptance criteria | Screen in Section 5 |
|---|---|---|---|---|
| **US-01 · R1** | Club Committee · Must-have | As a club committee member, I want to create an event and submit it from **Dashboard**, so that the s-CPD office can approve it for points before students see it. | A valid submission becomes **Pending review** and is not shown to students.<br>A field that breaks a rule (title 3–80 characters, description at least 20, capacity 1–500, 1–3 tags, start at least 2 days ahead, end after start) shows its error, and nothing is submitted.<br>**Changes requested** shows the office's comment and allows editing and resubmission. **Approved** details are read-only. **Rejected** is final and can't be booked. | Dashboard / Submission |
| **US-02 · R1** | Admin · Must-have | As the s-CPD office, I want to approve, reject or request changes to event submissions in the backend, so that only events that deserve s-CPD points are published. | **Approved** makes the event visible in Explore and bookable, and locks its details.<br>**Changes requested** returns it to the committee with a reason; they can edit and resubmit.<br>**Rejected** is final; the event can never be booked.<br>The committee sees each decision and its reason in Submissions. | Backend only |
| **US-03 · R1, R2** | Club Committee · Must-have | As a club committee member, I want to cancel an approved event, so that booked students aren't left waiting for an event that won't happen. | Cancelling needs a reason of at least 10 characters.<br>Bookings then close, every booked or waitlisted place changes to **Event cancelled**, and those students see the reason in My Events.<br>The approved details stay locked. | Submission approved |
| **US-04 · R2** | Student · Must-have | As a student, I want suitable events shown first, so that I can find one worth my time without scrolling through every event. | A **suitable** event is approved, not cancelled, still to come, not already booked by me, and doesn't overlap any of my bookings.<br>**Home** ranks suitable events: +3 for each tag matching my interests, +1 for each tag from events I attended (up to +2), +1 if it starts within 7 days and −2 if it is full. Ties go to the earliest start.<br>Each card shows its main reason, for example "Because you like Tech".<br>**Explore** search matches the title, club, venue or tag, with an empty state when nothing matches and an offline banner when there is no connection. | Home / Explore |
| **US-05 · R2** | Student · Must-have | As a student, I want to book an available event, so that I'm guaranteed a place. | Confirming with places left and no clash opens **Booking confirmed**, and places left drop by 1.<br>Two taps on Confirm create one booking. When two students try for the last place, exactly one is booked.<br>If the event overlaps one of my bookings, booking is disabled and the message names the other event and its start time.<br>If the event is full, the button changes to **Join waitlist** (US-23). | Event details / Review booking |
| **US-06 · R2** | Student · Must-have | As a student, I want to check or cancel my booking, so that my place goes to someone else if I can't attend. | **Your booking** shows the date, venue and the scans the event needs.<br>Cancelling more than 2 hours before the start releases my place, or gives it to the first student on the waitlist, and opens **Booking cancelled**.<br>Within 2 hours of the start there is no Cancel button, and the screen says cancellations have closed.<br>Offline, the booking stays and an error is shown. | My Events / Your booking |
| **US-07 · R2, R5** | Club Committee · Must-have | As a club committee member, I want to review participation before, during and after my event, so that I can plan the room and check who attended. | Before the event, **Bookings** lists each booked student's name and ID, and the waitlist count.<br>During check-in, **Attendee list** shows "x of y checked in", updated as each student is verified, with filters for Checked in, Not yet and Flagged.<br>After the event, **Attendance summary** shows each student's final outcome. | Events / Bookings / Attendance summary |
| **US-23 · R2** | Student · Nice-to-have | As a student, I want to join a waitlist when an event is full, so that I still have a chance of going. | A full event offers **Join waitlist** and shows my position, for example "You're #1 on the waitlist".<br>When a booked student cancels, the first student on the waitlist is booked automatically. | Event details / My Events |
| **US-24 · R2** | Admin · Must-have | As the s-CPD office, I want limits on bookings, so that one student can't hold places that others would use. | A student can hold at most 5 active bookings.<br>Cancelling closes 2 hours before the start.<br>After 3 no-shows in a semester, booking is paused, with a message saying why. | Event details |
| **US-25 · R1** | Club Committee · Must-have | As a club committee member, I want a clear way forward when an approved event has to change, so that students only ever see details the office approved. | An approved event opens read-only with "Approved events can't be changed. Cancel and resubmit instead."<br>Any edit attempt leaves the event **Approved** and unchanged. | Submission approved |
| **US-26 · R1** | Club Committee · Must-have | As a club committee member, I want to request s-CPD points and explain why, so that the office can judge whether the event deserves them. | With **Request s-CPD points** on, the reason must be at least 20 characters or the form shows an error.<br>With it off, no reason is needed and the event earns no points. | Create event |

**Check-in and offline attendance**

| Story | Actor / priority | User story | Acceptance criteria | Screen in Section 5 |
|---|---|---|---|---|
| **US-08 · R3** | Club Committee · Must-have | As a club committee member, I want to run check-in from **Scan**, so that students can prove they are in the room. | Starting check-in shows a QR code and a 6-digit backup code, and both change every 30 seconds.<br>If the committee phone goes offline, no new code is shown and the screen says check-in is paused.<br>Opening the checkpoint shows a separate checkpoint code.<br>Ending check-in stops new codes; scans already saved offline are still accepted for 2 hours. | Scan / End check-in |
| **US-09 · R3** | Student · Must-have | As a student, I want to scan the event QR, so that my attendance is recorded in seconds, even in a crowded room. | A booked student who scans the live code and confirms with their fingerprint or screen lock gets **Verified** for that stage.<br>A code more than one 30-second window old, a changed code, a code for another event or for the wrong stage is **Rejected** with the reason.<br>Known limitation: a live code relayed to someone outside, or a shared login, can still pass (Section 3). | Scan |
| **US-10 · R3** | Student · Must-have | As a student, I want to enter a backup code when my camera fails, so that a broken camera doesn't cost me my attendance. | The current 6-digit code entered online gives **Verified** for that stage.<br>A wrong or expired code shows an error. After 5 wrong codes in one session, code entry is disabled for that session.<br>Offline, code entry is disabled with a message, and nothing is saved or queued. | Enter code manually |
| **US-11 · R4** | Student · Must-have | As a student at a venue with no signal, I want my QR scan saved on my phone, so that a bad connection doesn't cost me my attendance. | Scanning offline shows the stage as **Saved on phone** and the result as **Pending sync**.<br>Until the server verifies it, the scan earns 0 points, adds no streak week, and Progress shows "1 check-in waiting to sync".<br>The scan stays on the phone (in Room) if the app is closed or restarted, until the server answers.<br>When the connection returns, it uploads automatically, with no tap needed. | Pending sync |
| **US-12 · R3, R4** | Student · Must-have | As a student, I want a clear result for each uploaded scan, so that I can contact the organiser while it can still be fixed. | Each upload becomes **Verified** or **Rejected** with a reason. The same scan uploaded twice counts once.<br>The server judges time by its own clock only: the code must have been issued during that event's check-in, and the upload must arrive within 2 hours of check-in closing. The time the phone reports is kept for diagnosis only, so changing the phone's clock can't make a late scan count.<br>The check-in screen shows when the phone last synced. | Attendance verification result |
| **US-13 · R3, R4** | Club Committee · Must-have | As a club committee member, I want an accurate attendee list, so that I can tell who really attended. | **Attendee list** and **Attendance summary** separate **Verified**, **Awaiting sync**, **Rejected** and **No-show**.<br>A student whose scan may still be uploading shows as **Awaiting sync**, not as a no-show, until the 2-hour upload window ends.<br>Claims flagged for review, such as one phone used by two accounts, are marked **Flagged**. | Attendee list / Attendance summary |
| **US-27 · R4** | Student · Must-have | As a student, I want a warning before I sign out with scans not yet synced, so that I don't lose attendance by accident. | With 1 or more scans saved on the phone, **Sign out** first shows how many would be lost, with **Stay signed in** as an option.<br>With none saved, sign-out happens straight away. | Profile |

**Points and access**

| Story | Actor / priority | User story | Acceptance criteria | Screen in Section 5 |
|---|---|---|---|---|
| **US-14 · R5** | Student · Must-have | As a student, I want to track my points, so that I know how far I am from the s-CPD requirement. | Each eligible event adds exactly 3 points once, when all its required scans are **Verified**.<br>**Pending sync**, **Incomplete attendance**, **Rejected** and **No-show** add 0.<br>The **Progress** total equals the sum of the points history, and **Attendance history** shows each event's outcome. | Progress / Attendance history |
| **US-15 · R5** | Admin · Must-have | As the s-CPD office, I want to reverse points for attendance later found invalid, so that students can't keep points they shouldn't have. | A reversal adds one −3 entry with the office's reason to the student's points history, and the record shows **Rejected** with that reason. Reversing the same record again adds nothing.<br>The streak and stamps are then worked out again without that event (see Feature 2). | Backend only |
| **US-16 · R6** | Student / Club Committee · Must-have | As a student or committee member, I want to sign in with my university account, so that I only get the features my role allows. | The role comes from the account, not from the role picked on **Welcome to s-CPD**.<br>An account signing in under the wrong role sees an error and nothing opens.<br>A wrong password shows "Email or password not recognised" and clears the password field.<br>While signing in, the button is disabled, so the request can't be sent twice. | Student entry / Committee entry |
| **US-17 · R6** | Student · Must-have | As a student, I want my records kept private, so that other students can't see my bookings, attendance or points. | The backend denies any request for my bookings, attendance or points from an account other than mine or the s-CPD office.<br>**Profile** lets me change my interests and notification settings, and sign out. | Profile / Privacy & settings |
| **US-18 · R6** | Club Committee · Must-have | As a club committee member, I want access limited to my own society, so that one club can't change another club's events. | The dashboard lists only my club's events. The backend denies edits to other clubs' events and any approval request.<br>**Account** shows verified committee access and asks me to confirm before signing out. | Account |
| **US-28 · R5** | Admin · Nice-to-have | As the s-CPD office, I want committee members to earn no points for their own club's events, so that clubs can't award points to themselves. | A member of club A who checks in at club A's event has their attendance recorded, and 0 points are added. | Backend only |

**Supplementary acceptance criteria for staged attendance (US-08–US-15)**

These criteria extend the submitted stories for implementation. Check-in is either arrival only or arrival plus one later checkpoint.

| Ref | Acceptance criteria |
|---|---|
| **R3** | Show the required stages before booking. Validate the signed-in account, booking, event, stage and code window. Each stage uses its own code, and an arrival code cannot satisfy a checkpoint. Manual entry requires an online connection. |
| **R4** | Save each captured offline QR independently with a request ID. Pending arrival sync does not block the checkpoint scanner when it opens. The queue survives app restart and retries the same request ID. Preserve the raw token until a definitive server decision is stored locally. Student **Pending sync** is local, while Committee **Awaiting sync** describes an outcome still waiting for server evidence. |
| **R3, R4** | Keep missing stages provisional until the sync deadline. At the deadline, one verified stage with the other required stage missing becomes **Incomplete attendance**, and no claim received by the server becomes **No-show**. A received claim under review stays unresolved. If the organiser never opens a required checkpoint, a verified arrival goes to office review for an authorised decision. |
| **R5** | Award 3 points once for an eligible event after all required stages are verified or an authorised office decision verifies attendance. Pending, incomplete and rejected outcomes earn zero. An authorised correction reverses an existing award once and recalculates progress. |

**Attendance limitation:** A live QR or backup code can be relayed to somebody outside the venue, and a shared login can't be told apart from the real student. A phone's clock can also be changed, so the server never uses it to decide a result. It accepts a code only within the 30-second window it was issued in. It accepts a saved offline scan only if the code was issued during check-in and the upload reaches the server within 2 hours of check-in closing, both measured by server time. Claims that still look suspicious, such as one phone used by two accounts, are flagged for the s-CPD office to review.

**Attempts to cheat**

| Story | Actor | Attempt | Expected system response |
|---|---|---|---|
| **AS-01 · R3, R4** | Student | Scans a photo of the QR that a friend sent, while offline, and uploads it hours later. | Rejected if it reaches the server more than 2 hours after check-in closed. |
| **AS-02 · R3** | Student | Signs out and in on one phone to check in friends. | A second account on the same phone at the same event is refused, and both accounts are flagged. |
| **AS-03 · R3** | Student | Hands an unlocked phone to a friend at the event. | The phone asks for the fingerprint or screen lock before sending a check-in. |
| **AS-04 · R3** | Club Committee | Posts the live QR in the club group chat. | Every claim is stored with its device, and unusual patterns are flagged for the office. |
| **AS-05 · R2** | Student | Books every event and doesn't turn up. | The limits in US-24 apply. |
| **AS-06 · R1, R5** | Club Committee | Creates events just to give members points. | Nothing is public until the office approves it (US-02), and members get no points for their own club's events (US-28). |
| **AS-07 · R3** | Student | Changes a QR code or calls the backend directly. | A code whose authentication tag doesn't match is rejected. Only the signed-in account is trusted from the phone. |
| **AS-08 · R3** | Student | Guesses backup codes. | After 5 wrong codes in one session, code entry is disabled for that student. |
| **AS-09 · R4** | Student | Sets the phone's clock back so a late offline scan looks on time. | The server uses its own receipt time and the issue time inside the code, so the scan is rejected. |

### Revisions after Part 1 feedback

- Every user story now states its benefit ("so that…"), and the story and its acceptance criteria are in separate columns.
- Acceptance criteria describe the system's behaviour with measurable values instead of repeating the story.
- US-04 defines a **suitable** event and how events are ranked.
- US-11 clarifies what a student sees and what counts while a scan is waiting to sync.
- Feature 2 explains, with an example, how a reversal changes the streak.
- The attendance limitation above, Section 3 and Section 4 explain how a changed phone clock is handled: the server only uses its own time.
- New stories US-23 to US-28 and the attempts-to-cheat table cover waitlists, booking limits, changes after approval, points requests, sign-out with unsynced scans and own-club points.

### Role permissions

| Action | Student | Club Committee | Admin (backend only) |
|---|:---:|:---:|:---:|
| Browse events and manage own bookings | Yes | No | No |
| Scan QR and view own points | Yes | No | No |
| Create drafts; edit drafts or Changes requested submissions; cancel approved events | No | Own club | No |
| Run check-in, view club bookings/attendance | No | Own club | No |
| Approve, request changes to, or reject event submissions | No | No | Yes |
| Reverse invalid attendance and points | No | No | Yes |

### Original features

| Feature | Value and scope | Screen in Section 5 |
|---|---|---|
| **1. Personalised recommendations** | Interests in Leadership, Sports, Tech, Volunteering, Arts and Career improve discovery. **Recommended for you** uses saved interests and verified attendance history; similar-student data is optional. | Make it yours / Home / Explore |
| **2. Weekly attendance streak** | **Progress** counts consecutive Monday-to-Sunday weeks (Malaysia time) with at least one verified attendance. Weeks with no approved events, such as exam weeks, are skipped. If the office reverses an attendance, that week stops counting and the streak is worked out again: for example, verified weeks −1, −2 and −3 give a 3-week streak, and reversing week −2 leaves a 1-week streak. Interest stamps are visual rewards, not a third feature. | Progress |

| Story | Actor / priority | User story | Acceptance criteria | Screen in Section 5 |
|---|---|---|---|---|
| **US-19 · Feature 1** | Student · Must-have | As a student, I want to choose and edit my interests, so that recommendations match what I care about from my first sign-in. | A student with no saved interests sees **Make it yours** before Home.<br>Saving interests changes the ranking on Home straight away. They can be changed later in **Interests**. | Make it yours / Interests |
| **US-20 · Feature 1** | Student · Must-have | As a student, I want to know why an event is recommended, so that I can decide quickly whether it suits me. | Each recommended card shows its main reason (scoring in US-04).<br>A new student with no attendance history is ranked by interests and start date only. | Home / Explore |
| **US-21 · Feature 1** | Student · Nice-to-have | As a student, I want suggestions informed by similar students, so that I discover events I wouldn't have searched for. | Ranking may use patterns combined across many students, and never shows any individual's attendance. | Home / Explore |
| **US-22 · Feature 2** | Student · Must-have | As a student, I want to track my weekly streak and stamps, so that I'm encouraged to keep attending through the semester. | Only **Verified** attendance counts towards the streak and stamps.<br>Each verified event gives one stamp per interest tag on it.<br>A late verification or a reversal updates the week it belongs to (Feature 2). | Progress |

[Back to project overview](../README.md)
