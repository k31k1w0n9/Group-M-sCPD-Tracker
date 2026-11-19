package com.example.scpd_tracker.ui.committee

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.scpd_tracker.ui.components.LocalSnackbar
import com.example.scpd_tracker.ui.components.ShowMessages
import com.example.scpd_tracker.ui.scpdViewModel
import com.example.scpd_tracker.ui.theme.ScpdTheme

object CommitteeRoutes {
    const val DASHBOARD = "dashboard"
    const val CHECK_IN_HUB = "checkInHub"
    const val ACCOUNT = "account"
    const val EVENT_FORM = "eventForm?eventId={eventId}"
    const val CHECK_IN = "committeeCheckIn/{eventId}"
    const val ATTENDEES = "attendees/{eventId}"

    fun eventForm(id: String? = null) = if (id == null) "eventForm" else "eventForm?eventId=$id"
    fun checkIn(id: String) = "committeeCheckIn/$id"
    fun attendees(id: String) = "attendees/$id"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val Tabs = listOf(
    Tab(CommitteeRoutes.DASHBOARD, "Events", Icons.Rounded.Event),
    Tab(CommitteeRoutes.CHECK_IN_HUB, "Check-in", Icons.Rounded.QrCode2),
    Tab(CommitteeRoutes.ACCOUNT, "Account", Icons.Rounded.AccountCircle),
)

private fun NavHostController.openTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

/** The club committee app: simpler than the student side, but it covers the whole event lifecycle. */
@Composable
fun CommitteeShell(onSignOut: () -> Unit) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val tab = Tabs.firstOrNull { it.route == entry?.destination?.route }
    val snackbar = remember { SnackbarHostState() }
    CompositionLocalProvider(LocalSnackbar provides snackbar) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding()) {
            Box(Modifier.weight(1f)) {
                NavHost(nav, startDestination = CommitteeRoutes.DASHBOARD) {
                    composable(CommitteeRoutes.DASHBOARD) {
                        val vm = scpdViewModel { c, _ -> DashboardViewModel(c.events, c.auth, c.network, c.clock) }
                        val state by vm.state.collectAsStateWithLifecycle()
                        DashboardScreen(
                            state,
                            onCreate = { nav.navigate(CommitteeRoutes.eventForm()) },
                            onOpen = { nav.navigate(CommitteeRoutes.eventForm(it)) },
                            onBookings = { nav.navigate(CommitteeRoutes.attendees(it)) },
                        )
                    }
                    composable(CommitteeRoutes.CHECK_IN_HUB) {
                        val vm = scpdViewModel { c, _ -> DashboardViewModel(c.events, c.auth, c.network, c.clock) }
                        val state by vm.state.collectAsStateWithLifecycle()
                        CheckInHubScreen(state) { nav.navigate(CommitteeRoutes.checkIn(it)) }
                    }
                    composable(CommitteeRoutes.ACCOUNT) {
                        val user by (LocalContext.current.applicationContext as com.example.scpd_tracker.ScpdApplication).container.auth.currentUser.collectAsStateWithLifecycle()
                        CommitteeAccountScreen(user?.displayName.orEmpty(), user?.email.orEmpty(), onSignOut)
                    }
                    composable(
                        CommitteeRoutes.EVENT_FORM,
                        arguments = listOf(navArgument("eventId") { type = NavType.StringType; nullable = true; defaultValue = null }),
                    ) {
                        val vm = scpdViewModel { c, s -> EventFormViewModel(c.events, c.auth, c.clock, s) }
                        val state by vm.state.collectAsStateWithLifecycle()
                        ShowMessages(vm.messages.messages)
                        LaunchedEffect(state.finished) { if (state.finished) nav.popBackStack() }
                        EventFormScreen(state, vm, onBack = { nav.popBackStack() })
                    }
                    composable(CommitteeRoutes.CHECK_IN, arguments = listOf(navArgument("eventId") { type = NavType.StringType })) {
                        val vm = scpdViewModel { c, s -> CommitteeCheckInViewModel(c.events, c.attendance, c.network, c.clock, s) }
                        val state by vm.state.collectAsStateWithLifecycle()
                        KeepScreenOn(state.sessionOpen)
                        CommitteeCheckInScreen(
                            state, onBack = { nav.popBackStack() },
                            onOpen = vm::openCheckIn, onOpenCheckpoint = vm::openCheckpoint, onEnd = vm::endCheckIn,
                            onAttendees = { state.event?.let { nav.navigate(CommitteeRoutes.attendees(it.id)) } },
                        )
                    }
                    composable(CommitteeRoutes.ATTENDEES, arguments = listOf(navArgument("eventId") { type = NavType.StringType })) {
                        val vm = scpdViewModel { c, s -> AttendeesViewModel(c.events, c.attendance, c.bookings, s) }
                        val state by vm.state.collectAsStateWithLifecycle()
                        AttendeesScreen(state, vm::onFilter, onBack = { nav.popBackStack() })
                    }
                }
                SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
            }
            if (tab != null) CommitteeNavigationBar(tab.route) { nav.openTab(it) }
        }
    }
}

/** The organiser's phone stays awake while it shows the live QR. */
@Composable
private fun KeepScreenOn(on: Boolean) {
    val activity = LocalContext.current as? Activity ?: return
    DisposableEffect(on) {
        if (on) activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
}

@Composable
private fun CommitteeNavigationBar(selected: String, onSelect: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val muted = ScpdTheme.colors.textTertiary
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = colors.onSurface, selectedTextColor = colors.onSurface, indicatorColor = colors.secondaryContainer,
        unselectedIconColor = muted, unselectedTextColor = muted,
    )
    Column {
        HorizontalDivider(color = colors.outlineVariant)
        NavigationBar(containerColor = colors.surfaceContainerLowest, tonalElevation = 0.dp) {
            Tabs.forEach { tab ->
                NavigationBarItem(
                    selected = tab.route == selected, onClick = { onSelect(tab.route) },
                    icon = { Icon(tab.icon, contentDescription = null) },
                    label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) }, colors = itemColors,
                )
            }
        }
    }
}
