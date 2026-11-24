package com.example.scpd_tracker.ui.student.progress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RemoveCircle
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.scpd_tracker.R
import com.example.scpd_tracker.domain.Policy
import com.example.scpd_tracker.domain.outcome
import com.example.scpd_tracker.domain.points
import com.example.scpd_tracker.model.AttendanceOutcome
import com.example.scpd_tracker.model.AttendanceRecord
import com.example.scpd_tracker.ui.components.*
import com.example.scpd_tracker.ui.scpdViewModel
import com.example.scpd_tracker.ui.student.ShellTopBar
import com.example.scpd_tracker.ui.theme.ScpdTheme

@Composable
internal fun ProgressRoute(onHistory: () -> Unit) {
    val vm = scpdViewModel { c, _ -> ProgressViewModel(c.attendance, c.progress, c.events, c.auth, c.clock) }
    val state by vm.state.collectAsStateWithLifecycle()
    ProgressScreen(state, onHistory)
}

@Composable
internal fun AttendanceHistoryRoute(onBack: () -> Unit) {
    val vm = scpdViewModel { c, _ -> ProgressViewModel(c.attendance, c.progress, c.events, c.auth, c.clock) }
    val state by vm.state.collectAsStateWithLifecycle()
    AttendanceHistoryScreen(state, onBack)
}

/** Points towards the next milestone, interest stamps, the points ledger and the weekly streak. */
@Composable
internal fun ProgressScreen(state: ProgressUiState, onHistory: () -> Unit) {
    val spacing = ScpdTheme.spacing
    CampusTabScaffold(
        title = "Progress",
        subtitle = "Every event, a step forward.",
        mascot = CampusMascot.Garden,
    ) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ReadableWidth {
                Column(Modifier.padding(spacing.gutter), verticalArrangement = Arrangement.spacedBy(spacing.space4)) {
                    PointsCard(state)
                    state.pendingNote?.let { StatusBanner("Not counted yet", body = it, modifier = Modifier.testTag("pending_note")) }
                    Text("Your interest stamps", Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
                    StampGrid(state.stamps, state.interests)
                    LedgerCard(state.entries)
                    StreakCard(state.streakWeeks, state.recentWeeks)
                    PrimaryAction("View attendance history", onClick = onHistory)
                }
            }
        }
    }
}

@Composable
private fun ProgressCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainerLowest, shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(ScpdTheme.spacing.space5), verticalArrangement = Arrangement.spacedBy(ScpdTheme.spacing.space4), content = content)
    }
}

@Composable
private fun PointsCard(state: ProgressUiState) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    ProgressCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.space5)) {
            Box(Modifier.size(112.dp).clearAndSetSemantics { contentDescription = "${state.total} of ${state.milestone} verified points" }, contentAlignment = Alignment.Center) {
                // Teal 700 for the arc: the brand teal is under 3:1 on white, so it can't carry meaning.
                CircularProgressIndicator(
                    progress = { (state.total.toFloat() / state.milestone).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxSize(), color = colors.onSecondaryContainer, trackColor = colors.outlineVariant,
                    strokeWidth = 10.dp, strokeCap = StrokeCap.Round,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${state.total}/${state.milestone}", style = type.headlineSmall, modifier = Modifier.testTag("points_total"))
                    Text("points", style = type.bodySmall, color = colors.onSurfaceVariant)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
                Text("Next milestone", style = type.labelMedium, color = colors.onSurfaceVariant)
                Text("${state.milestone - state.total} points to go", style = type.titleLarge)
                if (state.pendingCount > 0) TagChip("${state.pendingCount} awaiting sync", colors.tertiaryContainer, colors.onTertiaryContainer, Icons.Rounded.Schedule)
            }
        }
        Text(state.latestCredit ?: "Your first verified event earns ${Policy.POINTS_PER_EVENT} points.", style = type.bodyMedium, color = colors.onSurfaceVariant)
    }
}

