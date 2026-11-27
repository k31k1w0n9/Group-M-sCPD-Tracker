package com.example.scpd_tracker.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.example.scpd_tracker.BuildConfig
import com.example.scpd_tracker.data.AttendanceRepository
import com.example.scpd_tracker.data.AuthRepository
import com.example.scpd_tracker.data.BookingRepository
import com.example.scpd_tracker.data.DeviceIdSource
import com.example.scpd_tracker.data.EventRepository
import com.example.scpd_tracker.data.ProgressRepository
import com.example.scpd_tracker.data.fake.FakeAttendanceRepository
import com.example.scpd_tracker.data.fake.FakeAuthRepository
import com.example.scpd_tracker.data.fake.FakeBackend
import com.example.scpd_tracker.data.fake.FakeBookingRepository
import com.example.scpd_tracker.data.fake.FakeEventRepository
import com.example.scpd_tracker.data.fake.FakeProgressRepository
import com.example.scpd_tracker.data.firebase.FirebaseAttendanceRepository
import com.example.scpd_tracker.data.firebase.FirebaseAuthRepository
import com.example.scpd_tracker.data.firebase.FirebaseBookingRepository
import com.example.scpd_tracker.data.firebase.FirebaseEventRepository
import com.example.scpd_tracker.data.firebase.FirebaseNotificationFeed
import com.example.scpd_tracker.data.firebase.FirebaseProgressRepository
import com.example.scpd_tracker.data.firebase.FirebaseSetup
import com.example.scpd_tracker.data.firebase.FirebaseStore
import com.example.scpd_tracker.data.local.ScpdDatabase
import com.example.scpd_tracker.data.system.AdjustableClock
import com.example.scpd_tracker.data.system.DataStoreDeviceId
import com.example.scpd_tracker.data.system.EventNotificationMonitor
import com.example.scpd_tracker.data.system.NotificationFeed
import com.example.scpd_tracker.data.system.NotificationSettingsStore
import com.example.scpd_tracker.data.system.SwitchableNetworkMonitor
import com.example.scpd_tracker.data.system.connectivityFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first

private val Context.settingsStore by preferencesDataStore(name = "scpd_device")

/**
 * Manual dependency injection: one place builds every repository. The screens only see the interfaces in
 * data/Repositories.kt, so the backend is chosen here and nowhere else:
 *  - "fake" (default): the in-memory backend, used by every unit and UI test.
 *  - "firebase": Firebase Auth + Firestore on the free Spark plan, with the Room scan queue.
 * Pick it with the Gradle property scpd.backend (see app/build.gradle.kts and firebase/README.md).
 */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val usesFirebase: Boolean = BuildConfig.BACKEND == "firebase"

    val clock = AdjustableClock()
    val network = SwitchableNetworkMonitor(context.connectivityFlow(), appScope)
    val deviceId: DeviceIdSource = DataStoreDeviceId(context.settingsStore)

    /** The pretend server. Only used with the fake backend; the debug menu plays the s-CPD office on it. */
    val backend = FakeBackend(clock)

    val auth: AuthRepository
    val events: EventRepository
    val bookings: BookingRepository
    val attendance: AttendanceRepository
    val progress: ProgressRepository
    private val notificationFeed: NotificationFeed

    init {
        if (usesFirebase) {
            val firebaseAuth = FirebaseAuthRepository(FirebaseSetup.get(context), network, context.settingsStore, deviceId, appScope)
            val store = FirebaseStore(FirebaseSetup.get(context), firebaseAuth, appScope)
            val scans = ScpdDatabase.create(context).scanQueue()
            auth = firebaseAuth
            events = FirebaseEventRepository(store, network, clock)
            bookings = FirebaseBookingRepository(store, network, clock)
            attendance = FirebaseAttendanceRepository(context.applicationContext, store, scans, network, clock, appScope)
            progress = FirebaseProgressRepository(store)
            notificationFeed = FirebaseNotificationFeed(store, bookings)
        } else {
            val fakeAuth = FakeAuthRepository(backend, network)
            auth = fakeAuth
            events = FakeEventRepository(backend, fakeAuth, network)
            bookings = FakeBookingRepository(backend, fakeAuth, network)
            attendance = FakeAttendanceRepository(backend, fakeAuth, network, clock, appScope)
            progress = FakeProgressRepository(backend, fakeAuth)
            notificationFeed = object : NotificationFeed {
                override val events = backend.events
                override val bookings = backend.bookings
                override val alerts = backend.alerts
            }
        }
    }

    val notificationSettings = NotificationSettingsStore(context)
    private val eventNotifications = EventNotificationMonitor(context, notificationFeed, auth, notificationSettings, clock, appScope)

    /** Scans still waiting on the phone for the signed-in account (used by the background sync job). */
    suspend fun pendingScanCount(): Int = attendance.pendingCount().first()
}
