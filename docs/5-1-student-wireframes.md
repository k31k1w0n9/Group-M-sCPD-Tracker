[s-CPD overview](../README.md) › **5. UI design and wireframes** › Student

## 5. UI design and wireframes

*Part 1 of 2: Student journeys. The [Committee journeys](5-2-committee-wireframes.md) are on the next page. Every image links to its frame in the [Figma file](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=0-1).*

Main tabs open with an expanded campus header that collapses into a compact dark-blue bar when the student scrolls. Secondary screens use the compact bar. The screens use sample accounts and static states, and canvas annotations are left out of the exports. Open a section's drop-down to see its other states.

### Student entry and interests — R6

Students choose Student or Club Committee before signing in. The prototype uses approved test accounts; production sign-in depends on access to university single sign-on. Password recovery and network errors keep the student signed out. New students then pick interests for their recommendations.

[<img src="images/student/role-selection.png" alt="Choose a role" width="250" />](images/student/role-selection.png)

**Choose a role** · [Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-5)

<details>
<summary>Sign-in, recovery and interests</summary>

| Screen | Screen |
|---|---|
| **Student sign-in**<br>[<img src="images/student/sign-in.png" alt="Student sign-in" width="220" />](images/student/sign-in.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-97) | **Sign-in error**<br>[<img src="images/student/sign-in-error.png" alt="Sign-in error" width="220" />](images/student/sign-in-error.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-173) |
| **Password recovery**<br>[<img src="images/student/password-recovery.png" alt="Password recovery" width="220" />](images/student/password-recovery.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-6550) | **Sign-in network error**<br>[<img src="images/student/network-recovery.png" alt="Sign-in network error" width="220" />](images/student/network-recovery.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-6608) |
| **Choose interests**<br>[<img src="images/student/interests.png" alt="Choose interests" width="220" />](images/student/interests.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-254) |    |

</details>

### Event discovery — R2, Feature 1

Home and Explore show featured and recommended events, search and filters. Each recommendation says why it matches. Empty and offline states tell the student what to do next.

| Home | Explore |
|---|---|
| [<img src="images/student/home.png" alt="Home" width="220" />](images/student/home.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-1044) | [<img src="images/student/explore.png" alt="Explore" width="220" />](images/student/explore.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-838) |

<details>
<summary>Loading, search and offline states</summary>

| Screen | Screen |
|---|---|
| **Home loading**<br>[<img src="images/student/home-loading.png" alt="Home loading" width="220" />](images/student/home-loading.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-1291) | **Explore loading**<br>[<img src="images/student/explore-loading.png" alt="Explore loading" width="220" />](images/student/explore-loading.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-1227) |
| **No search results**<br>[<img src="images/student/search-empty.png" alt="No search results" width="220" />](images/student/search-empty.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-691) | **Explore offline**<br>[<img src="images/student/explore-offline.png" alt="Explore offline" width="220" />](images/student/explore-offline.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-759) |

</details>

### Event details and booking — R2

Event details show availability and the attendance requirements before booking. The booking flow covers full events, schedule clashes, a request in progress, and a failed or confirmed result.

[<img src="images/student/event-details.png" alt="Event details and attendance requirements" width="250" />](images/student/event-details.png)

**Event details and attendance requirements** · [Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-4537)

<details>
<summary>Booking and availability</summary>

| Screen | Screen |
|---|---|
| **Event full**<br>[<img src="images/student/event-full.png" alt="Event full" width="220" />](images/student/event-full.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-1453) | **Schedule clash**<br>[<img src="images/student/schedule-clash.png" alt="Schedule clash" width="220" />](images/student/schedule-clash.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-1561) |
| **Review booking**<br>[<img src="images/student/booking-review.png" alt="Review booking" width="220" />](images/student/booking-review.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-1397) | **Booking in progress**<br>[<img src="images/student/booking-in-progress.png" alt="Booking in progress" width="220" />](images/student/booking-in-progress.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-6305) |
| **Booking failed**<br>[<img src="images/student/booking-failed.png" alt="Booking failed" width="220" />](images/student/booking-failed.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-6363) | **Booking confirmed**<br>[<img src="images/student/booking-confirmed.png" alt="Booking confirmed" width="220" />](images/student/booking-confirmed.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-1923) |

</details>

### My Events and cancellations — R2, R3, R4

My Events lists the student's bookings and shows whether the arrival or checkpoint scan is open. An arrival scan waiting to upload does not hide the checkpoint action once it opens. Cancelling a booking and an organiser cancelling the event lead to different outcomes.

[<img src="images/student/my-events-arrival-required.png" alt="My Events — arrival required" width="250" />](images/student/my-events-arrival-required.png)

**My Events — arrival required** · [Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-4680)

