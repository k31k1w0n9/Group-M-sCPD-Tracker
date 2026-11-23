package com.example.scpd_tracker.data.fake

import com.example.scpd_tracker.model.Booking
import com.example.scpd_tracker.model.BookingStatus
import com.example.scpd_tracker.model.StageStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationTriggersTest {
    @Test
    fun cancellationQueuesAnAlertForEveryAffectedStudent() = runTest {
        val app = TestApp(backgroundScope)
        app.backend.bookings.value = app.backend.bookings.value +
            Booking("notify-cancel", "build-with-ai", "u-aisha", BookingStatus.Booked, TEST_NOW)
        app.signInCommittee()
        assertTrue(app.events.cancelEvent("build-with-ai", "Room unavailable").isSuccess)
        val notice = app.backend.alerts.value.single()
        assertEquals("u-aisha", notice.recipientId)
        assertEquals(AlertKind.Cancelled, notice.kind)
        assertEquals("Room unavailable", notice.detail)
        assertEquals("build-with-ai", notice.eventId)
    }

    @Test
    fun waitlistPromotionQueuesAnAlertWithoutStudentLogin() = runTest {
        val app = TestApp(backgroundScope)
        app.backend.bookings.value = app.backend.bookings.value + listOf(
            Booking("notify-booked", "open-mic", "u-aisha", BookingStatus.Booked, TEST_NOW),
            Booking("notify-waiting", "open-mic", "u-daniel", BookingStatus.Waitlisted, TEST_NOW),
        )
        assertTrue(app.backend.cancelBooking("u-aisha", "notify-booked").isSuccess)
        val notice = app.backend.alerts.value.single()
        assertEquals(AlertKind.Waitlist, notice.kind)
        assertEquals("u-daniel", notice.recipientId)
        assertEquals(BookingStatus.Booked,
            app.backend.bookings.value.first { it.id == "notify-waiting" }.status)
    }

    @Test
    fun checkpointAlertsOnlyStudentsWhoArrived() = runTest {
        val app = TestApp(backgroundScope)
        app.backend.evidence.value = app.backend.evidence.value +
            ("u-daniel|${FakeData.LIVE_EVENT}" to FakeBackend.Evidence(arrival = StageStatus.VERIFIED))
        assertTrue(app.backend.openCheckpoint(FakeData.LIVE_EVENT).isSuccess)
        assertTrue(app.backend.openCheckpoint(FakeData.LIVE_EVENT).isSuccess)
        val notices = app.backend.alerts.value
        assertTrue(notices.any { it.recipientId == "u-daniel" && it.kind == AlertKind.Checkpoint })
        assertTrue(notices.none { it.recipientId == "u-aisha" })
        assertEquals(notices.map { it.recipientId }.distinct().size, notices.size)
        assertTrue(notices.all { it.eventTitle == "Leadership Workshop" })
    }
}
