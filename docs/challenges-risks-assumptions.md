# 3. Challenges, risks, and assumptions

| Risk | Response | Remaining limitation |
|---|---|---|
| A friend uses a student's account or unlocked phone. | Check the signed-in account against its booking and accept one attendance award per student and event. | A friend using the same authenticated phone can still pass. |
| A live QR or backup code is sent to someone elsewhere. | Rotate both codes every 30 seconds, rate-limit backup code attempts and use a separate code for each required attendance stage. | Each live code can still be relayed while valid. |
| An offline scan is altered or uploaded late. | Verify the captured QR token, its stage and its server-issued window at sync. Apply an upload deadline and withhold points until all required stages are verified. | The server cannot independently confirm when an offline phone captured a valid token. |
| An organiser cannot offer a required checkpoint. | Explain the requirement before booking and provide an organiser contact for s-CPD office review. | The team still needs to agree how to resolve attendance when a required checkpoint cannot be offered. |
| Two students try to take the last place. | Check capacity and timetable clashes in a server transaction. Refresh the booking status after a failed request. | The student may briefly be unsure whether a request succeeded. |
| An account accesses another club or student's data. | Enforce account ownership, role and club membership in backend rules and functions. | A compromised account can still use its assigned permissions. |

## Assumptions

- The prototype uses pre-approved test accounts and backend-assigned roles. University of Nottingham single sign-on is the intended production integration, subject to university access.
- The s-CPD office confirms capacity and eligibility during review. Approval locks the event details.
- The committee device needs internet to issue or refresh codes. A student with a cached booking can save a captured QR offline. Manual code entry requires internet.
- A committee sees booked names and attendance for its own events. Cached bookings and queued scans are scoped to the signed-in account. Delete raw QR tokens from the queue after verification, rejection or expiry.
- Each eligible verified attendance earns 3 points once after all required scans are verified. Streak weeks run Monday to Sunday in Malaysia time and use the event date.
- The team will confirm which events require a checkpoint and agree the checkpoint window and sync grace period before implementation. The wireframes use a five-minute checkpoint and a 24-hour sync grace period as examples.

[Back to project overview](../README.md)
