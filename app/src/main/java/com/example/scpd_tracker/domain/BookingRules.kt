package com.example.scpd_tracker.domain

import com.example.scpd_tracker.model.BookingDecision
import com.example.scpd_tracker.model.Event
import com.example.scpd_tracker.model.ReviewStatus
import java.time.ZonedDateTime

/**
 * Booking rules (R2). The phone runs them to explain a decision before the student taps Book;
 * the server runs the same rules again, so the phone is never trusted to enforce them.
 */
object BookingRules {

    /** Two events clash when their time ranges overlap. Back-to-back events (end == start) do not. */
    fun overlaps(a: Event, b: Event): Boolean = a.start.isBefore(b.end) && b.start.isBefore(a.end)

    /**
     * @param myActive events the student currently holds a Booked place for.
     * @param noShowsThisSemester no-shows recorded against the student this semester.
     */
    fun canBook(event: Event, myActive: List<Event>, noShowsThisSemester: Int, now: ZonedDateTime): BookingDecision = when {
        event.reviewStatus != ReviewStatus.Approved || event.isCancelled || !now.isBefore(event.start) ->
            BookingDecision.Closed
        myActive.any { it.id == event.id } -> BookingDecision.AlreadyBooked
        noShowsThisSemester >= Policy.NO_SHOW_PAUSE_THRESHOLD -> BookingDecision.Paused
        myActive.size >= Policy.MAX_ACTIVE_BOOKINGS -> BookingDecision.CapReached
        else -> myActive.firstOrNull { overlaps(it, event) }?.let { BookingDecision.Clash(it) }
            ?: if (event.isFull) BookingDecision.Full(canWaitlist = true) else BookingDecision.Allowed
    }

    /** A booking can be cancelled until [Policy.CANCEL_CUTOFF] before the start, so the place can be reused. */
    fun canCancel(event: Event, now: ZonedDateTime): Boolean = now.isBefore(event.start.minus(Policy.CANCEL_CUTOFF))
}
