package com.example.scpd_tracker.ui.student

import androidx.lifecycle.SavedStateHandle
import com.example.scpd_tracker.data.fake.FakeData
import com.example.scpd_tracker.data.fake.TestApp
import com.example.scpd_tracker.model.Stage
import com.example.scpd_tracker.model.StageStatus
import com.example.scpd_tracker.ui.MainDispatcherRule
import com.example.scpd_tracker.ui.keepCollecting
import com.example.scpd_tracker.ui.student.checkin.AttemptResult
import com.example.scpd_tracker.ui.student.checkin.CheckInViewModel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CheckInViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val event = FakeData.LIVE_EVENT

    private fun TestApp.checkIn() = CheckInViewModel(events, attendance, deviceId, network, clock, SavedStateHandle(mapOf("eventId" to event)))
    private fun TestApp.liveQr(stage: Stage = Stage.Arrival) = backend.currentCode(event, stage)!!.qrPayload
    private fun TestApp.liveCode() = backend.currentCode(event, Stage.Arrival)!!.backupCode

    private suspend fun TestScope.ready(): Pair<TestApp, CheckInViewModel> {
        val app = TestApp(backgroundScope)
        app.signInStudent1()
        val vm = app.checkIn()
        keepCollecting(vm.state)
        advanceUntilIdle()
        return app to vm
    }

    /** Scan, then pass the identity check, the way the screen does it. */
    private fun TestScope.scan(vm: CheckInViewModel, token: String) {
        vm.onScanned(token)
        runCurrent()
        assertTrue(vm.state.value.awaitingIdentity)
        vm.onIdentityConfirmed()
        advanceUntilIdle()
    }

    @Test
    fun scanOnline_stageVerified() = runTest {
        val (app, vm) = ready()
        scan(vm, app.liveQr())
        assertEquals(AttemptResult.Verified(Stage.Arrival), vm.state.value.lastResult)
        assertEquals(StageStatus.VERIFIED, vm.state.value.stages.first().status)
    }

    @Test
    fun scanOffline_stagePendingSync_andChipSaysSavedOnPhone() = runTest {
        val (app, vm) = ready()
        app.network.set(false)
        runCurrent()
        scan(vm, app.liveQr())
        assertEquals(AttemptResult.Saved(Stage.Arrival), vm.state.value.lastResult)
        assertEquals(StageStatus.PENDING_SYNC, vm.state.value.stages.first().status)
        assertEquals("Saved on phone", vm.state.value.stages.first().status.label)
        assertEquals(1, vm.state.value.pendingUploads)
        app.network.set(true)
        advanceTimeBy(5_000); runCurrent()
        assertEquals(StageStatus.VERIFIED, vm.state.value.stages.first().status)
        assertTrue(vm.state.value.lastSyncedLabel!!.startsWith("Last synced"))
    }

    @Test
    fun backupCodeWhileOffline_blockedWithMessage() = runTest {
        val (app, vm) = ready()
        app.network.set(false)
        runCurrent()
        vm.onCodeChange(app.liveCode())
        vm.submitCode()
        runCurrent()
        assertEquals("Backup codes need internet. Scan the QR instead, or reconnect.", vm.state.value.codeError)
        assertEquals(false, vm.state.value.awaitingIdentity)
    }

    @Test
    fun correctBackupCode_verified() = runTest {
        val (app, vm) = ready()
        vm.onCodeChange(app.liveCode())
        vm.submitCode()
        runCurrent()
        vm.onIdentityConfirmed()
        advanceUntilIdle()
        assertEquals(AttemptResult.Verified(Stage.Arrival), vm.state.value.lastResult)
    }

    @Test
    fun claim_includesDeviceId_soSharedPhoneIsFlagged() = runTest {
        val (app, vm) = ready()
        app.deviceName = "shared-phone"
        scan(vm, app.liveQr())
        app.auth.signOut()
        app.signInStudent2()
        val second = app.checkIn()
        keepCollecting(second.state); advanceUntilIdle()
        scan(second, app.liveQr())
        val result = second.state.value.lastResult as AttemptResult.Rejected
        assertTrue(result.reason.startsWith("Flagged for review"))
    }

    @Test
    fun rejected_showsReason() = runTest {
        val (_, vm) = ready()
        scan(vm, "not-an-scpd-code")
        assertEquals(AttemptResult.Rejected("This QR code isn't an s-CPD check-in code."), vm.state.value.lastResult)
    }

    @Test
    fun rateLimited_disablesCodeEntryForSession() = runTest {
        val (_, vm) = ready()
        repeat(5) {
            vm.onCodeChange("000000")
            vm.submitCode()
            runCurrent()
            vm.onIdentityConfirmed()
            advanceUntilIdle()
        }
        assertTrue(vm.state.value.rateLimited)
    }

    @Test
    fun checkpointNotOpen_onlyArrivalIsActive() = runTest {
        val (app, vm) = ready()
        scan(vm, app.liveQr())
        assertNull(vm.state.value.activeStage) // arrival done, checkpoint not open
        assertEquals(false, vm.state.value.stages.last().open)
        app.backend.openCheckpoint(event)
        advanceUntilIdle()
        assertEquals(Stage.Checkpoint, vm.state.value.activeStage)
        scan(vm, app.liveQr(Stage.Checkpoint))
        assertEquals(com.example.scpd_tracker.model.AttendanceOutcome.VERIFIED, vm.state.value.outcome)
        assertEquals(3, vm.state.value.pointsEarned)
    }

    @Test
    fun identityCancelled_sendsNothing() = runTest {
        val (app, vm) = ready()
        vm.onScanned(app.liveQr())
        runCurrent()
        vm.onIdentityCancelled()
        advanceUntilIdle()
        assertEquals(StageStatus.NOT_SCANNED, vm.state.value.stages.first().status)
    }

    @Test
    fun codeEntry_survivesRecreation() = runTest {
        val app = TestApp(backgroundScope)
        app.signInStudent1()
        val saved = SavedStateHandle(mapOf("eventId" to event))
        CheckInViewModel(app.events, app.attendance, app.deviceId, app.network, app.clock, saved).onCodeChange("12a34")
        val restored = CheckInViewModel(app.events, app.attendance, app.deviceId, app.network, app.clock, saved)
        keepCollecting(restored.state); advanceUntilIdle()
        assertEquals("1234", restored.state.value.codeInput) // digits only
    }
}
