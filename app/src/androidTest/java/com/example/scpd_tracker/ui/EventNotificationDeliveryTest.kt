package com.example.scpd_tracker.ui

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.scpd_tracker.data.system.EventNotifications
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EventNotificationDeliveryTest {
    @Test
    fun eventNotificationContainsTheEventAndVenue() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        val title = "Reminder test event"
        val posted = EventNotifications.sendEvent(
            context, "test-reminder-delivery", title, System.currentTimeMillis() + 3_600_000,
            "F2A04", "Event reminder", "Your booked event is coming up.", "test-delivery",
        )
        assertTrue(posted)
        val notification = manager.activeNotifications.firstOrNull {
            it.notification.extras.getString(Notification.EXTRA_TITLE)?.contains(title) == true
        }
        try {
            assertNotNull(notification)
            // check the content after posting.
            assertTrue(notification!!.notification.extras.getString(Notification.EXTRA_TEXT)?.contains("F2A04") == true)
        } finally {
            notification?.let { manager.cancel(it.id) }
        }
    }
}
