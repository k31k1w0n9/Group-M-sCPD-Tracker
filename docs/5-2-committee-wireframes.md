[s-CPD overview](../README.md) › **5. UI design and wireframes** › Committee

## 5. UI design and wireframes

*Part 2 of 2: Committee journeys. The [Student journeys](5-1-student-wireframes.md) are on the previous page. The s-CPD office reviews submissions in the backend, so it has no app screens.*

### Committee entry and access — R6

Committee members start at the same [role picker](5-1-student-wireframes.md#student-entry-and-interests--r6) as students. Sign-in checks the account's assigned role and society. The prototype uses approved test accounts.

| Committee sign-in | Sign-in error | Access not authorised |
|---|---|---|
| [<img src="images/committee/sign-in.png" alt="Committee sign-in" width="190" />](images/committee/sign-in.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-8314) | [<img src="images/committee/sign-in-error.png" alt="Sign-in error" width="190" />](images/committee/sign-in-error.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-8389) | [<img src="images/committee/access-not-authorised.png" alt="Access not authorised" width="190" />](images/committee/access-not-authorised.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-8469) |

### Committee dashboard — R1, R3, R6

The dashboard shows event and submission status, the next public event, and shortcuts to creating an event and running check-in.

| Dashboard | Loading |
|---|---|
| [<img src="images/committee/dashboard.png" alt="Committee dashboard" width="220" />](images/committee/dashboard.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-8702) | [<img src="images/committee/dashboard-loading.png" alt="Dashboard loading" width="220" />](images/committee/dashboard-loading.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-8644) |

### Event submissions — R1

Submissions groups drafts, pending reviews, decisions and approved events. Withdrawing a pending submission returns the same event to an editable draft. A final rejection shows the reason and closes the submission. Committee members can update a submission marked Changes requested and cancel an approved event. Each action shows its progress, retry and confirmed states.

[<img src="images/committee/submissions.png" alt="Submissions" width="250" />](images/committee/submissions.png)

**Submissions** · [Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-11387)

<details>
<summary>Create and submit an event</summary>

| Screen | Screen |
|---|---|
| **Create event**<br>[<img src="images/committee/create-event.png" alt="Create event" width="220" />](images/committee/create-event.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-9170) | **Submitting**<br>[<img src="images/committee/submitting.png" alt="Submitting" width="220" />](images/committee/submitting.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-11464) |
| **Submission retry**<br>[<img src="images/committee/submit-retry.png" alt="Submission retry" width="220" />](images/committee/submit-retry.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-11516) | **Submission success**<br>[<img src="images/committee/submit-success.png" alt="Submission success" width="220" />](images/committee/submit-success.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-11572) |

</details>

<details>
<summary>Review, withdrawal and rejection</summary>

| Screen | Screen |
|---|---|
| **Changes requested**<br>[<img src="images/committee/changes-requested.png" alt="Changes requested" width="220" />](images/committee/changes-requested.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-8998) | **Pending review**<br>[<img src="images/committee/pending-review.png" alt="Pending review" width="220" />](images/committee/pending-review.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-9076) |
| **Confirm withdrawal**<br>[<img src="images/committee/withdraw.png" alt="Confirm withdrawal" width="220" />](images/committee/withdraw.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-11620) | **Withdrawing**<br>[<img src="images/committee/withdrawing.png" alt="Withdrawing" width="220" />](images/committee/withdrawing.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-11676) |
| **Returned to draft**<br>[<img src="images/committee/withdrawn.png" alt="Returned to draft" width="220" />](images/committee/withdrawn.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-11728) | **Withdrawal retry**<br>[<img src="images/committee/withdraw-retry.png" alt="Withdrawal retry" width="220" />](images/committee/withdraw-retry.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-11784) |
| **Rejected submission**<br>[<img src="images/committee/rejected-decision.png" alt="Rejected submission" width="220" />](images/committee/rejected-decision.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-12178) |    |

</details>

<details>
<summary>Approved event cancellation</summary>

| Screen | Screen |
|---|---|
| **Approved event**<br>[<img src="images/committee/event-approved.png" alt="Approved event" width="220" />](images/committee/event-approved.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-8874) | **Confirm event cancellation**<br>[<img src="images/committee/cancel-confirm.png" alt="Confirm event cancellation" width="220" />](images/committee/cancel-confirm.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-9283) |
| **Cancelling event**<br>[<img src="images/committee/cancelling-event.png" alt="Cancelling event" width="220" />](images/committee/cancelling-event.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-12024) | **Cancellation confirmed**<br>[<img src="images/committee/cancel-success.png" alt="Cancellation confirmed" width="220" />](images/committee/cancel-success.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-11840) |
| **Cancellation retry**<br>[<img src="images/committee/cancel-retry.png" alt="Cancellation retry" width="220" />](images/committee/cancel-retry.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-11896) |    |

</details>

### Committee Events — R1, R2, R3

Upcoming events link to their bookings, and past events link to their attendance summaries.

[<img src="images/committee/upcoming-events.png" alt="Upcoming events" width="250" />](images/committee/upcoming-events.png)

**Upcoming events** · [Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-9646)

<details>
<summary>Past and empty events</summary>

| Screen | Screen |
|---|---|
| **Past events**<br>[<img src="images/committee/past-events.png" alt="Past events" width="220" />](images/committee/past-events.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-9526) | **Committee Events empty**<br>[<img src="images/committee/events-empty.png" alt="Committee Events empty" width="220" />](images/committee/events-empty.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-9426) |

</details>

### Check-in — R3, R4, R5

The Committee shows the rotating arrival or checkpoint QR with its short-lived backup code. The checkpoint view shows a countdown and asks for confirmation before closing. An offline committee device pauses code refresh until it reconnects, while students can still upload scans they saved during the sync window.

[<img src="images/committee/arrival-qr.png" alt="Active arrival QR" width="250" />](images/committee/arrival-qr.png)

**Active arrival QR** · [Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-10631)

<details>
<summary>QR display and checkpoint closure</summary>

| Screen | Screen |
|---|---|
| **Checkpoint open**<br>[<img src="images/committee/checkpoint-qr.png" alt="Checkpoint open" width="220" />](images/committee/checkpoint-qr.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-10713) | **Offline — checkpoint QR paused**<br>[<img src="images/committee/checkpoint-offline.png" alt="Offline — checkpoint QR paused" width="220" />](images/committee/checkpoint-offline.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-10793) |
| **Confirm checkpoint closure**<br>[<img src="images/committee/close-checkpoint.png" alt="Confirm checkpoint closure" width="220" />](images/committee/close-checkpoint.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-11290) | **Checkpoint closed**<br>[<img src="images/committee/checkpoint-closed.png" alt="Checkpoint closed" width="220" />](images/committee/checkpoint-closed.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-11336) |

</details>

### Bookings and attendance — R2, R3, R4, R5

Before check-in opens, the bookings list shows each reservation as **Booked**. During and after the event, the Committee sees live attendance, claims awaiting sync and final outcomes. Missing uploads stay provisional until the sync deadline. At the deadline, one verified stage with the other required stage missing becomes **Incomplete attendance**, and **No-show** applies when the server received no claim. If an organiser never opens a required checkpoint, students with a verified arrival go to office review. **Under review** stays unresolved until an authorised decision.

[<img src="images/committee/bookings-list.png" alt="Event bookings" width="250" />](images/committee/bookings-list.png)

**Event bookings** · [Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-12084)

<details>
<summary>Attendance lists and summaries</summary>

| Screen | Screen |
|---|---|
| **Live attendee list**<br>[<img src="images/committee/live-attendees.png" alt="Live attendee list" width="220" />](images/committee/live-attendees.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-10854) | **Awaiting sync**<br>[<img src="images/committee/awaiting-sync.png" alt="Awaiting sync" width="220" />](images/committee/awaiting-sync.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-11180) |
| **Attendance summary**<br>[<img src="images/committee/final-summary.png" alt="Attendance summary" width="220" />](images/committee/final-summary.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-10962) | **Final attendee list**<br>[<img src="images/committee/final-attendees.png" alt="Final attendee list" width="220" />](images/committee/final-attendees.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-11070) |

</details>

### Committee account — R6

Committee members can check their verified club access and sign out after confirming.

[<img src="images/committee/account.png" alt="Committee account" width="250" />](images/committee/account.png)

**Committee account** · [Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-10508)

<details>
<summary>Sign-out</summary>

| Screen | Screen |
|---|---|
| **Sign-out confirmation**<br>[<img src="images/committee/sign-out.png" alt="Sign-out confirmation" width="220" />](images/committee/sign-out.png)<br>[Figma frame](https://www.figma.com/design/Um4ZLQMFP2mEoEEnYOL8oM/Mobile-Device-Programming?node-id=269-10563) |    |

</details>

---

[← Previous: Student wireframes](5-1-student-wireframes.md) · [Back to the overview](../README.md)
