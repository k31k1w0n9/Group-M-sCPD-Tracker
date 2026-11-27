package com.example.scpd_tracker.ui.student

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.scpd_tracker.ui.auth.InterestsScreen
import com.example.scpd_tracker.ui.components.BackButton
import com.example.scpd_tracker.ui.components.LocalSnackbar
import com.example.scpd_tracker.ui.scpdViewModel
import com.example.scpd_tracker.ui.student.checkin.CheckInRoute
import com.example.scpd_tracker.ui.student.details.EventDetailsRoute
import com.example.scpd_tracker.ui.student.home.HomeScreen
import com.example.scpd_tracker.ui.student.home.HomeViewModel
import com.example.scpd_tracker.ui.student.myevents.MyEventsRoute
import com.example.scpd_tracker.ui.student.profile.ProfileRoute
import com.example.scpd_tracker.ui.student.notifications.NotificationsRoute
import com.example.scpd_tracker.data.system.NotificationNavigation
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import com.example.scpd_tracker.ui.student.profile.ProfileViewModel
import com.example.scpd_tracker.ui.student.progress.AttendanceHistoryRoute
import com.example.scpd_tracker.ui.student.progress.ProgressRoute
import com.example.scpd_tracker.ui.student.settings.SettingsScreen
import com.example.scpd_tracker.ui.theme.ScpdTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

object StudentRoutes {
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val MY_EVENTS = "myEvents"
    const val PROGRESS = "progress"
    const val PROFILE = "profile"
    const val EVENT = "event/{eventId}"
    const val CHECK_IN = "checkIn/{eventId}"
    const val HISTORY = "history"
    const val EDIT_INTERESTS = "editInterests"
    const val SETTINGS = "settings"
    const val NOTIFICATIONS = "notifications"
    const val NOTIFICATION_EVENT = "notificationEvent/{eventId}"

    fun notificationEvent(id: String) = "notificationEvent/${Uri.encode(id)}"

    fun event(id: String) = "event/$id"
    fun checkIn(id: String) = "checkIn/$id"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val Tabs = listOf(
    Tab(StudentRoutes.HOME, "Home", Icons.Rounded.Home),
    Tab(StudentRoutes.EXPLORE, "Explore", Icons.Rounded.Explore),
    Tab(StudentRoutes.MY_EVENTS, "My Events", Icons.Rounded.ConfirmationNumber),
    Tab(StudentRoutes.PROGRESS, "Progress", Icons.Rounded.Insights),
    Tab(StudentRoutes.PROFILE, "Profile", Icons.Rounded.AccountCircle),
)

private fun NavHostController.openTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

/**
 * The student app. Tabs keep their own back stack; detail screens (event, check-in) hide the bar so the
 * sticky action at the bottom never sits on top of the navigation.
 */
@Composable
fun StudentShell(onSignOut: () -> Unit) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        // Focus can move from the removed sign-in form to the first field in the new screen.
        withFrameNanos { }
        focus.clearFocus(force = true)
        keyboard?.hide()
    }
    // Notifications are not asked for here: see ui/components/NotificationPermission.kt (asked in context instead).
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val tab = Tabs.firstOrNull { it.route == route } ?: when (route) {
        StudentRoutes.NOTIFICATIONS, StudentRoutes.SETTINGS -> Tabs.first { it.route == StudentRoutes.PROFILE }
        StudentRoutes.NOTIFICATION_EVENT -> Tabs.first { it.route == StudentRoutes.MY_EVENTS }
        else -> null
    }
    val snackbar = remember { SnackbarHostState() }
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val targetEvent by NotificationNavigation.targetEventId.collectAsStateWithLifecycle()
    LaunchedEffect(targetEvent) {
        val id = targetEvent ?: return@LaunchedEffect
        nav.navigate(StudentRoutes.notificationEvent(id)) { launchSingleTop = true }
        NotificationNavigation.targetEventId.value = null
    }

    CompositionLocalProvider(LocalSnackbar provides snackbar) {
        BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // From 600dp wide (phone landscape, tablets) the bottom bar becomes a side rail.
            val wide = maxWidth >= 600.dp
            if (wide) Box(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(MaterialTheme.colorScheme.primary))
            Row(Modifier.fillMaxSize().then(if (wide) Modifier.windowInsetsPadding(WindowInsets.statusBars) else Modifier)) {
                if (wide && tab != null) StudentRail(tab.route) { nav.openTab(it) }
                Column(Modifier.weight(1f).fillMaxHeight().imePadding()) {
                    Box(Modifier.weight(1f)) {
                        StudentNavHost(nav, onSignOut)
                        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
                    }
                    if (!wide && tab != null && !keyboardVisible) StudentNavigationBar(tab.route) { nav.openTab(it) }
                }
            }
        }
    }
}

