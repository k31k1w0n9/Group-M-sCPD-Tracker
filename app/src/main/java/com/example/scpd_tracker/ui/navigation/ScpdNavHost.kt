package com.example.scpd_tracker.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.scpd_tracker.model.AccountRole
import com.example.scpd_tracker.model.UserProfile
import com.example.scpd_tracker.ui.auth.AuthViewModel
import com.example.scpd_tracker.ui.auth.InterestsScreen
import com.example.scpd_tracker.ui.auth.PasswordHelp
import com.example.scpd_tracker.ui.auth.RolePicker
import com.example.scpd_tracker.ui.auth.SignIn
import com.example.scpd_tracker.ui.committee.CommitteeShell
import com.example.scpd_tracker.ui.scpdViewModel
import com.example.scpd_tracker.ui.student.StudentShell

/** Top-level destinations. Student and committee each have their own graph inside their shell. */
object Routes {
    const val ROLE_PICKER = "role"
    const val SIGN_IN = "signIn"
    const val PASSWORD_HELP = "passwordHelp"
    const val INTERESTS = "interests"
    const val STUDENT = "student"
    const val COMMITTEE = "committee"
}

/** Where a signed-in (or signed-out) user belongs. The role comes from the account, never from the picker (R6). */
internal fun homeRouteFor(user: UserProfile?): String = when {
    user == null -> Routes.ROLE_PICKER
    user.role == AccountRole.Committee -> Routes.COMMITTEE
    !user.hasChosenInterests -> Routes.INTERESTS
    else -> Routes.STUDENT
}

private fun NavHostController.resetTo(route: String) = navigate(route) {
    popUpTo(0) { inclusive = true }
    launchSingleTop = true
}

@Composable
fun ScpdNavHost() {
    val auth = scpdViewModel { c, s -> AuthViewModel(c.auth, s) }
    val user by auth.currentUser.collectAsStateWithLifecycle()
    val signIn by auth.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()

    // Signing in, signing out, or the account changing role all land on the right graph with no back stack.
    LaunchedEffect(user?.uid) {
        val target = homeRouteFor(user)
        if (nav.currentDestination?.route != target) nav.resetTo(target)
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            NavHost(nav, startDestination = homeRouteFor(user)) {
                composable(Routes.ROLE_PICKER) {
                    RolePicker(
                        selected = signIn.role,
                        onSelect = auth::selectRole,
                        onContinue = {
                            auth.clearForm()
                            nav.navigate(Routes.SIGN_IN)
                        },
                    )
                }
                composable(Routes.SIGN_IN) {
                    SignIn(
                        role = signIn.role,
                        email = signIn.email,
                        password = signIn.password,
                        error = signIn.error,
                        isSubmitting = signIn.isSubmitting,
                        onEmail = auth::onEmail,
                        onPassword = auth::onPassword,
                        onSignIn = auth::signIn,
                        onChangeRole = { nav.popBackStack() },
                        onPasswordHelp = { nav.navigate(Routes.PASSWORD_HELP) },
                        onContinueAsStudent = auth::continueAsStudent,
                        onReturnToSignIn = auth::clearForm,
                    )
                }
                composable(Routes.PASSWORD_HELP) { PasswordHelp { nav.popBackStack() } }
                composable(Routes.INTERESTS) {
                    // First-run interests. Editing later happens inside the student shell.
                    var chosen by rememberSaveable { mutableStateOf(user?.interests.orEmpty()) }
                    InterestsScreen(
                        selected = chosen,
                        onChange = { chosen = it },
                        onBack = auth::signOut,
                        onContinue = { auth.saveInterests(chosen) { nav.resetTo(Routes.STUDENT) } },
                    )
                }
                composable(Routes.STUDENT) { StudentShell(onSignOut = auth::signOut) }
                composable(Routes.COMMITTEE) { CommitteeShell(onSignOut = auth::signOut) }
            }
        }
    }
}
