[s-CPD overview](../../README.md) › **9. SQA review response**

## 9. SQA review response

SQA Group 13 (Ziming Guan and Bindan Xu) reviewed our Part 1 requirements. They found 5 missing requirements (M1–M5) and 15 unclear or untestable ones (P1–P15). This page lists only what we added or changed in response. Each rule is tagged with the finding it answers. Where a rule here differs from the older wording in [Section 2](../2-requirements-analysis.md) or [7. User stories v2](7-user-stories-v2.md), this page applies.

All times are Malaysia time (UTC+8) and are judged by the server's clock. "Before" a limit means strictly before it: an action at the exact limit counts as after it.

**Status:** rules without a tag already work in the app. *(Backend)* arrives with Firebase. *(Planned)* is agreed but not built yet.

### Roles and accounts

- **M1 · One account, two roles.** Every student has one university account with the Student role. Joining a club committee adds committee access for that club only, and never removes the Student role. A member of club A can book and attend club B's events and earn points, but cannot manage club B. At club A's own events they earn no points (US-28). *(Planned: the demo committee accounts still refuse the Student role, so `committeeAccountChoosingStudent_showsRoleNotAllowed` and `committeeAccount_cannotUseStudentApp` will change.)*
- **M5 · Admin accounts.** The s-CPD office uses separate staff accounts with the Admin role. They stay in the user table (the *admin* value in DG-07) so every decision records who made it, but they can sign in only to the backend console. The app refuses them with "Use the s-CPD office console". *(Planned)*

### Event submissions and points

- **M3 · Withdrawing a submission.** Only the organising club's committee can withdraw, and only while the event is *Pending review*. It returns to *Draft* and can be edited and resubmitted. For *Changes requested*, *Approved* or *Rejected* there is no Withdraw button, and the server refuses with "Only a pending submission can be withdrawn." Test: `withdrawPending_returnsToDraft`.
- **P14 · Points eligibility.** The office decides eligibility while approving: *Approve with points*, *Approve without points* (with a reason the club sees), request changes or reject. An event is eligible only if it is open to all students and has a stated learning or development outcome. For example, a CV workshop open to everyone is eligible; a members-only meeting or a social night is not. Eligibility locks with the other approved details. Only the office can withdraw it later, with a reason, and points already awarded are then reversed as in P5. *(Planned: Approve without points and withdrawing eligibility)*
- **P5 · Cancelling or voiding an event.** A committee can cancel an approved event only before check-in starts. After that, only the office can void it, with a reason. Then a scan still saved on a phone is *Rejected* ("This event was cancelled") when it uploads; a verified stage without points becomes *Rejected*; an attendance that already earned 3 points gets one −3 line with the reason, and the streak and stamps are counted again. Every affected student is notified. Voiding twice changes nothing more. *(Planned)*

### Booking and recommendations

- **P2 · Full and clash are different results.** Full shows "All *N* places are taken. Join the waitlist and you're booked automatically if a place opens." with a *Join waitlist* button. A clash disables booking and names the other event, for example "Clashes with Python: Next Steps (Wed 15:00)". When both apply, the clash message is shown, because the waitlist would not help.
- **P13 · Cancellation cut-off.** A cancellation counts if the server receives it before 2 hours before the start. For a 14:00 event, 11:59 is accepted and 12:00 is refused. The freed place goes at once to the first student on the waitlist, or can be booked straight away. After the cut-off, the screen says "Cancellations closed 2 hours before the start. If you can't come, tell the organiser." and not attending counts as a no-show. Tests: `canCancel_exactlyAtCutoff_false`, `canCancel_oneMinuteBeforeCutoff_true`, `cancel_promotesFirstWaitlisted`.
- **P7 · Recommendation order.** Ties on score go to the earliest start, then to the title in A–Z order. An event that scores 0 is still listed, last, with "Something new to try". With no suitable events, Home shows "No events yet. New events appear here once the s-CPD office approves them." Worked example with interests Tech and Career: A (Tech and Career, in 3 days) scores 7, B (Career, in 10 days) 3, C (Tech, full, in 2 days) 2, and D (Sports, in 1 day) 1, so the order is A, B, C, D. Tests: `workedExample_fromUserStories`, `sameScoreAndStart_tieBrokenByTitle`.

### Check-in

- **P3 · Check-in times.** Start check-in is available from 30 minutes before the start until the end. Check-in closes at the scheduled end, or earlier when the organiser ends it, and an early close is final. A live scan counts only if the server receives it before closing. The 2-hour upload window starts from the actual close. For a 14:00–16:00 event: start is available from 13:30, a scan received at 15:59:59 counts and one at 16:00:00 does not, and a saved scan must arrive before 18:00:00, or before 17:30:00 if check-in ended at 15:30. *(Planned: the 30-minute start and the automatic close)*
- **P6 · Two phones on one account.** The server handles claims one at a time. The first verifies the stage, a second claim for the same stage gets "Already checked in" and adds nothing, and at most one +3 is recorded. The second phone's claim is flagged for the office. Test: `bothStagesVerified_awardsThreePointsOnce`. *(Planned: the "Already checked in" reply and the flag)*
- **P12 · Backup code timeout.** If the server hasn't answered within 10 seconds, the student sees "No answer from the server. Your code wasn't counted. Try again with the code on screen now." The attempt does not count towards the limit of 5. *(Planned)*

### Offline scans and the attendee list

