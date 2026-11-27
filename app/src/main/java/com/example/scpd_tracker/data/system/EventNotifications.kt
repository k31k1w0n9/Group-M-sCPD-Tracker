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
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.scpd_tracker.MainActivity
import com.example.scpd_tracker.R
import com.example.scpd_tracker.ScpdApplication
import com.example.scpd_tracker.data.AppClock
import com.example.scpd_tracker.data.AuthRepository
import com.example.scpd_tracker.data.fake.BackendAlert
import kotlinx.coroutines.flow.Flow
import com.example.scpd_tracker.model.AccountRole
import com.example.scpd_tracker.model.Booking
import com.example.scpd_tracker.model.BookingStatus
import com.example.scpd_tracker.model.Event
import com.example.scpd_tracker.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter
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

    private fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun sendEvent(
        context: Context, eventId: String, eventTitle: String, startMillis: Long, venue: String,
        title: String, detail: String, kind: String,
    ): Boolean {
        if (!canNotify(context)) return false
        createChannel(context)
        val key = "$eventId:$kind"
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "com.example.scpd_tracker.OPEN_BOOKING"
            putExtra(NotificationNavigation.EXTRA_EVENT_ID, eventId)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = PendingIntent.getActivity(
            context, key.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val time = Instant.ofEpochMilli(startMillis).atZone(ReminderSchedule.zone)
            .format(DateTimeFormatter.ofPattern("EEE d MMM, h:mm a"))
        val info = "$time · $venue"
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("$title · $eventTitle")
            .setContentText(info)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$info\n$detail"))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        NotificationManagerCompat.from(context).notify(key.hashCode(), notification)
        return true
    }

    fun sendStreak(context: Context) {
        if (!canNotify(context)) return
        createChannel(context)
        val intent = Intent(context, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            context, "streak".hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Weekly streak reminder")
            .setContentText("Find an eligible event to keep your weekly streak going.")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify("streak".hashCode(), notification)
    }
}

class EventReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as ScpdApplication
        val settings = app.container.notificationSettings.settings.first()
        val uid = inputData.getString(KEY_USER) ?: return Result.success()
        if (!ReminderSchedule.canNotifyStudent(uid, app.container.notificationSettings.reminderRecipient.first()) ||
            !settings.eventReminders) {
            return Result.success()
        }
        val eventId = inputData.getString(KEY_EVENT) ?: return Result.success()
        val title = inputData.getString(KEY_TITLE) ?: return Result.success()
        val venue = inputData.getString(KEY_VENUE) ?: return Result.success()
        val start = inputData.getLong(KEY_START, 0L)
        val hours = inputData.getInt(KEY_HOURS, 1)
        if (start <= 0 || System.currentTimeMillis() >= start) return Result.success()
        val now = Instant.now().atZone(ReminderSchedule.zone)
        if (settings.quietHours && ReminderSchedule.inQuietHours(now)) {
            // defer a late worker instead of discarding its reminder.
            val morning = now.toLocalDate().atTime(8, 0).atZone(ReminderSchedule.zone)
                .let { if (it.isAfter(now)) it else it.plusDays(1) }
            if (morning.toInstant().toEpochMilli() < start) {
                val retry = OneTimeWorkRequestBuilder<EventReminderWorker>()
                    .setInitialDelay(morning.toInstant().toEpochMilli() - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                    .setInputData(inputData)
                    .build()
                val key = inputData.getString(KEY_WORK) ?: return Result.success()
                WorkManager.getInstance(applicationContext).enqueueUniqueWork(key, ExistingWorkPolicy.APPEND_OR_REPLACE, retry)
            }
            return Result.success()
        }
        EventNotifications.sendEvent(
            applicationContext, eventId, title, start, venue,
            "Event reminder", "Your booked event is coming up.", "reminder-$hours",
        )
        return Result.success()
    }

    companion object {
        const val KEY_USER = "user"
        const val KEY_EVENT = "event"
        const val KEY_TITLE = "title"
        const val KEY_VENUE = "venue"
        const val KEY_START = "start"
        const val KEY_HOURS = "hours"
        const val KEY_WORK = "work"
    }
}

class StreakReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as ScpdApplication
        val prefs = app.container.notificationSettings
        if (ReminderSchedule.canNotifyStudent(inputData.getString(EventReminderWorker.KEY_USER), prefs.reminderRecipient.first()) &&
            prefs.settings.first().streakReminders) {
            EventNotifications.sendStreak(applicationContext)
        }
        return Result.success()
    }
}

