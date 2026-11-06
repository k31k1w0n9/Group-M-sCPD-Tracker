# 5. UI design and wireframes

The wireframes cover Student and Club Committee journeys. The s-CPD office reviews event submissions in the backend. [Open the Figma file](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=0-1).

Main entry screens use the campus branding. Secondary screens use a compact app bar. The screenshots use sample events and accounts.

### 5.1. Student entry and interests — R6

Students choose Student or Club Committee and sign in with their university account. The sign-in flow includes password recovery and a route back after a network error. Students can select interests for event recommendations.

[<img src="images/wireframes/student-role-selection.png" alt="Choose a role" width="250" />](images/wireframes/student-role-selection.png)

**Choose a role** · [Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=82-101)

<details>
<summary>Sign-in and account recovery</summary>

| Screen | Screen |
|---|---|
| **Student sign-in**<br>[<img src="images/wireframes/student-sign-in.png" alt="Student sign-in" width="220" />](images/wireframes/student-sign-in.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=82-193) | **Sign-in error**<br>[<img src="images/wireframes/student-sign-in-error.png" alt="Sign-in error" width="220" />](images/wireframes/student-sign-in-error.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=82-269) |
| **Password recovery**<br>[<img src="images/wireframes/student-password-recovery.png" alt="Password recovery" width="220" />](images/wireframes/student-password-recovery.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=147-711) | **Sign-in network error**<br>[<img src="images/wireframes/student-network-recovery.png" alt="Sign-in network error" width="220" />](images/wireframes/student-network-recovery.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=147-768) |
| **Choose interests**<br>[<img src="images/wireframes/student-interests.png" alt="Choose interests" width="220" />](images/wireframes/student-interests.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=82-350) |   |

</details>

### 5.2. Event discovery — R2, Feature 1

Home and Explore show featured and recommended events, search and filters. Empty and offline states explain what the student can do next.

<img width="2952" height="2433" alt="STUDENT · Explore" src="https://github.com/user-attachments/assets/a851cde4-a013-4d5e-beda-ffb61c8c5ab6" />

### 5.3. Event details and booking — R2

Event details show availability and the attendance requirements before booking. The booking flow handles full events, schedule clashes, an in-progress request, and a failed or confirmed result.

[<img src="images/checkpoint/student-event-details.png" alt="Event details and attendance requirements" width="250" />](images/checkpoint/student-event-details.png)

**Event details and attendance requirements** · [Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=110-2)

<details>
<summary>Booking and availability</summary>

| Screen | Screen |
|---|---|
| **Event full**<br>[<img src="images/wireframes/student-event-full.png" alt="Event full" width="220" />](images/wireframes/student-event-full.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=82-1460) | **Schedule clash**<br>[<img src="images/wireframes/student-schedule-clash.png" alt="Schedule clash" width="220" />](images/wireframes/student-schedule-clash.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=82-1568) |
| **Review booking**<br>[<img src="images/wireframes/student-booking-review.png" alt="Review booking" width="220" />](images/wireframes/student-booking-review.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=82-1398) | **Booking in progress**<br>[<img src="images/wireframes/student-booking-in-progress.png" alt="Booking in progress" width="220" />](images/wireframes/student-booking-in-progress.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=147-478) |
| **Booking failed**<br>[<img src="images/wireframes/student-booking-failed.png" alt="Booking failed" width="220" />](images/wireframes/student-booking-failed.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=147-536) | **Booking confirmed**<br>[<img src="images/wireframes/student-booking-confirmed.png" alt="Booking confirmed" width="220" />](images/wireframes/student-booking-confirmed.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=82-2052) |

</details>

### 5.4. My Events and cancellations — R2, R3, R4

Students view their bookings and see whether arrival or checkpoint scanning is available. A queued arrival scan stays pending without removing the checkpoint action when it opens. Booking cancellation and organiser cancellation have distinct outcomes.

[<img src="images/checkpoint/student-my-events-arrival-required.png" alt="My Events — arrival required" width="250" />](images/checkpoint/student-my-events-arrival-required.png)

**My Events — arrival required** · [Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=110-125)

<details>
<summary>Upcoming events and checkpoint access</summary>

