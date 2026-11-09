package com.example.scpd_tracker

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private enum class AppScreen { RolePicker, SignIn, PasswordHelp, Interests, Home, Committee }

@Composable
internal fun ScpdApp() {
    val context = LocalContext.current
    val view = LocalView.current
    val preferences = context.getSharedPreferences("scpd_preferences", 0)
    var screen by rememberSaveable { mutableStateOf(AppScreen.RolePicker) }
    var role by rememberSaveable { mutableStateOf(AccountRole.Student) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<DemoSignInResult?>(null) }
    var interests by rememberSaveable {
        mutableStateOf(preferences.getString("interests", null)?.split("|") ?: listOf("Tech", "Career"))
    }
    SideEffect {
        val activity = context as? Activity
        if (activity != null) {
            val insets = WindowCompat.getInsetsController(activity.window, view)
            insets.isAppearanceLightStatusBars = screen == AppScreen.Interests || screen == AppScreen.PasswordHelp
            insets.isAppearanceLightNavigationBars = true
        }
    }

    fun changeScreen(next: AppScreen) {
        screen = next
        password = ""
        error = null
    }

    // Completed actions are confirmed with a short snackbar instead of a confirmation dialog.
    val snackbar = remember { SnackbarHostState() }
    var notice by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(notice) {
        val message = notice ?: return@LaunchedEffect
        snackbar.showSnackbar(message)
        notice = null
    }

    fun signOut() {
        email = ""
        changeScreen(AppScreen.RolePicker)
        notice = "You've signed out."
    }

    BackHandler(screen == AppScreen.SignIn || screen == AppScreen.PasswordHelp) {
        when (screen) {
            AppScreen.PasswordHelp -> changeScreen(AppScreen.SignIn)
            else -> changeScreen(AppScreen.RolePicker)
        }
    }

    // The theme itself is provided once, by ScpdTheme in MainActivity.
    Box(Modifier.fillMaxSize()) {
        Surface(
            Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ) {
            when (screen) {
                AppScreen.RolePicker -> RolePicker(
                    selected = role,
                    onSelect = { role = it },
                    onContinue = {
                        email = ""
                        changeScreen(AppScreen.SignIn)
                    },
                )
                AppScreen.SignIn -> SignIn(
                    role = role,
                    email = email,
                    password = password,
                    error = error,
                    onEmail = {
                        email = it
                        error = null
                    },
                    onPassword = {
                        password = it
                        error = null
                    },
                    onSignIn = {
                        // shortcut: local fixture authentication, replace with backend authentication before release.
                        when (val result = DemoAccounts.signIn(role, email, password)) {
                            is DemoSignInResult.SignedIn -> changeScreen(
                                when {
                                    result.role == AccountRole.Committee -> AppScreen.Committee
                                    !preferences.contains("interests") -> AppScreen.Interests
                                    else -> AppScreen.Home
                                },
                            )
                            else -> error = result
                        }
                    },
                    onChangeRole = { changeScreen(AppScreen.RolePicker) },
                    onPasswordHelp = { changeScreen(AppScreen.PasswordHelp) },
                    onContinueAsStudent = {
                        role = AccountRole.Student
                        changeScreen(AppScreen.SignIn)
                    },
                    onReturnToSignIn = { changeScreen(AppScreen.SignIn) },
                )
                AppScreen.PasswordHelp -> PasswordHelp { changeScreen(AppScreen.SignIn) }
                AppScreen.Interests -> InterestsScreen(
                    selected = interests,
                    onChange = { interests = it },
                    onBack = {
                        if (preferences.contains("interests")) changeScreen(AppScreen.Home) else signOut()
                    },
                    onContinue = {
                        val editing = preferences.contains("interests")
                        preferences.edit().putString("interests", interests.joinToString("|")).apply()
                        changeScreen(AppScreen.Home)
                        if (editing) notice = "Interests saved. Recommendations updated."
                    },
                )
                AppScreen.Home -> StudentHome(
                    interests = interests,
                    snackbar = snackbar,
                    onEditInterests = { changeScreen(AppScreen.Interests) },
                    onSignOut = ::signOut,
                )
                AppScreen.Committee -> CommitteeLanding(onSignOut = ::signOut)
            }
        }
        // Student Home anchors the same snackbar above its own navigation bar.
        if (screen != AppScreen.Home) {
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().imePadding())
        }
    }
}
