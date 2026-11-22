package com.example.scpd_tracker.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
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
import com.example.scpd_tracker.data.system.AdjustableClock
import com.example.scpd_tracker.data.system.DataStoreDeviceId
import com.example.scpd_tracker.data.system.SwitchableNetworkMonitor
import com.example.scpd_tracker.data.system.NotificationSettingsStore
import com.example.scpd_tracker.data.system.EventNotificationMonitor
import com.example.scpd_tracker.data.system.connectivityFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers

private val Context.settingsStore by preferencesDataStore(name = "scpd_device")

/**
 * Manual dependency injection: one place builds every repository. Swapping the fakes for Firebase next week
 * means changing the right-hand side of these lines only.
 */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val clock = AdjustableClock()
    val network = SwitchableNetworkMonitor(context.connectivityFlow(), appScope)
    val deviceId: DeviceIdSource = DataStoreDeviceId(context.settingsStore)

    /** The pretend server. Only the debug menu talks to it directly (to play the s-CPD office). */
    val backend = FakeBackend(clock)

    private val fakeAuth = FakeAuthRepository(backend, network)
    val auth: AuthRepository = fakeAuth
    val events: EventRepository = FakeEventRepository(backend, fakeAuth, network)
    val bookings: BookingRepository = FakeBookingRepository(backend, fakeAuth, network)
    val attendance: AttendanceRepository = FakeAttendanceRepository(backend, fakeAuth, network, clock, appScope)
    val progress: ProgressRepository = FakeProgressRepository(backend, fakeAuth)
    val notificationSettings = NotificationSettingsStore(context)
    private val eventNotifications = EventNotificationMonitor(context, backend, auth, notificationSettings, clock, appScope)
}