<details>
<summary>Upcoming events and checkpoint access</summary>

| Screen | Screen |
|---|---|
| **My Events empty**<br>[<img src="images/student/my-events-empty.png" alt="My Events empty" width="220" />](images/student/my-events-empty.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-2104) | **Waiting for checkpoint — arrival verified**<br>[<img src="images/student/waiting-arrival-verified.png" alt="Waiting for checkpoint — arrival verified" width="220" />](images/student/waiting-arrival-verified.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-5694) |
| **Waiting for checkpoint — arrival pending sync**<br>[<img src="images/student/waiting-arrival-pending.png" alt="Waiting for checkpoint — arrival pending sync" width="220" />](images/student/waiting-arrival-pending.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-5839) | **Checkpoint open — arrival verified**<br>[<img src="images/student/checkpoint-open-arrival-verified.png" alt="Checkpoint open — arrival verified" width="220" />](images/student/checkpoint-open-arrival-verified.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-4930) |
| **Checkpoint open — arrival pending sync**<br>[<img src="images/student/checkpoint-open-arrival-pending.png" alt="Checkpoint open — arrival pending sync" width="220" />](images/student/checkpoint-open-arrival-pending.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-5380) |    |

</details>

<details>
<summary>Cancelling a booking</summary>

| Screen | Screen |
|---|---|
| **Confirm booking cancellation**<br>[<img src="images/student/cancel-booking-confirm.png" alt="Confirm booking cancellation" width="220" />](images/student/cancel-booking-confirm.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-2222) | **Cancellation in progress**<br>[<img src="images/student/cancellation-in-progress.png" alt="Cancellation in progress" width="220" />](images/student/cancellation-in-progress.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-6666) |
| **Cancellation failed**<br>[<img src="images/student/cancel-failed.png" alt="Cancellation failed" width="220" />](images/student/cancel-failed.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-6422) | **Booking cancelled**<br>[<img src="images/student/booking-cancelled.png" alt="Booking cancelled" width="220" />](images/student/booking-cancelled.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-2157) |
| **Event cancelled by organiser**<br>[<img src="images/student/event-cancelled.png" alt="Event cancelled by organiser" width="220" />](images/student/event-cancelled.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-6723) |    |

</details>

### Attendance and check-in — R3, R4

Events with a checkpoint need an arrival scan and a later checkpoint scan. Students scan the rotating QR, or enter the current stage's backup code while online. Each offline scan stays **Pending sync** on the device until the app stores a definitive server response. The screens separate saved scans, retries, office review, rejection and incomplete attendance.

[<img src="images/student/arrival-scanner.png" alt="Scan arrival QR" width="250" />](images/student/arrival-scanner.png)

**Scan arrival QR** · [Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-4828)

<details>
<summary>Arrival and checkpoint scanning</summary>

| Screen | Screen |
|---|---|
| **Arrival saved — verified**<br>[<img src="images/student/arrival-verified.png" alt="Arrival saved — verified" width="220" />](images/student/arrival-verified.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-5341) | **Arrival saved — pending sync**<br>[<img src="images/student/arrival-pending-sync.png" alt="Arrival saved — pending sync" width="220" />](images/student/arrival-pending-sync.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-4890) |
| **Scan checkpoint QR**<br>[<img src="images/student/checkpoint-scanner-arrival-verified.png" alt="Scan checkpoint QR" width="220" />](images/student/checkpoint-scanner-arrival-verified.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-5079) | **Scan checkpoint QR — arrival pending sync**<br>[<img src="images/student/checkpoint-scanner-arrival-pending.png" alt="Scan checkpoint QR — arrival pending sync" width="220" />](images/student/checkpoint-scanner-arrival-pending.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-5530) |

</details>

<details>
<summary>Camera access and QR errors</summary>

| Screen | Screen |
|---|---|
| **Camera permission required**<br>[<img src="images/student/camera-required.png" alt="Camera permission required" width="220" />](images/student/camera-required.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-5984) | **Camera permission denied**<br>[<img src="images/student/camera-denied.png" alt="Camera permission denied" width="220" />](images/student/camera-denied.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-6037) |
| **QR unreadable**<br>[<img src="images/student/qr-unreadable.png" alt="QR unreadable" width="220" />](images/student/qr-unreadable.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-6090) | **QR expired**<br>[<img src="images/student/qr-expired.png" alt="QR expired" width="220" />](images/student/qr-expired.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-6150) |

</details>

<details>
<summary>Manual codes and connection recovery</summary>