- **P15 · Keeping saved scans.** A saved scan stays on the phone until the server gives a final answer, or confirms the upload window has passed. It must survive the app being closed, force-stopped or updated, and the phone restarting. Signing out removes it, after the US-27 warning. After the final answer, the raw QR data is deleted at once; the request ID and result are kept for 30 days to match an uncertain upload. If the phone can't save the scan, the student sees "Couldn't save this scan. Free some space and scan again." and never *Saved on phone*. *(Backend: Room queue)*
- **P4 · Late uploads and review flags.** A scan arriving after the 2-hour window is *Rejected* with "Uploaded too late…", earns 0 points and shows the organiser's contact. An accepted scan is flagged for the office when the same phone was used by another account at the event, when the phone's reported capture time is more than 10 minutes outside check-in, or when more than half of the event's claims came in as offline uploads. The office decides each flagged claim within 7 days. Until then it shows *Under review* and earns no points. Test: `offlineScan_uploadedAfterDeadline_rejectedWithReason`. *(Backend: the time-gap and offline-share flags and the review deadline)*
- **P1 · What the committee sees.** The attendee list shows only what the server knows; a scan still on a student's phone is invisible to the committee. A booked student with no accepted claim shows as *Awaiting sync* while the upload window is open, then *Verified* or *No-show*. The student's phone says *Pending sync* and the committee's list says *Awaiting sync*; the two words are kept apart on purpose. *(Planned: Awaiting sync after check-in ends)*

### Notifications

- **M2 · Notification rules.** Each notification names the event, time and venue, and opens that booking when tapped.
  - *Reminder:* 24 hours and 1 hour before each booked event. Can be turned off in Profile › Notifications.
  - *Event cancelled:* within 5 minutes of the organiser cancelling, with the reason.
  - *Waitlist place:* within 5 minutes of being booked from the waitlist.
  - *Checkpoint open:* when the organiser opens the checkpoint at an event the student has checked in to.

  Cancellation and waitlist alerts can't be turned off, because they change the booking. If notifications are blocked in Android settings, My Events still shows the same information. *(Planned)*

### Measurable non-functional requirements

These replace the Part 1 targets for M4 and P8–P12.

| Area | Target, test conditions and pass rule | Status |
|---|---|---|
| **Security** (P8) | The backend denies every request in the access matrix below. **Pass:** each row returns *permission denied* and leaves the data unchanged, in Firestore rules tests on the Firebase emulator. | Backend |
| **Booking integrity** (P9) | 20 different eligible students send one booking request each for the last place, all started within the same second. **Pass:** exactly 1 is booked, the other 19 see the full-event message, and the booked count equals the capacity, in 10 repeats. | 2-student test passes (`book_lastPlaceTwoUsers_onlyOneBooked`); 20 is Planned |
| **Reliability** (P10, P15) | Save one scan offline, then force-stop the app, restart the emulator and switch airplane mode on and off 3 times before reconnecting. **Pass:** the scan uploads by itself within 1 minute of reconnecting, there is exactly one +3 line, and resending the same request ID changes nothing. | Same-ID replay passes (`claim_sameRequestIdTwice_countsOnce`); restarts are Backend |
| **Responsiveness** (P11) | Pixel 2 emulator, Android 12 (API 31), airplane mode. Time from the camera decoding the QR to *Saved on phone*, using debug-build timestamps. **Pass:** at least 19 of 20 runs under 2 seconds and none over 3. A failed save shows the P15 error. | Planned |
| **Usability** (P12) | Every error says what happened and what to do next, with a button for the next step where one exists. These are at least 48 × 48 dp: buttons, icon buttons, navigation items, cards and rows that open something, chips, switches, checkboxes and text fields. **Pass:** the touch-target test passes, Accessibility Scanner finds no touch-target issues on the main screens, and at least 4 of 5 students finish each of 3 recovery tasks without help (full event, expired QR, offline scan). | Token test passes (`touchTargetTokenIsAtLeast48dp`); the rest is Planned |
| **Compatibility** (M4) | Installs on Android 7.0 (API 24) and above; targets API 37. **Pass:** every journey works on the Pixel 2 (API 31) in portrait and landscape, and the app opens and signs in on API 24 and the newest emulator image. | Pixel 2 tests pass; API 24 check Planned |
| **Accessibility** (M4) | Text and status colours at least 4.5:1; icons and control borders at least 3:1; a TalkBack label on every icon-only button. **Pass:** the contrast tests pass, and at the largest system font size on the Pixel 2 no text is cut off and no button is hidden. | Contrast tests pass (`ThemeTokensTest`); large-font check Planned |
| **Data retention** (M4) | *Phone:* cached data is cleared on sign-out; raw QR data is deleted after the final answer; request IDs are kept for 30 days. *Server:* bookings, attendance and points are kept while the student is enrolled and deleted or anonymised 12 months after they leave; device IDs and phone-reported times are deleted 90 days after the event. Students can ask the s-CPD office to see or delete their data. | Planned |

**Access matrix (P8)**

| Who | Tries to | Expected |
|---|---|---|
| Anyone not signed in | Read events, bookings, attendance or points | Denied |
| Student A | Read or change student B's bookings, attendance or points | Denied |
| Any student | Create, edit, approve or cancel an event | Denied |
| Any app account | Write a booking, attendance record or points line without the Cloud Functions | Denied |
| Committee of club A | Edit, cancel or run check-in for a club B event, or read club B's bookings or attendance | Denied |
| Any committee | Approve, reject or request changes to a submission, change an attendance outcome, or reverse points | Denied |
| An Admin account | Sign in to the Android app | Refused (M5) |

### Changes already made in the app

- Recommendations now break ties by title after the start time (P7), with the worked example above as a test.
- The cancellation-closed message now tells students what to do next (P13).

### Still to confirm

- **With the s-CPD office:** the data retention periods, the eligibility rule (P14) and the 7-day review deadline (P4).
- **With SQA Group 13:** that the pass rules above are the tests they will run on our releases.

---

[← Previous: 8. Threat model](8-threat-model.md)
