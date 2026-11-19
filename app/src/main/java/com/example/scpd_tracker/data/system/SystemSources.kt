package com.example.scpd_tracker.data.system

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.scpd_tracker.data.AppClock
import com.example.scpd_tracker.data.DeviceIdSource
import com.example.scpd_tracker.data.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import java.time.Duration
import java.time.Instant
import java.util.UUID

/** System time plus an offset the debug menu can move forward ("+1 hour") to demo deadlines. */
class AdjustableClock(private val base: () -> Instant = Instant::now) : AppClock {
    @Volatile var offset: Duration = Duration.ZERO
        private set

    override fun now(): Instant = base().plus(offset)

    fun advance(by: Duration) {
        offset = offset.plus(by)
    }
}

/**
 * Online only when the phone really has a connection AND the debug "simulate offline" switch is off.
 * The switch exists because the emulator's own network toggle is slow to use in a live demo.
 */
class SwitchableNetworkMonitor(source: Flow<Boolean>, scope: CoroutineScope) : NetworkMonitor {
    private val forcedOffline = MutableStateFlow(false)
    val isForcedOffline: StateFlow<Boolean> = forcedOffline

    override val isOnline: StateFlow<Boolean> =
        combine(source, forcedOffline) { connected, forced -> connected && !forced }
            .stateIn(scope, SharingStarted.Eagerly, true)

    fun setForcedOffline(offline: Boolean) {
        forcedOffline.value = offline
    }
}

/** Real connectivity from ConnectivityManager, as a Flow. */
fun Context.connectivityFlow(): Flow<Boolean> = callbackFlow {
    val manager = getSystemService(ConnectivityManager::class.java)
    fun connectedNow(): Boolean = manager.getNetworkCapabilities(manager.activeNetwork)
        ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { trySend(true) }
        override fun onLost(network: Network) { trySend(connectedNow()) }
    }
    trySend(connectedNow())
    manager.registerNetworkCallback(
        NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(), callback,
    )
    awaitClose { manager.unregisterNetworkCallback(callback) }
}

/** Creates a UUID the first time and returns the same one afterwards, across app restarts. */
class DataStoreDeviceId(private val store: DataStore<Preferences>) : DeviceIdSource {
    private val key = stringPreferencesKey("device_id")

    override suspend fun get(): String {
        store.data.first()[key]?.let { return it }
        var id = ""
        store.edit { prefs -> id = prefs[key] ?: UUID.randomUUID().toString().also { prefs[key] = it } }
        return id
    }
}