| Screen | Screen |
|---|---|
| **Waiting for checkpoint — arrival verified**<br>[<img src="images/checkpoint/student-waiting-arrival-verified.png" alt="Waiting for checkpoint — arrival verified" width="220" />](images/checkpoint/student-waiting-arrival-verified.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=115-456) | **Waiting for checkpoint — arrival pending sync**<br>[<img src="images/checkpoint/student-waiting-arrival-pending.png" alt="Waiting for checkpoint — arrival pending sync" width="220" />](images/checkpoint/student-waiting-arrival-pending.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=115-605) |
| **Checkpoint open — arrival verified**<br>[<img src="images/checkpoint/student-checkpoint-open-arrival-verified.png" alt="Checkpoint open — arrival verified" width="220" />](images/checkpoint/student-checkpoint-open-arrival-verified.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=110-441) | **Checkpoint open — arrival pending sync**<br>[<img src="images/checkpoint/student-checkpoint-open-arrival-pending.png" alt="Checkpoint open — arrival pending sync" width="220" />](images/checkpoint/student-checkpoint-open-arrival-pending.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=115-35) |

</details>

<details>
<summary>Cancelling a booking</summary>

| Screen | Screen |
|---|---|
| **Confirm booking cancellation**<br>[<img src="images/wireframes/student-cancel-booking-confirm.png" alt="Confirm booking cancellation" width="220" />](images/wireframes/student-cancel-booking-confirm.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=82-2429) | **Cancellation in progress**<br>[<img src="images/wireframes/student-cancellation-in-progress.png" alt="Cancellation in progress" width="220" />](images/wireframes/student-cancellation-in-progress.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=147-825) |
| **Cancellation failed**<br>[<img src="images/wireframes/student-cancel-failed.png" alt="Cancellation failed" width="220" />](images/wireframes/student-cancel-failed.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=147-595) | **Booking cancelled**<br>[<img src="images/wireframes/student-booking-cancelled.png" alt="Booking cancelled" width="220" />](images/wireframes/student-booking-cancelled.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=82-2364) |
| **Event cancelled by organiser**<br>[<img src="images/wireframes/student-event-cancelled.png" alt="Event cancelled by organiser" width="220" />](images/wireframes/student-event-cancelled.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=147-882) |   |

</details>

### 5.5. Attendance and check-in — R3, R4

Events with a checkpoint require an arrival scan and a checkpoint scan. Students scan the rotating QR or enter the stage’s backup code online. An offline QR scan remains Pending sync until upload. The app distinguishes saved scans, upload or retry, server review and incomplete attendance.

[<img src="images/checkpoint/student-arrival-scanner.png" alt="Scan arrival QR" width="250" />](images/checkpoint/student-arrival-scanner.png)

**Scan arrival QR** · [Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=110-326)

<details>
<summary>Arrival and checkpoint scanning</summary>

| Screen | Screen |
|---|---|
| **Arrival saved — verified**<br>[<img src="images/checkpoint/student-arrival-verified.png" alt="Arrival saved — verified" width="220" />](images/checkpoint/student-arrival-verified.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=115-2) | **Arrival saved — pending sync**<br>[<img src="images/checkpoint/student-arrival-pending-sync.png" alt="Arrival saved — pending sync" width="220" />](images/checkpoint/student-arrival-pending-sync.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=110-389) |
| **Scan checkpoint QR**<br>[<img src="images/checkpoint/student-checkpoint-scanner-arrival-verified.png" alt="Scan checkpoint QR" width="220" />](images/checkpoint/student-checkpoint-scanner-arrival-verified.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=110-642) | **Scan checkpoint — arrival pending sync**<br>[<img src="images/checkpoint/student-checkpoint-scanner-arrival-pending.png" alt="Scan checkpoint — arrival pending sync" width="220" />](images/checkpoint/student-checkpoint-scanner-arrival-pending.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=115-185) |

</details>

<details>
<summary>Camera access and QR errors</summary>

| Screen | Screen |
|---|---|
| **Camera permission required**<br>[<img src="images/wireframes/student-camera-required.png" alt="Camera permission required" width="220" />](images/wireframes/student-camera-required.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=147-3) | **Camera permission denied**<br>[<img src="images/wireframes/student-camera-denied.png" alt="Camera permission denied" width="220" />](images/wireframes/student-camera-denied.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=147-56) |
| **QR unreadable**<br>[<img src="images/wireframes/student-qr-unreadable.png" alt="QR unreadable" width="220" />](images/wireframes/student-qr-unreadable.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=147-109) | **QR expired**<br>[<img src="images/wireframes/student-qr-expired.png" alt="QR expired" width="220" />](images/wireframes/student-qr-expired.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=147-169) |

