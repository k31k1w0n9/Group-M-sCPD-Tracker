[s-CPD overview](../../README.md) › **7. User stories v2**

## 7. User stories v2

Part 1 marked 21 of its 22 stories as must-have, which did not show real priorities. This page re-ranks them by what the app has to deliver, adds the stories that came out of building it, and adds abuse stories for the ways a user might try to cheat. Each story names the automated test that checks it.

**Priority:** **Must** means the app fails a requirement without it. **Should** means the app is weaker but still works. **Could** means nice to have. **Won't** means dropped, with the reason.

**Status:** **Done** means it works in the app today on the in-memory backend. **Backend** means the screens and rules are done, and the server side lands with Firebase next week.

Tests in `ui/` and `data/` are JVM unit tests. `EndToEndTest` and `ScreenTests` are instrumented tests that run on the emulator.

### Part 1 stories, re-ranked

| Story | Priority | Status | Checked by |
|---|---|---|---|
| **US-01** Club creates and submits an event | Must | Done | `submitValid_movesToPendingReview`, `submitWithErrors_showsFieldErrors_noRepositoryCall` |
| **US-02** Office approves, rejects or requests changes | Must | Done (debug menu stands in for the console) | `committeeApproval_reachesStudentsStraightAway`, `changesRequested_showsAdminComment_andEditable` |
| **US-03** Club cancels an approved event | Must | Done | `cancelApproved_requiresReason_thenCancelsBookings` |
| **US-04** Student finds relevant events | Must | Done | `exploreSearch_filtersByText`, `recommendations_excludeBookedAndClashingEvents` |
| **US-05** Student books an event | Must | Done | `doubleTapBook_booksOnce`, `book_lastPlaceTwoUsers_onlyOneBooked`, `clash_showsOtherEventName` |
| **US-06** Student checks or cancels a booking | Must | Done | `cancelBooking_movesToCancelledPage`, `insideCutoff_cancelNotAllowed` |
| **US-07** Club sees who is taking part | Should | Done | `countShowsCheckedInOfBooked`, `attendees_flaggedFilterShowsReason` |
| **US-08** Club runs check-in | Must | Done | `codeRotatesEvery30Seconds`, `openCheckpoint_switchesStage`, `endCheckIn_closesSession` |
| **US-09** Student scans the event QR | Must | Done | `scanOnline_stageVerified`, `alteredToken_rejected`, `expiredLiveToken_rejected` |
| **US-10** Backup code when the camera fails | Must | Done | `correctBackupCode_verified`, `backupCodeWhileOffline_blockedWithMessage` |
| **US-11** Offline scan is kept | Must | Done in memory; survives a restart once Room is added (Backend) | `scanOffline_stagePendingSync_andChipSaysSavedOnPhone` |
| **US-12** Clear result after syncing | Must | Done | `claim_offline_returnsPendingSync_thenSyncVerifies`, `offlineScan_uploadedAfterDeadline_rejectedWithReason` |
| **US-13** Accurate attendee list | Should | Done | `attendees_flaggedFilterShowsReason` |
| **US-14** Student tracks points | Must | Done | `bothStagesVerified_awardsThreePointsOnce`, `pointsAndStreak_fromLedgerAndHistory` |
| **US-15** Office reverses invalid attendance | Must | Done (debug menu stands in for the console) | `reversal_showsMinusThreeWithReason` |
| **US-16** Secure sign-in with the real role | Must | Done on demo accounts; university sign-in is out of scope | `committeeAccountChoosingStudent_showsRoleNotAllowed`, `committeeAccount_cannotUseStudentApp` |
| **US-17** Student's records stay private | Must | Backend (Firestore rules) | Rules unit tests, planned |
| **US-18** Club sees only its own society | Must | Done in the app; enforced by rules next week | `committeeOnlySeesOwnClubEvents` |
| **US-19** Student chooses interests | Should | Done | `newStudentWithoutInterests_goesToInterestsFirst` |
| **US-20** Recommendations explain themselves | Should | Done | `RecommendationRankerTest` (8 tests) |
| **US-21** Suggestions from similar students | Won't | Dropped | We would not have enough attendance data to make it meaningful. |
| **US-22** Weekly streak and stamps | Should | Done | `StreakCalculatorTest` (6 tests), `stampsCountOnlyVerifiedAttendancePerTag` |

### New stories

