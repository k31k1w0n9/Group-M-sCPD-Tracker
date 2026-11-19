package com.example.scpd_tracker.ui.student.myevents

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scpd_tracker.data.AppClock
import com.example.scpd_tracker.data.AttendanceRepository
import com.example.scpd_tracker.data.BookingRepository
import com.example.scpd_tracker.data.EventRepository
import com.example.scpd_tracker.data.NetworkMonitor
import com.example.scpd_tracker.domain.BookingRules
import com.example.scpd_tracker.domain.Policy
import com.example.scpd_tracker.domain.outcome
import com.example.scpd_tracker.model.AttendanceRecord
import com.example.scpd_tracker.model.Booking
import com.example.scpd_tracker.model.BookingStatus
import com.example.scpd_tracker.model.Event
import com.example.scpd_tracker.model.EventState
import com.example.scpd_tracker.model.StageStatus
import com.example.scpd_tracker.ui.components.MessageQueue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookingCardUi(
    val booking: Booking,
    val event: Event,
    val canCancel: Boolean,
    val checkInOpen: Boolean,
    val arrival: StageStatus = StageStatus.NOT_SCANNED,
    val checkpoint: StageStatus = StageStatus.NOT_SCANNED,
)

data class PastCardUi(val event: Event, val status: String)

enum class MyEventsTab(val label: String) { Upcoming("Upcoming"), Waitlist("Waitlist"), Past("Past") }

enum class MyEventsPage { List, Details, Cancelled }

data class MyEventsUiState(
    val tab: MyEventsTab = MyEventsTab.Upcoming,
    val upcoming: List<BookingCardUi> = emptyList(),
    val waitlisted: List<BookingCardUi> = emptyList(),
    val past: List<PastCardUi> = emptyList(),
    val page: MyEventsPage = MyEventsPage.List,
    val selected: BookingCardUi? = null,
    /** The event a just-cancelled booking was for, shown on the Cancelled page. */
    val cancelledEvent: Event? = null,
    val confirmCancel: Boolean = false,
    val cancelling: Boolean = false,
    val error: String? = null,
    val isOffline: Boolean = false,
)

class MyEventsViewModel(
    events: EventRepository,
    private val bookings: BookingRepository,
    attendance: AttendanceRepository,
    network: NetworkMonitor,
    private val clock: AppClock,
    private val saved: SavedStateHandle,
) : ViewModel() {
    private data class Local(
        val confirmCancel: Boolean = false,
        val cancelling: Boolean = false,
        val error: String? = null,
        val cancelledEvent: Event? = null,
    )

    private val tab = saved.getStateFlow(KEY_TAB, MyEventsTab.Upcoming.name)
    private val selectedId = saved.getStateFlow<String?>(KEY_SELECTED, null)
    private val page = saved.getStateFlow(KEY_PAGE, MyEventsPage.List.name)
    private val local = MutableStateFlow(Local())
    val messages = MessageQueue()

    private val lists = combine(events.studentEvents(), bookings.myBookings(), attendance.myAttendance(), network.isOnline) { all, mine, records, online ->
        Lists(build(all, mine, records), !online)
    }

    val state: StateFlow<MyEventsUiState> = combine(lists, tab, selectedId, page, local) { lists, t, sel, p, l ->
        val (cards, offline) = lists
        val selected = (cards.upcoming + cards.waitlisted).firstOrNull { it.booking.id == sel }
        MyEventsUiState(
            tab = MyEventsTab.valueOf(t), upcoming = cards.upcoming, waitlisted = cards.waitlisted, past = cards.past,
            page = if (p == MyEventsPage.Details.name && selected == null) MyEventsPage.List else MyEventsPage.valueOf(p),
            selected = selected, cancelledEvent = l.cancelledEvent, confirmCancel = l.confirmCancel,
            cancelling = l.cancelling, error = l.error, isOffline = offline,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MyEventsUiState())

    private data class Cards(val upcoming: List<BookingCardUi>, val waitlisted: List<BookingCardUi>, val past: List<PastCardUi>)
    private data class Lists(val cards: Cards, val offline: Boolean)

    private fun build(all: List<Event>, mine: List<Booking>, records: List<AttendanceRecord>): Cards {
        val now = clock.now().atZone(Policy.ZONE)
        val byId = all.associateBy { it.id }
        val upcoming = mutableListOf<BookingCardUi>()
        val waitlisted = mutableListOf<BookingCardUi>()
        val past = mutableListOf<PastCardUi>()
        mine.sortedBy { byId[it.eventId]?.start }.forEach { booking ->
            val event = byId[booking.eventId] ?: return@forEach
            val record = records.firstOrNull { it.eventId == event.id }
            val ended = !event.end.isAfter(now)
            when {
                booking.status == BookingStatus.Cancelled ->
                    past += PastCardUi(event, if (booking.cancelledByOrganiser) "Event cancelled" else "Cancelled")
                booking.status == BookingStatus.NoShow -> past += PastCardUi(event, "No-show")
                booking.status == BookingStatus.Waitlisted -> waitlisted += BookingCardUi(booking, event, canCancel = true, checkInOpen = false)
                ended -> past += PastCardUi(event, record?.outcome?.label ?: "Ended")
                else -> upcoming += BookingCardUi(
                    booking, event, canCancel = BookingRules.canCancel(event, now), checkInOpen = event.state == EventState.CheckInOpen,
                    arrival = record?.arrival ?: StageStatus.NOT_SCANNED, checkpoint = record?.checkpoint ?: StageStatus.NOT_SCANNED,
                )
            }
        }
        return Cards(upcoming, waitlisted, past.reversed())
    }

    fun selectTab(tab: MyEventsTab) {
        saved[KEY_TAB] = tab.name
    }

    fun openBooking(bookingId: String) {
        saved[KEY_SELECTED] = bookingId
        saved[KEY_PAGE] = MyEventsPage.Details.name
        local.update { it.copy(error = null) }
    }

    fun backToList() {
        saved[KEY_PAGE] = MyEventsPage.List.name
        saved[KEY_SELECTED] = null
        local.update { Local() }
    }

    fun askCancel() = local.update { it.copy(confirmCancel = true, error = null) }
    fun dismissCancel() = local.update { it.copy(confirmCancel = false) }

    fun confirmCancel() {
        val card = state.value.selected ?: return
        if (local.value.cancelling) return
        local.update { it.copy(confirmCancel = false, cancelling = true, error = null) }
        viewModelScope.launch {
            val result = bookings.cancel(card.booking.id)
            if (result.isSuccess) {
                saved[KEY_PAGE] = MyEventsPage.Cancelled.name
                saved[KEY_SELECTED] = null
                local.update { Local(cancelledEvent = card.event) }
                messages.send(if (card.booking.status == BookingStatus.Waitlisted) "You left the waitlist" else "Booking cancelled")
            } else {
                // The booking is still active; say so and keep the place.
                local.update { it.copy(cancelling = false, error = result.exceptionOrNull()?.message ?: "Couldn't cancel. Your place is still booked.") }
            }
        }
    }

    private companion object {
        const val KEY_TAB = "tab"
        const val KEY_SELECTED = "selected"
        const val KEY_PAGE = "page"
    }
}
