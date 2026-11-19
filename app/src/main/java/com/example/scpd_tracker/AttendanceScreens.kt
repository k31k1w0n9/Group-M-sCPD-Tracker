package com.example.scpd_tracker.attendance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * ONE-LINE HOOK: call AttendanceDemoScreen() from your navigation
 * (or temporarily from MainActivity's setContent, inside the app theme).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceDemoScreen() {
    var tab by remember { mutableStateOf(0) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Text("📷") },
                    label = { Text("Check-in") }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Text("🏆") },
                    label = { Text("Progress") }
                )
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (tab == 0) CheckInScreen() else ProgressScreen()
        }
    }
}

private fun stageText(s: StageStatus) = when (s) {
    StageStatus.NOT_SCANNED -> "Not scanned"
    StageStatus.PENDING_SYNC -> "Pending sync (saved on this phone)"
    StageStatus.VERIFIED -> "Verified"
    StageStatus.REJECTED -> "Rejected"
}

@Composable
fun CheckInScreen() {
    val record = AttendanceStore.records.first { it.eventId == AttendanceStore.LIVE_EVENT_ID }
    val arrivalScanned = record.arrival != StageStatus.NOT_SCANNED
    val checkpointScanned = record.checkpoint != StageStatus.NOT_SCANNED

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Check-in", style = MaterialTheme.typography.headlineMedium)
        Text(record.eventTitle, style = MaterialTheme.typography.titleMedium)
        Text(
            "This event needs an arrival scan and one checkpoint scan. " +
                "You earn $POINTS_PER_EVENT points only after both are verified.",
            style = MaterialTheme.typography.bodyMedium
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Simulate offline", modifier = Modifier.weight(1f))
            Switch(
                checked = AttendanceStore.offline,
                onCheckedChange = { AttendanceStore.offline = it }
            )
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("1. Arrival scan", style = MaterialTheme.typography.titleSmall)
                Text(stageText(record.arrival))
                Button(
                    onClick = { AttendanceStore.scanArrival(record.eventId) },
                    enabled = !arrivalScanned,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Scan arrival QR (demo)") }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("2. Checkpoint scan", style = MaterialTheme.typography.titleSmall)
                Text(stageText(record.checkpoint))
                // A pending arrival does not block the checkpoint scanner.
                Button(
                    onClick = { AttendanceStore.scanCheckpoint(record.eventId) },
                    enabled = arrivalScanned && !checkpointScanned,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Scan checkpoint QR (demo)") }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Result", style = MaterialTheme.typography.titleSmall)
                Text(record.outcome.label, style = MaterialTheme.typography.titleLarge)
                Text("Points earned: ${record.points}")
            }
        }

        OutlinedButton(
            onClick = {
                AttendanceStore.offline = false
                AttendanceStore.syncPending()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Reconnect and sync now (demo)") }
    }
}

@Composable
fun ProgressScreen() {
    val records = AttendanceStore.records.sortedBy { it.weeksAgo }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Progress", style = MaterialTheme.typography.headlineMedium) }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(Modifier.weight(1f)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Points")
                        Text("${AttendanceStore.totalPoints}", style = MaterialTheme.typography.headlineLarge)
                    }
                }
                Card(Modifier.weight(1f)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Weekly streak")
                        Text(
                            "${AttendanceStore.streakWeeks} wk",
                            style = MaterialTheme.typography.headlineLarge
                        )
                    }
                }
            }
        }

        item { Text("Attendance history", style = MaterialTheme.typography.titleMedium) }

        items(records, key = { it.eventId }) { r ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(r.eventTitle, style = MaterialTheme.typography.titleSmall)
                    Text(r.outcome.label)
                    Text(if (r.points > 0) "+${r.points} points" else "0 points")
                }
            }
        }
    }
}
