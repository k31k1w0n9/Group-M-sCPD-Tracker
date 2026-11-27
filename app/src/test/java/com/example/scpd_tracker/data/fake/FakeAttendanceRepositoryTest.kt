package com.example.scpd_tracker.data.fake

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.scpd_tracker.data.system.DataStoreDeviceId
import com.example.scpd_tracker.domain.Policy
import com.example.scpd_tracker.domain.outcome
import com.example.scpd_tracker.model.AttendanceClaim
import com.example.scpd_tracker.model.AttendanceOutcome
import com.example.scpd_tracker.model.ClaimResult
import com.example.scpd_tracker.model.Stage
import com.example.scpd_tracker.model.StageStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.Duration

class FakeAttendanceRepositoryTest {
    @get:Rule val folder = TemporaryFolder()

    private val event = FakeData.LIVE_EVENT
    private fun window(app: TestApp) = app.clock.now().epochSecond / Policy.CODE_ROTATION.seconds

    private fun qrClaim(app: TestApp, requestId: String, device: String = "phone-A", stage: Stage = Stage.Arrival) =
        AttendanceClaim(requestId, event, stage, app.backend.qrToken(event, stage, window(app)), device, app.clock.now(), viaBackupCode = false)

    private fun codeClaim(app: TestApp, requestId: String, code: String) =
        AttendanceClaim(requestId, event, Stage.Arrival, code, "phone-A", app.clock.now(), viaBackupCode = true)

    private suspend fun arrival(app: TestApp) = app.attendance.myAttendance().first().first { it.eventId == event }.arrival

    @Test
    fun scanOnline_verified() = runTest {
        val app = TestApp(backgroundScope)
        app.signInStudent1()
        assertEquals(ClaimResult.Verified, app.attendance.submitClaim(qrClaim(app, "r1")))
        assertEquals(StageStatus.VERIFIED, arrival(app))
    }

    @Test
    fun claim_offline_returnsPendingSync_thenSyncVerifies() = runTest {
        val app = TestApp(backgroundScope)
        app.signInStudent1()
        app.network.set(false)
        advanceTimeBy(5_000); runCurrent()
        assertEquals(ClaimResult.PendingSync, app.attendance.submitClaim(qrClaim(app, "r-off")))
        assertEquals(1, app.attendance.pendingCount().first())
        assertEquals(StageStatus.PENDING_SYNC, arrival(app))
        app.network.set(true) // reconnecting triggers the upload by itself
        advanceTimeBy(5_000); runCurrent()
        assertEquals(0, app.attendance.pendingCount().first())
        assertEquals(StageStatus.VERIFIED, arrival(app))
    }

    @Test
    fun offlineScan_uploadedAfterDeadline_rejectedWithReason() = runTest {
        val app = TestApp(backgroundScope)
        app.signInStudent1()
        app.network.set(false)
        advanceTimeBy(5_000); runCurrent()
        app.attendance.submitClaim(qrClaim(app, "r-late"))
        app.backend.endCheckIn(event)
        app.clock.advance(Duration.ofHours(3)) // past the 2-hour upload window
        app.network.set(true)
        advanceTimeBy(5_000); runCurrent()
        val record = app.attendance.myAttendance().first().first { it.eventId == event }
        assertEquals(StageStatus.REJECTED, record.arrival)
        assertTrue(record.rejectReason!!.contains("2 hours"))
    }

    /** Lecturer's Part 1 question: a phone's clock can be changed, so the server must never trust it. */
    @Test
    fun phoneClockIgnored_lateUploadStillRejected() = runTest {
        val app = TestApp(backgroundScope)
        app.signInStudent1()
        app.backend.endCheckIn(event)
        val closedAt = app.clock.now()
        app.clock.advance(Duration.ofHours(3)) // the upload really arrives 3 hours after check-in closed
        // The phone claims it captured the scan while check-in was still open, and sends the live-at-the-time code.
        val token = app.backend.qrToken(event, Stage.Arrival, closedAt.epochSecond / Policy.CODE_ROTATION.seconds)
        val claim = AttendanceClaim("r-clock", event, Stage.Arrival, token, "phone-A", capturedAt = closedAt.minusSeconds(60), viaBackupCode = false)
        val result = app.backend.verifyClaim("u-aisha", claim, uploadedLater = true)
        assertTrue(result is ClaimResult.Rejected && result.reason.contains("2 hours"))
    }

