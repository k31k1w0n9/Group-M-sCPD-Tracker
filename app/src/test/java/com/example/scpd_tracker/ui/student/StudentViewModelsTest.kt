package com.example.scpd_tracker.ui.student

import androidx.lifecycle.SavedStateHandle
import com.example.scpd_tracker.data.fake.FakeData
import com.example.scpd_tracker.data.fake.TestApp
import com.example.scpd_tracker.model.BookResult
import com.example.scpd_tracker.model.BookingDecision
import com.example.scpd_tracker.model.ReviewStatus
import com.example.scpd_tracker.ui.MainDispatcherRule
import com.example.scpd_tracker.ui.keepCollecting
import com.example.scpd_tracker.ui.student.details.BookingOutcomeUi
import com.example.scpd_tracker.ui.student.details.EventDetailsViewModel
import com.example.scpd_tracker.ui.student.home.HomeViewModel
import com.example.scpd_tracker.ui.student.myevents.MyEventsTab
import com.example.scpd_tracker.ui.student.myevents.MyEventsViewModel
import com.example.scpd_tracker.ui.student.profile.ProfileViewModel
import com.example.scpd_tracker.ui.student.progress.ProgressViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StudentViewModelsTest {
    @get:Rule val main = MainDispatcherRule()

    private suspend fun TestScope.signedIn(): TestApp = TestApp(backgroundScope).also { it.signInStudent1() }

    private fun TestApp.home(browsing: Boolean = false) =
        HomeViewModel(events, bookings, attendance, progress, auth, network, clock, SavedStateHandle(), browsing)

    private fun TestApp.details(id: String) = EventDetailsViewModel(events, bookings, network, clock, SavedStateHandle(mapOf("eventId" to id)))

    // ---- Home (Task 4) ----

    @Test
    fun greeting_usesSignedInName() = runTest {
        val vm = signedIn().home()
        keepCollecting(vm.state); advanceUntilIdle()
        assertEquals("Aisha", vm.state.value.greetingName)
    }

    @Test
    fun bookedEventWithCheckInOpen_showsHappeningNow() = runTest {
        val vm = signedIn().home()
        keepCollecting(vm.state); advanceUntilIdle()
        assertEquals(FakeData.LIVE_EVENT, vm.state.value.happeningNow?.id)
    }

    @Test
    fun recommendations_excludeBookedAndClashingEvents() = runTest {
        val vm = signedIn().home()
        keepCollecting(vm.state); advanceUntilIdle()
        val ids = vm.state.value.cards.map { it.event.id }
        assertFalse("python-next-steps" in ids) // already booked
        assertFalse("tech-talk-android" in ids) // clashes with Python
        // The best match is featured at the top, with its reason.
        assertEquals("build-with-ai", vm.state.value.featured?.event?.id)
        assertEquals("Because you like Tech", vm.state.value.featured?.reason)
        assertFalse("build-with-ai" in ids)
    }

    @Test
    fun pointsAndStreak_fromLedgerAndHistory() = runTest {
        val vm = signedIn().home()
        keepCollecting(vm.state); advanceUntilIdle()
        assertEquals(9, vm.state.value.points) // 4 awards, 1 reversal
        assertEquals(3, vm.state.value.streakWeeks)
    }

    @Test
    fun noEvents_showsEmptyState() = runTest {
        val app = signedIn()
        app.backend.events.value = emptyMap()
        val vm = app.home()
        keepCollecting(vm.state); advanceUntilIdle()
        assertTrue(vm.state.value.cards.isEmpty())
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun homeFilteredFeed_isNotLabelledRecommendations() = runTest {
        val vm = signedIn().home(browsing = false)
        keepCollecting(vm.state)
        vm.onFilter("All interests")
        runCurrent()
        assertEquals("Events", vm.state.value.sectionTitle)
    }

    @Test
    fun exploreSearch_filtersByText() = runTest {
        val vm = signedIn().home(browsing = true)
        keepCollecting(vm.state)
        vm.onQuery("garden")
        advanceUntilIdle()
        assertEquals(listOf("garden-volunteering"), vm.state.value.cards.map { it.event.id })
    }

    // ---- Event details (Task 5) ----

    @Test
    fun full_ctaIsJoinWaitlist() = runTest {
        val vm = signedIn().details("garden-volunteering")
        keepCollecting(vm.state); advanceUntilIdle()
        assertEquals(BookingDecision.Full(canWaitlist = true), vm.state.value.decision)
    }

    @Test
    fun clash_showsOtherEventName() = runTest {
        val vm = signedIn().details("tech-talk-android")
        keepCollecting(vm.state); advanceUntilIdle()
        assertEquals("Clashes with Python: Next Steps (Wed 15:00)", vm.state.value.blockedReason)
    }

    @Test
    fun doubleTapBook_booksOnce() = runTest {
        val app = signedIn()
        val vm = app.details("build-with-ai")
        keepCollecting(vm.state); advanceUntilIdle()
        vm.confirmBooking()
        vm.confirmBooking()
        advanceUntilIdle()
        assertEquals(BookingOutcomeUi.Confirmed, vm.state.value.result)
        assertEquals(1, app.bookings.myBookings().first().count { it.eventId == "build-with-ai" })
    }

    @Test
    fun offline_bookShowsRetry_noBooking() = runTest {
        val app = signedIn()
        app.network.set(false)
        val vm = app.details("build-with-ai")
        keepCollecting(vm.state); advanceUntilIdle()
        vm.confirmBooking()
        advanceUntilIdle()
        assertTrue(vm.state.value.result is BookingOutcomeUi.Failed)
        assertTrue(app.bookings.myBookings().first().none { it.eventId == "build-with-ai" })
    }

    @Test
    fun capReached_explainsLimit() = runTest {
        val app = signedIn()
        // Three more bookings on events that don't overlap, so Aisha holds the maximum of five.
        listOf("open-mic", "five-a-side", "build-with-ai").forEach { app.bookings.book(it, "cap-$it") }
        val vm = app.details("garden-volunteering")
        keepCollecting(vm.state); advanceUntilIdle()
        assertEquals("You already have 5 active bookings. Cancel one to book this.", vm.state.value.blockedReason)
    }

    // ---- My Events (Task 6) ----

    private fun TestApp.myEvents() = MyEventsViewModel(events, bookings, attendance, network, clock, SavedStateHandle())

    @Test
    fun insideCutoff_cancelNotAllowed() = runTest {
        val vm = signedIn().myEvents()
        keepCollecting(vm.state); advanceUntilIdle()
        val live = vm.state.value.upcoming.first { it.event.id == FakeData.LIVE_EVENT }
        assertFalse(live.canCancel)
        assertTrue(vm.state.value.upcoming.first { it.event.id == "python-next-steps" }.canCancel)
    }

    @Test
    fun checkInOpen_showsCheckInButton() = runTest {
        val vm = signedIn().myEvents()
        keepCollecting(vm.state); advanceUntilIdle()
        assertTrue(vm.state.value.upcoming.first { it.event.id == FakeData.LIVE_EVENT }.checkInOpen)
    }

    @Test
    fun waitlisted_showsPosition() = runTest {
        val app = signedIn()
        app.bookings.joinWaitlist("garden-volunteering", "w1")
        val vm = app.myEvents()
        keepCollecting(vm.state); advanceUntilIdle()
        assertEquals(1, vm.state.value.waitlisted.single().booking.waitlistPosition)
    }

    @Test
    fun pastTab_showsOrganiserCancelledNoShowAndOutcomes() = runTest {
        val vm = signedIn().myEvents()
        keepCollecting(vm.state)
        vm.selectTab(MyEventsTab.Past)
        advanceUntilIdle()
        val statuses = vm.state.value.past.associate { it.event.id to it.status }
        assertEquals("Event cancelled", statuses["career-fair"])
        assertEquals("No-show", statuses["sports-day"])
        assertEquals("Incomplete attendance", statuses["design-sprint"])
        assertEquals("Verified", statuses["intro-python"])
    }

    @Test
    fun cancelBooking_movesToCancelledPage() = runTest {
        val app = signedIn()
        val vm = app.myEvents()
        keepCollecting(vm.state); advanceUntilIdle()
        val python = vm.state.value.upcoming.first { it.event.id == "python-next-steps" }
        vm.openBooking(python.booking.id)
        runCurrent()
        vm.askCancel()
        vm.confirmCancel()
        advanceUntilIdle()
        assertEquals("Python: Next Steps", vm.state.value.cancelledEvent?.title)
        assertTrue(vm.state.value.upcoming.none { it.event.id == "python-next-steps" })
    }

    // ---- Progress and Profile (Task 8) ----

    @Test
    fun reversal_showsMinusThreeWithReason() = runTest {
        val app = signedIn()
        val vm = ProgressViewModel(app.attendance, app.progress, app.events, app.auth, app.clock)
        keepCollecting(vm.state); advanceUntilIdle()
        val reversal = vm.state.value.entries.single { it.points < 0 }
        assertEquals(-3, reversal.points)
        assertTrue(reversal.reason.startsWith("Office review"))
        assertEquals(9, vm.state.value.total)
    }

    /** Lecturer's Part 1 question: what "corrections recalculate the streak" means, as a worked example. */
    @Test
    fun reversal_recalculatesStreak() = runTest {
        val app = signedIn()
        val vm = ProgressViewModel(app.attendance, app.progress, app.events, app.auth, app.clock)
        keepCollecting(vm.state); advanceUntilIdle()
        assertEquals(3, vm.state.value.streakWeeks) // verified in weeks -1, -2 and -3
        // The office finds the week -2 record invalid: that week no longer counts, so the streak stops at week -1.
        app.backend.reverseAward("u-aisha", "volunteer-day", "Office review: record found invalid.")
        advanceUntilIdle()
        assertEquals(1, vm.state.value.streakWeeks)
        assertEquals(6, vm.state.value.total)
    }

    @Test
    fun pendingClaims_shownAsNotYetCounted() = runTest {
        val app = signedIn()
        app.network.set(false)
        runCurrent()
        val token = app.backend.currentCode(FakeData.LIVE_EVENT, com.example.scpd_tracker.model.Stage.Arrival)!!.qrPayload
        app.attendance.submitClaim(
            com.example.scpd_tracker.model.AttendanceClaim("p1", FakeData.LIVE_EVENT, com.example.scpd_tracker.model.Stage.Arrival, token, "phone-A", app.clock.now(), false),
        )
        val vm = ProgressViewModel(app.attendance, app.progress, app.events, app.auth, app.clock)
        keepCollecting(vm.state); runCurrent()
        assertEquals("1 check-in waiting to sync. Points appear once it's verified.", vm.state.value.pendingNote)
        assertEquals(9, vm.state.value.total) // not counted yet
    }

    @Test
    fun signOutWithPending_showsWarningFirst() = runTest {
        val app = signedIn()
        app.network.set(false)
        runCurrent()
        val token = app.backend.currentCode(FakeData.LIVE_EVENT, com.example.scpd_tracker.model.Stage.Arrival)!!.qrPayload
        app.attendance.submitClaim(
            com.example.scpd_tracker.model.AttendanceClaim("p2", FakeData.LIVE_EVENT, com.example.scpd_tracker.model.Stage.Arrival, token, "phone-A", app.clock.now(), false),
        )
        val vm = ProfileViewModel(app.auth, app.attendance, app.bookings, app.events)
        keepCollecting(vm.state)
        vm.requestSignOut()
        runCurrent()
        assertEquals(1, vm.state.value.signOutWarning)
        assertFalse(vm.state.value.signOutConfirmed)
        vm.confirmSignOut()
        runCurrent()
        assertTrue(vm.state.value.signOutConfirmed)
    }

    @Test
    fun signOutWithNoPending_signsOutDirectly() = runTest {
        val app = signedIn()
        val vm = ProfileViewModel(app.auth, app.attendance, app.bookings, app.events)
        keepCollecting(vm.state)
        vm.requestSignOut()
        advanceUntilIdle()
        assertNull(vm.state.value.signOutWarning)
        assertTrue(vm.state.value.signOutConfirmed)
    }

    @Test
    fun committeeApproval_reachesStudentsStraightAway() = runTest {
        val app = signedIn()
        val vm = app.home(browsing = true)
        keepCollecting(vm.state); advanceUntilIdle()
        assertFalse(vm.state.value.cards.any { it.event.id == "prototype-playground" })
        app.backend.adminDecide("prototype-playground", ReviewStatus.Approved, null)
        advanceUntilIdle()
        assertNotNull(vm.state.value.cards.firstOrNull { it.event.id == "prototype-playground" })
    }

    @Test
    fun bookResult_offlineIsTypedOffline() = runTest {
        val app = signedIn()
        app.network.set(false)
        assertEquals(BookResult.Offline, app.bookings.book("open-mic", "x"))
    }
}