@Composable
private fun StudentNavHost(nav: NavHostController, onSignOut: () -> Unit) {
    NavHost(nav, startDestination = StudentRoutes.HOME) {
        composable(StudentRoutes.HOME) {
            val vm = scpdViewModel { c, s -> HomeViewModel(c.events, c.bookings, c.attendance, c.progress, c.auth, c.network, c.clock, s, browsing = false) }
            val state by vm.state.collectAsStateWithLifecycle()
            HomeScreen(
                state, vm::onQuery, vm::onFilter, vm::clearSearch,
                onOpenEvent = { nav.navigate(StudentRoutes.event(it)) },
                onCheckIn = { nav.navigate(StudentRoutes.checkIn(it)) },
                onProgress = { nav.openTab(StudentRoutes.PROGRESS) },
                onSeeAll = { nav.openTab(StudentRoutes.EXPLORE) },
            )
        }
        composable(StudentRoutes.EXPLORE) {
            val vm = scpdViewModel { c, s -> HomeViewModel(c.events, c.bookings, c.attendance, c.progress, c.auth, c.network, c.clock, s, browsing = true) }
            val state by vm.state.collectAsStateWithLifecycle()
            HomeScreen(
                state, vm::onQuery, vm::onFilter, vm::clearSearch,
                onOpenEvent = { nav.navigate(StudentRoutes.event(it)) },
                onCheckIn = { nav.navigate(StudentRoutes.checkIn(it)) },
                onProgress = { nav.openTab(StudentRoutes.PROGRESS) },
                onSeeAll = {},
            )
        }
        composable(StudentRoutes.MY_EVENTS) {
            MyEventsRoute(
                onBrowse = { nav.openTab(StudentRoutes.EXPLORE) },
                onEditInterests = { nav.navigate(StudentRoutes.EDIT_INTERESTS) },
                onCheckIn = { nav.navigate(StudentRoutes.checkIn(it)) },
                onHistory = { nav.navigate(StudentRoutes.HISTORY) },
            )
        }
        composable(StudentRoutes.PROGRESS) { ProgressRoute(onHistory = { nav.navigate(StudentRoutes.HISTORY) }) }
        composable(StudentRoutes.HISTORY) { AttendanceHistoryRoute(onBack = { nav.popBackStack() }) }
        composable(StudentRoutes.PROFILE) {
            ProfileRoute(
                onEditInterests = { nav.navigate(StudentRoutes.EDIT_INTERESTS) },
                onNotifications = { nav.navigate(StudentRoutes.NOTIFICATIONS) },
                onSignOut = onSignOut,
                onOpenSettings = { nav.navigate(StudentRoutes.SETTINGS) },
                onHistory = { nav.navigate(StudentRoutes.HISTORY) },
                onCheckIn = { nav.navigate(StudentRoutes.checkIn(it)) },
            )
        }
        composable(StudentRoutes.NOTIFICATIONS) {
            NotificationsRoute(onBack = { nav.popBackStack() })
        }
        composable(StudentRoutes.NOTIFICATION_EVENT, arguments = listOf(navArgument("eventId") { type = NavType.StringType })) {
            MyEventsRoute(
                onBrowse = { nav.openTab(StudentRoutes.EXPLORE) },
                onEditInterests = { nav.navigate(StudentRoutes.EDIT_INTERESTS) },
                onCheckIn = { nav.navigate(StudentRoutes.checkIn(it)) },
                onHistory = { nav.navigate(StudentRoutes.HISTORY) },
                initialEventId = nav.currentBackStackEntry?.arguments?.getString("eventId"),
            )
        }
        composable(StudentRoutes.SETTINGS) {
            SettingsScreen(
                onBack = { nav.popBackStack() },
                onNotifications = { nav.navigate(StudentRoutes.NOTIFICATIONS) },
            )
        }
        composable(StudentRoutes.EDIT_INTERESTS) {
            val vm = scpdViewModel { c, _ -> ProfileViewModel(c.auth, c.attendance, c.bookings, c.events) }
            val profile by vm.state.collectAsStateWithLifecycle()
            var chosen by rememberSaveable(profile.interests) { mutableStateOf(profile.interests) }
            InterestsScreen(
                selected = chosen,
                onChange = { chosen = it },
                onBack = { nav.popBackStack() },
                onContinue = { vm.saveInterests(chosen) { nav.popBackStack() } },
            )
        }
        composable(StudentRoutes.EVENT, arguments = listOf(navArgument("eventId") { type = NavType.StringType })) {
            EventDetailsRoute(
                onBack = { nav.popBackStack() },
                onCheckIn = { nav.navigate(StudentRoutes.checkIn(it)) },
                onMyEvents = { nav.openTab(StudentRoutes.MY_EVENTS) },
            )
        }
        composable(StudentRoutes.CHECK_IN, arguments = listOf(navArgument("eventId") { type = NavType.StringType })) {
            CheckInRoute(onBack = { nav.popBackStack() }, onProgress = { nav.openTab(StudentRoutes.PROGRESS) })
        }
    }
}

