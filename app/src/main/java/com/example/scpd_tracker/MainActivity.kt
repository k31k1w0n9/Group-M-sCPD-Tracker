package com.example.scpd_tracker

import android.graphics.Color
import android.content.Intent
import com.example.scpd_tracker.data.system.NotificationNavigation
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.scpd_tracker.ui.navigation.ScpdNavHost
import com.example.scpd_tracker.ui.theme.ScpdTheme

/** FragmentActivity (a ComponentActivity) because BiometricPrompt needs one for the check-in identity check. */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Fresh launches only: after a rotation the same intent is still here and would reopen the booking.
        if (savedInstanceState == null) {
            NotificationNavigation.targetEventId.value = intent.getStringExtra(NotificationNavigation.EXTRA_EVENT_ID)
        }
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        setContent {
            ScpdTheme {
                ScpdNavHost()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        NotificationNavigation.targetEventId.value = intent.getStringExtra(NotificationNavigation.EXTRA_EVENT_ID)
    }
}
