package com.example.scpd_tracker.model

import java.time.Instant
import java.time.LocalDate

enum class Stage(val label: String) { Arrival("Arrival"), Checkpoint("Checkpoint") }

/** Status of ONE scan (arrival or checkpoint). */
enum class StageStatus(val label: String) {
    NOT_SCANNED("Not scanned"),
    PENDING_SYNC("Saved on phone"),
    VERIFIED("Verified"),
    REJECTED("Rejected"),
}

/** Overall attendance outcome for an event, following the Part 1 design. */
enum class AttendanceOutcome(val label: String) {
    IN_PROGRESS("In progress"),
    PENDING_SYNC("Awaiting sync"),
    /** A late or suspicious record the s-CPD office is checking. No points until it is cleared. */
    UNDER_REVIEW("Under review"),
    VERIFIED("Verified"),
    INCOMPLETE("Incomplete attendance"),
    REJECTED("Rejected"),
    NO_SHOW("No-show"),
}

/** One student's attendance at one event. The outcome and points are worked out in `domain/`. */
data class AttendanceRecord(
    val eventId: String,
    val eventTitle: String,
    val eventDate: LocalDate,
    val eventEnded: Boolean,
    val requiresCheckpoint: Boolean,
    val arrival: StageStatus = StageStatus.NOT_SCANNED,
    val checkpoint: StageStatus = StageStatus.NOT_SCANNED,
    val tags: List<String> = emptyList(),
    val rejectReason: String? = null,
    /** Set when the office flagged this record (for example the same phone used by two accounts). */
    val reviewReason: String? = null,
)

/** A scan or backup code a student submits. [capturedAt] is only for diagnosis: the server never trusts it. */
data class AttendanceClaim(
    val requestId: String,
    val eventId: String,
    val stage: Stage,
    val token: String,
    val deviceId: String,
    val capturedAt: Instant,
    val viaBackupCode: Boolean,
)

sealed interface ClaimResult {
    data object Verified : ClaimResult
    /** Saved on the phone; it uploads when the connection comes back. */
    data object PendingSync : ClaimResult
    data class Rejected(val reason: String) : ClaimResult
    /** Too many wrong backup codes for this check-in session. */
    data object RateLimited : ClaimResult
}

/** What the committee screen shows: a signed QR payload, the 6-digit backup code and when they expire. */
data class CheckInCode(val qrPayload: String, val backupCode: String, val stage: Stage, val expiresAt: Instant)

/** One row in the committee's live attendee list. [flagged] says why a claim needs the office to look at it. */
data class AttendeeRow(
    val userId: String,
    val name: String,
    val studentId: String,
    val arrival: StageStatus,
    val checkpoint: StageStatus,
    val flagged: String? = null,
)
