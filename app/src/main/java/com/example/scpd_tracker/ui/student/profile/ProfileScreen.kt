package com.example.scpd_tracker.ui.student.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Interests
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.scpd_tracker.BuildConfig
import com.example.scpd_tracker.ui.components.*
import com.example.scpd_tracker.ui.debug.DebugMenu
import com.example.scpd_tracker.ui.scpdViewModel
import com.example.scpd_tracker.ui.student.ShellTopBar
import com.example.scpd_tracker.ui.theme.ScpdTheme

@Composable
internal fun ProfileRoute(
    onEditInterests: () -> Unit,
    onNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    onSignOut: () -> Unit,
) {
    val vm = scpdViewModel { c, _ -> ProfileViewModel(c.auth, c.attendance) }
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.signOutConfirmed) { if (state.signOutConfirmed) onSignOut() }
    ProfileScreen(
        state = state,
        onEditInterests = onEditInterests,
        onNotifications = onNotifications,
        onSignOut = vm::requestSignOut,
        onConfirmSignOut = vm::confirmSignOut,
        onDismissSignOut = vm::dismissSignOut,
        onOpenSettings = onOpenSettings,
    )
}

@Composable
internal fun ProfileScreen(
    state: ProfileUiState,
    onEditInterests: () -> Unit,
    onNotifications: () -> Unit,
    onSignOut: () -> Unit,
    onConfirmSignOut: () -> Unit,
    onDismissSignOut: () -> Unit,
    onOpenSettings: () -> Unit = {},
) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    var showDebug by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(colors.background)) {
        ShellTopBar("Profile")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ReadableWidth {
                Column(Modifier.padding(spacing.gutter), verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.space4)) {
                        Box(Modifier.size(56.dp).background(colors.secondaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                            Text(state.initials, style = MaterialTheme.typography.titleMedium, color = colors.onSecondaryContainer)
                        }
                        Column {
                            Text(state.name, style = MaterialTheme.typography.titleLarge)
                            Text("Student · ${state.studentId}", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        }
                    }
                    TagChip("Account verified", colors.secondaryContainer, colors.onSecondaryContainer, Icons.Rounded.VerifiedUser)
                    Spacer(Modifier.height(spacing.space2))
                    ActionRow("Notifications", Icons.Rounded.NotificationsNone, onNotifications, supporting = "Reminders and quiet hours")
                    ActionRow("Interests", Icons.Rounded.Interests, onEditInterests, supporting = state.interests.joinToString(", ").ifEmpty { "None chosen" })
                    ActionRow("Settings", Icons.Rounded.Settings, onOpenSettings, supporting = "Reminders and offline check-ins")
                    ActionRow(
                        "Sign out", Icons.AutoMirrored.Rounded.Logout, onSignOut, tint = colors.error,
                        supporting = if (state.pendingCount > 0) "${state.pendingCount} check-ins still on this phone" else null,
                    )
                    if (BuildConfig.DEBUG) ActionRow("Developer menu", Icons.Rounded.BugReport, { showDebug = true }, supporting = "Debug builds only")
                }
            }
        }
    }
    state.signOutWarning?.let { pending ->
        AlertDialog(
            onDismissRequest = onDismissSignOut,
            title = { Text("Check-ins not synced yet") },
            text = {
                Text(
                    "$pending check-in${if (pending == 1) " is" else "s are"} saved on this phone and not on the server yet. " +
                        "If you sign out now, they're lost and earn no points. Reconnect first to upload them.",
                )
            },
            confirmButton = { TextButton(onClick = onConfirmSignOut) { Text("Sign out anyway") } },
            dismissButton = { TextButton(onClick = onDismissSignOut) { Text("Stay signed in") } },
        )
    }
    if (showDebug) DebugMenu(onDismiss = { showDebug = false })
}
