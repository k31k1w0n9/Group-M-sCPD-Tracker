package com.example.scpd_tracker.ui.student.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.scpd_tracker.data.system.NotificationPreferences
import com.example.scpd_tracker.ui.components.ReadableWidth
import com.example.scpd_tracker.ui.components.rememberNotificationPermission
import com.example.scpd_tracker.ui.scpdViewModel
import com.example.scpd_tracker.ui.student.ShellTopBar
import com.example.scpd_tracker.ui.theme.ScpdTheme

@Composable
internal fun NotificationsRoute(onBack: () -> Unit) {
    val vm = scpdViewModel { c, _ -> NotificationsViewModel(c.notificationSettings) }
    val state by vm.state.collectAsStateWithLifecycle()
    // Asked only when the student taps the button below, never on opening the screen.
    val permission = rememberNotificationPermission()

    NotificationsScreen(
        settings = state,
        onBack = onBack,
        onEventReminders = { vm.eventReminders(it) },
        onStreakReminders = { vm.streakReminders(it) },
        onQuietHours = { vm.quietHours(it) },
        notificationsBlocked = !permission.allowed,
        onAllowNotifications = permission.request,
    )
}

@Composable
internal fun NotificationsScreen(
    settings: NotificationPreferences,
    onBack: () -> Unit,
    onEventReminders: (Boolean) -> Unit,
    onStreakReminders: (Boolean) -> Unit,
    onQuietHours: (Boolean) -> Unit,
    notificationsBlocked: Boolean = false,
    onAllowNotifications: () -> Unit = {},
) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography

    Column(Modifier.fillMaxSize().background(colors.background)) {
        ShellTopBar("Notifications", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ReadableWidth {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = spacing.gutter, vertical = spacing.space6),
                    verticalArrangement = Arrangement.spacedBy(spacing.space5),
                ) {
                    Text(
                        "Reminders, on your terms",
                        Modifier.semantics { heading() },
                        style = type.headlineSmall,
                        color = colors.onBackground,
                    )
                    Text(
                        "Choose notifications for this device.",
                        style = type.bodyLarge,
                        color = colors.onSurfaceVariant,
                    )
                    if (notificationsBlocked) {
                        Surface(color = colors.secondaryContainer, shape = MaterialTheme.shapes.extraLarge) {
                            Column(Modifier.fillMaxWidth().padding(spacing.space5), verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
                                Text("Notifications are off for s-CPD", style = type.titleMedium, color = colors.onSecondaryContainer)
                                Text(
                                    "Your reminders below won't appear until you allow them. My Events always shows the same updates.",
                                    style = type.bodyMedium, color = colors.onSecondaryContainer,
                                )
                                Button(onClick = onAllowNotifications) { Text("Allow notifications") }
                            }
                        }
                    }
                    Surface(color = colors.surfaceContainerLowest, shape = MaterialTheme.shapes.extraLarge) {
                        Column(Modifier.fillMaxWidth().padding(spacing.space5)) {
                            NotificationSwitchRow(
                                "Event reminders", "Get a reminder before your booked events.",
                                Icons.Rounded.NotificationsNone, settings.eventReminders, onEventReminders,
                            )
                            HorizontalDivider(Modifier.padding(vertical = spacing.space5), color = colors.outlineVariant)
                            NotificationSwitchRow(
                                "Streak reminders", "A weekly reminder to find an eligible event.",
                                Icons.Rounded.Insights, settings.streakReminders, onStreakReminders,
                            )
                        }
                    }
                    Surface(color = colors.surfaceContainerLowest, shape = MaterialTheme.shapes.extraLarge) {
                        Column(Modifier.fillMaxWidth().padding(spacing.space5)) {
                            NotificationSwitchRow(
                                "Quiet hours", "Pause event and streak reminders during your downtime.",
                                Icons.Rounded.NightsStay, settings.quietHours, onQuietHours,
                            )
                            HorizontalDivider(Modifier.padding(vertical = spacing.space5), color = colors.outlineVariant)
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(spacing.space3),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Icon(Icons.Rounded.AccessTime, contentDescription = null, tint = colors.primary)
                                Column(verticalArrangement = Arrangement.spacedBy(spacing.space1)) {
                                    Text("22:00 - 08:00", style = type.titleLarge)
                                    Text("Every day · Malaysia time (MYT)", style = type.bodyMedium, color = colors.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationSwitchRow(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.space3), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.space3)) {
                Icon(icon, contentDescription = null, tint = colors.primary)
                Text(title, style = type.titleLarge, color = colors.onSurface)
            }
            Text(description, style = type.bodyLarge, color = colors.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(spacing.space1)) {
            Switch(
                modifier = Modifier.semantics { contentDescription = title },
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = colors.secondary,
                    checkedThumbColor = Color.White,
                    uncheckedTrackColor = colors.outlineVariant,
                    uncheckedThumbColor = Color.White,
                ),
            )
            Text(if (checked) "On" else "Off", style = type.labelLarge, color = colors.onSurface)
        }
    }
}
