package com.example.scpd_tracker.data.system

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.scpd_tracker.MainActivity
import com.example.scpd_tracker.R
import com.example.scpd_tracker.ScpdApplication
import com.example.scpd_tracker.data.AppClock
import com.example.scpd_tracker.data.AuthRepository
import com.example.scpd_tracker.data.fake.FakeBackend
import com.example.scpd_tracker.model.AccountRole
import com.example.scpd_tracker.model.Booking
import com.example.scpd_tracker.model.BookingStatus
import com.example.scpd_tracker.model.Event
import com.example.scpd_tracker.model.StageStatus
import com.example.scpd_tracker.ui.components.whenLabel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object NotificationNavigation {
    const val EXTRA_EVENT_ID = "notificationEventId"
    val targetEventId = MutableStateFlow<String?>(null)
}

internal object EventNotifications {
    private const val CHANNEL = "scpd_event_alerts"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL, "Event updates", NotificationManager.IMPORTANCE_DEFAULT)
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun send(context: Context, event: Event, title: String, detail: String, kind: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        createChannel(context)
        val key = "${event.id}:$kind"
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "com.example.scpd_tracker.OPEN_BOOKING"
            putExtra(NotificationNavigation.EXTRA_EVENT_ID, event.id)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = PendingIntent.getActivity(
            context, key.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val info = "${event.whenLabel} · ${event.venue}"
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("$title · ${event.title}")
            .setContentText(info)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$info\n$detail"))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        NotificationManagerCompat.from(context).notify(key.hashCode(), notification)
    }
}

class EventReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as ScpdApplication
        val uid = inputData.getString(KEY_USER) ?: return Result.success()
        val eventId = inputData.getString(KEY_EVENT) ?: return Result.success()
        val expectedStart = inputData.getLong(KEY_START, 0L)
        if (app.container.auth.currentUser.value?.uid != uid) return Result.success()
        val settings = app.container.notificationSettings.settings.first()
        if (!settings.eventReminders) return Result.success()
        val now = ZonedDateTime.now(ZoneId.of("Asia/Kuala_Lumpur"))
        if (settings.quietHours && (now.hour >= 22 || now.hour < 8)) return Result.success()
        val event = app.container.backend.events.value[eventId] ?: return Result.success()
        if (event.start.toInstant().toEpochMilli() != expectedStart || event.isCancelled) return Result.success()
        val booked = app.container.backend.bookings.value.any {
            it.userId == uid && it.eventId == eventId && it.status == BookingStatus.Booked
        }
        if (!booked) return Result.success()
        EventNotifications.send(applicationContext, event, "Event reminder", "Your booked event is coming up.", "reminder-${inputData.getInt(KEY_HOURS, 1)}")
        return Result.success()
    }

    companion object {
        const val KEY_USER = "user"
        const val KEY_EVENT = "event"
        const val KEY_START = "start"
        const val KEY_HOURS = "hours"
    }
}

internal class EventNotificationMonitor(
    private val context: Context,
    private val backend: FakeBackend,
    auth: AuthRepository,
    settings: NotificationSettingsStore,
    private val clock: AppClock,
    scope: CoroutineScope,
) {
    private data class Snapshot(
        val userId: String?,
        val events: Map<String, Event>,
        val bookings: List<Booking>,
        val evidence: Map<String, FakeBackend.Evidence>,
        val settings: NotificationPreferences,
    )

    private val work = WorkManager.getInstance(context)
    private val scheduled = mutableMapOf<String, Long>()
    private var previous: Snapshot? = null

    init {
        EventNotifications.createChannel(context)
        scope.launch {
            combine(auth.currentUser, backend.events, backend.bookings, backend.evidence, settings.settings) {
                user, events, bookings, evidence, preferences ->
                Snapshot(
                    userId = user?.takeIf { it.role == AccountRole.Student }?.uid,
                    events = events,
                    bookings = bookings,
                    evidence = evidence,
                    settings = preferences,
                )
            }.collect { update(it) }
        }
    }

    private fun update(current: Snapshot) {
        schedule(current)
        val before = previous
        previous = current
        if (current.userId == null || before?.userId != current.userId) return
        val uid = current.userId
        val oldBookings = before.bookings.filter { it.userId == uid }.associateBy { it.id }
        val newBookings = current.bookings.filter { it.userId == uid }
        for (booking in newBookings) {
            val event = current.events[booking.eventId] ?: continue
            val oldBooking = oldBookings[booking.id]
            if (oldBooking?.status == BookingStatus.Waitlisted && booking.status == BookingStatus.Booked) {
                EventNotifications.send(context, event, "Waitlist place confirmed", "You now have a place at this event.", "waitlist")
            }
            if (oldBooking?.status == BookingStatus.Booked && !before.events[booking.eventId].isCancelledOrMissing() && event.isCancelled) {
                EventNotifications.send(context, event, "Event cancelled", event.cancelReason.orEmpty(), "cancelled")
            }
        }
        for ((id, event) in current.events) {
            if (event.checkpointOpen && before.events[id]?.checkpointOpen == false &&
                newBookings.any { it.eventId == id && it.status == BookingStatus.Booked } &&
                current.evidence["$uid|$id"]?.arrival == StageStatus.VERIFIED
            ) {
                EventNotifications.send(context, event, "Checkpoint open", "Scan the checkpoint QR now.", "checkpoint")
            }
        }
    }

    private fun Event?.isCancelledOrMissing(): Boolean = this?.isCancelled != false

    private fun schedule(current: Snapshot) {
        val now = clock.now().toEpochMilli()
        val desired = mutableMapOf<String, Pair<Event, Int>>()
        val uid = current.userId
        if (uid != null && current.settings.eventReminders) {
            for (booking in current.bookings.filter { it.userId == uid && it.status == BookingStatus.Booked }) {
                val event = current.events[booking.eventId] ?: continue
                if (event.isCancelled) continue
                for (hours in listOf(24, 1)) {
                    val at = event.start.toInstant().toEpochMilli() - TimeUnit.HOURS.toMillis(hours.toLong())
                    if (at > now) desired["reminder:$uid:${booking.id}:$hours"] = event to hours
                }
            }
        }
        for (key in scheduled.keys - desired.keys) {
            work.cancelUniqueWork(key)
            scheduled.remove(key)
        }
        for ((key, pair) in desired) {
            val (event, hours) = pair
            val at = event.start.toInstant().toEpochMilli() - TimeUnit.HOURS.toMillis(hours.toLong())
            if (scheduled[key] == at) continue
            val request = OneTimeWorkRequestBuilder<EventReminderWorker>()
                .setInitialDelay(at - now, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(
                    EventReminderWorker.KEY_USER to uid,
                    EventReminderWorker.KEY_EVENT to event.id,
                    EventReminderWorker.KEY_START to event.start.toInstant().toEpochMilli(),
                    EventReminderWorker.KEY_HOURS to hours,
                ))
                .build()
            work.enqueueUniqueWork(key, ExistingWorkPolicy.REPLACE, request)
            scheduled[key] = at
        }
    }
}