    @Test
    fun reversal_isRecordedOnce() = runTest {
        val app = TestApp(backgroundScope)
        app.signInStudent1()
        val before = app.progress.ledger().first().sumOf { it.points }
        app.backend.reverseAward("u-aisha", "volunteer-day", "Office review: record found invalid.")
        app.backend.reverseAward("u-aisha", "volunteer-day", "Office review: record found invalid.")
        assertEquals(before - 3, app.progress.ledger().first().sumOf { it.points })
    }

    @Test
    fun claim_sameRequestIdTwice_countsOnce() = runTest {
        val app = TestApp(backgroundScope)
        app.signInStudent1()
        val claim = qrClaim(app, "r-dup")
        assertEquals(ClaimResult.Verified, app.attendance.submitClaim(claim))
        assertEquals(ClaimResult.Verified, app.attendance.submitClaim(claim))
        assertEquals(StageStatus.VERIFIED, arrival(app))
    }

    @Test
    fun alteredToken_rejected() = runTest {
        val app = TestApp(backgroundScope)
        app.signInStudent1()
        val claim = qrClaim(app, "r-alt").let { it.copy(token = it.token.replace(event, "build-with-ai")) }
        val result = app.attendance.submitClaim(claim)
        assertTrue(result is ClaimResult.Rejected && result.reason.contains("altered"))
    }

    @Test
    fun expiredLiveToken_rejected() = runTest {
        val app = TestApp(backgroundScope)
        app.signInStudent1()
        val claim = qrClaim(app, "r-old")
        app.clock.advance(Duration.ofMinutes(2))
        val result = app.attendance.submitClaim(claim)
        assertTrue(result is ClaimResult.Rejected && result.reason.contains("expired"))
    }

    @Test
    fun correctBackupCode_verified() = runTest {
        val app = TestApp(backgroundScope)
        app.signInStudent1()
        val code = app.backend.backupCode(event, Stage.Arrival, window(app))
        assertEquals(ClaimResult.Verified, app.attendance.submitClaim(codeClaim(app, "r-code", code)))
    }

    @Test
    fun claim_wrongBackupCodeFiveTimes_returnsRateLimited() = runTest {
        val app = TestApp(backgroundScope)
        app.signInStudent1()
        val results = (1..5).map { app.attendance.submitClaim(codeClaim(app, "r-w$it", "000000")) }
        assertTrue(results.take(4).all { it is ClaimResult.Rejected })
        assertEquals(ClaimResult.RateLimited, results.last())
        // Even the right code is refused for the rest of the session.
        val right = app.backend.backupCode(event, Stage.Arrival, window(app))
        assertEquals(ClaimResult.RateLimited, app.attendance.submitClaim(codeClaim(app, "r-right", right)))
    }

    @Test
    fun backupCodeOffline_refused() = runTest {
        val app = TestApp(backgroundScope)
        app.signInStudent1()
        app.network.set(false)
        advanceTimeBy(5_000); runCurrent()
        val result = app.attendance.submitClaim(codeClaim(app, "r-code-off", "123456"))
        assertTrue(result is ClaimResult.Rejected && result.reason.contains("need internet"))
        assertEquals(0, app.attendance.pendingCount().first())
    }

    @Test
    fun claim_sameDeviceDifferentAccountsSameEvent_flagged() = runTest {
        val app = TestApp(backgroundScope)
        app.signInStudent1()
        assertEquals(ClaimResult.Verified, app.attendance.submitClaim(qrClaim(app, "r-a", device = "shared-phone")))
        app.auth.signOut()
        app.signInStudent2()
        val result = app.attendance.submitClaim(qrClaim(app, "r-b", device = "shared-phone"))
        assertEquals(
            ClaimResult.Rejected("Flagged for review: this phone has already checked in another account at this event."),
            result,
        )
        app.signInCommittee()
        val rows = app.attendance.liveAttendees(event).first()
        assertEquals("Same phone used by another account", rows.first { it.userId == "u-daniel" }.flagged)
    }

    @Test
    fun flaggedRecord_holdsPoints_untilTheOfficeClearsIt() = runTest {
        val app = TestApp(backgroundScope)
        app.signInStudent1()
        val before = app.progress.ledger().first().sumOf { it.points }
        app.attendance.submitClaim(qrClaim(app, "r-a1", device = "shared-phone"))
        app.backend.openCheckpoint(event)
        app.attendance.submitClaim(qrClaim(app, "r-a2", stage = Stage.Checkpoint, device = "shared-phone"))
        assertEquals(before + 3, app.progress.ledger().first().sumOf { it.points })
        app.auth.signOut()
        app.signInStudent2()
        app.attendance.submitClaim(qrClaim(app, "r-b", device = "shared-phone"))
        app.auth.signOut()
        app.signInStudent1()
        assertEquals(before, app.progress.ledger().first().sumOf { it.points })
        app.backend.clearFlag("u-aisha", event)
        assertEquals(before + 3, app.progress.ledger().first().sumOf { it.points })
    }

