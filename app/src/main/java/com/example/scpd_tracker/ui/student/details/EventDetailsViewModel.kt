package com.example.scpd_tracker.ui.student.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scpd_tracker.data.AppClock
import com.example.scpd_tracker.data.BookingRepository
import com.example.scpd_tracker.data.EventRepository
import com.example.scpd_tracker.data.NetworkMonitor
import com.example.scpd_tracker.domain.BookingRules
import com.example.scpd_tracker.domain.Policy
import com.example.scpd_tracker.model.BookResult
import com.example.scpd_tracker.model.Booking
import com.example.scpd_tracker.model.BookingDecision
import com.example.scpd_tracker.model.BookingStatus
import com.example.scpd_tracker.model.Event
import com.example.scpd_tracker.ui.components.MessageQueue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

sealed interface BookingOutcomeUi {
    data object Confirmed : BookingOutcomeUi
    data class Waitlisted(val position: Int) : BookingOutcomeUi
    data class Failed(val message: String) : BookingOutcomeUi
}

data class EventDetailsUiState(
    val loading: Boolean = true,
    val event: Event? = null,
    val decision: BookingDecision? = null,
    val myBooking: Booking? = null,
    val isSubmitting: Boolean = false,
    val showReview: Boolean = false,
    val result: BookingOutcomeUi? = null,
    val isOffline: Boolean = false,
) {
    /** Why the student can't book right now, in one sentence, or null when they can. */
    val blockedReason: String? get() = event?.let { e -> decision?.let { decisionMessage(it, e) } }
}

private val ClashTime = DateTimeFormatter.ofPattern("EEE HH:mm", Locale.UK)

/** One clear sentence per booking decision (R2: what happens when an event is full or clashes). */
fun decisionMessage(decision: BookingDecision, event: Event): String? = when (decision) {
    BookingDecision.Allowed -> null
    BookingDecision.AlreadyBooked -> null
    is BookingDecision.Full -> "All ${event.capacity} places are taken. Join the waitlist and you're booked automatically if a place opens."
    is BookingDecision.Clash -> "Clashes with ${decision.other.title} (${decision.other.start.withZoneSameInstant(Policy.ZONE).format(ClashTime)})"
    BookingDecision.CapReached -> "You already have ${Policy.MAX_ACTIVE_BOOKINGS} active bookings. Cancel one to book this."
    BookingDecision.Paused -> "Booking is paused after ${Policy.NO_SHOW_PAUSE_THRESHOLD} no-shows this semester. Contact the s-CPD office."
    BookingDecision.Closed -> if (event.isCancelled) "This event was cancelled: ${event.cancelReason.orEmpty()}" else "Booking has closed for this event."
}

class EventDetailsViewModel(
    events: EventRepository,
    private val bookings: BookingRepository,
    network: NetworkMonitor,
    private val clock: AppClock,
    private val saved: SavedStateHandle,
) : ViewModel() {
    private val eventId: String = checkNotNull(saved["eventId"])
    private val ui = MutableStateFlow(EventDetailsUiState())
    val messages = MessageQueue()

    val state: StateFlow<EventDetailsUiState> = combine(
        events.event(eventId), bookings.myBookings(), events.studentEvents(), bookings.myNoShowCount(), network.isOnline,
    ) { event, mine, all, noShows, online ->
        val myBooking = mine.firstOrNull { it.eventId == eventId && it.status != BookingStatus.Cancelled }
            ?: mine.firstOrNull { it.eventId == eventId }
        val bookedIds = mine.filter { it.status == BookingStatus.Booked }.map { it.eventId }.toSet()
        val myActive = all.filter { it.id in bookedIds && !it.isCancelled && it.end.isAfter(clock.now().atZone(Policy.ZONE)) }
        val decision = event?.let { BookingRules.canBook(it, myActive, noShows, clock.now().atZone(Policy.ZONE)) }
        Triple(event, myBooking, decision) to online
    }.combine(ui) { (data, online), local ->
        val (event, myBooking, decision) = data
        local.copy(loading = false, event = event, myBooking = myBooking, decision = decision, isOffline = !online)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EventDetailsUiState())

    /** A retry reuses the same request id, so the server never books twice for one tap. */
    private fun requestId(): String = saved.get<String>(KEY_REQUEST) ?: UUID.randomUUID().toString().also { saved[KEY_REQUEST] = it }

    fun openReview() = ui.update { it.copy(showReview = true, result = null) }
    fun dismissReview() = ui.update { it.copy(showReview = false) }
    fun dismissResult() = ui.update { it.copy(result = null) }

    fun confirmBooking() = submit(waitlist = false)
    fun joinWaitlist() = submit(waitlist = true)

    private fun submit(waitlist: Boolean) {
        if (ui.value.isSubmitting) return
        ui.update { it.copy(isSubmitting = true, result = null) }
        viewModelScope.launch {
            val id = requestId()
            val result = if (waitlist) bookings.joinWaitlist(eventId, id) else bookings.book(eventId, id)
            val outcome = when (result) {
                is BookResult.Booked -> BookingOutcomeUi.Confirmed
                is BookResult.Waitlisted -> BookingOutcomeUi.Waitlisted(result.booking.waitlistPosition ?: 1)
                BookResult.Offline -> BookingOutcomeUi.Failed("No connection. Your place isn't booked yet. Try again when you're back online.")
                is BookResult.Rejected -> BookingOutcomeUi.Failed(
                    state.value.event?.let { decisionMessage(result.decision, it) } ?: "This event can't be booked.",
                )
            }
            // A definite answer ends this attempt; the next tap is a new request.
            if (result !is BookResult.Offline) saved.remove<String>(KEY_REQUEST)
            ui.update { it.copy(isSubmitting = false, showReview = false, result = outcome) }
            when (outcome) {
                BookingOutcomeUi.Confirmed -> messages.send("Booking confirmed")
                is BookingOutcomeUi.Waitlisted -> messages.send("You're #${outcome.position} on the waitlist")
                is BookingOutcomeUi.Failed -> Unit
            }
        }
    }

    private companion object {
        const val KEY_REQUEST = "bookingRequestId"
    }
}
