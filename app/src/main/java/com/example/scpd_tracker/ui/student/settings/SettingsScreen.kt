package com.example.scpd_tracker.ui.student.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.example.scpd_tracker.BuildConfig
import com.example.scpd_tracker.domain.Policy
import com.example.scpd_tracker.ui.components.ActionRow
import com.example.scpd_tracker.ui.components.ReadableWidth
import com.example.scpd_tracker.ui.student.ShellTopBar
import com.example.scpd_tracker.ui.theme.ScpdTheme

@Composable
internal fun SettingsScreen(onBack: () -> Unit, onNotifications: () -> Unit) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme

    Column(Modifier.fillMaxSize().background(colors.background)) {
        ShellTopBar("Settings", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ReadableWidth {
                Column(Modifier.padding(spacing.gutter), verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
                    SectionTitle("Preferences")
                    ActionRow("Notifications", Icons.Rounded.NotificationsNone, onNotifications, supporting = "Event and streak reminders, quiet hours")

                    SectionTitle("Offline check-ins")
                    InfoCard(
                        "Pending sync",
                        "If you scan without internet, the scan is saved on this phone and shows as Pending sync. " +
                            "It uploads when you reconnect. In the demo, closing the app process clears unsynced scans.",
                    )
                    InfoCard(
                        "Upload deadline",
                        "Saved scans must reach the server within ${Policy.OFFLINE_UPLOAD_WINDOW.toHours()} hours after check-in closes. " +
                            "Later uploads are Rejected. If the office flags a record, it shows as Under review until they decide.",
                    )

                    SectionTitle("About")
                    InfoCard("s-CPD Tracker", "Version ${BuildConfig.VERSION_NAME}. ${Policy.POINTS_PER_EVENT} points for each eligible event after attendance is verified.")
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
