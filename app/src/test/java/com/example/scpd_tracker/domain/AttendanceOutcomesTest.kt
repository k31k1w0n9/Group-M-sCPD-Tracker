package com.example.scpd_tracker.domain

import com.example.scpd_tracker.model.AttendanceOutcome
import com.example.scpd_tracker.model.AttendanceOutcome.INCOMPLETE
import com.example.scpd_tracker.model.AttendanceOutcome.IN_PROGRESS
import com.example.scpd_tracker.model.AttendanceOutcome.NO_SHOW
import com.example.scpd_tracker.model.AttendanceOutcome.PENDING_SYNC
import com.example.scpd_tracker.model.AttendanceOutcome.REJECTED
import com.example.scpd_tracker.model.AttendanceOutcome.VERIFIED
import com.example.scpd_tracker.model.AttendanceRecord
import com.example.scpd_tracker.model.StageStatus
import com.example.scpd_tracker.model.StageStatus.NOT_SCANNED
import com.example.scpd_tracker.model.StageStatus.PENDING_SYNC as P
import com.example.scpd_tracker.model.StageStatus.REJECTED as R
import com.example.scpd_tracker.model.StageStatus.VERIFIED as V
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class AttendanceOutcomesTest {
    private val day = LocalDate.parse("2026-11-16")

    private fun record(
        arrival: StageStatus, checkpoint: StageStatus, ended: Boolean,
        requiresCheckpoint: Boolean = true, tags: List<String> = emptyList(), date: LocalDate = day,
    ) = AttendanceRecord("e", "Talk", date, ended, requiresCheckpoint, arrival, checkpoint, tags)

    /** Every arrival x checkpoint x ended combination against the Part 1 outcome table. */
    @Test
    fun everyCombinationMatchesThePart1Table() {
        val expected = mapOf(
            Triple(NOT_SCANNED, NOT_SCANNED, false) to IN_PROGRESS,
            Triple(NOT_SCANNED, NOT_SCANNED, true) to NO_SHOW,
            Triple(V, NOT_SCANNED, false) to IN_PROGRESS,
            Triple(V, NOT_SCANNED, true) to INCOMPLETE,
            Triple(V, V, false) to VERIFIED,
            Triple(V, V, true) to VERIFIED,
            Triple(V, P, false) to PENDING_SYNC,
            Triple(P, P, true) to PENDING_SYNC,
            Triple(P, NOT_SCANNED, true) to PENDING_SYNC,
            Triple(R, V, true) to REJECTED,
            Triple(V, R, false) to REJECTED,
            Triple(P, R, true) to REJECTED,
            Triple(NOT_SCANNED, V, true) to INCOMPLETE,
        )
        expected.forEach { (input, outcome) ->
            val (arrival, checkpoint, ended) = input
            assertEquals("$input", outcome, record(arrival, checkpoint, ended).outcome)
        }
    }

    @Test
    fun arrivalOnlyEvent_verifiedWithoutCheckpoint() {
        val r = record(V, NOT_SCANNED, ended = true, requiresCheckpoint = false)
        assertEquals(VERIFIED, r.outcome)
        assertEquals(3, r.points)
    }

    @Test
    fun pointsOnlyForVerified() {
        AttendanceOutcome.entries.forEach { _ -> }
        assertEquals(3, record(V, V, true).points)
        assertEquals(0, record(V, NOT_SCANNED, true).points)
        assertEquals(0, record(V, P, true).points)
        assertEquals(0, record(R, V, true).points)
    }

    @Test
    fun flaggedRecord_isUnderReview_withNoPoints() {
        val r = record(V, V, ended = true).copy(reviewReason = "Same phone used by another account")
        assertEquals(AttendanceOutcome.UNDER_REVIEW, r.outcome)
        assertEquals(0, r.points)
    }

    @Test
    fun rejectedBeatsUnderReview() {
        val r = record(R, V, ended = true).copy(reviewReason = "Checked by the office")
        assertEquals(REJECTED, r.outcome)
    }

    @Test
    fun pendingSyncLabelSaysAwaitingSync() {
        assertEquals("Awaiting sync", PENDING_SYNC.label)
    }

    @Test
    fun milestoneIsTheNextTargetAbovePoints() {
        assertEquals(15, AttendanceStats.nextMilestone(0))
        assertEquals(30, AttendanceStats.nextMilestone(15))
        assertEquals(60, AttendanceStats.nextMilestone(42))
        assertEquals(120, AttendanceStats.nextMilestone(95))
    }

    @Test
    fun stampsCountOnlyVerifiedAttendancePerTag() {
        val stamps = AttendanceStats.stampCounts(
            listOf(
                record(V, V, true, tags = listOf("Tech", "Career")),
                record(V, V, true, tags = listOf("Tech")),
                record(NOT_SCANNED, NOT_SCANNED, true, tags = listOf("Tech")),
            ),
        )
        assertEquals(2, stamps["Tech"])
        assertEquals(1, stamps["Career"])
        assertEquals(0, stamps["Arts"])
    }

    @Test
    fun pendingScansAwaitCredit() {
        assertEquals(1, AttendanceStats.awaitingCredit(listOf(record(V, P, false), record(V, V, true))))
    }
}
