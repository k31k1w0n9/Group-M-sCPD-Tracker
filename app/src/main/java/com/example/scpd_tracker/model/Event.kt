package com.example.scpd_tracker.model

import androidx.annotation.DrawableRes
import java.time.ZonedDateTime

/** How an event poster is drawn. Every poster's text comes from the event itself, never hard-coded. */
sealed interface PosterArt {
    data class Photo(@DrawableRes val image: Int) : PosterArt
    data object Teal : PosterArt
    data object Gold : PosterArt
}

/** Where an event is in the s-CPD office review (R1). Only [Approved] events can be booked. */
enum class ReviewStatus(val label: String) {
    Draft("Draft"),
    PendingReview("Pending review"),
    ChangesRequested("Changes requested"),
    Approved("Approved"),
    Rejected("Rejected"),
}

/** Day-of-event state for an approved event. "Full" is not a state: it is worked out from the counts. */
enum class EventState { BookingOpen, CheckInOpen, CheckInClosed, Cancelled }

/** Arrival only, or arrival plus one checkpoint scan near the end (R3). */
enum class AttendanceMode { ArrivalOnly, ArrivalAndCheckpoint }

data class Event(
    val id: String,
    val clubId: String,
    val clubName: String,
    val title: String,
    val description: String,
    val start: ZonedDateTime,
    val end: ZonedDateTime,
    val venue: String,
    val capacity: Int,
    val bookedCount: Int,
    val tags: List<String>,
    val reviewStatus: ReviewStatus,
    val reviewComment: String? = null,
    val state: EventState = EventState.BookingOpen,
    val attendanceMode: AttendanceMode = AttendanceMode.ArrivalOnly,
    /** The club asked the s-CPD office for points; the office grants them by approving the event. */
    val pointsRequested: Boolean = true,
    val pointsJustification: String? = null,
    val organiserContact: String,
    val poster: PosterArt = PosterArt.Teal,
    val featured: Boolean = false,
    val cancelReason: String? = null,
    /** True while the committee has the checkpoint window open. */
    val checkpointOpen: Boolean = false,
    val submittedAt: ZonedDateTime? = null,
) {
    val placesLeft: Int get() = (capacity - bookedCount).coerceAtLeast(0)
    val isFull: Boolean get() = placesLeft == 0
    val requiresCheckpoint: Boolean get() = attendanceMode == AttendanceMode.ArrivalAndCheckpoint
    val isCancelled: Boolean get() = state == EventState.Cancelled
    /** Approved events are locked: the club can only cancel them (R1, "Can a club change an event after approval? NO"). */
    val isLocked: Boolean get() = reviewStatus == ReviewStatus.Approved || reviewStatus == ReviewStatus.Rejected
    val isEditable: Boolean get() = reviewStatus == ReviewStatus.Draft || reviewStatus == ReviewStatus.ChangesRequested
}