@Composable
private fun NavLabel(label: String) {
    Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/** Unselected labels use text-tertiary (6.10:1) instead of #707D89 (4.21:1). */
@Composable
private fun StudentNavigationBar(selected: String, onSelect: (String) -> Unit) {
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
                    icon = { Icon(tab.icon, contentDescription = null) }, label = { NavLabel(tab.label) }, colors = itemColors,
                )
            }
        }
    }
}

@Composable
private fun StudentRail(selected: String, onSelect: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val muted = ScpdTheme.colors.textTertiary
    val itemColors = NavigationRailItemDefaults.colors(
        selectedIconColor = colors.onSurface, selectedTextColor = colors.onSurface, indicatorColor = colors.secondaryContainer,
        unselectedIconColor = muted, unselectedTextColor = muted,
    )
    Row {
        NavigationRail(
            containerColor = colors.surfaceContainerLowest,
            windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start),
        ) {
            Spacer(Modifier.weight(1f))
            Tabs.forEach { tab ->
                NavigationRailItem(
                    selected = tab.route == selected, onClick = { onSelect(tab.route) },
                    icon = { Icon(tab.icon, contentDescription = null) }, label = { NavLabel(tab.label) }, colors = itemColors,
                )
            }
            Spacer(Modifier.weight(1f))
        }
        VerticalDivider(color = colors.outlineVariant)
    }
}

/** Navy title bar for secondary screens. */
@Composable
internal fun ShellTopBar(title: String, onBack: (() -> Unit)? = null) {
    val spacing = ScpdTheme.spacing
    Surface(color = MaterialTheme.colorScheme.primary, contentColor = ScpdTheme.colors.onHero) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().heightIn(min = 56.dp)
                .padding(start = if (onBack != null) spacing.space1 else spacing.gutter, end = spacing.gutter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.space1),
        ) {
            if (onBack != null) BackButton(onBack, tint = ScpdTheme.colors.onHero)
            Text(title, Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
