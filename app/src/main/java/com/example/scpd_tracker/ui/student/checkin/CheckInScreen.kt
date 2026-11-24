package com.example.scpd_tracker.ui.student.checkin

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.scpd_tracker.BuildConfig
import com.example.scpd_tracker.R
import com.example.scpd_tracker.ScpdApplication
import com.example.scpd_tracker.model.AttendanceOutcome
import com.example.scpd_tracker.model.Stage
import com.example.scpd_tracker.ui.components.*
import com.example.scpd_tracker.ui.scpdViewModel
import com.example.scpd_tracker.ui.student.ShellTopBar
import com.example.scpd_tracker.ui.theme.ScpdTheme
import com.example.scpd_tracker.ui.theme.PillShape

@Composable
internal fun CheckInRoute(onBack: () -> Unit, onProgress: () -> Unit) {
    val vm = scpdViewModel { c, s -> CheckInViewModel(c.events, c.attendance, c.deviceId, c.network, c.clock, s) }
    val container = (LocalContext.current.applicationContext as ScpdApplication).container
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var scanningStage by rememberSaveable { mutableStateOf<Stage?>(null) }
    var cameraDenied by rememberSaveable { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) scanningStage = state.activeStage else cameraDenied = true
    }

    // Identity check before anything leaves the phone.
    LaunchedEffect(state.awaitingIdentity) {
        if (!state.awaitingIdentity) return@LaunchedEffect
        val activity = context as? FragmentActivity
        if (activity == null) {
            vm.onIdentityConfirmed()
        } else {
            confirmIdentity(activity) { result ->
                when (result) {
                    IdentityResult.Confirmed -> vm.onIdentityConfirmed()
                    IdentityResult.NoScreenLock -> vm.onIdentityConfirmed("Set a screen lock so nobody else can check in with your phone.")
                    IdentityResult.Cancelled -> vm.onIdentityCancelled()
                }
            }
        }
    }

    BackHandler(scanningStage != null) { scanningStage = null }
    val activeScanner = scanningStage
    if (activeScanner != null) {
        ScannerOverlay(
            stage = activeScanner,
            requiresCheckpoint = state.event?.requiresCheckpoint == true,
            onQr = {
                scanningStage = null
                if (activeScanner == state.activeStage) vm.onScanned(it)
            },
            onCancel = { scanningStage = null },
            onUnavailable = { scanningStage = null; cameraDenied = true },
        )
        return
    }
    CheckInScreen(
        state = state,
        cameraDenied = cameraDenied,
        onBack = onBack,
        onScan = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) scanningStage = state.activeStage
            else permission.launch(Manifest.permission.CAMERA)
        },
        onCode = vm::onCodeChange,
        onSubmitCode = vm::submitCode,
        onSyncNow = vm::syncNow,
        onProgress = onProgress,
        // Debug builds only: stands in for the camera reading the organiser's screen, so the QR flow (offline
        // included) can be demoed on an emulator that has no camera pointed at a second phone.
        onDebugScan = if (BuildConfig.DEBUG) {
            {
                val stage = state.activeStage
                val id = state.event?.id
                if (stage != null && id != null) container.backend.currentCode(id, stage)?.qrPayload?.let(vm::onScanned)
            }
        } else null,
    )
}

@Composable
internal fun CheckInScreen(
    state: CheckInUiState,
    cameraDenied: Boolean,
    onBack: () -> Unit,
    onScan: () -> Unit,
    onCode: (String) -> Unit,
    onSubmitCode: () -> Unit,
    onSyncNow: () -> Unit,
    onProgress: () -> Unit,
    onDebugScan: (() -> Unit)?,
) {
    val spacing = ScpdTheme.spacing
    val type = MaterialTheme.typography
    val event = state.event
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ShellTopBar("Check-in", onBack)
        if (state.isOffline) OfflineBanner(body = "You're offline. QR scans are saved on this phone and upload when you reconnect.")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ReadableWidth {
                Column(Modifier.padding(spacing.gutter), verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
                    Text(event?.title.orEmpty(), Modifier.semantics { heading() }, style = type.headlineSmall)
                    Text(
                        if (state.pointsEligible == false) state.pointsReason ?: "This event does not award points."
                        else if (event?.requiresCheckpoint == true) "This event needs an arrival scan and one checkpoint scan. You earn 3 points once both are verified."
                        else "This event needs one scan on arrival. You earn 3 points once it's verified.",
                        style = type.bodyMedium,
                    )
                    ResultBanner(state.lastResult)
                    state.identityNotice?.let { StatusBanner("No screen lock", body = it) }
                    if (cameraDenied) StatusBanner("Camera unavailable", body = "Use the backup code below, or allow the camera in Settings.", tone = BannerTone.Warning)
                    state.stages.forEachIndexed { index, stage -> StageCard(index + 1, stage) }
                    OutcomeCard(state, onSyncNow)
                    if (state.activeStage != null) BackupCodeEntry(state, onCode, onSubmitCode)
                }
            }
        }
        BottomActionBar(supporting = if (state.activeStage == null && state.outcome != AttendanceOutcome.VERIFIED) "Nothing to scan right now." else null) {
            if (state.activeStage != null) {
                PrimaryAction(
                    if (state.isSubmitting) "Checking…" else "Scan ${state.activeStage.label.lowercase()} QR",
                    enabled = !state.isSubmitting && !state.awaitingIdentity, onClick = onScan,
                )
                if (onDebugScan != null) SecondaryAction("Debug: read the organiser's live QR", onClick = onDebugScan)
            }
            if (state.outcome == AttendanceOutcome.VERIFIED) SecondaryAction("View progress", onClick = onProgress)
        }
    }
}

