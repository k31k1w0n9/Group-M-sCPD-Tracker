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

| Story | Actor / priority | User need and acceptance criteria | Screen in Section 5 |
|---|---|---|---|
| **US-01 · R1** | Club Committee · Must-have | I want to create and submit events from **Dashboard**. Submission produces **Pending review**. **Changes requested** submissions show feedback and can be edited and resubmitted. **Approved** details are locked. **Rejected** submissions are closed and cannot be booked. | Dashboard / Submission |
| **US-02 · R1** | Admin · Must-have | I want to approve, reject or request changes to event submissions. Admin can approve, request changes or reject in the backend. **Changes requested** allows editing and resubmission; **Rejected** is final; **Approved** details are locked, and the event can be booked. The Committee sees the decision and reason in Submissions.  | Backend only |
| **US-03 · R1, R2** | Club Committee · Must-have | I want to cancel an approved event. Cancellation closes bookings and informs booked students without unlocking approved details. | Submission approved |
| **US-04 · R2** | Student · Must-have | I want to find suitable activities. **Home** and **Explore** show approved events, search, filters, recommendations and useful empty/offline states. | Home / Explore |
| **US-05 · R2** | Student · Must-have | I want to book an available event. **Event details** explains full capacity and clashes; **Review booking** rechecks them on the server. Concurrent requests cannot overbook, and only success opens **Booking confirmed**. | Event details / Review booking |
| **US-06 · R2** | Student · Must-have | I want to check or cancel my booking. **My Events** opens **Your booking**; cancelling releases the place and opens **Booking cancelled**. **Booked** applies only before the event. | My Events / Your booking |
| **US-07 · R2, R5** | Club Committee · Must-have | I want to review participation. Before an event, **Events > Upcoming > Bookings** shows reservations. During it, **Scan > active check-in > Attendee list** shows live check-ins. After it ends, **Events > Past > Attendance summary > Attendee list** shows final outcomes. | Events / Bookings / Attendance summary |

**Check-in and offline attendance**

| Story | Actor / priority | User need and acceptance criteria | Screen in Section 5 |
|---|---|---|---|
| **US-08 · R3** | Club Committee · Must-have | I want to manage check-in using **Scan**. The QR and six-digit backup code refresh every 30 seconds during an active session. Lost connectivity pauses new code display; ending check-in closes the session, not pending claims. | Scan / End check-in |
| **US-09 · R3** | Student · Must-have | I want to scan the displayed event QR. The server ties the scan to the signed-in student’s own booking and rejects a booking mismatch. A live QR relay or shared account may still allow someone to check in for a friend; this remains a security limitation. | Scan |
| **US-10 · R3** | Student · Must-have | I want a fallback when the camera fails. **Enter code manually** checks the current backup code online; an incorrect or expired code is rejected. Manual code entry requires online server verification. If the student is offline, no attendance record is saved or queued; they must reconnect and enter the current code. | Enter code manually |
| **US-11 · R4** | Student · Must-have | I want to keep an offline QR scan. It survives app restarts as **Pending sync**, without awarding points or a streak. | Pending sync |
| **US-12 · R3, R4** | Student · Must-have | I want a reliable verification result. Upload retries after reconnection; valid records become **Verified**, while invalid or duplicate claims become **Rejected** with a reason. Suspicious offline timestamps are flagged for review. | Attendance verification result |
| **US-13 · R3, R4** | Club Committee · Must-have | I want an accurate attendee list. **Attendee list** and **Attendance summary** distinguish **Verified**, **Pending sync**, **Rejected** and **No-show**. A pending scan is not treated as a no-show. | Attendee list / Attendance summary |

**Points and access**

| Story | Actor / priority | User need and acceptance criteria | Screen in Section 5 |
|---|---|---|---|
| **US-14 · R5** | Student · Must-have | I want to track my points. **Progress** and **Attendance history** award **3 points** once per eligible **Verified** attendance. **Pending sync**, **Rejected** and **No-show** earn zero. | Progress / Attendance history |
| **US-15 · R5** | Admin · Must-have | I want to correct incorrect attendance. An authorised backend reversal removes 3 points, records the correction and updates the streak and stamps. | Backend only |
| **US-16 · R6** | Student / Club Committee · Must-have | I want to sign in securely. **Welcome to s-CPD** offers the two app roles; **Sign in to s-CPD** checks university credentials and actual permissions. Invalid sign-in or unauthorised committee access displays an error. | Student entry / Committee entry |
| **US-17 · R6** | Student · Must-have | I want my records kept private. Backend checks prevent access to another student's bookings, points and attendance; **Profile** supports interests, notifications and sign-out. | Profile / Privacy & settings |
| **US-18 · R6** | Club Committee · Must-have | I want access limited to my own society. Backend checks prevent editing other clubs' events or approving submissions. **Account** shows verified access and confirms sign-out. | Account |

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
| **2. Weekly attendance streak** | **Progress** counts consecutive Monday-to-Sunday weeks with verified attendance. Corrections recalculate the streak. Interest stamps are visual rewards, not a third feature. | Progress |

| Story | Actor / priority | User need and acceptance criteria | Screen in Section 5 |
|---|---|---|---|
| **US-19 · Feature 1** | Student · Must-have | I want to choose and edit interests. **Make it yours** saves selected categories; **Interests** changes them later and updates recommendations. | Make it yours / Interests |
| **US-20 · Feature 1** | Student · Must-have | I want relevant recommendations. **Home** explains why suggested approved events match my interests; new students receive interest-based results. | Home / Explore |
| **US-21 · Feature 1** | Student · Nice-to-have | I want suggestions informed by similar students. Aggregated patterns may improve ranking without exposing individual attendance histories. | Home / Explore |
| **US-22 · Feature 2** | Student · Must-have | I want to track my weekly streak and stamps. Only **Verified** attendance counts. Late verification or reversal updates the relevant week and visual rewards. | Progress |

[Back to project overview](../README.md)
