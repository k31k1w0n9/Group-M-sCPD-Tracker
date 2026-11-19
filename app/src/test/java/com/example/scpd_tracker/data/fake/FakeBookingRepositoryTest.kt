package com.example.scpd_tracker.data.fake

import com.example.scpd_tracker.model.AccountRole
import com.example.scpd_tracker.model.BookResult
import com.example.scpd_tracker.model.BookingDecision
import com.example.scpd_tracker.model.BookingStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeBookingRepositoryTest {

    @Test
    fun book_sameRequestIdTwice_createsOneBooking() = runTest {
        val app = TestApp(backgroundScope)
        app.signInStudent1()
        val first = app.bookings.book("build-with-ai", "req-1")
        val second = app.bookings.book("build-with-ai", "req-1")
        assertTrue(first is BookResult.Booked)
        assertEquals(first, second)
        assertEquals(1, app.bookings.myBookings().first().count { it.eventId == "build-with-ai" })
        assertEquals(29, app.backend.events.value.getValue("build-with-ai").bookedCount)
    }

    @Test
    fun book_lastPlaceTwoUsers_onlyOneBooked() = runTest {
        val app = TestApp(backgroundScope)
        // Leave exactly one place on Build with AI.
        app.backend.events.value = app.backend.events.value + ("build-with-ai" to app.backend.events.value.getValue("build-with-ai").copy(bookedCount = 39))
        val results = listOf("u-aisha", "u-daniel").map { uid ->
            async { app.backend.book(uid, "build-with-ai", "req-$uid", waitlist = false) }
        }.awaitAll()
        assertEquals(1, results.count { it is BookResult.Booked })
        assertEquals(1, results.count { it == BookResult.Rejected(BookingDecision.Full(canWaitlist = true)) })
        assertEquals(40, app.backend.events.value.getValue("build-with-ai").bookedCount)
    }

    @Test
    fun book_whileOffline_returnsOffline_andNoBooking() = runTest {
        val app = TestApp(backgroundScope)
        app.signInStudent1()
        app.network.set(false)
        assertEquals(BookResult.Offline, app.bookings.book("build-with-ai", "req-off"))
        assertTrue(app.bookings.myBookings().first().none { it.eventId == "build-with-ai" })
    }

    @Test
    fun cancel_promotesFirstWaitlisted() = runTest {
        val app = TestApp(backgroundScope)
        app.signInStudent1()
        // Aisha joins the full garden event's waitlist; Daniel holds a place and cancels it.
        val waiting = app.bookings.joinWaitlist("garden-volunteering", "req-wait") as BookResult.Waitlisted
        assertEquals(1, waiting.booking.waitlistPosition)
        val danielBooking = app.backend.bookings.value.first { it.userId == "u-daniel" && it.eventId == "garden-volunteering" }
        app.backend.cancelBooking("u-daniel", danielBooking.id).getOrThrow()
        val promoted = app.bookings.myBookings().first().first { it.eventId == "garden-volunteering" }
        assertEquals(BookingStatus.Booked, promoted.status)
        assertEquals(25, app.backend.events.value.getValue("garden-volunteering").bookedCount)
    }

    @Test
    fun clashingEvent_rejectedWithClash() = runTest {
        val app = TestApp(backgroundScope)
        app.signInStudent1() // already booked on Python: Next Steps, which overlaps the Android talk
        val result = app.bookings.book("tech-talk-android", "req-clash")
        assertTrue(result is BookResult.Rejected && result.decision is BookingDecision.Clash)
    }

    @Test
    fun committeeAccount_cannotSignInAsStudent() = runTest {
        val app = TestApp(backgroundScope)
        val result = app.auth.signIn(FakeData.COMMITTEE_TECH, FakeData.PASSWORD, AccountRole.Student)
        assertEquals(com.example.scpd_tracker.model.SignInResult.RoleNotAllowed, result)
    }
}