@Composable
private fun ResultBanner(result: AttemptResult?) {
    when (result) {
        is AttemptResult.Verified -> StatusBanner("${result.stage.label} verified", tone = BannerTone.Success)
        is AttemptResult.Saved -> StatusBanner(
            "${result.stage.label} saved on this phone",
            body = "It uploads by itself when you reconnect. Points follow once the server verifies it.",
        )
        is AttemptResult.Rejected -> StatusBanner("Not checked in", body = result.reason, tone = BannerTone.Warning)
        null -> Unit
    }
}

@Composable
private fun StageCard(number: Int, stage: StageUi) {
    val spacing = ScpdTheme.spacing
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shape = MaterialTheme.shapes.large, modifier = Modifier.testTag("stage_${stage.stage.name}")) {
        Column(Modifier.fillMaxWidth().padding(spacing.space4), verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
            Text("$number. ${stage.stage.label} scan", style = MaterialTheme.typography.titleMedium)
            StatusChip(stage.status)
            Text(stage.note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun OutcomeCard(state: CheckInUiState, onSyncNow: () -> Unit) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.surfaceContainerLowest, shape = MaterialTheme.shapes.large) {
        Column(Modifier.fillMaxWidth().padding(spacing.space4), verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
            Text("Result", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
            Text(state.outcome.label, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.testTag("outcome"))
            Text("Points earned: ${state.pointsEarned}", style = MaterialTheme.typography.bodyLarge)
            if (state.pendingUploads > 0) {
                Text(
                    if (state.pendingUploads == 1) "1 scan waiting to upload" else "${state.pendingUploads} scans waiting to upload",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (!state.isOffline) TextButton(onClick = onSyncNow) { Text("Sync now") }
            }
            state.lastSyncedLabel?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
        }
    }
}

@Composable
private fun BackupCodeEntry(state: CheckInUiState, onCode: (String) -> Unit, onSubmit: () -> Unit) {
    val spacing = ScpdTheme.spacing
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shape = MaterialTheme.shapes.large) {
        Column(Modifier.fillMaxWidth().padding(spacing.space4), verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
            Text("Camera not working?", style = MaterialTheme.typography.titleMedium)
            Text("Type the 6-digit code under the QR. Codes change every 30 seconds and need internet.", style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = state.codeInput, onValueChange = onCode,
                modifier = Modifier.fillMaxWidth().testTag("backup_code"),
                label = { Text("Backup code") }, singleLine = true,
                enabled = !state.rateLimited && !state.isOffline,
                isError = state.codeError != null,
                supportingText = {
                    Text(
                        when {
                            state.rateLimited -> "Too many wrong codes for this session. Scan the QR instead."
                            state.isOffline -> "Backup codes need internet. Scan the QR instead, or reconnect."
                            else -> state.codeError ?: "6 digits"
                        },
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            )
            SecondaryAction("Submit code", onClick = { if (!state.rateLimited && !state.isOffline) onSubmit() })
        }
    }
}

@Composable
private fun ScannerOverlay(
    stage: Stage,
    requiresCheckpoint: Boolean,
    onQr: (String) -> Unit,
    onCancel: () -> Unit,
    onUnavailable: () -> Unit,
) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    val total = if (requiresCheckpoint) 2 else 1
    val step = if (stage == Stage.Checkpoint) 2 else 1
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        QrScanner(onQr = onQr, onUnavailable = onUnavailable, modifier = Modifier.fillMaxSize())
        Box(Modifier.matchParentSize().background(colors.primary.copy(alpha = 0.62f)))
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(spacing.space4),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.space2),
        ) {
            Text(
                "Point your camera at the QR on the organiser's screen",
                color = colors.onPrimary, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center,
            )
            TagChip("${stage.label} · scan $step of $total", colors.surfaceContainerLowest, colors.primary)
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Image(
                    painterResource(R.drawable.scanner_viewfinder),
                    contentDescription = null,
                    modifier = Modifier.size(minOf(240.dp, maxHeight, maxWidth)),
                )
            }
            Text("Hold steady. It scans automatically.", color = colors.onPrimary, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            ReadableWidth {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth().heightIn(min = spacing.buttonHeight),
                    shape = PillShape,
                    border = BorderStroke(1.dp, colors.onPrimary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.onPrimary),
                ) { Text("Cancel") }
            }
        }
    }
}
