package com.example.scpd_tracker.ui.student.profile

import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.scpd_tracker.BuildConfig
import com.example.scpd_tracker.R
import com.example.scpd_tracker.model.Stage
import com.example.scpd_tracker.ui.components.*
import com.example.scpd_tracker.ui.debug.DebugMenu
import com.example.scpd_tracker.ui.scpdViewModel
import com.example.scpd_tracker.ui.student.ShellTopBar
import com.example.scpd_tracker.ui.student.progress.ProgressUiState
import com.example.scpd_tracker.ui.student.progress.ProgressViewModel
import com.example.scpd_tracker.ui.student.progress.StreakCard
import com.example.scpd_tracker.ui.theme.ScpdTheme

@Composable
internal fun ProfileRoute(
    onEditInterests: () -> Unit,
    onNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    onHistory: () -> Unit,
    onCheckIn: (String) -> Unit,
    onSignOut: () -> Unit,
) {
    val vm = scpdViewModel { c, _ -> ProfileViewModel(c.auth, c.attendance, c.bookings, c.events) }
    val progressVm = scpdViewModel { c, _ -> ProgressViewModel(c.attendance, c.progress, c.events, c.auth, c.clock) }
    val state by vm.state.collectAsStateWithLifecycle()
    val progress by progressVm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.signOutConfirmed) { if (state.signOutConfirmed) onSignOut() }
    ProfileScreen(
        state = state,
        progress = progress,
        onEditInterests = onEditInterests,
        onNotifications = onNotifications,
        onSignOut = vm::requestSignOut,
        onConfirmSignOut = vm::confirmSignOut,
        onDismissSignOut = vm::dismissSignOut,
        onOpenSettings = onOpenSettings,
        onHistory = onHistory,
        onCheckIn = onCheckIn,
    )
}

