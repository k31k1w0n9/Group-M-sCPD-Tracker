package com.example.scpd_tracker.ui.committee

import androidx.lifecycle.SavedStateHandle
import com.example.scpd_tracker.data.fake.FakeData
import com.example.scpd_tracker.data.fake.TestApp
import com.example.scpd_tracker.domain.FormError
import com.example.scpd_tracker.model.ReviewStatus
import com.example.scpd_tracker.model.Stage
import com.example.scpd_tracker.ui.MainDispatcherRule
import com.example.scpd_tracker.ui.keepCollecting
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Duration

class CommitteeViewModelsTest {
    @get:Rule val main = MainDispatcherRule()

    private suspend fun TestScope.committee(): TestApp = TestApp(backgroundScope).also { it.signInCommittee() }

    private fun TestApp.form(id: String? = null) =
        EventFormViewModel(events, auth, clock, SavedStateHandle(if (id == null) emptyMap() else mapOf("eventId" to id)))

    private fun EventFormViewModel.fillValid(app: TestApp) {
        val start = app.clock.now().atZone(com.example.scpd_tracker.domain.Policy.ZONE).plusDays(5)
        onTitle("Git for Group Projects")
        onDescription("Branches, pull requests and resolving merge conflicts.")
        onVenue("F1A02")
        onStart(start)
        onCapacity("30")
        onToggleTag("Tech")
        onJustification("Practical skill every computing student needs.")
    }

    // ---- Dashboard ----

    @Test
    fun committeeOnlySeesOwnClubEvents() = runTest {
        val app = committee()
        val vm = DashboardViewModel(app.events, app.auth, app.network, app.clock)
        keepCollecting(vm.state); advanceUntilIdle()
        val all = with(vm.state.value) { upcoming + pending + drafts + past + changesRequested + closed }
        assertTrue(all.isNotEmpty())
        assertTrue(all.all { it.clubId == "cs-society" })
        assertEquals(listOf(FakeData.LIVE_EVENT), vm.state.value.runnable.map { it.id })
    }

    // ---- Event form (Task 9) ----

    @Test
    fun approvedEvent_formIsLockedReadOnly() = runTest {
        val vm = committee().form("build-with-ai")
        keepCollecting(vm.state); advanceUntilIdle()
        assertTrue(vm.state.value.isLocked)
        assertTrue(vm.state.value.isApproved)
        vm.onTitle("Changed")
        vm.submit()
        advanceUntilIdle()
        assertEquals(ReviewStatus.Approved, vm.state.value.status)
    }

    @Test
    fun changesRequested_showsAdminComment_andEditable() = runTest {
        val vm = committee().form("campus-hack-night")
        keepCollecting(vm.state); advanceUntilIdle()
        assertEquals("Venue approval needed. Upload the booking confirmation and resubmit.", vm.state.value.reviewComment)
        assertEquals(false, vm.state.value.isLocked)
        assertEquals("Campus Hack Night", vm.state.value.form.title)
    }

    @Test
    fun submitWithErrors_showsFieldErrors_noRepositoryCall() = runTest {
        val app = committee()
        val before = app.backend.events.value.size
        val vm = app.form()
        keepCollecting(vm.state)
        vm.onTitle("AI")
        vm.submit()
        advanceUntilIdle()
        assertEquals(FormError.TitleLength, vm.state.value.errors["title"])
        assertEquals(before, app.backend.events.value.size)
    }

    @Test
    fun submitValid_movesToPendingReview() = runTest {
        val app = committee()
        val vm = app.form()
        keepCollecting(vm.state)
        vm.fillValid(app)
        vm.submit()
        advanceUntilIdle()
        assertTrue(vm.state.value.finished)
        val created = app.backend.events.value.values.single { it.title == "Git for Group Projects" }
        assertEquals(ReviewStatus.PendingReview, created.reviewStatus)
        assertEquals("cs-society", created.clubId)
    }

