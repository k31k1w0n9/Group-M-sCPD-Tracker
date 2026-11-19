package com.example.scpd_tracker

import android.graphics.Color
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
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        setContent {
            ScpdTheme {
                ScpdNavHost()
            }
        }
    }
}
