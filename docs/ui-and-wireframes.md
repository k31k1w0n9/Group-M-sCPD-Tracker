# 5. UI design and wireframes

The wireframes cover Student and Club Committee. The s-CPD office reviews submissions in the backend. [Open the Figma file](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=0-1).

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

- **Student.** Reuse My Events and the event scanner. Show arrival saved, checkpoint open, checkpoint saved or pending sync, and the final outcome. Use **Incomplete attendance** for a verified arrival with a missing checkpoint after the sync deadline.
- **Committee.** Add an **Open checkpoint** action to Active QR and reuse the offline pause frame for a checkpoint state. Adapt the live attendee list and past summary to distinguish arrival, checkpoint and final attendance.
- **Account.** Update the badge copy where needed. The existing account and role screens cover the flow.

The screenshots above show the current Figma frames. The checkpoint states are proposed.

[Back to project overview](../README.md)
