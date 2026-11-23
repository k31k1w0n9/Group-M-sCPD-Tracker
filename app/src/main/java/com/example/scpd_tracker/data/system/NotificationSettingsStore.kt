package com.example.scpd_tracker.data.system

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.notificationStore by preferencesDataStore(name = "scpd_notifications")

data class NotificationPreferences(
    val eventReminders: Boolean = true,
    val streakReminders: Boolean = false,
    val quietHours: Boolean = true,
)

class NotificationSettingsStore(private val context: Context) {
    private val eventKey = booleanPreferencesKey("event_reminders")
    private val streakKey = booleanPreferencesKey("streak_reminders")
    private val quietKey = booleanPreferencesKey("quiet_hours")
    private val recipientKey = stringPreferencesKey("reminder_recipient")
    val reminderRecipient: Flow<String?> = context.notificationStore.data.map { it[recipientKey] }

    suspend fun setReminderRecipient(uid: String?) {
        context.notificationStore.edit { preferences ->
            if (uid == null) preferences.remove(recipientKey)
            else preferences[recipientKey] = uid
        }
    }

    val settings: Flow<NotificationPreferences> = context.notificationStore.data.map { data ->
        NotificationPreferences(
            eventReminders = data[eventKey] ?: true,
            streakReminders = data[streakKey] ?: false,
            quietHours = data[quietKey] ?: true,
        )
    }

    suspend fun setEventReminders(enabled: Boolean) {
        context.notificationStore.edit { it[eventKey] = enabled }
    }

    suspend fun setStreakReminders(enabled: Boolean) {
        context.notificationStore.edit { it[streakKey] = enabled }
    }

    suspend fun setQuietHours(enabled: Boolean) {
        context.notificationStore.edit { it[quietKey] = enabled }
    }
}