</details>

<details>
<summary>Manual codes and connection recovery</summary>

| Screen | Screen |
|---|---|
| **Enter arrival code online**<br>[<img src="images/checkpoint/student-arrival-code.png" alt="Enter arrival code online" width="220" />](images/checkpoint/student-arrival-code.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=115-283) | **Enter checkpoint code online**<br>[<img src="images/wireframes/student-checkpoint-code-entry.png" alt="Enter checkpoint code online" width="220" />](images/wireframes/student-checkpoint-code-entry.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=149-63) |
| **Arrival code — connection lost**<br>[<img src="images/wireframes/student-arrival-code-connection-lost.png" alt="Arrival code — connection lost" width="220" />](images/wireframes/student-arrival-code-connection-lost.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=149-126) | **Checkpoint code — connection lost**<br>[<img src="images/checkpoint/student-checkpoint-code.png" alt="Checkpoint code — connection lost" width="220" />](images/checkpoint/student-checkpoint-code.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=110-861) |
| **Checkpoint code — offline**<br>[<img src="images/wireframes/student-checkpoint-code-offline.png" alt="Checkpoint code — offline" width="220" />](images/wireframes/student-checkpoint-code-offline.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=149-189) |   |

</details>

<details>
<summary>Offline sync and attendance outcomes</summary>

| Screen | Screen |
|---|---|
| **Checkpoint saved — pending sync**<br>[<img src="images/checkpoint/student-checkpoint-pending-sync.png" alt="Checkpoint saved — pending sync" width="220" />](images/checkpoint/student-checkpoint-pending-sync.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=110-705) | **Both scans saved — pending sync**<br>[<img src="images/checkpoint/student-both-scans-pending-sync.png" alt="Both scans saved — pending sync" width="220" />](images/checkpoint/student-both-scans-pending-sync.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=115-250) |
| **Sync in progress**<br>[<img src="images/wireframes/student-sync-in-progress.png" alt="Sync in progress" width="220" />](images/wireframes/student-sync-in-progress.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=147-263) | **Uploaded — under review**<br>[<img src="images/wireframes/student-under-review.png" alt="Uploaded — under review" width="220" />](images/wireframes/student-under-review.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=147-293) |
| **Retry failed sync**<br>[<img src="images/wireframes/student-sync-retry.png" alt="Retry failed sync" width="220" />](images/wireframes/student-sync-retry.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=147-325) | **Incomplete attendance**<br>[<img src="images/checkpoint/student-incomplete-attendance.png" alt="Incomplete attendance" width="220" />](images/checkpoint/student-incomplete-attendance.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=110-809) |

</details>

### 5.6. Points and progress — R5, Feature 2

An eligible event earns 3 points once after all required scans are verified. Progress shows verified attendance, points, interest stamps and the weekly streak. Pending scans do not earn credit, and corrections can remove previously credited points.

[<img src="images/checkpoint/student-attendance-verified.png" alt="Attendance verified — +3 pts" width="250" />](images/checkpoint/student-attendance-verified.png)

**Attendance verified — +3 pts** · [Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=110-757)

<details>
<summary>Progress and verified attendance</summary>

| Screen | Screen |
|---|---|
| **Your progress**<br>[<img src="images/wireframes/student-progress.png" alt="Your progress" width="220" />](images/wireframes/student-progress.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=82-4257) | **Points correction**<br>[<img src="images/wireframes/student-points-correction.png" alt="Points correction" width="220" />](images/wireframes/student-points-correction.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=82-3635) |

</details>

### 5.7. Profile and settings — R6

Students view their account and update interests, notification preferences and privacy settings. The Profile attendance entry opens the relevant event stage.

[<img src="images/wireframes/student-profile.png" alt="Student profile" width="250" />](images/wireframes/student-profile.png)

**Student profile** · [Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=82-4650)

<details>
<summary>Account and preferences</summary>

