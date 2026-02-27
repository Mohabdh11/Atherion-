package com.aetherion.noc.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.aetherion.noc.core.security.SecurityManager
import com.aetherion.noc.core.security.SessionState
import com.aetherion.noc.presentation.auth.AuthViewModel
import com.aetherion.noc.presentation.common.navigation.*
import com.aetherion.noc.presentation.common.theme.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// ═══════════════════════════════════════════════════════════════════════════
// Aetherion Mobile NOC — Main Activity
// Developer: Mohammad Abdalftah Ibrahime
// ═══════════════════════════════════════════════════════════════════════════

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var securityManager: SecurityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AetherionNOCTheme {
                AetherionApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Check session timeout on every app resume
        securityManager.checkSessionTimeout()
    }
}

// ─── App Shell ────────────────────────────────────────────────────────────────

@Composable
private fun AetherionApp() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val isAuthenticated by authViewModel.isAuthenticated.collectAsStateWithLifecycle()

    val currentRoute = navController
        .currentBackStackEntryAsState()
        .value
        ?.destination
        ?.route

    val showBottomNav = isAuthenticated && currentRoute !in listOf(
        Screen.Login.route
    )

    Scaffold(
        containerColor = AetherionColors.SurfaceDark,
        bottomBar = {
            if (showBottomNav) {
                AetherionBottomBar(navController = navController, currentRoute = currentRoute)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AetherionNavGraph(
                navController = navController,
                authViewModel = authViewModel
            )
        }
    }
}

// ─── Bottom Navigation Bar ────────────────────────────────────────────────────

private data class NavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon
)

private val NAV_ITEMS = listOf(
    NavItem(Screen.Dashboard,   "Dashboard",  Icons.Outlined.Dashboard),
    NavItem(Screen.AlertCenter, "Alerts",     Icons.Outlined.Notifications),
    NavItem(Screen.Topology,    "Topology",   Icons.Outlined.Hub),
    NavItem(Screen.AiInsights,  "AI",         Icons.Outlined.Psychology),
    NavItem(Screen.GeoNetwork,  "Geo",        Icons.Outlined.Map)
)

@Composable
private fun AetherionBottomBar(
    navController: NavHostController,
    currentRoute: String?
) {
    NavigationBar(
        containerColor = AetherionColors.SurfaceCard,
        tonalElevation = 0.dp
    ) {
        NAV_ITEMS.forEach { item ->
            val selected = currentRoute == item.screen.route

            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.screen.route) {
                        popUpTo(Screen.Dashboard.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(item.label, style = MaterialTheme.typography.labelSmall)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = AetherionColors.AetherBlue,
                    selectedTextColor   = AetherionColors.AetherBlue,
                    unselectedIconColor = AetherionColors.TextSecondary,
                    unselectedTextColor = AetherionColors.TextSecondary,
                    indicatorColor      = AetherionColors.AetherBlueDark.copy(alpha = 0.2f)
                )
            )
        }
    }
}
