[s-CPD overview](../../README.md) · **10. Backend on the free Firebase plan**

## 10. Backend on the free Firebase plan

We have no budget, so the backend runs on Firebase's free **Spark** plan. Spark gives us Authentication, Cloud Firestore, security rules and Hosting. It does **not** give us Cloud Functions or new Cloud Storage buckets, because both need the paid Blaze plan and a card.

Part 1 put the server-side checks in Cloud Functions. This section shows where each check lives now. The main idea: **Firestore security rules run on Google's servers, not on the phone**, so the phone can't skip them. Rules can:

- read any document with `get()`, including a document the student is not allowed to read (we use this to check signed QR codes against a secret the student never sees);
- use `request.time`, the server's clock, so the phone clock is never trusted;
- compute `hashing.sha256(...)`, so they can re-derive a QR signature or a backup code;
- use `getAfter()` to check that a batched write is consistent (for example, a new booking and the event's `bookedCount + 1` are written together, or neither is).

### Where each server job lives now

| Job (Part 1 design) | Free replacement | What we give up |
|---|---|---|
| Sign-in, sessions | Firebase Auth (email and password). The SDK keeps the session on disk, so it survives the app being killed when a permission is revoked. | Nothing |
| Club roles in the auth token (custom claims) | Rules read `clubs/{clubId}/members/{uid}` with `exists()` on every committee write. | One extra document read per committee write (free quota: 50,000 reads a day) |
| Capacity check in a transaction | The booking and `events/{id}.bookedCount + 1` go in one batch. Rules use `getAfter()` to check that both changed together and the count stays at or below capacity. | Nothing |
| Waitlist order | `events/{id}.waitlist` is an ordered list of user IDs. When a booked student cancels, the same batch must give the place to `waitlist[0]`. Rules check the order. | Nothing |
| Signed rotating QR | The committee phone makes a random secret when it opens check-in and stores it in `eventSecrets/{eventId}`, which only club members can read. QR = `SCPD2|event|stage|window|sha256(secret\|event\|stage\|window)`. Rules re-compute the hash and check that the window matches the server clock. | A committee member could leak their own event's secret. They already run that check-in, so this is the same trust as before. |
| Backup code and the 5-attempt limit | The code is the first 6 digits of a second hash. Before each try the phone must write `codeAttempts/{event_uid}` with `count + 1`, and rules cap it at 5. The claim is only accepted if it matches the attempt that was written. | Nothing |
| One phone, one account | `users/{uid}.deviceId` binds an account to a phone (24-hour cooldown to switch, checked against `request.time`). `deviceUse/{event_device}` records the first account to use a phone at an event. A second account on the same phone can only save its claim as **Under review**, and the flag is added to the first account's record too. | A student can reinstall to get a new install ID, but then the 24-hour cooldown applies. |
| Points awarded once | The award is written by the student's phone as `ledger/{event_uid_award}`. Rules allow it only if the attendance record is complete, not flagged, the event awards points and the student is not in the organising club. The fixed document ID means a second award can't exist. | Nothing |
| Points held while under review | The app leaves out the points of any event whose record is flagged. When the office clears the flag, the app writes the award. | Nothing |
| No-show after the sync deadline | Worked out when read: a booking with no verified arrival, 2 hours after check-in closed, shows as **No-show**. | The no-show pause is checked on the phone only (stated limit) |
| 5 active bookings, schedule clash | Checked on the phone (`BookingRules`), as before. | Rules can't run a query, so a modified app could skip these two (stated limit). Capacity is still enforced on the server. |
| Push notifications (FCM from a function) | The app listens to Firestore for the student's events and raises **local** notifications. A WorkManager job checks every 15 minutes while the app is closed. | Alerts can be up to 15 minutes late when the app is closed |
| Poster images (Cloud Storage) | Posters are the built-in art styles (Teal, Gold, bundled photos). | No custom poster uploads |
| s-CPD office review console | The office edits documents in the Firebase console, which the brief allows ("directly in the backend database"). Accounts in `admins/{uid}` can do everything in the rules. | No custom console in this release |

### Offline

- **Student offline.** A scan goes into a Room table on the phone (`pending_scans`) and shows as **Saved on phone**. WorkManager uploads it once the phone is online, even if the app was closed. The upload is marked `offline: true`. Rules accept it only if the code was issued during the check-in session and it arrives within **2 hours** of check-in closing. Backup codes need internet.
- **Committee offline.** The phone already holds the session secret, so it keeps showing fresh codes. It also stores the difference between its clock and the server's, measured when check-in opened, so the codes stay in step with `request.time`.
- **Reading offline.** Firestore keeps a cache on disk, so Explore and My Events still show the last data.

### Collections

| Collection | Who writes it |
|---|---|
| `users/{uid}` | The office creates it. The student updates only their interests and device binding. |
| `admins/{uid}` | The office (Firebase console) |
| `clubs/{clubId}` and `clubs/{clubId}/members/{uid}` (`role`: owner, committee or scanner) | The office |
| `events/{eventId}` | Club members (draft, submit, withdraw, cancel, check-in state). Students change only `bookedCount` and `waitlist`, through a booking batch. The office approves or rejects. |
| `eventSecrets/{eventId}` | Club members |
| `bookings/{eventId_uid}` | The student. One document per student per event. |
| `attendance/{eventId_uid}` | The student, one stage at a time, checked by the rules |
| `codeAttempts/{eventId_uid}`, `deviceUse/{eventId_deviceId}` | The student, checked by the rules |
| `ledger/{entryId}` | The student (award only). The office (reversals). |

### Running it

Everything runs on the free **Firebase Local Emulator Suite** first, with the project ID `demo-scpd` (a `demo-` ID needs no real project). See `firebase/README.md`.