internal class EventNotificationMonitor(
    private val context: Context,
    private val feed: NotificationFeed,
    auth: AuthRepository,
    private val settings: NotificationSettingsStore,
    private val clock: AppClock,
    scope: CoroutineScope,
) {
    private data class Snapshot(
        val user: UserProfile?,
        val events: Map<String, Event>,
        val bookings: List<Booking>,
        val settings: NotificationPreferences,
    )

    private val work = WorkManager.getInstance(context)
    private val scheduled = mutableMapOf<String, Pair<Long, Int>>()
    private val seenAlerts = mutableSetOf<String>()
    private var lastUid: String? = null
    private var streakWork: String? = null

    init {
        EventNotifications.createChannel(context)
        scope.launch {
            combine(auth.currentUser, feed.events, feed.bookings, settings.settings) { user, events, bookings, preferences ->
                Snapshot(user, events, bookings, preferences)
            }.collect { schedule(it) }
        }
        scope.launch {
            combine(auth.currentUser, feed.alerts) { user, alerts -> user to alerts }.collect { (user, alerts) ->
                if (user?.role != AccountRole.Student) return@collect
                // deliver pending alerts when the student returns to the app.
                alerts.filter { it.recipientId == user.uid && it.id !in seenAlerts }.forEach { alert ->
                    if (postAlert(alert)) seenAlerts.add(alert.id)
                }
            }
        }
    }

    private fun postAlert(alert: BackendAlert): Boolean = EventNotifications.sendEvent(
        context, alert.eventId, alert.eventTitle, alert.startMillis, alert.venue,
        alert.kind.heading, alert.detail, "${alert.kind.name}-${alert.id}",
    )

    private suspend fun schedule(current: Snapshot) {
        val user = current.user?.takeIf { it.role == AccountRole.Student }
        if (user == null) {
            // keep scheduled reminders after a cold start.
            if (lastUid != null) cancelCurrent()
            if (lastUid != null || current.user != null) settings.setReminderRecipient(null)
            lastUid = null
            return
        }
        if (lastUid != user.uid) {
            if (lastUid != null) cancelCurrent()
            settings.setReminderRecipient(user.uid)
        }
        lastUid = user.uid

        val now = clock.now().toEpochMilli()
        val desired = mutableMapOf<String, Triple<Event, Int, Long>>()
        if (current.settings.eventReminders) {
            current.bookings.filter { it.userId == user.uid && it.status == BookingStatus.Booked }.forEach { booking ->
                val event = current.events[booking.eventId] ?: return@forEach
                if (event.isCancelled) return@forEach
                for (hours in listOf(24, 1)) {
                    val time = ReminderSchedule.reminderAt(event.start, hours.toLong(), current.settings.quietHours)
                        .toInstant().toEpochMilli()
                    if (time > now && time < event.start.toInstant().toEpochMilli()) {
                        desired["reminder:${user.uid}:${booking.id}:$hours"] = Triple(event, hours, time)
                    }
                }
            }
        }
        (scheduled.keys - desired.keys).forEach { key -> work.cancelUniqueWork(key); scheduled.remove(key) }
        desired.forEach { (key, value) ->
            val (event, hours, at) = value
            val details = event.title.hashCode() * 31 + event.venue.hashCode()
            if (scheduled[key] == (at to details)) return@forEach
            val request = OneTimeWorkRequestBuilder<EventReminderWorker>()
                .setInitialDelay((at - now).coerceAtLeast(0), TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(
                    EventReminderWorker.KEY_USER to user.uid,
                    EventReminderWorker.KEY_EVENT to event.id,
                    EventReminderWorker.KEY_TITLE to event.title,
                    EventReminderWorker.KEY_VENUE to event.venue,
                    EventReminderWorker.KEY_START to event.start.toInstant().toEpochMilli(),
                    EventReminderWorker.KEY_HOURS to hours,
                    EventReminderWorker.KEY_WORK to key,
                )).build()
            work.enqueueUniqueWork(key, ExistingWorkPolicy.REPLACE, request)
            scheduled[key] = at to details
        }

        val expectedStreak = if (current.settings.streakReminders) "streak:${user.uid}" else null
        if (streakWork != expectedStreak) {
            streakWork?.let(work::cancelUniqueWork)
            streakWork = expectedStreak
            if (expectedStreak != null) {
                val next = ReminderSchedule.nextStreakAt(clock.now())
                val delay = (next.toInstant().toEpochMilli() - now).coerceAtLeast(0)
                val periodic = PeriodicWorkRequestBuilder<StreakReminderWorker>(7, TimeUnit.DAYS)
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setInputData(workDataOf(EventReminderWorker.KEY_USER to user.uid))
                    .build()
                work.enqueueUniquePeriodicWork(expectedStreak, ExistingPeriodicWorkPolicy.KEEP, periodic)
            }
        }
    }

    private fun cancelCurrent() {
        scheduled.keys.forEach(work::cancelUniqueWork)
        scheduled.clear()
        streakWork?.let(work::cancelUniqueWork)
        streakWork = null
    }
}

/** What the notification monitor watches. The fake backend and Firebase each supply one. */
interface NotificationFeed {
    val events: Flow<Map<String, Event>>
    val bookings: Flow<List<Booking>>
    val alerts: Flow<List<BackendAlert>>
}