| Screen | Screen |
|---|---|
| **Enter arrival code online**<br>[<img src="images/student/arrival-code.png" alt="Enter arrival code online" width="220" />](images/student/arrival-code.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-5631) | **Enter checkpoint code online**<br>[<img src="images/student/checkpoint-code-entry.png" alt="Enter checkpoint code online" width="220" />](images/student/checkpoint-code-entry.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-6782) |
| **Arrival code — connection lost**<br>[<img src="images/student/arrival-code-connection-lost.png" alt="Arrival code — connection lost" width="220" />](images/student/arrival-code-connection-lost.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-6845) | **Checkpoint code — connection lost**<br>[<img src="images/student/checkpoint-code.png" alt="Checkpoint code — connection lost" width="220" />](images/student/checkpoint-code.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-5276) |
| **Checkpoint code — offline**<br>[<img src="images/student/checkpoint-code-offline.png" alt="Checkpoint code — offline" width="220" />](images/student/checkpoint-code-offline.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-6908) |    |

</details>

<details>
<summary>Offline sync and attendance outcomes</summary>

| Screen | Screen |
|---|---|
| **Checkpoint saved — pending sync**<br>[<img src="images/student/checkpoint-pending-sync.png" alt="Checkpoint saved — pending sync" width="220" />](images/student/checkpoint-pending-sync.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-5141) | **Both scans saved — pending sync**<br>[<img src="images/student/both-scans-pending-sync.png" alt="Both scans saved — pending sync" width="220" />](images/student/both-scans-pending-sync.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-5592) |
| **Sync in progress**<br>[<img src="images/student/sync-in-progress.png" alt="Sync in progress" width="220" />](images/student/sync-in-progress.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-6210) | **Uploaded — under review**<br>[<img src="images/student/under-review.png" alt="Uploaded — under review" width="220" />](images/student/under-review.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-6240) |
| **Retry failed sync**<br>[<img src="images/student/sync-retry.png" alt="Retry failed sync" width="220" />](images/student/sync-retry.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-6272) | **Incomplete attendance**<br>[<img src="images/student/incomplete-attendance.png" alt="Incomplete attendance" width="220" />](images/student/incomplete-attendance.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-5240) |
| **Rejected attendance**<br>[<img src="images/student/rejected-attendance.png" alt="Rejected attendance" width="220" />](images/student/rejected-attendance.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-7669) | **Already checked in**<br>[<img src="images/student/already-checked-in.png" alt="Already checked in" width="220" />](images/student/already-checked-in.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-7346) |

</details>

### Points and progress — R5, Feature 2

Verified attendance at an eligible event earns 3 points once. Verification needs every stage to pass or an authorised office decision. Progress shows verified attendance, points, interest stamps and the weekly streak. Pending scans earn nothing, and an authorised correction can reverse an earlier award.

[<img src="images/student/attendance-verified.png" alt="Attendance verified — +3 pts" width="250" />](images/student/attendance-verified.png)

**Attendance verified — +3 pts** · [Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-5179)

<details>
<summary>Progress, history and corrections</summary>

| Screen | Screen |
|---|---|
| **Your progress**<br>[<img src="images/student/progress.png" alt="Your progress" width="220" />](images/student/progress.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-3090) | **Your first stamp**<br>[<img src="images/student/first-stamp.png" alt="Your first stamp" width="220" />](images/student/first-stamp.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-2973) |
| **Attendance history**<br>[<img src="images/student/attendance-history.png" alt="Attendance history" width="220" />](images/student/attendance-history.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-2521) | **Points correction**<br>[<img src="images/student/points-correction.png" alt="Points correction" width="220" />](images/student/points-correction.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-2572) |

</details>

### Profile and settings — R6

Students see their account and can change interests, notifications and privacy settings. The attendance entry on Profile opens the right event stage.

[<img src="images/student/profile.png" alt="Student profile" width="250" />](images/student/profile.png)

**Student profile** · [Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-3587)

<details>
<summary>Account and preferences</summary>

| Screen | Screen |
|---|---|
| **Privacy and settings**<br>[<img src="images/student/settings.png" alt="Privacy and settings" width="220" />](images/student/settings.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-3939) | **Notifications**<br>[<img src="images/student/notifications.png" alt="Notifications" width="220" />](images/student/notifications.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-3751) |
| **Edit interests**<br>[<img src="images/student/edit-interests.png" alt="Edit interests" width="220" />](images/student/edit-interests.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-4010) | **Account**<br>[<img src="images/student/account.png" alt="Account" width="220" />](images/student/account.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-4386) |
| **Sign-out confirmation**<br>[<img src="images/student/sign-out.png" alt="Sign-out confirmation" width="220" />](images/student/sign-out.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-4455) |    |

</details>

---

[← Previous: 4. Attendance security and offline sync](4-4-attendance-security-and-sync.md) · [Next: Committee wireframes →](5-2-committee-wireframes.md)
