package com.example.scpd_tracker.ui.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/** The shell's snackbar, so any screen can confirm a finished action without its own host. */
val LocalSnackbar = staticCompositionLocalOf { SnackbarHostState() }

/**
 * One-off messages from a ViewModel ("Booking cancelled"). They are events, not state: each is shown once
 * and never replayed after rotation (MVVM lecture, "State vs events").
 */
class MessageQueue {
    private val channel = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = channel.receiveAsFlow()
    fun send(message: String) {
        channel.trySend(message)
    }
}

@Composable
fun ShowMessages(messages: Flow<String>) {
    val snackbar = LocalSnackbar.current
    LaunchedEffect(messages) { messages.collect { snackbar.showSnackbar(it) } }
}
