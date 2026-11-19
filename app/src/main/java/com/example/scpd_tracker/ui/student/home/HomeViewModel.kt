package com.example.scpd_tracker.ui.student.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scpd_tracker.data.AppClock
import com.example.scpd_tracker.data.AttendanceRepository
import com.example.scpd_tracker.data.AuthRepository
import com.example.scpd_tracker.data.BookingRepository
import com.example.scpd_tracker.data.EventRepository
import com.example.scpd_tracker.data.NetworkMonitor
import com.example.scpd_tracker.data.ProgressRepository
import com.example.scpd_tracker.domain.BookingRules
import com.example.scpd_tracker.domain.Policy
import com.example.scpd_tracker.domain.RecommendationRanker
import com.example.scpd_tracker.domain.StreakCalculator
import com.example.scpd_tracker.domain.outcome
import com.example.scpd_tracker.model.AttendanceOutcome
import com.example.scpd_tracker.model.BookingStatus
import com.example.scpd_tracker.model.Event
import com.example.scpd_tracker.model.EventState
import com.example.scpd_tracker.model.ReviewStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** One card in a list: the event, the single most useful reason to show, and whether the student is booked. */
data class EventCardUi(val event: Event, val reason: String, val booked: Boolean)

data class HomeUiState(
    val browsing: Boolean = false,
    val greetingName: String = "",
    val query: String = "",
    val filter: String = FOR_YOU,
    val filters: List<String> = listOf(FOR_YOU, ALL),
    /** A booked event whose check-in is open right now. */
    val happeningNow: Event? = null,
    val points: Int = 0,
    val streakWeeks: Int = 0,
    val featured: EventCardUi? = null,
    val cards: List<EventCardUi> = emptyList(),
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
) {
    val showFeatured: Boolean get() = !browsing && query.isBlank() && filter == FOR_YOU && featured != null
    val sectionTitle: String get() = if (browsing || query.isNotBlank()) "Events" else "Recommended for you"

    companion object {
        const val FOR_YOU = "For you"
        const val ALL = "All interests"
    }
}

/**
 * Home and Explore share this ViewModel: Home ranks events for the student (own feature 1),
 * Explore lists every upcoming approved event. Search and filter live in SavedStateHandle.
 */
class HomeViewModel(
    events: EventRepository,
    bookings: BookingRepository,
    attendance: AttendanceRepository,
    progress: ProgressRepository,
    auth: AuthRepository,
    network: NetworkMonitor,
    private val clock: AppClock,
    private val saved: SavedStateHandle,
    browsing: Boolean,
) : ViewModel() {

    private val query = saved.getStateFlow(KEY_QUERY, "")
    private val filter = saved.getStateFlow(KEY_FILTER, if (browsing) HomeUiState.ALL else HomeUiState.FOR_YOU)

    private val data = combine(events.studentEvents(), bookings.myBookings(), attendance.myAttendance(), progress.ledger(), auth.currentUser) { e, b, a, l, u ->
        Inputs(e, b, a, l, u?.displayName.orEmpty(), u?.interests.orEmpty())
    }

    val state: StateFlow<HomeUiState> = combine(data, query, filter, network.isOnline) { inputs, q, f, online ->
        build(inputs, q, f, online, browsing)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(browsing = browsing))

    fun onQuery(value: String) {
        saved[KEY_QUERY] = value
    }

    fun onFilter(value: String) {
        saved[KEY_FILTER] = value
    }

    fun clearSearch() {
        saved[KEY_QUERY] = ""
        saved[KEY_FILTER] = HomeUiState.ALL
    }

    private data class Inputs(
        val events: List<Event>,
        val bookings: List<com.example.scpd_tracker.model.Booking>,
        val attendance: List<com.example.scpd_tracker.model.AttendanceRecord>,
        val ledger: List<com.example.scpd_tracker.model.PointsEntry>,
        val name: String,
        val interests: List<String>,
    )

    private fun build(inputs: Inputs, q: String, f: String, online: Boolean, browsing: Boolean): HomeUiState {
        val now = clock.now().atZone(Policy.ZONE)
        val bookedIds = inputs.bookings.filter { it.status == BookingStatus.Booked }.map { it.eventId }.toSet()
        val myBooked = inputs.events.filter { it.id in bookedIds && !it.isCancelled }
        val attendedTags = inputs.attendance.filter { it.outcome == AttendanceOutcome.VERIFIED }.flatMap { it.tags }
        val ranked = RecommendationRanker.rank(inputs.events, inputs.interests, attendedTags, myBooked, now)
        val upcoming = inputs.events.filter { it.reviewStatus == ReviewStatus.Approved && !it.isCancelled && it.end.isAfter(now) }

        fun reasonFor(event: Event): String = when {
            event.id in bookedIds -> "You're booked"
            else -> ranked.firstOrNull { it.event.id == event.id }?.reasons?.first()
                ?: myBooked.firstOrNull { BookingRules.overlaps(it, event) }?.let { "Clashes with ${it.title}" }
                ?: "Something new to try"
        }

        val featuredEvent = upcoming.firstOrNull { it.featured && it.start.isAfter(now) }
        val tagFilter = f.takeIf { it != HomeUiState.FOR_YOU && it != HomeUiState.ALL }
        val searching = q.isNotBlank()
        val cards = if (!browsing && !searching && f == HomeUiState.FOR_YOU) {
            ranked.filter { it.event.id != featuredEvent?.id }.map { EventCardUi(it.event, it.reasons.first(), false) }
        } else {
            upcoming.asSequence()
                .filter { tagFilter == null || tagFilter in it.tags }
                .filter { e -> !searching || listOf(e.title, e.clubName, e.venue, e.tags.joinToString(" ")).any { it.contains(q.trim(), ignoreCase = true) } }
                .map { EventCardUi(it, reasonFor(it), it.id in bookedIds) }
                .toList()
        }

        val weeksWithEvents = inputs.events.filter { it.reviewStatus == ReviewStatus.Approved && !it.isCancelled }
            .map { StreakCalculator.weekStart(it.start.toLocalDate()) }.toSet()
        val verifiedDates = inputs.attendance.filter { it.outcome == AttendanceOutcome.VERIFIED }.map { it.eventDate }

        return HomeUiState(
            browsing = browsing,
            greetingName = inputs.name.substringBefore(' '),
            query = q,
            filter = f,
            filters = (listOf(HomeUiState.FOR_YOU) + inputs.interests.take(2) + HomeUiState.ALL).distinct(),
            happeningNow = myBooked.firstOrNull { it.state == EventState.CheckInOpen },
            points = inputs.ledger.sumOf { it.points },
            streakWeeks = StreakCalculator.currentStreak(verifiedDates, weeksWithEvents, now.toLocalDate()),
            featured = featuredEvent?.let { EventCardUi(it, reasonFor(it), it.id in bookedIds) },
            cards = cards,
            isLoading = false,
            isOffline = !online,
        )
    }

    private companion object {
        const val KEY_QUERY = "query"
        const val KEY_FILTER = "filter"
    }
}