| Screen | Screen |
|---|---|
| **Privacy and settings**<br>[<img src="images/wireframes/student-settings.png" alt="Privacy and settings" width="220" />](images/wireframes/student-settings.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=82-5002) |   |

</details>

### 5.8. Committee entry and access — R6

Committee sign-in checks the account’s assigned role and society. The screens cover sign-in errors and unauthorised committee access.

<img width="1936" height="1089" alt="COMMITTEE · Entry" src="https://github.com/user-attachments/assets/453d9c01-b26a-4c9c-bc9b-c4d30dc15b02" />

### 5.9. Committee dashboard — R1, R3, R6

The dashboard shows event and submission status, the next public event, and shortcuts to creating an event and managing check-in.

<img width="920" height="1931" alt="COMMITTEE · Dashboard" src="https://github.com/user-attachments/assets/53a18d9b-592f-4d95-baae-ad7c783e1fee" />

### 5.10. Event submissions — R1

Submissions groups drafts, pending reviews, decisions and approved events. Committee members can update returned submissions, withdraw a pending submission, and cancel an approved event. The flow shows progress, retry and confirmed outcomes.

[<img src="images/wireframes/committee-submissions.png" alt="Submissions" width="250" />](images/wireframes/committee-submissions.png)

**Submissions** · [Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=147-1040)

<details>
<summary>Create and submit an event</summary>

| Screen | Screen |
|---|---|
| **Create event**<br>[<img src="images/wireframes/committee-create-event.png" alt="Create event" width="220" />](images/wireframes/committee-create-event.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=82-6529) | **Submitting**<br>[<img src="images/wireframes/committee-submitting.png" alt="Submitting" width="220" />](images/wireframes/committee-submitting.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=147-1117) |
| **Submission retry**<br>[<img src="images/wireframes/committee-submit-retry.png" alt="Submission retry" width="220" />](images/wireframes/committee-submit-retry.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=147-1170) | **Submission success**<br>[<img src="images/wireframes/committee-submit-success.png" alt="Submission success" width="220" />](images/wireframes/committee-submit-success.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=147-1227) |

</details>

<details>
<summary>Review and withdrawal</summary>

| Screen | Screen |
|---|---|
| **Changes requested**<br>[<img src="images/wireframes/committee-changes-requested.png" alt="Changes requested" width="220" />](images/wireframes/committee-changes-requested.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=82-6355) | **Pending review**<br>[<img src="images/wireframes/committee-pending-review.png" alt="Pending review" width="220" />](images/wireframes/committee-pending-review.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=82-6434) |
| **Confirm withdrawal**<br>[<img src="images/wireframes/committee-withdraw.png" alt="Confirm withdrawal" width="220" />](images/wireframes/committee-withdraw.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=147-1276) | **Withdrawing**<br>[<img src="images/wireframes/committee-withdrawing.png" alt="Withdrawing" width="220" />](images/wireframes/committee-withdrawing.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=147-1333) |
| **Withdrawn**<br>[<img src="images/wireframes/committee-withdrawn.png" alt="Withdrawn" width="220" />](images/wireframes/committee-withdrawn.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=147-1386) | **Withdrawal retry**<br>[<img src="images/wireframes/committee-withdraw-retry.png" alt="Withdrawal retry" width="220" />](images/wireframes/committee-withdraw-retry.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=147-1443) |

</details>

<details>
<summary>Approved event cancellation</summary>

| Screen | Screen |
|---|---|
| **Approved event**<br>[<img src="images/wireframes/committee-event-approved.png" alt="Approved event" width="220" />](images/wireframes/committee-event-approved.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=82-6231) | **Confirm event cancellation**<br>[<img src="images/wireframes/committee-cancel-confirm.png" alt="Confirm event cancellation" width="220" />](images/wireframes/committee-cancel-confirm.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=82-6643) |
| **Cancelling event**<br>[<img src="images/wireframes/committee-cancelling-event.png" alt="Cancelling event" width="220" />](images/wireframes/committee-cancelling-event.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=149-523) | **Cancellation confirmed**<br>[<img src="images/wireframes/committee-cancel-success.png" alt="Cancellation confirmed" width="220" />](images/wireframes/committee-cancel-success.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=147-1500) |
| **Cancellation retry**<br>[<img src="images/wireframes/committee-cancel-retry.png" alt="Cancellation retry" width="220" />](images/wireframes/committee-cancel-retry.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=147-1557) |   |

