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
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.localhost.cli.TerminalScreen
import com.localhost.cli.TerminalViewModel
import com.localhost.core.designsystem.component.AppLogo
import com.localhost.core.designsystem.theme.DarkBackground
import com.localhost.core.designsystem.theme.DarkSurface
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
import com.localhost.feature.supabase.SupabaseScreen
import com.localhost.feature.supabase.SupabaseViewModel
import com.localhost.feature.tunnel.TunnelScreen
import com.localhost.feature.tunnel.TunnelViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    data object Monitor : Screen("monitor", "Monitor", Icons.Default.BarChart)
    data object Projects : Screen("projects", "Projects", Icons.Default.Folder)
    data object Terminal : Screen("terminal", "Terminal", Icons.Default.Terminal)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    data object Tunnel : Screen("tunnel", "Tunnel")
    data object CreateProject : Screen("create_project", "New Project")
    data object ProjectDetail : Screen("project_detail/{id}", "Project Detail")
    data object Logs : Screen("logs?projectId={projectId}", "Logs")
    data object Files : Screen("files?projectId={projectId}", "Files")
    data object Supabase : Screen("supabase", "Supabase")
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            LocalhostTheme {
                AppWithSplash()
            }
        }
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
        delay(950)
        isSplashVisible = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MainApp()

        AnimatedVisibility(
            visible = isSplashVisible,
            enter = fadeIn(),
            exit = fadeOut(animationSpec = tween(350))
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
                            .size(100.dp)
                            .scale(logoScale)
                            .alpha(logoAlpha)
                            .clip(CircleShape)
                            .background(DarkSurface)
                            .border(1.5.dp, PrimaryAccent.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        AppLogo(modifier = Modifier.size(60.dp))
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Localhost",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.alpha(logoAlpha)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Local Server & Runtime Environment",
                        fontSize = 13.sp,
                        color = TextSecondary,
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

    val showBottomBar = bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = DarkSurface,
                    contentColor = TextPrimary,
                    tonalElevation = 8.dp
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
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
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
            modifier = Modifier.padding(padding),
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

            composable(Screen.CreateProject.route) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    try {
                        navController.getBackStackEntry(Screen.Projects.route)
                    } catch (_: Exception) {
                        backStackEntry
                    }
                }
                val vm: ProjectsViewModel = hiltViewModel(parentEntry)
                CreateProjectScreen(
                    viewModel = vm,
                    onBackClick = { navController.popBackStack() },
                    onProjectCreated = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.ProjectDetail.route,
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id") ?: ""
                val parentEntry = remember(backStackEntry) {
                    try {
                        navController.getBackStackEntry(Screen.Projects.route)
                    } catch (_: Exception) {
                        backStackEntry
                    }
                }
                val vm: ProjectsViewModel = hiltViewModel(parentEntry)
                ProjectDetailScreen(
                    projectId = id,
                    viewModel = vm,
                    onBackClick = { navController.popBackStack() },
                    onOpenLogs = { projectId -> navController.navigate("logs?projectId=$projectId") },
                    onOpenFiles = { projectId -> navController.navigate("files?projectId=$projectId") }
                )
            }

            composable(
                route = Screen.Logs.route,
                arguments = listOf(navArgument("projectId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId")
                val vm: LogsViewModel = hiltViewModel()
                LogsScreen(viewModel = vm, initialProjectId = projectId)
            }

            composable(
                route = Screen.Files.route,
                arguments = listOf(navArgument("projectId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId")
                val vm: FilesViewModel = hiltViewModel()
                FileExplorerScreen(viewModel = vm, initialProjectId = projectId)
            }

            composable(Screen.Terminal.route) {
                val vm: TerminalViewModel = hiltViewModel()
                TerminalScreen(viewModel = vm)
            }

            composable(Screen.Tunnel.route) {
                val vm: TunnelViewModel = hiltViewModel()
                TunnelScreen(viewModel = vm)
            }

            composable(Screen.Settings.route) {
                val vm: SettingsViewModel = hiltViewModel()
                SettingsScreen(viewModel = vm)
            }

            composable(Screen.Supabase.route) {
                val vm: SupabaseViewModel = hiltViewModel()
                SupabaseScreen(viewModel = vm)
            }
        }
    }
}