    @Test
    fun offlineSharedPhoneFlag_clearedByOffice_paysTheFirstAward() = runTest {
        val app = TestApp(backgroundScope)
        app.signInStudent1()
        app.attendance.submitClaim(qrClaim(app, "r-a", device = "shared-phone"))
        app.auth.signOut()
        app.signInStudent2()
        val before = app.progress.ledger().first().sumOf { it.points }
        app.backend.openCheckpoint(event)
        app.network.set(false)
        advanceTimeBy(5_000); runCurrent()
        app.attendance.submitClaim(qrClaim(app, "r-b1", device = "shared-phone"))
        app.attendance.submitClaim(qrClaim(app, "r-b2", stage = Stage.Checkpoint, device = "shared-phone"))
        app.network.set(true)
        advanceTimeBy(5_000); runCurrent()
        assertEquals(before, app.progress.ledger().first().sumOf { it.points }) // no points while under review
        app.backend.clearFlag("u-daniel", event)
        val ledger = app.progress.ledger().first()
        assertEquals(before + 3, ledger.sumOf { it.points })
        assertEquals("Verified attendance", ledger.first { it.eventId == event }.reason)
        assertEquals(AttendanceOutcome.VERIFIED, app.attendance.myAttendance().first().first { it.eventId == event }.outcome)
    }

    @Test
    fun clearingAFlagOnAnOwnClubEvent_paysNothing() = runTest {
        val app = TestApp(backgroundScope)
        app.signInStudent1()
        app.attendance.submitClaim(qrClaim(app, "r-a", device = "shared-phone"))
        app.auth.signOut()
        app.signInStudent2()
        app.attendance.submitClaim(qrClaim(app, "r-b", device = "shared-phone"))
        // Daniel joins the CS Society, which organises the live event.
        val cs = app.backend.accounts.value.first { it.profile.uid == "u-cs" }
        app.backend.accounts.value = app.backend.accounts.value.map {
            if (it.profile.uid == "u-daniel") it.copy(profile = it.profile.copy(clubIds = cs.profile.clubIds)) else it
        }
        app.backend.clearFlag("u-daniel", event)
        assertTrue(app.backend.ledger.value.none { it.userId == "u-daniel" && it.entry.eventId == event })
    }

    @Test
    fun offlineScanOnSharedPhone_showsUnderReview_notRejected() = runTest {
        val app = TestApp(backgroundScope)
        app.signInStudent1()
        assertEquals(ClaimResult.Verified, app.attendance.submitClaim(qrClaim(app, "r-a", device = "shared-phone")))
        app.auth.signOut()
        app.signInStudent2()
        app.network.set(false)
        advanceTimeBy(5_000); runCurrent()
        assertEquals(ClaimResult.PendingSync, app.attendance.submitClaim(qrClaim(app, "r-b", device = "shared-phone")))
        app.network.set(true)
        advanceTimeBy(5_000); runCurrent()
        val record = app.attendance.myAttendance().first().first { it.eventId == event }
        assertEquals(AttendanceOutcome.UNDER_REVIEW, record.outcome)
    }

    @Test
    fun checkpointCode_beforeCheckpointOpens_notIssued() = runTest {
        val app = TestApp(backgroundScope)
        assertEquals(null, app.backend.currentCode(event, Stage.Checkpoint))
        app.backend.openCheckpoint(event)
        assertTrue(app.backend.currentCode(event, Stage.Checkpoint) != null)
    }

    @Test
    fun bothStagesVerified_awardsThreePointsOnce() = runTest {
        val app = TestApp(backgroundScope)
        app.signInStudent1()
        val before = app.progress.ledger().first().sumOf { it.points }
        app.attendance.submitClaim(qrClaim(app, "r-arr"))
        app.backend.openCheckpoint(event)
        app.attendance.submitClaim(qrClaim(app, "r-cp", stage = Stage.Checkpoint))
        app.attendance.submitClaim(qrClaim(app, "r-cp2", stage = Stage.Checkpoint))
        assertEquals(before + 3, app.progress.ledger().first().sumOf { it.points })
    }

    @Test
    fun deviceId_isStableAcrossCalls() = runTest {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) { java.io.File(folder.root, "device.preferences_pb") }
        val source = DataStoreDeviceId(store)
        val first = source.get()
        assertEquals(first, source.get())
        assertEquals(36, first.length)
    }
}