| Story | Priority | Status | Acceptance criteria | Checked by |
|---|---|---|---|---|
| **US-23** As a student, I want to join a waitlist when an event is full, so that I still have a chance of going. | Should | Done | **Given** an event with no places left, **when** I tap Join waitlist, **then** I see my place in the queue. **When** a booked student cancels, **then** the first student in the queue is booked automatically. | `full_ctaIsJoinWaitlist`, `waitlisted_showsPosition`, `cancel_promotesFirstWaitlisted` |
| **US-24** As the s-CPD office, I want booking limits, so that one student can't block places for others. | Must | Done | **Given** a student with 5 active bookings, **when** they try to book another, **then** booking is refused with the reason. Cancelling closes 2 hours before the start. After 3 no-shows in a semester, booking is paused. | `fiveActive_returnsCapReached`, `capReached_explainsLimit`, `canCancel_exactlyAtCutoff_false`, `threeNoShows_returnsPaused` |
| **US-25** As a club, I want a clear way forward when an approved event must change, so that students aren't misled. | Must | Done | **Given** an approved event, **when** I open it, **then** the form is read-only and offers Cancel with a reason. Booked students see the reason. | `approvedEvent_formIsLockedReadOnly`, `cancelApproved_requiresReason_thenCancelsBookings` |
| **US-26** As a club, I want to request s-CPD points for an event, so that the office knows why it should earn them. | Should | Done | **Given** Request s-CPD points is on, **when** I submit without a reason of at least 20 characters, **then** the form shows an error and nothing is sent. | `justificationTooShort`, `noPointsRequested_justificationNotNeeded` |
| **US-27** As a student, I want a warning before signing out with scans not yet synced, so that I don't lose points by accident. | Must | Done | **Given** a scan saved on the phone, **when** I tap Sign out, **then** I see how many scans would be lost and can stay signed in. | `signOutWithPending_showsWarningFirst`, `signOutWithNoPending_signsOutDirectly` |
| **US-28** As the s-CPD office, I want committee members to earn no points for their own club's events, so that clubs can't farm points. | Should | Backend | **Given** a committee member of club A, **when** they check in at club A's event, **then** attendance is recorded with no award. | Cloud Function test, planned |

### Abuse stories

Each abuse story is written from the cheater's side, followed by what the system must do. The full defences and remaining weaknesses are in [8. Threat model](8-threat-model.md).

| Story | What the system must do | Status | Checked by |
|---|---|---|---|
| **AS-01** As an absent student, I scan a photo of the QR that a friend sent me, while offline, and upload it hours later. | Accept a saved scan only if the code was issued during check-in and the upload arrives within 2 hours of check-in closing. | Done | `offlineScan_uploadedAfterDeadline_rejectedWithReason` |
| **AS-02** As a student, I sign out and back in on one phone to check in my friends. | Refuse a second account on the same phone at the same event, and flag both for the office. | Done | `claim_sameDeviceDifferentAccountsSameEvent_flagged`, `claim_includesDeviceId_soSharedPhoneIsFlagged` |
| **AS-03** As a student, I hand my unlocked phone to a friend to check in for me. | Ask for the fingerprint or screen lock before sending a check-in. | Done | `identityCancelled_sendsNothing` (the system prompt itself can't be automated) |
| **AS-04** As an organiser, I post the live QR in the club group chat. | Show the office and committee unusual patterns, such as flagged phones, and record every claim. | Partly done: flags are shown; burst and location checks are Backend | `attendees_flaggedFilterShowsReason` |
| **AS-05** As a student, I book every event and don't turn up. | Apply the booking cap, the cancellation cut-off and the no-show pause. | Done | See US-24 |
| **AS-06** As a club, I create events just to give my members points. | Require office approval before an event is public, and give no points for your own club's events. | Approval is done; the own-club rule is Backend | `committeeApproval_reachesStudentsStraightAway` |
| **AS-07** As a student, I change a QR code to point at another event, or I call the backend directly. | Check the code's signature on the server and trust nothing the phone sends except the signed-in account. | Done in the fake server; Firestore rules are Backend | `alteredToken_rejected`, `book_whileOffline_returnsOffline_andNoBooking` |
| **AS-08** As a student, I guess backup codes until one works. | Block backup codes for that student after 5 wrong attempts in a session. | Done | `claim_wrongBackupCodeFiveTimes_returnsRateLimited`, `rateLimited_disablesCodeEntryForSession` |

---

[← Previous: 6. Changes since Part 1](6-changes-since-part1.md) · [Next: 8. Threat model →](8-threat-model.md)
