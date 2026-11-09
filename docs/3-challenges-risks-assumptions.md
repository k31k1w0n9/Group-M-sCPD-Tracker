[s-CPD overview](../README.md) › **3. Challenges, risks, and assumptions**

## 3. Challenges, risks, and assumptions

### Challenges and risks

| Risk | Response | Remaining limitation |
|---|---|---|
| A friend uses a student's account or unlocked phone. | Check the signed-in account against its booking, and allow one attendance award per student and event. | A friend holding the same signed-in phone can still pass. |
| A live QR or backup code is sent to someone elsewhere. | Rotate both codes every 30 seconds, rate-limit backup code attempts, and use a separate code for each required stage. | Each live code can still be relayed while it is valid. |
| An offline scan is altered or uploaded late. | At sync, verify the captured QR token, its stage and its server-issued window. Apply an upload deadline and withhold points until the outcome is **Verified**. | The server cannot independently confirm when an offline phone captured a valid token. |
| An organiser never opens a required checkpoint. | Show the requirement before booking, send students with a verified arrival to s-CPD office review, and give them the organiser's contact. | Those outcomes are not final until the office makes an authorised decision. |
| Two students try to take the last place. | Check capacity and timetable clashes in one server transaction, and refresh the booking status after a failed request. | The student may briefly be unsure whether the request succeeded. |
| An account reaches another club's or student's data. | Enforce account ownership, role and club membership in backend rules and functions. | A compromised account can still use the permissions it was given. |

### Assumptions

- The prototype uses pre-approved test accounts with roles assigned in the backend. University of Nottingham single sign-on is the intended production integration, subject to the university granting access.
- The s-CPD office confirms capacity and eligibility during review, and approval locks the event details.
- The committee device needs internet to issue or refresh codes. A student with a cached booking can save a captured QR offline. Manual code entry needs internet.
- A committee sees booked names and attendance only for its own events. Cached bookings and queued scans belong to the signed-in account. Raw QR tokens are cleared once a definitive server response or a confirmed expiry is saved, and the request ID is kept to reconcile an uncertain upload.
- Each eligible verified attendance earns 3 points once, after every required stage passes or the office verifies attendance. Streak weeks run Monday to Sunday in Malaysia time and use the event date.
- Before implementation, the team will agree which events need a checkpoint, the checkpoint window and the sync grace period. The wireframes use a five-minute checkpoint and a 24-hour grace period as examples.
- On the emulator, the offline QR flow is tested with a controlled camera QR fixture: capture offline, restart, reconnect and check that the outcome is verified once. The online manual code covers camera failures.

---

[← Previous: 2. Requirements analysis](2-requirements-analysis.md) · [Next: 4. Technical design — Architecture →](4-1-architecture.md)
