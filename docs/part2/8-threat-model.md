[s-CPD overview](../../README.md) › **8. Threat model**

## 8. Threat model

This table updates the attendance security analysis from [Part 1](../4-4-attendance-security-and-sync.md). The first four rows are the attacks Part 1 already covered. The other eight came up while we built and tested the app.

"Now" means the defence works in the app today, with the checks running in the in-memory server (`data/fake/FakeBackend.kt`). "Firebase" means the same check moves to Cloud Functions and Firestore security rules next week, where the phone can't bypass it.

| # | Attack | Defence now | Firebase | Test | Remaining weakness |
|---|---|---|---|---|---|
| 1 | **Proxy check-in**: checking in for a friend from their account | The claim is tied to the signed-in account and its booking. A phone shared between accounts is flagged (row 6). | The same checks run in `submitAttendance`. | `claim_sameDeviceDifferentAccountsSameEvent_flagged` | A friend who signs in to your account on their own phone still passes (row 7). |
| 2 | **Check-in from home** with a relayed live code | QR and backup codes rotate every 30 seconds. Arrival-plus-checkpoint events need evidence from two moments. | The signing key moves to Secret Manager. | `expiredLiveToken_rejected`, `checkpointNotOpen_onlyArrivalIsActive` | Someone in the room can still relay a live code while it is valid. |
| 3 | **Late or altered offline scan** | The server checks the code's signature, that it was issued during check-in, and that the upload came within 2 hours of check-in closing. The phone's clock is never trusted. | Same checks, using the server's receive time. | `alteredToken_rejected`, `offlineScan_uploadedAfterDeadline_rejectedWithReason` | A genuine code copied within the window can still be uploaded later (row 5). |
| 4 | **Duplicate claim** or a retried request | Every claim and booking carries a request ID, and repeats return the stored answer. Points are awarded once per student and event. | Deterministic document IDs (`eventId_uid`) give the same guarantee. | `claim_sameRequestIdTwice_countsOnce`, `book_sameRequestIdTwice_createsOneBooking`, `bothStagesVerified_awardsThreePointsOnce` | None found for this attack. |
| 5 | **Photographed QR used offline later**: a friend sends a photo of the QR and the absent student scans it in airplane mode | Saved scans must upload within 2 hours of check-in closing (Part 1 suggested 24 hours). | Also flag events with an unusually high share of offline claims for the office. | `offlineScan_uploadedAfterDeadline_rejectedWithReason` | Inside the 2-hour window the attack still works. The strongest fix, the organiser scanning students' phones instead, would slow the queue at the door. |
| 6 | **One phone, many accounts**: signing out and in to check in friends | Each install has a random device ID sent with every claim. A second account on the same phone at the same event is refused, and both are flagged. | Same check; the office reviews flagged records. | `claim_sameDeviceDifferentAccountsSameEvent_flagged`, `attendees_flaggedFilterShowsReason` | Reinstalling the app creates a new ID. A determined cheater with a second phone isn't stopped. |
| 7 | **Handing over an unlocked phone**, or sharing a login | The phone asks for the fingerprint or screen lock before sending a check-in. | Flag an account that signs in on a new device during an event. | `identityCancelled_sendsNothing` | Someone who knows the screen lock passes. A phone with no screen lock skips the check, and the student is told to set one. |
| 8 | **Organiser collusion**: posting the live QR in a group chat | Every claim is recorded with its device. The committee sees flagged rows. | Office alerts for check-ins above bookings, bursts of check-ins within seconds, and many offline claims. | `attendees_flaggedFilterShowsReason` | The office has to act on the alerts; the app can't prove where the scanner was. |
| 9 | **Booking hoarding**: booking everything and not turning up | 5 active bookings at most, cancellation closes 2 hours before the start, and booking pauses after 3 no-shows in a semester. | The booking transaction runs in `bookEvent`. | `fiveActive_returnsCapReached`, `canCancel_exactlyAtCutoff_false`, `threeNoShows_returnsPaused` | A student can still book 5 events they never attend before the pause applies. |
| 10 | **Club farming points**: fake events, or members attending their own club's events | Only office-approved events are public, and approved events are locked. | No points in `submitAttendance` for a member of the organising club. | `committeeApproval_reachesStudentsStraightAway`, `approvedEvent_formIsLockedReadOnly` | The own-club rule isn't built yet. |
| 11 | **Calling the backend directly**, or a modified app | The server checks the signature, booking, stage and time. It ignores the phone's own fields, such as capture time. | Firestore rules deny direct writes to bookings, attendance and points. Only Cloud Functions write them. | `alteredToken_rejected` | Until the rules are deployed and tested, the fake backend can only show the checks, not enforce them against a real client. |
| 12 | **Guessing backup codes** | After 5 wrong codes in a session, backup codes are blocked for that student. Codes change every 30 seconds. | Also limit attempts across all students for one session. | `claim_wrongBackupCodeFiveTimes_returnsRateLimited`, `rateLimited_disablesCodeEntryForSession` | Many accounts working together get 5 guesses each. |

### Testing on the emulator

The emulator can't point a camera at another phone. We cover this in two ways:

- The 6-digit backup code works on any device.
- Debug builds have a **Debug: read the organiser's live QR** button on the check-in screen. It takes the current QR payload straight from the organiser's session, so the full QR path, offline saves included, can be shown in the demo. Release builds don't include the button.

---

[← Previous: 7. User stories v2](7-user-stories-v2.md)