/** Six categories, the student's own interests first. Unearned stamps are outlined instead of filled. */
@Composable
private fun StampGrid(stamps: Map<String, Int>, interests: List<String>) {
    val spacing = ScpdTheme.spacing
    val ordered = Policy.INTEREST_CATEGORIES.sortedBy { if (it in interests) 0 else 1 }
    BoxWithConstraints {
        val columns = if (maxWidth >= 340.dp) 3 else 2
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
            ordered.chunked(columns).forEach { row ->
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(spacing.space3)) {
                    row.forEach { StampTile(it, stamps[it] ?: 0, Modifier.weight(1f).fillMaxHeight()) }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun StampTile(category: String, count: Int, modifier: Modifier) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    val earned = count > 0
    Surface(
        modifier.semantics(mergeDescendants = true) {},
        color = if (earned) colors.secondaryContainer else colors.surfaceContainerLowest,
        shape = MaterialTheme.shapes.medium,
        border = if (earned) null else BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = spacing.space2, vertical = spacing.space3), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(spacing.space1)) {
            Box(Modifier.size(36.dp).background(if (earned) colors.surfaceContainerLowest else colors.secondaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                Icon(tagIcon(category), contentDescription = null, Modifier.size(20.dp), tint = colors.onSecondaryContainer)
            }
            Text(category, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                when (count) { 0 -> "Not earned yet"; 1 -> "1 stamp"; else -> "$count stamps" },
                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, textAlign = TextAlign.Center,
            )
        }
    }
}

/** Every award and reversal, newest first. A reversal keeps the office's reason (R5). */
@Composable
private fun LedgerCard(entries: List<LedgerRowUi>) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    ProgressCard {
        Text("Points history", Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge)
        if (entries.isEmpty()) Text("No verified events yet.", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        entries.forEachIndexed { index, row ->
            if (index > 0) HorizontalDivider(color = colors.outlineVariant)
            Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}.testTag("ledger_row"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.space3)) {
                Icon(
                    if (row.points >= 0) Icons.Rounded.CheckCircle else Icons.Rounded.RemoveCircle, contentDescription = null,
                    tint = if (row.points >= 0) colors.onSecondaryContainer else colors.error,
                )
                Column(Modifier.weight(1f)) {
                    Text(row.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${row.date} · ${row.reason}", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                Text(if (row.points > 0) "+${row.points}" else "${row.points}", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
internal fun StreakCard(
    streak: Int,
    weeks: List<Boolean>,
    onHistory: (() -> Unit)? = null,
    mascot: @Composable () -> Unit = {
        Image(painterResource(R.drawable.gilbert), contentDescription = null, Modifier.size(72.dp))
    },
) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    ProgressCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Weekly streak", Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleLarge)
            if (onHistory != null) TextButton(onClick = onHistory) { Text("Attendance history") }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.space4)) {
            mascot()
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
                Text(when (streak) { 0 -> "No streak yet"; 1 -> "1-week streak"; else -> "$streak-week streak" }, style = MaterialTheme.typography.headlineSmall)
                if (weeks.last()) TagChip("This week: verified", colors.secondaryContainer, colors.onSecondaryContainer, Icons.Rounded.Check)
                else TagChip("This week: not yet", colors.primaryContainer, colors.onSurfaceVariant, Icons.Rounded.Schedule)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            weeks.forEachIndexed { index, verified -> WeekDot(weeks.size - 1 - index, verified) }
        }
        Text("Weeks with no approved events, like exam weeks, don't break your streak.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
    }
}

@Composable
private fun WeekDot(weeksAgo: Int, verified: Boolean) {
    val colors = MaterialTheme.colorScheme
    val label = if (weeksAgo == 0) "This wk" else "Wk -$weeksAgo"
    val spoken = when (weeksAgo) { 0 -> "This week"; 1 -> "1 week ago"; else -> "$weeksAgo weeks ago" }
    Column(
        Modifier.clearAndSetSemantics { contentDescription = "$spoken: ${if (verified) "verified" else "no verified event"}" },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(ScpdTheme.spacing.space1),
    ) {
        val dot = Modifier.size(40.dp)
        Box(if (verified) dot.background(colors.onSecondaryContainer, CircleShape) else dot.border(1.dp, colors.outline, CircleShape), contentAlignment = Alignment.Center) {
            Icon(if (verified) Icons.Rounded.Check else Icons.Rounded.Remove, contentDescription = null, Modifier.size(20.dp), tint = if (verified) colors.onPrimary else colors.onSurfaceVariant)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
    }
}

/** Every attendance record with its outcome, each stage and the reason when no points were earned. */
@Composable
internal fun AttendanceHistoryScreen(state: ProgressUiState, onBack: () -> Unit) {
    val spacing = ScpdTheme.spacing
    val verified = state.records.count { it.outcome == AttendanceOutcome.VERIFIED }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ShellTopBar("Attendance history", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ReadableWidth {
                Column(Modifier.padding(spacing.gutter), verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
                    Text("$verified verified · ${state.total} points", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (state.records.isEmpty()) Text("No attendance yet. Your check-ins appear here after each event.", style = MaterialTheme.typography.bodyLarge)
                    state.records.forEach { HistoryCard(it) }
                }
            }
        }
    }
}

private data class OutcomeStyle(val container: Color, val content: Color, val icon: ImageVector, val note: String?)

@Composable
private fun outcomeStyle(record: AttendanceRecord): OutcomeStyle {
    val colors = MaterialTheme.colorScheme
    return when (record.outcome) {
        AttendanceOutcome.VERIFIED -> OutcomeStyle(colors.secondaryContainer, colors.onSecondaryContainer, Icons.Rounded.CheckCircle, null)
        AttendanceOutcome.PENDING_SYNC -> OutcomeStyle(colors.tertiaryContainer, colors.onTertiaryContainer, Icons.Rounded.CloudUpload, "Saved on this phone. Points follow after sync.")
        AttendanceOutcome.UNDER_REVIEW -> OutcomeStyle(colors.tertiaryContainer, colors.onTertiaryContainer, Icons.Rounded.WarningAmber, "The s-CPD office is checking this record. Points are held until it is cleared.")
        AttendanceOutcome.INCOMPLETE -> OutcomeStyle(colors.tertiaryContainer, colors.onTertiaryContainer, Icons.Rounded.WarningAmber, "Checkpoint scan missing, so no points.")
        AttendanceOutcome.REJECTED -> OutcomeStyle(colors.errorContainer, colors.onErrorContainer, Icons.Rounded.Block, record.rejectReason ?: "Scan rejected. Contact the organiser.")
        AttendanceOutcome.NO_SHOW -> OutcomeStyle(colors.primaryContainer, colors.onSurfaceVariant, Icons.Rounded.Remove, "No scan was received.")
        AttendanceOutcome.IN_PROGRESS -> OutcomeStyle(colors.primaryContainer, colors.onSurface, Icons.Rounded.Schedule, null)
    }
}

@Composable
private fun HistoryCard(record: AttendanceRecord) {
    val spacing = ScpdTheme.spacing
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val style = outcomeStyle(record)
    Surface(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, color = colors.surfaceContainerLowest, shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(spacing.space4), verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(spacing.space3)) {
                Column(Modifier.weight(1f)) {
                    Text(record.eventTitle, style = type.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(record.eventDate.label(), style = type.bodySmall, color = colors.onSurfaceVariant)
                }
                val settled = record.outcome != AttendanceOutcome.IN_PROGRESS && record.outcome != AttendanceOutcome.PENDING_SYNC &&
                    record.outcome != AttendanceOutcome.UNDER_REVIEW
                if (settled) Text(if (record.points > 0) "+${record.points}" else "0", style = type.titleMedium)
            }
            TagChip(record.outcome.label, style.container, style.content, style.icon)
            if (record.outcome == AttendanceOutcome.VERIFIED && !record.pointsEligible) {
                Text(record.pointsReason ?: "Attendance verified · no points awarded", style = type.bodySmall)
            }
            Text(
                if (record.requiresCheckpoint) "Arrival: ${record.arrival.label} · Checkpoint: ${record.checkpoint.label}" else "Arrival: ${record.arrival.label}",
                style = type.bodyMedium, color = colors.onSurfaceVariant,
            )
            style.note?.let { Text(it, style = type.bodyMedium) }
        }
    }
}
