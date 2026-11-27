package com.example.scpd_tracker.data.firebase

import com.example.scpd_tracker.data.BookingRepository
import com.example.scpd_tracker.data.fake.AlertKind
import com.example.scpd_tracker.data.fake.BackendAlert
import com.example.scpd_tracker.data.system.NotificationFeed
import com.example.scpd_tracker.model.Booking
import com.example.scpd_tracker.model.BookingStatus
import com.example.scpd_tracker.model.Event
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Notifications without a server. Sending push messages needs Cloud Functions (paid plan), so the app raises
 * local notifications itself from what it sees change in Firestore: a booked event cancelled, the checkpoint
 * opening, or a waitlist place coming free. The alert IDs are fixed, so the same news is only shown once.
 */
class FirebaseNotificationFeed(store: FirebaseStore, bookingRepo: BookingRepository) : NotificationFeed {
    override val events: Flow<Map<String, Event>> = store.approvedEvents
    override val bookings: Flow<List<Booking>> = bookingRepo.myBookings()

    /** Waitlist entries seen in this run of the app, to spot the move from Waitlisted to Booked. */
    private val waitlisted = mutableSetOf<String>()

    override val alerts: Flow<List<BackendAlert>> =
        combine(events, bookings, store.myAttendanceDocs) { events, bookings, records ->
            val arrived = records.filter { it.get("arrival") != null && it.get("checkpoint") == null }
                .mapNotNull { it.getString("eventId") }.toSet()
            bookings.mapNotNull { booking ->
                val event = events[booking.eventId] ?: return@mapNotNull null
                fun alert(kind: AlertKind, detail: String) = BackendAlert(
                    "${kind.name}-${booking.id}", booking.userId, event.id, event.title,
                    event.start.toInstant().toEpochMilli(), event.venue, kind, detail,
                )
                when {
                    booking.cancelledByOrganiser -> alert(AlertKind.Cancelled, event.cancelReason ?: "The organiser cancelled this event.")
                    booking.status == BookingStatus.Booked && event.checkpointOpen && event.id in arrived ->
                        alert(AlertKind.Checkpoint, "Scan the checkpoint QR now.")
                    booking.status == BookingStatus.Booked && waitlisted.remove(booking.id) ->
                        alert(AlertKind.Waitlist, "You now have a place at this event.")
                    else -> {
                        if (booking.status == BookingStatus.Waitlisted) waitlisted.add(booking.id)
                        null
                    }
                }
            }
        }
}
