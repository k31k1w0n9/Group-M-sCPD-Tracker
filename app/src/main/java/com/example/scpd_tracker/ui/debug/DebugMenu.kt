package com.example.scpd_tracker.ui.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.scpd_tracker.BuildConfig
import com.example.scpd_tracker.ScpdApplication
import com.example.scpd_tracker.model.ReviewStatus
import com.example.scpd_tracker.ui.components.ReadableWidth
import com.example.scpd_tracker.ui.components.SecondaryAction
import com.example.scpd_tracker.ui.theme.ScpdTheme
import java.time.Duration

/**
 * Debug builds only. Controls for the demo and for testing on the emulator:
 * simulate offline, move time forward, and play the s-CPD office, which in the real system works in the
 * backend console (the brief allows the admin to have no Android screens).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugMenu(onDismiss: () -> Unit) {
    if (!BuildConfig.DEBUG) return
    val container = (LocalContext.current.applicationContext as ScpdApplication).container
    val backend = container.backend
    val spacing = ScpdTheme.spacing
    val offline by container.network.isForcedOffline.collectAsStateWithLifecycle()
    val events by backend.events.collectAsStateWithLifecycle()
    val user by container.auth.currentUser.collectAsStateWithLifecycle()
    val ledger by backend.ledger.collectAsStateWithLifecycle()
    val evidence by backend.evidence.collectAsStateWithLifecycle()
    var hours by remember { mutableIntStateOf((container.clock.offset.toMinutes() / 60).toInt()) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
        ReadableWidth {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(horizontal = spacing.gutter, vertical = spacing.space3),
                verticalArrangement = Arrangement.spacedBy(spacing.space3),
            ) {
                Text("Developer menu", Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineSmall)
                Text("Debug builds only. Nothing here exists in a release build.", style = MaterialTheme.typography.bodySmall)

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Simulate offline", style = MaterialTheme.typography.titleSmall)
                        Text("Same as turning off Wi-Fi and data on the emulator.", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = offline, onCheckedChange = container.network::setForcedOffline)
                }

                Text("Clock: +$hours h", style = MaterialTheme.typography.titleSmall)
                SecondaryAction("Move time forward 1 hour") {
                    container.clock.advance(Duration.ofHours(1))
                    hours++
                }

                HorizontalDivider()
                Text("s-CPD office (backend console)", style = MaterialTheme.typography.titleMedium)
                if (container.usesFirebase) {
                    // On Firebase the office edits documents directly: the Emulator UI locally, the Firebase console live.
                    Text(
                        "This build uses Firebase. Approve events, clear flags and reverse awards in the Firestore " +
                            "data viewer (Emulator UI at http://localhost:4000, or the Firebase console).",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(spacing.space6))
                    return@Column
                }
                val pending = events.values.filter { it.reviewStatus == ReviewStatus.PendingReview }
                if (pending.isEmpty()) Text("No submissions waiting for review.", style = MaterialTheme.typography.bodyMedium)
                pending.forEach { event ->
                    Text(event.title, style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.space2)) {
                        TextButton(onClick = { backend.adminDecide(event.id, ReviewStatus.Approved, null) }) { Text("Approve") }
                        TextButton(onClick = { backend.adminDecide(event.id, ReviewStatus.ChangesRequested, "Add the venue booking confirmation and resubmit.") }) { Text("Request changes") }
                        TextButton(onClick = { backend.adminDecide(event.id, ReviewStatus.Rejected, "Not eligible for s-CPD points.") }) { Text("Reject") }
                    }
                }

                val uid = user?.uid
                val lastAward = ledger.lastOrNull { it.userId == uid && it.entry.points > 0 && ledger.none { r -> r.userId == uid && r.entry.eventId == it.entry.eventId && r.entry.points < 0 } }
                if (lastAward != null && uid != null) {
                    SecondaryAction("Office: reverse award for ${lastAward.entry.eventTitle}") {
                        backend.reverseAward(uid, lastAward.entry.eventId, "Office review: attendance record found invalid.")
                    }
                }
                val flaggedEntry = evidence.entries.firstOrNull { it.key.startsWith("$uid|") && it.value.flagged != null }
                if (flaggedEntry != null && uid != null) {
                    val flaggedEventId = flaggedEntry.key.substringAfter('|')
                    SecondaryAction("Office: clear review flag for ${events[flaggedEventId]?.title ?: flaggedEventId}") {
                        backend.clearFlag(uid, flaggedEventId)
                    }
                }
                Spacer(Modifier.height(spacing.space6))
            }
        }
    }
}
