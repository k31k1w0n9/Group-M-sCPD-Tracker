package com.example.scpd_tracker.ui.student.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.scpd_tracker.data.system.NotificationPreferences
import com.example.scpd_tracker.data.system.NotificationSettingsStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationsViewModel(private val store: NotificationSettingsStore) : ViewModel() {
    val state: StateFlow<NotificationPreferences> = store.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        NotificationPreferences(),
    )

    fun eventReminders(enabled: Boolean) = viewModelScope.launch { store.setEventReminders(enabled) }
    fun streakReminders(enabled: Boolean) = viewModelScope.launch { store.setStreakReminders(enabled) }
    fun quietHours(enabled: Boolean) = viewModelScope.launch { store.setQuietHours(enabled) }
}
