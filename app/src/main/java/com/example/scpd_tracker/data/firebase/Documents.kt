package com.example.scpd_tracker.data.firebase

import com.example.scpd_tracker.R
import com.example.scpd_tracker.domain.Policy
import com.example.scpd_tracker.model.AttendanceMode
import com.example.scpd_tracker.model.Event
import com.example.scpd_tracker.model.EventState
import com.example.scpd_tracker.model.PosterArt
import com.example.scpd_tracker.model.ReviewStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.DocumentReference
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.time.Instant
import java.time.ZonedDateTime
import java.util.Date

/*
 * How app models map to Firestore documents. Field names match firebase/firestore.rules and firebase/scripts/seed.mjs.
 */

object Collections {
    const val USERS = "users"
    const val CLUBS = "clubs"
    const val EVENTS = "events"
    const val SECRETS = "eventSecrets"
    const val BOOKINGS = "bookings"
    const val ATTENDANCE = "attendance"
    const val ATTEMPTS = "codeAttempts"
    const val DEVICE_USE = "deviceUse"
    const val LEDGER = "ledger"
}

/** Booking, attendance and attempt documents have one fixed ID per student per event. */
fun pairId(eventId: String, uid: String) = "${eventId}_$uid"

fun Timestamp.toZoned(): ZonedDateTime = toDate().toInstant().atZone(Policy.ZONE)
fun ZonedDateTime.toTimestamp(): Timestamp = Timestamp(Date.from(toInstant()))
fun Timestamp.toInstantValue(): Instant = toDate().toInstant()

/** Posters are built-in art styles: Cloud Storage needs the paid plan, so there are no uploads. */
fun posterFromKey(key: String?): PosterArt = when (key) {
    "Gold" -> PosterArt.Gold
    "PhotoFeatured" -> PosterArt.Photo(R.drawable.poster_featured_photo)
    "PhotoAi" -> PosterArt.Photo(R.drawable.poster_ai_photo)
    else -> PosterArt.Teal
}

fun posterKey(poster: PosterArt): String = when (poster) {
    PosterArt.Gold -> "Gold"
    PosterArt.Teal -> "Teal"
    is PosterArt.Photo -> if (poster.image == R.drawable.poster_ai_photo) "PhotoAi" else "PhotoFeatured"
}

private inline fun <reified T : Enum<T>> enumOr(name: String?, fallback: T): T =
    name?.let { n -> enumValues<T>().firstOrNull { it.name == n } } ?: fallback

@Suppress("UNCHECKED_CAST")
fun DocumentSnapshot.toEvent(): Event? {
    if (!exists()) return null
    val start = getTimestamp("start")?.toZoned() ?: return null
    return Event(
        id = id,
        clubId = getString("clubId").orEmpty(),
        clubName = getString("clubName").orEmpty(),
        title = getString("title").orEmpty(),
        description = getString("description").orEmpty(),
        start = start,
        end = getTimestamp("end")?.toZoned() ?: start.plusHours(2),
        venue = getString("venue").orEmpty(),
        capacity = getLong("capacity")?.toInt() ?: 0,
        bookedCount = getLong("bookedCount")?.toInt() ?: 0,
        tags = (get("tags") as? List<String>).orEmpty(),
        reviewStatus = enumOr(getString("reviewStatus"), ReviewStatus.Draft),
        reviewComment = getString("reviewComment"),
        state = enumOr(getString("state"), EventState.BookingOpen),
        attendanceMode = enumOr(getString("attendanceMode"), AttendanceMode.ArrivalOnly),
        pointsRequested = getBoolean("pointsRequested") ?: true,
        pointsJustification = getString("pointsJustification"),
        organiserContact = getString("organiserContact") ?: "s-cpd@nottingham.edu.my",
        poster = posterFromKey(getString("poster")),
        featured = getBoolean("featured") ?: false,
        cancelReason = getString("cancelReason"),
        checkpointOpen = getBoolean("checkpointOpen") ?: false,
        submittedAt = getTimestamp("submittedAt")?.toZoned(),
    )
}

/** Session times the app needs for check-in and the offline upload window. */
data class SessionTimes(val openedAt: Instant?, val closedAt: Instant?, val checkpointOpenedAt: Instant?)

fun DocumentSnapshot.sessionTimes() = SessionTimes(
    getTimestamp("checkInOpenedAt")?.toInstantValue(),
    getTimestamp("checkInClosedAt")?.toInstantValue(),
    getTimestamp("checkpointOpenedAt")?.toInstantValue(),
)

@Suppress("UNCHECKED_CAST")
fun DocumentSnapshot.waitlist(): List<String> = (get("waitlist") as? List<String>).orEmpty()

/** A live query as a Flow. A permission error (for example after signing out) ends in an empty result, not a crash. */
fun Query.snapshots(): Flow<QuerySnapshot?> = callbackFlow {
    val registration: ListenerRegistration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) trySend(null)
            return@addSnapshotListener
        }
        trySend(snapshot)
    }
    awaitClose { registration.remove() }
}

fun DocumentReference.snapshots(): Flow<DocumentSnapshot?> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) trySend(null)
            return@addSnapshotListener
        }
        trySend(snapshot)
    }
    awaitClose { registration.remove() }
}

/** True when the rules refused the write. Anything else (timeouts, no network) is worth retrying. */
fun Throwable.isPermissionDenied(): Boolean =
    (this as? FirebaseFirestoreException)?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED

fun Throwable.isNetworkProblem(): Boolean = this is com.google.firebase.FirebaseNetworkException ||
    (this as? FirebaseFirestoreException)?.code in setOf(
        FirebaseFirestoreException.Code.UNAVAILABLE, FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
    ) || this is kotlinx.coroutines.TimeoutCancellationException
