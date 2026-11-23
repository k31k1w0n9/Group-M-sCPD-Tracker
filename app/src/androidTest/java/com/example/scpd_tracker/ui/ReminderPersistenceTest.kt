package com.example.scpd_tracker.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.scpd_tracker.data.system.NotificationSettingsStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderPersistenceTest {
    @Test
    fun reminderRecipientSurvivesStoreRecreation() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = NotificationSettingsStore(context)
        try {
            store.setReminderRecipient("u-reminder-test")
            val reopened = NotificationSettingsStore(context)
            assertEquals("u-reminder-test", reopened.reminderRecipient.first())
        } finally {
            store.setReminderRecipient(null)
        }
    }
}
