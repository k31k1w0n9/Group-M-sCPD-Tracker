package com.example.scpd_tracker.ui.student.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.scpd_tracker.BuildConfig
import com.example.scpd_tracker.domain.Policy
import com.example.scpd_tracker.ui.components.ReadableWidth
import com.example.scpd_tracker.ui.student.ShellTopBar
import com.example.scpd_tracker.ui.theme.ScpdTheme

/**
 * Student settings: reminders, how offline check-ins work, and app info.
 * The reminder switches only change this screen for now; saving them is a later step.
 */
@Composable
internal fun SettingsScreen(onBack: () -> Unit) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    var eventReminders by rememberSaveable { mutableStateOf(true) }
    var checkInReminders by rememberSaveable { mutableStateOf(true) }

    Column(Modifier.fillMaxSize().background(colors.background)) {
        ShellTopBar("Settings", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ReadableWidth {
                Column(Modifier.padding(spacing.gutter), verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
                    SectionTitle("Reminders")
                    SwitchRow("Event reminders", "Remind me before events I have booked", eventReminders) { eventReminders = it }
                    SwitchRow("Check-in reminders", "Remind me when check-in opens", checkInReminders) { checkInReminders = it }

                    SectionTitle("Offline check-ins")
                    InfoCard(
                        "Pending sync",
                        "If you scan without internet, the scan is saved on this phone and shows as Pending sync. " +
                            "It uploads when you are back online, and points are added only after it is verified.",
                    )
                    InfoCard(
                        "Upload deadline",
                        "Saved scans must reach the server within ${Policy.OFFLINE_UPLOAD_WINDOW.toHours()} hours after check-in closes. " +
                            "Later uploads are Rejected. If the office flags a record, it shows as Under review until they decide.",
                    )

                    SectionTitle("About")
                    InfoCard("s-CPD Tracker", "Version ${BuildConfig.VERSION_NAME}. ${Policy.POINTS_PER_EVENT} points for each fully verified event.")
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        Modifier.padding(top = ScpdTheme.spacing.space2).semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun SwitchRow(title: String, supporting: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.surfaceContainerLowest, shape = MaterialTheme.shapes.large) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp)
                .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
                .padding(horizontal = spacing.space4, vertical = spacing.space3),
            horizontalArrangement = Arrangement.spacedBy(spacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(supporting, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            // The row handles the tap, so the switch itself is display-only.
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxWidth(), color = colors.surfaceContainerLowest, shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(spacing.space4), verticalArrangement = Arrangement.spacedBy(spacing.space1)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
    }
}
