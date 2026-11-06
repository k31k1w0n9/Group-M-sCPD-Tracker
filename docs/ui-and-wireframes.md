# 5. UI design and wireframes

The wireframes cover Student and Club Committee. The s-CPD office reviews submissions in the backend. [Open the Figma file](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=0-1).

The [revised checkpoint screens](#proposed-checkpoint-wireframes) document the latest longer-event attendance flow. The earlier images retain the baseline booking, account and attendance journeys.

Main tabs use the campus photo and branding in the expanded header. The header collapses to a compact dark blue app bar on scroll. Secondary screens start with the compact header.

### Student entry and interests — R6

Student - entry and interests
<img width="1936" height="1133" alt="STUDENT · Entry" src="https://github.com/user-attachments/assets/5089640b-e6f5-4c04-b6f0-de759e6683ff" />

Users choose Student or Club Committee before sign-in. The backend checks the signed-in account’s permissions. Students can select interests for recommendations.

### Event discovery — R2, Feature 1

Student - explore events
<img width="2952" height="2433" alt="STUDENT · Explore" src="https://github.com/user-attachments/assets/a851cde4-a013-4d5e-beda-ffb61c8c5ab6" />

Students browse featured and recommended events, search and filter the listings, and see helpful empty-result or offline states.

### Event details and booking — R2

Student - booking flow
<img width="3460" height="1764" alt="STUDENT · Booking" src="https://github.com/user-attachments/assets/e3332479-1304-4c51-b495-c442de9b0d16" />

Featured and Recommended cards on **Home** and search or filtered results in **Explore** open the same event details and booking flow. Recommended cards explain the match.

Event details show availability and relevant information. The flow covers full events, timetable clashes, booking review and confirmation.

### My Events and cancellations — R2

Student - My Events
<img width="1936" height="1896" alt="STUDENT · My Events" src="https://github.com/user-attachments/assets/fbc3e433-122a-4b5b-90da-b67a8d63858f" />

Students view upcoming and past bookings, open booking details, and cancel a booking. The confirmation and cancelled states show the outcome.

### Attendance and check-in — R3, R4

Student - attendance and check-in
<img width="7524" height="1941" alt="STUDENT · Attendance" src="https://github.com/user-attachments/assets/57184104-4fa9-4309-b11d-b7edc2f2533b" />

Students scan the committee’s rotating QR or enter the active backup code online. A captured QR can show **Pending sync** offline. Invalid and expired codes have separate error states.

### Points and progress — R5, Feature 2

Student - progress and attendance history
<img width="1936" height="1628" alt="STUDENT · Progress" src="https://github.com/user-attachments/assets/dda466bc-6d65-4241-b677-984224fc7cd8" />

Students view verified points, attendance history, interest stamps and their weekly streak. Pending records do not earn points or streak credit; invalid attendance can have its points removed.

### Profile and settings — R6

Student - profile and account settings
<img width="3460" height="1406" alt="STUDENT · Account" src="https://github.com/user-attachments/assets/a74ee55b-092f-4e6c-8eff-cfa1fc43b9f1" />

Students view their account, edit interests and notification preferences, and sign out. Use **University account verified** for university sign-in and **Account verified** for the prototype.

### Committee entry and access — R6

Committee - entry and sign-in
<img width="1936" height="1089" alt="COMMITTEE · Entry" src="https://github.com/user-attachments/assets/453d9c01-b26a-4c9c-bc9b-c4d30dc15b02" />

Committee sign-in checks the account’s assigned role and society. The wireframes show both sign-in errors and unauthorised committee access.

### Committee dashboard — R1, R3, R6

Committee - dashboard
<img width="920" height="1931" alt="COMMITTEE · Dashboard" src="https://github.com/user-attachments/assets/53a18d9b-592f-4d95-baae-ad7c783e1fee" />

The dashboard summarises event and submission statuses, links to event creation and check-in, and shows the next public event. The Account button provides access to the committee profile.

### Event submissions — R1

Committee event submissions
<img width="2952" height="1862" alt="COMMITTEE · Submissions" src="https://github.com/user-attachments/assets/27eb2d00-39f8-48e9-84d8-c9816f1f05af" />

Committee members create drafts and submit events for review. In **Submissions**, they can see **Pending review**, **Changes requested**, or a final **Rejected** decision and its reason. They can edit and resubmit events marked **Changes requested**. Approved details are locked, but the event can be cancelled.

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

**Events > Upcoming** opens bookings. **Scan** opens the live QR and attendee list. **Events > Past** and the closed Scan flow lead to the attendance summary.

The summary distinguishes verified and rejected claims from bookings awaiting sync. Show **No-show** only after the sync deadline when the server has received no claim.

### Committee account — R6

Committee - account and sign-out
<img width="920" height="1062" alt="COMMITTEE · Account" src="https://github.com/user-attachments/assets/483af387-fca3-4742-97cb-cbfb77e126b2" />

Committee members can view their verified club access and sign out with a confirmation step.

## Proposed checkpoint wireframes

The revised 412 px designs below contain **17 Student and 7 Committee states** for the proposed arrival-and-checkpoint flow. For longer events, these replace the earlier single-scan attendance outcomes shown above.

The five-minute checkpoint window and next-day sync deadline are example settings from the [checkpoint proposal](attendance-security-and-sync.md#proposed-late-event-checkpoint). **Pending sync** describes a scan saved on the student's device; the committee uses **No claim yet** until the server receives it. Eligible attendance earns 3 points once after both required scans are verified, and a streak extends at most once per week.

These are static design references. Controls labelled **Demo** illustrate server responses or time passing; tap interactions are not yet wired in Figma.

<details>
<summary>Student — Event details and My Events (2 screens)</summary>

| Screen | Description |
|---|---|
| **Event details — two required scans**<br>[<img src="images/checkpoint/student-event-details.png" alt="Event details — two required scans" width="240" />](images/checkpoint/student-event-details.png)<br>[Open in Figma](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=110-2) | Explains the arrival and checkpoint requirement before booking, including the sync deadline; the exact checkpoint time appears only when it opens. |
| **My Events — arrival required**<br>[<img src="images/checkpoint/student-my-events-arrival-required.png" alt="My Events — arrival required" width="240" />](images/checkpoint/student-my-events-arrival-required.png)<br>[Open in Figma](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=110-125) | The booked event opens the arrival scanner and explains that both QR scans can be saved offline, while manual codes require internet. |

</details>

<details>
<summary>Student — Arrival scan and result (3 screens)</summary>

| Screen | Description |
|---|---|
| **Scan arrival QR**<br>[<img src="images/checkpoint/student-arrival-scanner.png" alt="Scan arrival QR" width="240" />](images/checkpoint/student-arrival-scanner.png)<br>[Open in Figma](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=110-326) | Captures the first scan and offers online manual entry, with separate examples for online verification and offline saving. |
| **Arrival saved — Verified**<br>[<img src="images/checkpoint/student-arrival-verified.png" alt="Arrival saved — Verified" width="240" />](images/checkpoint/student-arrival-verified.png)<br>[Open in Figma](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=115-2) | The server has verified arrival, but the student still needs the checkpoint before attendance earns credit. |
| **Arrival saved — Pending sync**<br>[<img src="images/checkpoint/student-arrival-pending-sync.png" alt="Arrival saved — Pending sync" width="240" />](images/checkpoint/student-arrival-pending-sync.png)<br>[Open in Figma](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=110-389) | The arrival QR is saved on the student's device for later upload, with no attendance credit yet. |

</details>

<details>
<summary>Student — Waiting for the checkpoint (2 screens)</summary>

| Screen | Description |
|---|---|
| **My Events — arrival Verified**<br>[<img src="images/checkpoint/student-waiting-arrival-verified.png" alt="My Events — arrival Verified" width="240" />](images/checkpoint/student-waiting-arrival-verified.png)<br>[Open in Figma](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=115-456) | Returning to My Events preserves the verified arrival while showing that the checkpoint has not opened. |
| **My Events — arrival Pending sync**<br>[<img src="images/checkpoint/student-waiting-arrival-pending.png" alt="My Events — arrival Pending sync" width="240" />](images/checkpoint/student-waiting-arrival-pending.png)<br>[Open in Figma](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=115-605) | Returning to My Events preserves the offline arrival status without revealing the checkpoint's exact time. |

</details>

<details>
<summary>Student — Checkpoint open and scanner (4 screens)</summary>

| Screen | Description |
|---|---|
| **Checkpoint open — arrival Verified**<br>[<img src="images/checkpoint/student-checkpoint-open-arrival-verified.png" alt="Checkpoint open — arrival Verified" width="240" />](images/checkpoint/student-checkpoint-open-arrival-verified.png)<br>[Open in Figma](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=110-441) | Shows the announced window and countdown, with a route to scan the checkpoint QR. |
| **Checkpoint open — arrival Pending sync**<br>[<img src="images/checkpoint/student-checkpoint-open-arrival-pending.png" alt="Checkpoint open — arrival Pending sync" width="240" />](images/checkpoint/student-checkpoint-open-arrival-pending.png)<br>[Open in Figma](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=115-35) | A saved arrival awaiting sync does not block the student from scanning the open checkpoint. |
| **Scan checkpoint QR — arrival Verified**<br>[<img src="images/checkpoint/student-checkpoint-scanner-arrival-verified.png" alt="Scan checkpoint QR — arrival Verified" width="240" />](images/checkpoint/student-checkpoint-scanner-arrival-verified.png)<br>[Open in Figma](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=110-642) | Captures the checkpoint while preserving the verified arrival and showing the remaining scan window. |
| **Scan checkpoint QR — arrival Pending sync**<br>[<img src="images/checkpoint/student-checkpoint-scanner-arrival-pending.png" alt="Scan checkpoint QR — arrival Pending sync" width="240" />](images/checkpoint/student-checkpoint-scanner-arrival-pending.png)<br>[Open in Figma](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=115-185) | Captures the checkpoint while preserving the pending arrival; each scan keeps its own verification status. |

</details>

<details>
<summary>Student — Manual code entry (2 screens)</summary>

| Screen | Description |
|---|---|
| **Enter arrival code — online only**<br>[<img src="images/checkpoint/student-arrival-code.png" alt="Enter arrival code — online only" width="240" />](images/checkpoint/student-arrival-code.png)<br>[Open in Figma](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=115-283) | The current six-digit arrival code replaces the camera scan and requires server verification; it cannot be queued offline. |
| **Enter checkpoint code — online only**<br>[<img src="images/checkpoint/student-checkpoint-code.png" alt="Enter checkpoint code — online only" width="240" />](images/checkpoint/student-checkpoint-code.png)<br>[Open in Figma](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=110-861) | The current checkpoint code needs internet; any saved arrival is uploaded first, and attendance credit requires both stages to pass server verification. |

</details>

<details>
<summary>Student — Pending scans and final outcomes (4 screens)</summary>

| Screen | Description |
|---|---|
| **Checkpoint saved — Pending sync**<br>[<img src="images/checkpoint/student-checkpoint-pending-sync.png" alt="Checkpoint saved — Pending sync" width="240" />](images/checkpoint/student-checkpoint-pending-sync.png)<br>[Open in Figma](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=110-705) | Arrival is verified, while the checkpoint QR waits on the device for upload and server verification. |
| **Both QR scans saved — Pending sync**<br>[<img src="images/checkpoint/student-both-scans-pending-sync.png" alt="Both QR scans saved — Pending sync" width="240" />](images/checkpoint/student-both-scans-pending-sync.png)<br>[Open in Figma](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=115-250) | Both scans are stored separately on the device and must reach the server before the sync deadline. |
| **Attendance verified — +3 pts**<br>[<img src="images/checkpoint/student-attendance-verified.png" alt="Attendance verified — +3 pts" width="240" />](images/checkpoint/student-attendance-verified.png)<br>[Open in Figma](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=110-757) | Both required scans are server verified for an eligible event, earning 3 points once and contributing to the weekly streak. |
| **Incomplete attendance**<br>[<img src="images/checkpoint/student-incomplete-attendance.png" alt="Incomplete attendance" width="240" />](images/checkpoint/student-incomplete-attendance.png)<br>[Open in Figma](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=110-809) | Arrival was verified but no checkpoint reached the server by the deadline, so the event earns no points or streak credit. |

</details>

<details>
<summary>Committee — Arrival QR, checkpoint and offline pause (3 screens)</summary>

| Screen | Description |
|---|---|
| **Active arrival QR — Open checkpoint**<br>[<img src="images/checkpoint/committee-arrival-qr.png" alt="Active arrival QR — Open checkpoint" width="240" />](images/checkpoint/committee-arrival-qr.png)<br>[Open in Figma](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=110-925) | The committee displays the rotating arrival QR and backup code, then opens and announces one checkpoint in the event's final third. |
| **Checkpoint open**<br>[<img src="images/checkpoint/committee-checkpoint-qr.png" alt="Checkpoint open" width="240" />](images/checkpoint/committee-checkpoint-qr.png)<br>[Open in Figma](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=110-1064) | Displays the checkpoint QR, online backup code and countdown, with access to the live attendee list and Close checkpoint. |
| **Offline — checkpoint QR paused**<br>[<img src="images/checkpoint/committee-checkpoint-offline.png" alt="Offline — checkpoint QR paused" width="240" />](images/checkpoint/committee-checkpoint-offline.png)<br>[Open in Figma](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=110-1203) | Hides unusable codes while the committee is offline; students' previously captured QR scans remain available for later sync. |

</details>

<details>
<summary>Committee — Live attendance, sync grace period and final results (4 screens)</summary>

| Screen | Description |
|---|---|
| **Live attendee list**<br>[<img src="images/checkpoint/committee-live-attendees.png" alt="Live attendee list" width="240" />](images/checkpoint/committee-live-attendees.png)<br>[Open in Figma](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=110-1283) | Separates arrival, checkpoint and overall status using server-received claims; No claim yet does not mean the student has no scan saved locally. |
| **Awaiting sync — checkpoint closed**<br>[<img src="images/checkpoint/committee-awaiting-sync.png" alt="Awaiting sync — checkpoint closed" width="240" />](images/checkpoint/committee-awaiting-sync.png)<br>[Open in Figma](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=115-347) | Keeps received scan results visible and allows uploads until the sync deadline, leaving missing claims unresolved. |
| **Past attendance summary**<br>[<img src="images/checkpoint/committee-final-summary.png" alt="Past attendance summary" width="240" />](images/checkpoint/committee-final-summary.png)<br>[Open in Figma](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=110-1365) | After the sync deadline, summarises Verified, Incomplete attendance, Rejected and No-show outcomes without awarding credit to incomplete or rejected attendance. |
| **Final attendee list**<br>[<img src="images/checkpoint/committee-final-attendees.png" alt="Final attendee list" width="240" />](images/checkpoint/committee-final-attendees.png)<br>[Open in Figma](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=110-1440) | Shows each student's arrival, checkpoint and final outcome, retaining rejection reasons and distinguishing a missing checkpoint from no claim received. |

</details>

[Back to project overview](../README.md)
