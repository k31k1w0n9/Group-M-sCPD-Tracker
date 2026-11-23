package com.example.scpd_tracker.data.fake

import com.example.scpd_tracker.model.AccountRole
import com.example.scpd_tracker.model.Stage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class M1PermissionsTest {
    private val live = FakeData.LIVE_EVENT

    @Test
    fun studentAndSignedOutCannotReadCommitteeData() = runTest {
        val app = TestApp(backgroundScope)
        assertNull(app.attendance.currentCode(live, Stage.Arrival))
        assertTrue(app.attendance.liveAttendees(live).first().isEmpty())
        assertTrue(app.bookings.eventBookings(live).first().isEmpty())

        app.signInStudent1()
        assertNull(app.attendance.currentCode(live, Stage.Arrival))
        assertTrue(app.attendance.liveAttendees(live).first().isEmpty())
        assertTrue(app.bookings.eventBookings(live).first().isEmpty())

        // student mode must not expose committee data.
        app.auth.signIn(FakeData.COMMITTEE_TECH, FakeData.PASSWORD, AccountRole.Student)
        assertNull(app.attendance.currentCode(live, Stage.Arrival))
        assertTrue(app.attendance.liveAttendees(live).first().isEmpty())
        assertTrue(app.bookings.eventBookings(live).first().isEmpty())
    }

    @Test
    fun committeeCanReadOwnClubOnly() = runTest {
        val app = TestApp(backgroundScope)
        app.signInCommittee()
        assertTrue(app.attendance.currentCode(live, Stage.Arrival) != null)
        assertFalse(app.attendance.liveAttendees(live).first().isEmpty())
        assertFalse(app.bookings.eventBookings(live).first().isEmpty())
        assertNull(app.attendance.currentCode("open-mic", Stage.Arrival))
        assertTrue(app.attendance.liveAttendees("open-mic").first().isEmpty())
        assertTrue(app.bookings.eventBookings("open-mic").first().isEmpty())

        app.auth.signOut()
        app.auth.signIn(FakeData.COMMITTEE_ARTS, FakeData.PASSWORD, AccountRole.Committee)
        assertNull(app.attendance.currentCode(live, Stage.Arrival))
        assertTrue(app.attendance.liveAttendees(live).first().isEmpty())
        assertTrue(app.bookings.eventBookings(live).first().isEmpty())
    }
}
