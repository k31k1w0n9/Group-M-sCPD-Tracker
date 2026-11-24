package com.example.scpd_tracker.domain

import com.example.scpd_tracker.model.AttendanceOutcome
import com.example.scpd_tracker.model.AttendanceRecord
import com.example.scpd_tracker.model.Event
import com.example.scpd_tracker.model.EventState
import com.example.scpd_tracker.model.Stage
import com.example.scpd_tracker.model.StageStatus

private val AttendanceRecord.requiredStages: List<StageStatus>
    get() = if (requiresCheckpoint) listOf(arrival, checkpoint) else listOf(arrival)

/** Overall outcome from the stage results, following the Part 1 status table. */
val AttendanceRecord.outcome: AttendanceOutcome
    get() {
        val stages = requiredStages
        return when {
            reviewReason != null -> AttendanceOutcome.UNDER_REVIEW
            stages.any { it == StageStatus.REJECTED } -> AttendanceOutcome.REJECTED
            stages.all { it == StageStatus.VERIFIED } -> AttendanceOutcome.VERIFIED
            stages.any { it == StageStatus.PENDING_SYNC } -> AttendanceOutcome.PENDING_SYNC
            !eventEnded -> AttendanceOutcome.IN_PROGRESS
            stages.any { it == StageStatus.VERIFIED } -> AttendanceOutcome.INCOMPLETE
            else -> AttendanceOutcome.NO_SHOW
        }
    }

/** 3 points only when ALL required stages are verified. */
val AttendanceRecord.points: Int get() = if (pointsEligible && outcome == AttendanceOutcome.VERIFIED) Policy.POINTS_PER_EVENT else 0

/** The scanner and its entry points use the same stage even when an arrival is still queued. */
internal fun Event?.activeCheckInStage(record: AttendanceRecord?): Stage? {
    if (this == null || state != EventState.CheckInOpen) return null
    val arrival = record?.arrival ?: StageStatus.NOT_SCANNED
    val checkpoint = record?.checkpoint ?: StageStatus.NOT_SCANNED
    return when {
        arrival == StageStatus.NOT_SCANNED -> Stage.Arrival
        requiresCheckpoint && checkpointOpen && checkpoint == StageStatus.NOT_SCANNED -> Stage.Checkpoint
        arrival == StageStatus.REJECTED -> Stage.Arrival
        else -> null
    }
}

/** Progress-screen numbers: milestones, interest stamps and scans still waiting to sync. */
object AttendanceStats {
    /** Points needed for each milestone on the Progress ring. After the last one, every 30 points is a milestone. */
    val MILESTONES = listOf(15, 30, 60, 90)

    fun nextMilestone(points: Int): Int = MILESTONES.firstOrNull { it > points } ?: ((points / 30 + 1) * 30)

    /** One stamp per tag for each Verified attendance. */
    fun stampCounts(records: List<AttendanceRecord>): Map<String, Int> =
        Policy.INTEREST_CATEGORIES.associateWith { category ->
            records.count { it.outcome == AttendanceOutcome.VERIFIED && category in it.tags }
        }

    fun awaitingCredit(records: List<AttendanceRecord>): Int = records.count { it.outcome == AttendanceOutcome.PENDING_SYNC }
}
