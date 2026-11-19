package com.example.scpd_tracker.model

import java.time.Instant

enum class BookingStatus { Booked, Waitlisted, Cancelled, NoShow }

data class Booking(
    val id: String,
    val eventId: String,
    val userId: String,
    val status: BookingStatus,
    val createdAt: Instant,
    /** 1-based place in the queue while [status] is [BookingStatus.Waitlisted]. */
    val waitlistPosition: Int? = null,
    val cancelledByOrganiser: Boolean = false,
)

/** What a booking attempt is allowed to do, before or after it reaches the server (R2). */
sealed interface BookingDecision {
    data object Allowed : BookingDecision
    data class Full(val canWaitlist: Boolean) : BookingDecision
    data class Clash(val other: Event) : BookingDecision
    data object CapReached : BookingDecision
    /** Not approved, cancelled or already started. */
    data object Closed : BookingDecision
    /** Too many no-shows this semester. */
    data object Paused : BookingDecision
    data object AlreadyBooked : BookingDecision
}

sealed interface BookResult {
    data class Booked(val booking: Booking) : BookResult
    data class Waitlisted(val booking: Booking) : BookResult
    data class Rejected(val decision: BookingDecision) : BookResult
    data object Offline : BookResult
}
