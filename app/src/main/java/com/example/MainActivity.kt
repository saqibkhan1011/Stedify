package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.AuthModalSheet
import com.example.ui.navigation.Screen
import com.example.ui.screens.AchievementsScreen
import com.example.ui.screens.AiOptimizerScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.RoutineManagerScreen
import com.example.ui.theme.GeoPurpleDark
import com.example.ui.theme.GeoSecondaryContainer
import com.example.ui.theme.StedifyTheme
import com.example.ui.viewmodel.AchievementsViewModel
import com.example.ui.viewmodel.AiOptimizerViewModel
import com.example.ui.viewmodel.AnalyticsViewModel
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.DashboardViewModel
import com.example.ui.viewmodel.RoutineViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as StedifyApplication

        setContent {
            StedifyTheme {
                // Request notification permission if Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val context = LocalContext.current
                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission(),
                        onResult = { /* Permission handled */ }
                    )
                    LaunchedEffect(Unit) {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!hasPermission) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                StedifyMainApp(app = app)
            }
        }
    }
}

@Composable
fun StedifyMainApp(
    app: StedifyApplication
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Dashboard.route

    var showAuthModal by remember { mutableStateOf(false) }

    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.provideFactory(app.repository)
    )
    val routineViewModel: RoutineViewModel = viewModel(
        factory = RoutineViewModel.provideFactory(app.repository)
    )
    val analyticsViewModel: AnalyticsViewModel = viewModel(
        factory = AnalyticsViewModel.provideFactory(app.repository)
    )
    val aiOptimizerViewModel: AiOptimizerViewModel = viewModel(
        factory = AiOptimizerViewModel.provideFactory(app.repository)
    )
    val achievementsViewModel: AchievementsViewModel = viewModel(
        factory = AchievementsViewModel.provideFactory(app.gamificationRepository)
    )
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.provideFactory(app.authService, app.syncService)
    )

    val achievementsUiState by achievementsViewModel.uiState.collectAsStateWithLifecycle()
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 0.dp,
                modifier = Modifier.testTag("main_bottom_nav")
            ) {
                Screen.items.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    letterSpacing = 0.2.sp
                                ),
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier.testTag(screen.testTag)
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToAiOptimizer = {
                        navController.navigate(Screen.AiOptimizer.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenAuthSheet = { showAuthModal = true }
                )
            }

            composable(Screen.Routines.route) {
                RoutineManagerScreen(viewModel = routineViewModel)
            }

            composable(Screen.Analytics.route) {
                AnalyticsScreen(viewModel = analyticsViewModel)
            }

            composable(Screen.Achievements.route) {
                AchievementsScreen(
                    uiState = achievementsUiState,
                    viewModel = achievementsViewModel,
                    onOpenAuthSheet = { showAuthModal = true }
                )
            }

            composable(Screen.AiOptimizer.route) {
                AiOptimizerScreen(viewModel = aiOptimizerViewModel)
            }
        }
    }

    if (showAuthModal) {
        AuthModalSheet(
            uiState = authUiState,
            viewModel = authViewModel,
            onDismiss = { showAuthModal = false }
        )
    }
}
