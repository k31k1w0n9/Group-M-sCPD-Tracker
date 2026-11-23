package com.example.scpd_tracker.data.fake

import com.example.scpd_tracker.model.AccountRole
import com.example.scpd_tracker.model.AttendanceClaim
import com.example.scpd_tracker.model.BookResult
import com.example.scpd_tracker.model.Booking
import com.example.scpd_tracker.model.BookingStatus
import com.example.scpd_tracker.model.ClaimResult
import com.example.scpd_tracker.model.Stage
import com.example.scpd_tracker.domain.outcome
import com.example.scpd_tracker.domain.points
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DualRoleRulesTest {
    @Test
    fun committeeMember_booksAnotherClubAsStudent() = runTest {
        val app = TestApp(backgroundScope)
        app.auth.signIn(FakeData.COMMITTEE_TECH, FakeData.PASSWORD, AccountRole.Student)
        assertTrue(app.bookings.book("open-mic", "other-club-1") is BookResult.Booked)
        assertTrue(app.bookings.myBookings().first().any { it.eventId == "open-mic" })
    }

    @Test
    fun committeeMember_cannotManageAnotherClub() = runTest {
        val app = TestApp(backgroundScope)
        app.signInCommittee()
        assertTrue(app.events.clubEvents("cs-society").first().isNotEmpty())
        assertTrue(app.events.clubEvents("arts-club").first().isEmpty())
        assertTrue(app.events.cancelEvent("open-mic", "Testing another club").isFailure)
        assertFalse(app.backend.events.value.getValue("open-mic").isCancelled)
    }

    @Test
    fun ownClubCheckIn_addsNoPoints() = runTest {
        val app = TestApp(backgroundScope)
        val seed = FakeData.seed(TEST_NOW)
        val booking = Booking("own-club-booking", FakeData.LIVE_EVENT, "u-cs", BookingStatus.Booked, TEST_NOW)
        val backend = FakeBackend(app.clock, seed.copy(bookings = seed.bookings + booking))
        fun claim(stage: Stage, suffix: String) = AttendanceClaim(
            "own-club-$suffix", FakeData.LIVE_EVENT, stage,
            backend.qrToken(FakeData.LIVE_EVENT, stage, TEST_NOW.epochSecond / 30L),
            "committee-phone", TEST_NOW, false,
        )
        assertEquals(ClaimResult.Verified, backend.verifyClaim("u-cs", claim(Stage.Arrival, "arrival"), false))
        backend.openCheckpoint(FakeData.LIVE_EVENT)
        assertEquals(ClaimResult.Verified, backend.verifyClaim("u-cs", claim(Stage.Checkpoint, "checkpoint"), false))
        assertEquals(0, backend.ledger.value.filter { it.userId == "u-cs" }.sumOf { it.entry.points })
        val record = backend.attendanceFor("u-cs").first { it.eventId == FakeData.LIVE_EVENT }
        assertEquals(com.example.scpd_tracker.model.AttendanceOutcome.VERIFIED, record.outcome)
        assertEquals(0, record.points)
    }
}
