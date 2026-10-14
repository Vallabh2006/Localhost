package com.localhost.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.localhost.core.data.repository.SettingsRepository
import com.localhost.core.designsystem.component.AppLogo
import com.localhost.core.designsystem.theme.DarkBackground
import com.localhost.core.designsystem.theme.DarkBorderSubtle
import com.localhost.core.designsystem.theme.DarkSurface
import com.localhost.core.designsystem.theme.DarkSurfaceElevated
import com.localhost.core.designsystem.theme.LocalhostTheme
import com.localhost.core.designsystem.theme.PrimaryAccent
import com.localhost.core.designsystem.theme.PrimaryAccentContainer
import com.localhost.core.designsystem.theme.TextMuted
import com.localhost.core.designsystem.theme.TextPrimary
import com.localhost.core.designsystem.theme.TextSecondary
import com.localhost.feature.files.FileExplorerScreen
import com.localhost.feature.files.FilesViewModel
import com.localhost.feature.logs.LogsScreen
import com.localhost.feature.logs.LogsViewModel
import com.localhost.feature.monitor.MonitorScreen
import com.localhost.feature.monitor.MonitorViewModel
import com.localhost.feature.projects.CreateProjectScreen
import com.localhost.feature.projects.ProjectDetailScreen
import com.localhost.feature.projects.ProjectListScreen
import com.localhost.feature.projects.ProjectsViewModel
import com.localhost.feature.settings.SettingsScreen
import com.localhost.feature.settings.SettingsViewModel
import com.localhost.feature.tunnel.TunnelScreen
import com.localhost.feature.tunnel.TunnelViewModel
import com.localhost.cli.TerminalScreen
import com.localhost.cli.TerminalViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import javax.inject.Inject

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    data object Monitor : Screen("monitor", "Monitor", Icons.Default.BarChart)
    data object Projects : Screen("projects", "Projects", Icons.Default.Folder)
    data object Terminal : Screen("terminal", "Terminal", Icons.Default.Terminal)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    data object ProjectDetail : Screen("project_detail/{projectId}", "Detail")
    data object CreateProject : Screen("create_project", "Create")
    data object Logs : Screen("logs/{projectId}", "Logs")
    data object Files : Screen("files/{projectId}", "Files")
    data object Tunnel : Screen("tunnel", "Tunnel")
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } catch (_: Exception) {}
        enableEdgeToEdge()
        try {
            val intent = Intent(this, SupervisorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (_: Exception) {}

        setContent {
            val accentName by settingsRepository.accentColorName.collectAsState(initial = "Indigo")
            LocalhostTheme(accentName = accentName) {
                AppWithSplash()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } catch (_: Exception) {}
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        try {
            requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } catch (_: Exception) {}
    }
}

@Composable
fun AppWithSplash() {
    var isSplashVisible by remember { mutableStateOf(true) }
    var startAnimation by remember { mutableStateOf(false) }

    val logoScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.75f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "logoScale"
    )

    val logoAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "logoAlpha"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(1200)
        isSplashVisible = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MainApp()

        AnimatedVisibility(
            visible = isSplashVisible,
            enter = fadeIn(),
            exit = fadeOut(animationSpec = tween(durationMillis = 400))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .scale(logoScale)
                            .alpha(logoAlpha)
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceElevated)
                            .border(1.dp, DarkBorderSubtle, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        AppLogo(size = 54.dp)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Localhost",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.alpha(logoAlpha)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Development Platform",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.alpha(logoAlpha)
                    )
                }
            }
        }
    }
}

@Composable
fun MainApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        Screen.Monitor,
        Screen.Projects,
        Screen.Terminal,
        Screen.Settings
    )

    val isImeVisible = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp
    val showBottomBar = bottomNavItems.any { it.route == currentRoute } && !isImeVisible

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = DarkSurface,
                    contentColor = TextPrimary,
                    tonalElevation = 8.dp,
                    windowInsets = NavigationBarDefaults.windowInsets
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { screen.icon?.let { Icon(it, contentDescription = screen.title) } },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryAccent,
                                selectedTextColor = PrimaryAccent,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted,
                                indicatorColor = PrimaryAccentContainer
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Monitor.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding()),
            enterTransition = { slideInHorizontally(initialOffsetX = { 300 }) + fadeIn() },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -300 }) + fadeOut() },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -300 }) + fadeIn() },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { 300 }) + fadeOut() }
        ) {
            composable(Screen.Monitor.route) {
                val vm: MonitorViewModel = hiltViewModel()
                MonitorScreen(
                    viewModel = vm,
                    onProjectClick = { id -> navController.navigate("project_detail/$id") }
                )
            }

            composable(Screen.Projects.route) { backStackEntry ->
                val vm: ProjectsViewModel = hiltViewModel(backStackEntry)
                ProjectListScreen(
                    viewModel = vm,
                    onCreateClick = { navController.navigate(Screen.CreateProject.route) },
                    onProjectClick = { id -> navController.navigate("project_detail/$id") }
                )
            }

            composable(Screen.Terminal.route) {
                val vm: TerminalViewModel = hiltViewModel()
                TerminalScreen(viewModel = vm)
            }

            composable(Screen.Settings.route) {
                val vm: SettingsViewModel = hiltViewModel()
                SettingsScreen(
                    viewModel = vm,
                    onNavigateToTunnel = { navController.navigate(Screen.Tunnel.route) }
                )
            }

            composable(
                route = Screen.ProjectDetail.route,
                arguments = listOf(navArgument("projectId") { type = NavType.StringType })
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
                val vm: ProjectsViewModel = hiltViewModel()
                ProjectDetailScreen(
                    projectId = projectId,
                    viewModel = vm,
                    onBackClick = { navController.popBackStack() },
                    onOpenLogs = { id -> navController.navigate("logs/$id") },
                    onOpenFiles = { id -> navController.navigate("files/$id") }
                )
            }

            composable(Screen.CreateProject.route) {
                val vm: ProjectsViewModel = hiltViewModel()
                CreateProjectScreen(
                    viewModel = vm,
                    onBackClick = { navController.popBackStack() },
                    onProjectCreated = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = Screen.Logs.route,
                arguments = listOf(navArgument("projectId") { type = NavType.StringType })
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
                val vm: LogsViewModel = hiltViewModel()
                LogsScreen(
                    viewModel = vm,
                    initialProjectId = projectId,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.Files.route,
                arguments = listOf(navArgument("projectId") { type = NavType.StringType })
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
                val vm: FilesViewModel = hiltViewModel()
                FileExplorerScreen(
                    projectId = projectId,
                    viewModel = vm,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.Tunnel.route) {
                val vm: TunnelViewModel = hiltViewModel()
                TunnelScreen(
                    viewModel = vm,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
