# 3. Challenges, risks, and assumptions

| Risk | Response | Remaining limitation |
|---|---|---|
| A friend uses a student's account or unlocked phone. | Check the signed-in account against its booking and accept one attendance award per student and event. | A friend using the same authenticated phone can still pass. |
| A live QR or backup code is sent to someone elsewhere. | Rotate both codes every 30 seconds, rate-limit backup code attempts and use a separate code for each required attendance stage. | Each live code can still be relayed while valid. |
| An offline scan is altered or uploaded late. | Verify the captured QR token, its stage and its server-issued window at sync. Apply an upload deadline and withhold points until the final outcome is Verified. | The server cannot independently confirm when an offline phone captured a valid token. |
| An organiser never opens a required checkpoint. | Disclose the requirement before booking and send students with verified arrival to s-CPD office review. Provide the organiser contact. | An authorised office decision is needed before those outcomes are final. |
| Two students try to take the last place. | Check capacity and timetable clashes in a server transaction. Refresh the booking status after a failed request. | The student may briefly be unsure whether a request succeeded. |
| An account accesses another club or student's data. | Enforce account ownership, role and club membership in backend rules and functions. | A compromised account can still use its assigned permissions. |

## Assumptions

- The prototype uses pre-approved test accounts and backend-assigned roles. University of Nottingham single sign-on is the intended production integration, subject to university access.
- The s-CPD office confirms capacity and eligibility during review. Approval locks the event details.
- The committee device needs internet to issue or refresh codes. A student with a cached booking can save a captured QR offline. Manual code entry requires internet.
- A committee sees booked names and attendance for its own events. Cached bookings and queued scans are scoped to the signed-in account. Clear raw QR tokens after saving a definitive server response or confirmed expiry. Preserve the request ID to reconcile an uncertain upload.
- Each eligible verified attendance earns 3 points once after all required stages pass or the office verifies attendance through an authorised decision. Streak weeks run Monday to Sunday in Malaysia time and use the event date.
- The team will confirm which events require a checkpoint and agree the checkpoint window and sync grace period before implementation. The wireframes use a five-minute checkpoint and a 24-hour sync grace period as examples.
- For emulator testing, exercise the offline QR flow using a controlled camera QR fixture: capture offline, restart, reconnect and verify the outcome once. The online manual code fallback covers camera access failures.

[Back to project overview](../README.md)
