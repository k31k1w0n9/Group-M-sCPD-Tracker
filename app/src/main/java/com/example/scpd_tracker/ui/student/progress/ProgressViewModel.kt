package com.example.scpd_tracker.ui.student.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scpd_tracker.data.AppClock
import com.example.scpd_tracker.data.AttendanceRepository
import com.example.scpd_tracker.data.AuthRepository
import com.example.scpd_tracker.data.EventRepository
import com.example.scpd_tracker.data.ProgressRepository
import com.example.scpd_tracker.domain.AttendanceStats
import com.example.scpd_tracker.domain.Policy
import com.example.scpd_tracker.domain.StreakCalculator
import com.example.scpd_tracker.domain.outcome
import com.example.scpd_tracker.model.AttendanceOutcome
import com.example.scpd_tracker.model.AttendanceRecord
import com.example.scpd_tracker.model.ReviewStatus
import com.example.scpd_tracker.ui.components.label
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class LedgerRowUi(val title: String, val date: String, val points: Int, val reason: String)

data class ProgressUiState(
    val total: Int = 0,
    val milestone: Int = AttendanceStats.nextMilestone(0),
    val latestCredit: String? = null,
    val pendingCount: Int = 0,
    val interests: List<String> = emptyList(),
    val stamps: Map<String, Int> = emptyMap(),
    val streakWeeks: Int = 0,
    /** Oldest first, this week last: did the week have Verified attendance? */
    val recentWeeks: List<Boolean> = List(STREAK_WEEKS_SHOWN) { false },
    val entries: List<LedgerRowUi> = emptyList(),
    val records: List<AttendanceRecord> = emptyList(),
) {
    val pendingNote: String?
        get() = when (pendingCount) {
            0 -> null
            1 -> "1 check-in waiting to sync. Points appear once it's verified."
            else -> "$pendingCount check-ins waiting to sync. Points appear once they're verified."
        }

    companion object {
        const val STREAK_WEEKS_SHOWN = 5
    }
}

/** Points come from the server's ledger, so a reversal by the office shows here as its own -3 line (R5). */
class ProgressViewModel(
    attendance: AttendanceRepository,
    progress: ProgressRepository,
    events: EventRepository,
    auth: AuthRepository,
    private val clock: AppClock,
) : ViewModel() {

    val state: StateFlow<ProgressUiState> = combine(
        attendance.myAttendance(), progress.ledger(), events.studentEvents(), attendance.pendingCount(), auth.currentUser,
    ) { records, ledger, allEvents, pending, user ->
        val today = clock.now().atZone(Policy.ZONE).toLocalDate()
        val thisWeek = StreakCalculator.weekStart(today)
        val verified = records.filter { it.outcome == AttendanceOutcome.VERIFIED }
        val verifiedWeeks = verified.map { StreakCalculator.weekStart(it.eventDate) }.toSet()
        val weeksWithEvents = allEvents.filter { it.reviewStatus == ReviewStatus.Approved && !it.isCancelled }
            .map { StreakCalculator.weekStart(it.start.toLocalDate()) }.toSet()
        val total = ledger.sumOf { it.points }
        val latest = ledger.firstOrNull { it.points > 0 }
        ProgressUiState(
            total = total,
            milestone = AttendanceStats.nextMilestone(total),
            latestCredit = latest?.let { "+${it.points} points from ${it.eventTitle}." },
            pendingCount = pending,
            interests = user?.interests.orEmpty(),
            stamps = AttendanceStats.stampCounts(records),
            streakWeeks = StreakCalculator.currentStreak(verified.map { it.eventDate }, weeksWithEvents, today),
            recentWeeks = (ProgressUiState.STREAK_WEEKS_SHOWN - 1 downTo 0).map { thisWeek.minusWeeks(it.toLong()) in verifiedWeeks },
            entries = ledger.map { LedgerRowUi(it.eventTitle, it.date.label(), it.points, it.reason) },
            records = records,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())
}
