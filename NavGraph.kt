package com.aetherion.noc.presentation.common.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.aetherion.noc.presentation.ai.AiInsightsScreen
import com.aetherion.noc.presentation.alerts.AlertCenterScreen
import com.aetherion.noc.presentation.auth.LoginScreen
import com.aetherion.noc.presentation.auth.AuthViewModel
import com.aetherion.noc.presentation.dashboard.DashboardScreen
import com.aetherion.noc.presentation.device.DeviceDetailScreen
import com.aetherion.noc.presentation.geo.GeoNetworkScreen
import com.aetherion.noc.presentation.topology.TopologyScreen

// ═══════════════════════════════════════════════════════════════════════════
// Aetherion Mobile NOC — Navigation Graph
// Developer: Mohammad Abdalftah Ibrahime
// ═══════════════════════════════════════════════════════════════════════════

sealed class Screen(val route: String) {
    data object Login       : Screen("login")
    data object Dashboard   : Screen("dashboard")
    data object AlertCenter : Screen("alerts")
    data object Topology    : Screen("topology")
    data object AiInsights  : Screen("ai_insights")
    data object GeoNetwork  : Screen("geo_network")

    data object DeviceDetail : Screen("device/{deviceId}") {
        fun createRoute(deviceId: String) = "device/$deviceId"
    }

    // Deep link destinations (FCM)
    data object AlertDetail : Screen("alerts/{alertId}") {
        fun createRoute(alertId: String) = "alerts/$alertId"
    }
}

// Bottom navigation items
sealed class BottomNavItem(
    val screen: Screen,
    val label: String,
    val labelAr: String,
    val iconRes: String  // material icon name reference
) {
    data object Dashboard   : BottomNavItem(Screen.Dashboard,   "Dashboard",   "لوحة التحكم",  "dashboard")
    data object Alerts      : BottomNavItem(Screen.AlertCenter, "Alerts",      "التنبيهات",    "notifications")
    data object Topology    : BottomNavItem(Screen.Topology,    "Topology",    "الطوبولوجيا",  "hub")
    data object AI          : BottomNavItem(Screen.AiInsights,  "AI Insights", "رؤى الذكاء",  "psychology")
    data object Geo         : BottomNavItem(Screen.GeoNetwork,  "Geo View",    "الخريطة",      "map")
}

@Composable
fun AetherionNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()

    NavHost(
        navController = navController,
        startDestination = if (isAuthenticated) Screen.Dashboard.route else Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToAlerts = { navController.navigate(Screen.AlertCenter.route) },
                onNavigateToTopology = { navController.navigate(Screen.Topology.route) },
                onNavigateToDevice = { deviceId ->
                    navController.navigate(Screen.DeviceDetail.createRoute(deviceId))
                }
            )
        }

        composable(Screen.AlertCenter.route) {
            AlertCenterScreen(
                onNavigateToDevice = { deviceId ->
                    navController.navigate(Screen.DeviceDetail.createRoute(deviceId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.DeviceDetail.route,
            arguments = listOf(navArgument("deviceId") { type = NavType.StringType }),
            deepLinks = listOf(
                navDeepLink { uriPattern = "aetherion://noc/device/{deviceId}" }
            )
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: return@composable
            DeviceDetailScreen(
                deviceId = deviceId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Topology.route) {
            TopologyScreen(
                onNodeClick = { nodeId ->
                    navController.navigate(Screen.DeviceDetail.createRoute(nodeId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AiInsights.route) {
            AiInsightsScreen(
                onNavigateToDevice = { deviceId ->
                    navController.navigate(Screen.DeviceDetail.createRoute(deviceId))
                }
            )
        }

        composable(Screen.GeoNetwork.route) {
            GeoNetworkScreen()
        }

        // Alert deep link from FCM
        composable(
            route = Screen.AlertDetail.route,
            arguments = listOf(navArgument("alertId") { type = NavType.StringType }),
            deepLinks = listOf(
                navDeepLink { uriPattern = "aetherion://noc/alert/{alertId}" }
            )
        ) {
            AlertCenterScreen(onBack = { navController.popBackStack() })
        }
    }
}
