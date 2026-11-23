package com.example.scpd_tracker.ui.student.checkin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scpd_tracker.data.AppClock
import com.example.scpd_tracker.data.AttendanceRepository
import com.example.scpd_tracker.data.DeviceIdSource
import com.example.scpd_tracker.data.EventRepository
import com.example.scpd_tracker.data.NetworkMonitor
import com.example.scpd_tracker.domain.outcome
import com.example.scpd_tracker.domain.points
import com.example.scpd_tracker.model.AttendanceClaim
import com.example.scpd_tracker.model.AttendanceOutcome
import com.example.scpd_tracker.model.ClaimResult
import com.example.scpd_tracker.model.Event
import com.example.scpd_tracker.model.EventState
import com.example.scpd_tracker.model.Stage
import com.example.scpd_tracker.model.StageStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.util.UUID

data class StageUi(val stage: Stage, val status: StageStatus, val open: Boolean, val note: String)

/** The result of the last attempt, shown inline until the next one. */
sealed interface AttemptResult {
    data class Verified(val stage: Stage) : AttemptResult
    data class Saved(val stage: Stage) : AttemptResult
    data class Rejected(val reason: String) : AttemptResult
}

data class CheckInUiState(
    val event: Event? = null,
    val stages: List<StageUi> = emptyList(),
    /** The stage a scan or code would count for right now, or null when nothing is open. */
    val activeStage: Stage? = null,
    val outcome: AttendanceOutcome = AttendanceOutcome.IN_PROGRESS,
    val pointsEarned: Int = 0,
    val pointsEligible: Boolean? = null,
    val pointsReason: String? = null,
    val pendingUploads: Int = 0,
    val isOffline: Boolean = false,
    val lastSyncedLabel: String? = null,
    val codeInput: String = "",
    val codeError: String? = null,
    val rateLimited: Boolean = false,
    val isSubmitting: Boolean = false,
    /** A claim waiting for the student to confirm it's them (fingerprint or screen lock). */
    val awaitingIdentity: Boolean = false,
    val lastResult: AttemptResult? = null,
    val identityNotice: String? = null,
)

/**
 * Student check-in (R3, R4). The phone only collects evidence: the signed token or backup code, the stage,
 * a request id for safe retries and this install's device id. Every decision is the server's.
 */