@Composable
internal fun ProfileScreen(
    state: ProfileUiState,
    progress: ProgressUiState,
    onEditInterests: () -> Unit,
    onNotifications: () -> Unit,
    onSignOut: () -> Unit,
    onConfirmSignOut: () -> Unit,
    onDismissSignOut: () -> Unit,
    onOpenSettings: () -> Unit,
    onHistory: () -> Unit,
    onCheckIn: (String) -> Unit,
) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    var showDebug by remember { mutableStateOf(false) }
    BoxWithConstraints(Modifier.fillMaxSize().background(colors.background)) {
        val short = maxHeight < 480.dp
        Column(Modifier.fillMaxSize()) {
            if (short) ShellTopBar("Profile")
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                if (!short) ProfileHeader(progress, state.checkIn != null)
                ReadableWidth {
                    Column(
                        Modifier.padding(horizontal = spacing.gutter).padding(top = if (short) spacing.space4 else 0.dp, bottom = spacing.space4),
                        verticalArrangement = Arrangement.spacedBy(spacing.space4),
                    ) {
                        ProfileIdentity(state, overlapHeader = !short)
                        StreakCard(progress.streakWeeks, progress.recentWeeks, onHistory) {
                            ProfileGilbert(Modifier.size(64.dp), scale = 4.1801f, x = -0.0417f, y = -2.1667f)
                        }
                        state.checkIn?.let { checkIn ->
                            ProfileCheckInCard(checkIn) { onCheckIn(checkIn.event.id) }
                        }
                        Text("Account", Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
                        ActionRow("Notifications", Icons.Rounded.NotificationsNone, onNotifications, supporting = "Reminders and quiet hours")
                        ActionRow("Interests", Icons.Rounded.Interests, onEditInterests, supporting = state.interests.joinToString(", ").ifEmpty { "None chosen" })
                        ActionRow("Settings", Icons.Rounded.Settings, onOpenSettings, supporting = "Preferences and offline check-ins")
                        ActionRow(
                            "Sign out", Icons.AutoMirrored.Rounded.Logout, onSignOut, tint = colors.error,
                            supporting = if (state.pendingCount > 0) "${state.pendingCount} check-ins still on this phone" else null,
                        )
                        if (BuildConfig.DEBUG) ActionRow("Developer menu", Icons.Rounded.BugReport, { showDebug = true }, supporting = "Debug builds only")
                    }
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

@Composable
private fun ProfileHeader(progress: ProgressUiState, live: Boolean) {
    val spacing = ScpdTheme.spacing
    val hero = ScpdTheme.colors
    Box(Modifier.fillMaxWidth()) {
        CampusBackground(Modifier.matchParentSize(), home = true)
        ReadableWidth(Modifier.statusBarsPadding()) {
            Column(Modifier.padding(spacing.gutter)) {
                BrandIdentities()
                Row(
                    Modifier.fillMaxWidth().padding(top = spacing.space4, bottom = spacing.space5),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.space3),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Profile", Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineSmall, color = hero.onHero)
                        Text(
                            "${progress.total}/${progress.milestone} verified points" + if (live) " · Live event" else "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = hero.onHeroMuted,
                        )
                    }
                    // Crop past the neighbouring sprite's feet at the top of this cell.
                    ProfileGilbert(Modifier.size(75.dp), scale = 3.9809f, x = -2.9619f, y = -3.02f)
                }
            }
        }
    }
}

/** Two crops from the same Gilbert sprite sheet used by the Figma Profile frame. */
@Composable
private fun ProfileGilbert(modifier: Modifier, scale: Float, x: Float, y: Float) {
    Box(modifier.clipToBounds()) {
        Image(
            painterResource(R.drawable.gilbert_sheet),
            contentDescription = null,
            modifier = Modifier.matchParentSize().graphicsLayer {
                transformOrigin = TransformOrigin(0f, 0f)
                scaleX = scale
                scaleY = scale
                translationX = x * size.width
                translationY = y * size.height
            },
            contentScale = ContentScale.FillBounds,
        )
    }
}

@Composable
private fun ProfileIdentity(state: ProfileUiState, overlapHeader: Boolean) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Surface(
            Modifier.fillMaxWidth(),
            color = colors.surfaceContainerLowest,
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                Modifier.padding(horizontal = spacing.space4).padding(top = if (overlapHeader) 52.dp else 80.dp, bottom = spacing.space4),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.space2),
            ) {
                Text(state.name, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                if (state.studentId.isNotBlank()) {
                    Text("Student ID: ${state.studentId}", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    TagChip("Account verified", colors.secondaryContainer, colors.onSecondaryContainer, Icons.Rounded.VerifiedUser)
                }
            }
        }
        Box(
            Modifier.offset(y = if (overlapHeader) (-30).dp else spacing.space3)
                .size(76.dp).background(colors.surfaceContainerLowest, CircleShape).padding(3.dp)
                .background(colors.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(state.initials, style = MaterialTheme.typography.titleLarge, color = colors.onSecondaryContainer)
        }
    }
}

@Composable
private fun ProfileCheckInCard(checkIn: ProfileCheckIn, onOpen: () -> Unit) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    val checkpoint = checkIn.stage == Stage.Checkpoint
    val event = checkIn.event
    Surface(Modifier.fillMaxWidth(), color = colors.secondaryContainer, shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(spacing.space4), verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
            Text(event.title, style = MaterialTheme.typography.titleMedium)
            Text("${event.timeLabel} · ${event.venue}", style = MaterialTheme.typography.bodyMedium)
            if (checkpoint) checkIn.arrival?.let { Text("Arrival: ${it.label}", style = MaterialTheme.typography.bodySmall) }
            Text(if (checkpoint) "Checkpoint open" else "Arrival check-in open", style = MaterialTheme.typography.bodySmall)
            PrimaryAction(if (checkpoint) "Scan checkpoint" else "Scan arrival QR", onClick = onOpen)
        }
    }
}
