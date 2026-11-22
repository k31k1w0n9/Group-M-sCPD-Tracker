package com.example.scpd_tracker.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.scpd_tracker.data.system.NotificationPreferences
import com.example.scpd_tracker.ui.student.notifications.NotificationsScreen
import com.example.scpd_tracker.ui.theme.ScpdTheme
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class NotificationsScreenTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun notificationSettings_showWireframeLabels() {
        rule.setContent {
            ScpdTheme {
                NotificationsScreen(NotificationPreferences(), {}, {}, {}, {})
            }
        }
        rule.onNodeWithText("Reminders, on your terms").assertIsDisplayed()
        rule.onNodeWithText("Event reminders").assertIsDisplayed()
        rule.onNodeWithText("Streak reminders").assertExists()
        rule.onNodeWithText("Quiet hours").assertExists()
        rule.onNodeWithText("22:00 - 08:00").assertExists()
    }

    @Test
    fun eventReminderSwitch_updatesPreference() {
        var enabled = true
        rule.setContent {
            ScpdTheme {
                NotificationsScreen(NotificationPreferences(), {}, { enabled = it }, {}, {})
            }
        }
        rule.onNodeWithContentDescription("Event reminders").performClick()
        assertFalse(enabled)
    }
}