    @Test
    fun withdrawPending_returnsToDraft() = runTest {
        val app = committee()
        val vm = app.form("prototype-playground")
        keepCollecting(vm.state); advanceUntilIdle()
        vm.withdraw()
        advanceUntilIdle()
        assertEquals(ReviewStatus.Draft, app.events.event("prototype-playground").first()?.reviewStatus)
    }

    @Test
    fun cancelApproved_requiresReason_thenCancelsBookings() = runTest {
        val app = committee()
        val vm = app.form("python-next-steps")
        keepCollecting(vm.state); advanceUntilIdle()
        vm.cancelEvent()
        runCurrent()
        assertEquals("Tell students why, in at least 10 characters.", vm.state.value.cancelError)
        vm.onCancelReason("Speaker is ill, new date to follow.")
        vm.cancelEvent()
        advanceUntilIdle()
        assertTrue(app.events.event("python-next-steps").first()!!.isCancelled)
        assertTrue(app.backend.bookings.value.filter { it.eventId == "python-next-steps" }.all { it.cancelledByOrganiser })
    }

    @Test
    fun halfFilledForm_survivesRecreation() = runTest {
        val app = committee()
        val saved = SavedStateHandle()
        EventFormViewModel(app.events, app.auth, app.clock, saved).apply { onTitle("Half"); onCapacity("12x") }
        val restored = EventFormViewModel(app.events, app.auth, app.clock, saved)
        keepCollecting(restored.state); advanceUntilIdle()
        assertEquals("Half", restored.state.value.form.title)
        assertEquals("12", restored.state.value.form.capacity)
    }

    // ---- Committee check-in (Task 10) ----

    private fun TestApp.checkIn(id: String = FakeData.LIVE_EVENT) =
        CommitteeCheckInViewModel(events, attendance, network, clock, SavedStateHandle(mapOf("eventId" to id)))

    @Test
    fun codeRotatesEvery30Seconds() = runTest {
        val app = committee()
        val vm = app.checkIn()
        keepCollecting(vm.state); runCurrent()
        val first = vm.state.value.code!!.backupCode
        app.clock.advance(Duration.ofSeconds(30))
        advanceTimeBy(1_100); runCurrent()
        assertNotEquals(first, vm.state.value.code!!.backupCode)
    }

    @Test
    fun offline_pausesNewCodes() = runTest {
        val app = committee()
        val vm = app.checkIn()
        keepCollecting(vm.state); runCurrent()
        app.network.set(false)
        advanceTimeBy(1_100); runCurrent()
        assertNull(vm.state.value.code)
        assertTrue(vm.state.value.isOffline)
    }

    @Test
    fun openCheckpoint_switchesStage() = runTest {
        val app = committee()
        val vm = app.checkIn()
        keepCollecting(vm.state); runCurrent()
        vm.openCheckpoint()
        advanceTimeBy(2_000); runCurrent()
        assertEquals(Stage.Checkpoint, vm.state.value.stage)
    }

    @Test
    fun countShowsCheckedInOfBooked() = runTest {
        val app = committee()
        val vm = app.checkIn()
        keepCollecting(vm.state); runCurrent()
        assertEquals("14 of 22 checked in", vm.state.value.countLabel)
        assertEquals(1, vm.state.value.flaggedCount)
    }

    @Test
    fun endCheckIn_closesSession() = runTest {
        val app = committee()
        val vm = app.checkIn()
        keepCollecting(vm.state); runCurrent()
        vm.endCheckIn()
        advanceTimeBy(2_000); runCurrent()
        assertTrue(vm.state.value.sessionClosed)
        assertNull(vm.state.value.code)
    }

    @Test
    fun attendees_flaggedFilterShowsReason() = runTest {
        val app = committee()
        val vm = AttendeesViewModel(app.events, app.attendance, app.bookings, SavedStateHandle(mapOf("eventId" to FakeData.LIVE_EVENT)))
        keepCollecting(vm.state)
        vm.onFilter(AttendeeFilter.Flagged)
        advanceUntilIdle()
        assertEquals("Same phone used by another account", vm.state.value.rows.single().flagged)
        assertEquals(22, vm.state.value.counts[AttendeeFilter.All])
    }
}