class CheckInViewModel(
    events: EventRepository,
    private val attendance: AttendanceRepository,
    private val deviceId: DeviceIdSource,
    private val network: NetworkMonitor,
    private val clock: AppClock,
    private val saved: SavedStateHandle,
) : ViewModel() {
    private val eventId: String = checkNotNull(saved["eventId"])

    private data class Local(
        val isSubmitting: Boolean = false,
        val pending: AttendanceClaim? = null,
        val lastResult: AttemptResult? = null,
        val codeError: String? = null,
        val rateLimited: Boolean = false,
        val identityNotice: String? = null,
    )

    private val local = MutableStateFlow(Local())
    private val code = saved.getStateFlow(KEY_CODE, "")

    private val remote = combine(events.event(eventId), attendance.myAttendance(), attendance.pendingCount(), attendance.lastSyncedAt(), network.isOnline) { e, r, p, s, o ->
        Remote(e, r.firstOrNull { it.eventId == eventId }, p, s, o)
    }

    val state: StateFlow<CheckInUiState> = combine(remote, local, code) { remote, l, c -> build(remote, l, c) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CheckInUiState())

    private data class Remote(
        val event: Event?,
        val record: com.example.scpd_tracker.model.AttendanceRecord?,
        val pending: Int,
        val lastSynced: Instant?,
        val online: Boolean,
    )

    private fun build(remote: Remote, l: Local, codeText: String): CheckInUiState {
        val event = remote.event
        val arrival = remote.record?.arrival ?: StageStatus.NOT_SCANNED
        val checkpoint = remote.record?.checkpoint ?: StageStatus.NOT_SCANNED
        val open = event?.state == EventState.CheckInOpen
        val stages = buildList {
            add(StageUi(Stage.Arrival, arrival, open, if (open) "Scan the QR on the organiser's screen." else "Check-in isn't open."))
            if (event?.requiresCheckpoint == true) {
                val cpOpen = open && event.checkpointOpen
                add(StageUi(Stage.Checkpoint, checkpoint, cpOpen, if (cpOpen) "The checkpoint is open now. Scan the new QR." else "Not open yet. The organiser announces it near the end."))
            }
        }
        fun done(s: StageStatus) = s == StageStatus.VERIFIED || s == StageStatus.PENDING_SYNC
        val active = when {
            !open -> null
            !done(arrival) && arrival != StageStatus.REJECTED -> Stage.Arrival
            event?.requiresCheckpoint == true && event.checkpointOpen && !done(checkpoint) && checkpoint != StageStatus.REJECTED -> Stage.Checkpoint
            arrival == StageStatus.REJECTED -> Stage.Arrival
            else -> null
        }
        return CheckInUiState(
            event = event, stages = stages, activeStage = active,
            outcome = remote.record?.outcome ?: AttendanceOutcome.IN_PROGRESS, pointsEarned = remote.record?.points ?: 0,
            pointsEligible = remote.record?.pointsEligible,
            pointsReason = remote.record?.pointsReason,
            pendingUploads = remote.pending, isOffline = !remote.online, lastSyncedLabel = remote.lastSynced?.let { syncedLabel(it) },
            codeInput = codeText, codeError = l.codeError, rateLimited = l.rateLimited, isSubmitting = l.isSubmitting,
            awaitingIdentity = l.pending != null, lastResult = l.lastResult, identityNotice = l.identityNotice,
        )
    }

    private fun syncedLabel(at: Instant): String {
        val minutes = Duration.between(at, clock.now()).toMinutes()
        return when {
            minutes < 1 -> "Last synced just now"
            minutes == 1L -> "Last synced 1 min ago"
            minutes < 60 -> "Last synced $minutes min ago"
            else -> "Last synced ${minutes / 60} h ago"
        }
    }

    fun onCodeChange(value: String) {
        saved[KEY_CODE] = value.filter(Char::isDigit).take(6)
        local.update { it.copy(codeError = null) }
    }

    /** A QR was read by the camera (or, in debug builds, taken from the organiser's live screen). */
    fun onScanned(token: String) = prepare(token, viaBackupCode = false)

    fun submitCode() {
        // Read the saved field, not the derived state, so the last keystroke is never lost.
        val text = code.value
        when {
            text.length != 6 -> local.update { it.copy(codeError = "Enter the 6-digit code on the organiser's screen.") }
            !network.isOnline.value -> local.update { it.copy(codeError = "Backup codes need internet. Scan the QR instead, or reconnect.") }
            else -> prepare(text, viaBackupCode = true)
        }
    }

    private fun prepare(token: String, viaBackupCode: Boolean) {
        val stage = state.value.activeStage ?: run {
            local.update { it.copy(lastResult = AttemptResult.Rejected("Nothing to scan right now. Check-in or the checkpoint isn't open.")) }
            return
        }
        // The rate limit only blocks backup codes; scanning the live QR still works.
        if (local.value.isSubmitting || local.value.pending != null || (viaBackupCode && local.value.rateLimited)) return
        viewModelScope.launch {
            val claim = AttendanceClaim(
                requestId = UUID.randomUUID().toString(), eventId = eventId, stage = stage, token = token,
                deviceId = deviceId.get(), capturedAt = clock.now(), viaBackupCode = viaBackupCode,
            )
            local.update { it.copy(pending = claim, lastResult = null) }
        }
    }

    /** The student confirmed it's them. [notice] is set when the phone has no screen lock to confirm with. */
    fun onIdentityConfirmed(notice: String? = null) {
        val claim = local.value.pending ?: return
        local.update { it.copy(pending = null, isSubmitting = true, identityNotice = notice) }
        viewModelScope.launch {
            val result = attendance.submitClaim(claim)
            local.update {
                when (result) {
                    ClaimResult.Verified -> it.copy(isSubmitting = false, lastResult = AttemptResult.Verified(claim.stage))
                    ClaimResult.PendingSync -> it.copy(isSubmitting = false, lastResult = AttemptResult.Saved(claim.stage))
                    is ClaimResult.Rejected -> it.copy(isSubmitting = false, lastResult = AttemptResult.Rejected(result.reason))
                    ClaimResult.RateLimited -> it.copy(
                        isSubmitting = false, rateLimited = true,
                        lastResult = AttemptResult.Rejected("Too many wrong codes. Scan the QR instead, or ask the organiser."),
                    )
                }
            }
            if (result == ClaimResult.Verified || result == ClaimResult.PendingSync) saved[KEY_CODE] = ""
        }
    }

    fun onIdentityCancelled() = local.update { it.copy(pending = null) }

    fun syncNow() {
        viewModelScope.launch { attendance.syncPending() }
    }

    private companion object {
        const val KEY_CODE = "code"
    }
}