</details>

### 5.11. Committee Events — R1, R2, R3

Upcoming events link to bookings. Past events link to attendance summaries.

[<img src="images/wireframes/committee-upcoming-events.png" alt="Upcoming events" width="250" />](images/wireframes/committee-upcoming-events.png)

**Upcoming events** · [Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=82-6943)

<details>
<summary>Upcoming and past events</summary>

| Screen | Screen |
|---|---|
| **Past events**<br>[<img src="images/wireframes/committee-past-events.png" alt="Past events" width="220" />](images/wireframes/committee-past-events.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=82-6823) |   |

</details>

### 5.12. Check-in — R3, R4, R5

The Committee displays the rotating arrival or checkpoint QR with the short-lived backup code. The checkpoint view shows the countdown and an explicit close confirmation. An offline Committee device pauses code refresh until reconnection. Students can still upload previously saved scans during the sync window.

[<img src="images/checkpoint/committee-arrival-qr.png" alt="Active arrival QR" width="250" />](images/checkpoint/committee-arrival-qr.png)

**Active arrival QR** · [Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=110-925)

<details>
<summary>QR display and checkpoint closure</summary>

| Screen | Screen |
|---|---|
| **Checkpoint open**<br>[<img src="images/checkpoint/committee-checkpoint-qr.png" alt="Checkpoint open" width="220" />](images/checkpoint/committee-checkpoint-qr.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=110-1064) | **Offline — checkpoint QR paused**<br>[<img src="images/checkpoint/committee-checkpoint-offline.png" alt="Offline — checkpoint QR paused" width="220" />](images/checkpoint/committee-checkpoint-offline.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=110-1203) |
| **Confirm checkpoint closure**<br>[<img src="images/wireframes/committee-close-checkpoint.png" alt="Confirm checkpoint closure" width="220" />](images/wireframes/committee-close-checkpoint.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=147-1618) | **Checkpoint closed**<br>[<img src="images/wireframes/committee-checkpoint-closed.png" alt="Checkpoint closed" width="220" />](images/wireframes/committee-checkpoint-closed.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=147-1665) |

</details>

### 5.13. Bookings and attendance — R2, R3, R4, R5

The Committee views bookings, live attendance, claims awaiting sync, and the final attendance summary. Missing uploads stay provisional through the sync window. Uploaded claims under review remain unresolved until a decision, and No-show applies only when no claim arrives by the deadline.

[<img src="images/wireframes/committee-bookings-list.png" alt="Event bookings" width="250" />](images/wireframes/committee-bookings-list.png)

**Event bookings** · [Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=82-8117)

<details>
<summary>Attendance lists and summaries</summary>

| Screen | Screen |
|---|---|
| **Live attendee list**<br>[<img src="images/checkpoint/committee-live-attendees.png" alt="Live attendee list" width="220" />](images/checkpoint/committee-live-attendees.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=110-1283) | **Awaiting sync**<br>[<img src="images/checkpoint/committee-awaiting-sync.png" alt="Awaiting sync" width="220" />](images/checkpoint/committee-awaiting-sync.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=115-347) |
| **Attendance summary**<br>[<img src="images/checkpoint/committee-final-summary.png" alt="Attendance summary" width="220" />](images/checkpoint/committee-final-summary.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=110-1365) | **Final attendee list**<br>[<img src="images/checkpoint/committee-final-attendees.png" alt="Final attendee list" width="220" />](images/checkpoint/committee-final-attendees.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=110-1440) |

</details>

### 5.14. Committee account — R6

Committee members can view their verified club access and confirm sign-out.

[<img src="images/wireframes/committee-account.png" alt="Committee account" width="250" />](images/wireframes/committee-account.png)

**Committee account** · [Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=82-8212)

<details>
<summary>Committee account and sign-out</summary>

| Screen | Screen |
|---|---|
| **Sign-out confirmation**<br>[<img src="images/wireframes/committee-sign-out.png" alt="Sign-out confirmation" width="220" />](images/wireframes/committee-sign-out.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=82-8267) |   |

</details>

[Back to project overview](../README.md)
