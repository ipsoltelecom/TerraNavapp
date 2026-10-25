package com.example.northstar.ui.navigation

import com.example.northstar.settings.L10n
import com.example.northstar.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.northstar.ui.TerraNavIcons
import com.example.northstar.ui.screens.*
import com.example.northstar.ui.theme.*
import com.example.northstar.viewmodel.AppViewModel
import com.example.northstar.viewmodel.AuthViewModel
import com.example.northstar.viewmodel.ConnStage
import com.example.northstar.viewmodel.ConnectionState
import com.example.northstar.viewmodel.DashViewModel
import com.example.northstar.viewmodel.RouteViewModel

sealed class Screen(val route: String) {
    object Login    : Screen("login")
    object Home     : Screen("home")
    object Route    : Screen("route")
    object Dash     : Screen("dash")
    object Garage   : Screen("garage")
    object Rides    : Screen("rides")
    object Settings : Screen("settings")
}

private data class NavTab(val screen: Screen, val icon: ImageVector, val label: String)

private val bottomTabs = listOf(
    NavTab(Screen.Home,   TerraNavIcons.Home,    "Home"),
    NavTab(Screen.Route,  TerraNavIcons.Navi,    "Route"),
    NavTab(Screen.Dash,   TerraNavIcons.Dash,    "Dash"),
    NavTab(Screen.Garage, TerraNavIcons.Wrench,  "Garage"),
    NavTab(Screen.Rides,  TerraNavIcons.History, "Rides"),
)

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel = viewModel(),
    appViewModel: AppViewModel = viewModel(),
    dashViewModel: DashViewModel = viewModel(),
    routeViewModel: RouteViewModel = viewModel(),
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomNav = currentRoute in listOf(
        Screen.Home.route, Screen.Route.route, Screen.Dash.route,
        Screen.Garage.route, Screen.Rides.route,
    )

    val garageTab by appViewModel.garageTab.collectAsState()
    val routeState by routeViewModel.state.collectAsState()

    // Single source of truth for connection status: the real dash stage. Fixes Home
    // claiming L10n.text(R.string.ui_c2f9b7b4897f) while the Dash screen says it isn't.
    val dashUi by dashViewModel.ui.collectAsState()
    val updateContext = androidx.compose.ui.platform.LocalContext.current
    val updater = remember { com.example.northstar.update.AppUpdater.get(updateContext) }
    val updateState by updater.state.collectAsState()
    val rideActive = dashUi.stage !in listOf(ConnStage.OFFLINE,ConnStage.ERROR) || dashUi.hasRoute
    LaunchedEffect(rideActive) {
        updater.setRideActive(rideActive)
        if (!rideActive) updater.checkForUpdates()
    }
    val conn = when (dashUi.stage) {
        ConnStage.STREAMING -> ConnectionState.Connected
        ConnStage.WIFI, ConnStage.AUTH -> ConnectionState.Searching
        else -> ConnectionState.Offline
    }

    // Prefetch map tiles the moment a destination resolves (internet still reachable here)
    LaunchedEffect(routeState.destination?.lat, routeState.destination?.lng) {
        val d = routeState.destination
        if (d?.lat != null && d.lng != null) dashViewModel.prefetchTiles(d.lat, d.lng)
    }

    // Auto-navigate to Route when a Maps share arrives
    LaunchedEffect(routeState.pendingNavigate, currentRoute) {
        if (routeState.pendingNavigate && currentRoute != null && currentRoute != Screen.Login.route) {
            if (currentRoute != Screen.Route.route) {
                navController.navigate(Screen.Route.route) { launchSingleTop = true }
            }
            routeViewModel.onNavigated()
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        if (currentRoute == Screen.Home.route && !rideActive && updateState.release != null) {
            Text(L10n.text(R.string.ui_297aeda70f5d, updateState.release?.versionName),
                color = Gold, fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth().background(Surf1)
                    .clickable { navController.navigate(Screen.Settings.route) }.padding(14.dp))
        }
        Box(Modifier.weight(1f)) {
            NavHost(
                navController = navController,
                startDestination = Screen.Login.route,
            ) {
                composable(Screen.Login.route) {
                    LoginScreen(
                        authViewModel = authViewModel,
                        onSignedIn = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        },
                        onSkip = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        },
                    )
                }

                composable(Screen.Home.route) {
                    HomeScreen(
                        conn = conn,
                        onNavigate = { dest ->
                            when (dest) {
                                "route" -> navController.navigate(Screen.Route.route)
                                "dash" -> navController.navigate(Screen.Dash.route)
                                "garage" -> navController.navigate(Screen.Garage.route)
                                "settings" -> navController.navigate(Screen.Settings.route)
                            }
                        },
                        routeViewModel = routeViewModel,
                    )
                }

                composable(Screen.Route.route) {
                    RouteScreen(
                        routeViewModel = routeViewModel,
                        onBack = { navController.popBackStack() },
                        onSentToDash = { destName ->
                            dashViewModel.setDestination(
                                name = destName,
                                lat  = routeState.destination?.lat,
                                lng  = routeState.destination?.lng,
                            )
                            // Start navigation: open the dash view and begin the
                            // WiFi → auth → stream flow (no-op if already streaming).
                            dashViewModel.connect()
                            navController.navigate(Screen.Dash.route) {
                                popUpTo(Screen.Home.route)
                            }
                        },
                    )
                }

                composable(Screen.Dash.route) {
                    DashScreen(vm = dashViewModel)
                }

                composable(Screen.Garage.route) {
                    GarageScreen(
                        tab = garageTab,
                        onTabChange = { appViewModel.setGarageTab(it) },
                    )
                }

                composable(Screen.Rides.route) {
                    RidesScreen()
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        conn = conn,
                        onConnChange = { appViewModel.setConn(it) },
                        authViewModel = authViewModel,
                        onSignedOut = {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }

        if (showBottomNav) {
            TerraNavBottomNav(
                currentRoute = currentRoute,
                onNavSelect = { screen ->
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
    }
}

@Composable
private fun TerraNavBottomNav(
    currentRoute: String?,
    onNavSelect: (Screen) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xE60D0F11))
            .border(width = 1.dp, color = Line, shape = androidx.compose.ui.graphics.RectangleShape)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .navigationBarsPadding(),
    ) {
        bottomTabs.forEach { tab ->
            val active = currentRoute == tab.screen.route
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onNavSelect(tab.screen) }
                    .padding(vertical = 6.dp),
            ) {
                // Fixed-height container with the icon top-aligned and the active dot
                // bottom-aligned — so reserving space for the dot doesn't shove the
                // selected icon downward relative to the others.
                Box(Modifier.height(29.dp), contentAlignment = Alignment.TopCenter) {
                    Icon(
                        tab.icon, contentDescription = L10n.known(tab.label),
                        tint = if (active) Gold else TextLo,
                        modifier = Modifier.size(23.dp),
                    )
                    if (active) {
                        Box(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(Gold)
                        )
                    }
                }
                Text(
                    L10n.known(tab.label),
                    color = if (active) Gold else TextLo,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = GeistFamily,
                )
            }
        }
    }
}
