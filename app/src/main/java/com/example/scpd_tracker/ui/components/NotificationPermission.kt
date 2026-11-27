package com.example.scpd_tracker.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

/*
 * Notifications are asked for in context, never at launch (Permissions lecture, 24 Nov): once, right after the
 * student's first successful booking, and from the button on the Notifications screen. Saying no is never a dead
 * end: My Events shows the same information.
 */

fun Context.notificationsAllowed(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private const val PREFS = "scpd_permissions"
private const val ASKED_AFTER_BOOKING = "asked_notifications_after_booking"

/** The live permission state, re-checked whenever the screen comes back (the student may have changed it in Settings). */
class NotificationPermissionState(
    val allowed: Boolean,
    /** Shows the system prompt, or opens the app's notification settings if the student already said no. */
    val request: () -> Unit,
    /** Shows the system prompt once per install, after the first booking. Does nothing after that. */
    val requestAfterFirstBooking: () -> Unit,
)

@Composable
fun rememberNotificationPermission(): NotificationPermissionState {
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(context.notificationsAllowed()) }
    var deniedOnce by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        allowed = context.notificationsAllowed()
        onPauseOrDispose { }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        allowed = granted
        if (!granted) deniedOnce = true
    }
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    return NotificationPermissionState(
        allowed = allowed,
        request = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !deniedOnce) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        },
        requestAfterFirstBooking = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !allowed && !prefs.getBoolean(ASKED_AFTER_BOOKING, false)) {
                prefs.edit().putBoolean(ASKED_AFTER_BOOKING, true).apply()
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
    )
}
