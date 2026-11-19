package com.example.scpd_tracker.attendance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Status of ONE scan (arrival or checkpoint). */
enum class StageStatus { NOT_SCANNED, PENDING_SYNC, VERIFIED, REJECTED }

/** Overall attendance outcome for an event, following the CW1 design. */
enum class AttendanceOutcome(val label: String) {
    IN_PROGRESS("In progress"),
    PENDING_SYNC("Pending sync"),
    VERIFIED("Verified"),
    INCOMPLETE("Incomplete attendance"),
    REJECTED("Rejected"),
    NO_SHOW("No-show")
}

const val POINTS_PER_EVENT = 3

data class AttendanceRecord(
    val eventId: Int,
    val eventTitle: String,
    /** 0 = this week, 1 = last week, ... (Monday-to-Sunday weeks). */
    val weeksAgo: Int,
    val eventEnded: Boolean,
    val requiresCheckpoint: Boolean = true,
    val arrival: StageStatus = StageStatus.NOT_SCANNED,
    val checkpoint: StageStatus = StageStatus.NOT_SCANNED,
) {
    private val requiredStages: List<StageStatus>
        get() = if (requiresCheckpoint) listOf(arrival, checkpoint) else listOf(arrival)

    val outcome: AttendanceOutcome
        get() {
            val stages = requiredStages
            return when {
                stages.any { it == StageStatus.REJECTED } -> AttendanceOutcome.REJECTED
                stages.all { it == StageStatus.VERIFIED } -> AttendanceOutcome.VERIFIED
                stages.any { it == StageStatus.PENDING_SYNC } -> AttendanceOutcome.PENDING_SYNC
                !eventEnded -> AttendanceOutcome.IN_PROGRESS
                stages.any { it == StageStatus.VERIFIED } -> AttendanceOutcome.INCOMPLETE
                else -> AttendanceOutcome.NO_SHOW
            }
        }

    /** 3 points only when ALL required stages are verified. */
    val points: Int get() = if (outcome == AttendanceOutcome.VERIFIED) POINTS_PER_EVENT else 0
}

/** Demo-only in-memory store with fake data. Replace with Room/Firebase later. */
object AttendanceStore {
    const val LIVE_EVENT_ID = 1

    val records = mutableStateListOf(
        // The event happening "now" (used on the Check-in screen)
        AttendanceRecord(1, "Leadership Workshop", weeksAgo = 0, eventEnded = false),
        AttendanceRecord(
            2, "Tech Talk: Android", weeksAgo = 1, eventEnded = true,
            arrival = StageStatus.VERIFIED, checkpoint = StageStatus.VERIFIED
        ),
        AttendanceRecord(
            3, "Volunteer Day", weeksAgo = 2, eventEnded = true,
            arrival = StageStatus.VERIFIED, checkpoint = StageStatus.VERIFIED
        ),
        // Arrived but missed the checkpoint -> Incomplete attendance, 0 points
        AttendanceRecord(
            4, "Career Fair", weeksAgo = 4, eventEnded = true,
            arrival = StageStatus.VERIFIED
        ),
        // Never scanned -> No-show
        AttendanceRecord(5, "Sports Day", weeksAgo = 5, eventEnded = true),
    )

    /** Simulates the phone having no internet. */
    var offline by mutableStateOf(false)

    val totalPoints: Int get() = records.sumOf { it.points }

    /** Consecutive weeks with Verified attendance (this week may still be empty). */
    val streakWeeks: Int
        get() {
            val verifiedWeeks = records
                .filter { it.outcome == AttendanceOutcome.VERIFIED }
                .map { it.weeksAgo }
                .toSet()
            var week = if (0 in verifiedWeeks) 0 else 1
            var streak = 0
            while (week in verifiedWeeks) {
                streak++
                week++
            }
            return streak
        }

    private fun update(eventId: Int, change: (AttendanceRecord) -> AttendanceRecord) {
        val i = records.indexOfFirst { it.eventId == eventId }
        if (i >= 0) records[i] = change(records[i])
    }

    private fun scanResult() =
        if (offline) StageStatus.PENDING_SYNC else StageStatus.VERIFIED

    fun scanArrival(eventId: Int) = update(eventId) { it.copy(arrival = scanResult()) }

    fun scanCheckpoint(eventId: Int) = update(eventId) { it.copy(checkpoint = scanResult()) }

    /** Pretend the server verified everything that was waiting. */
    fun syncPending() {
        for (i in records.indices) {
            val r = records[i]
            records[i] = r.copy(
                arrival = if (r.arrival == StageStatus.PENDING_SYNC) StageStatus.VERIFIED else r.arrival,
                checkpoint = if (r.checkpoint == StageStatus.PENDING_SYNC) StageStatus.VERIFIED else r.checkpoint,
            )
        }
    }

    /** Pretend the server rejected a scan (for demoing the Rejected state). */
    fun rejectArrival(eventId: Int) = update(eventId) { it.copy(arrival = StageStatus.REJECTED) }
}
