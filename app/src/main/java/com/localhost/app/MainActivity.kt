package com.localhost.app

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import com.localhost.core.designsystem.theme.TextOnAccent

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
import androidx.compose.foundation.layout.navigationBars

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
import com.localhost.feature.projects.database.DatabaseScreen
import androidx.compose.material.icons.filled.Storage
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
    data object Databases : Screen("databases", "Database", Icons.Default.Storage)
    data object Terminal : Screen("terminal", "Terminal", Icons.Default.Terminal)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    data object ProjectDetail : Screen("project_detail/{projectId}", "Detail")
    data object CreateProject : Screen("create_project", "Create")
    data object Logs : Screen("logs/{projectId}", "Logs")
    data object Files : Screen("files/{projectId}", "Files")
    data object Tunnel : Screen("tunnel", "Tunnel")
}


data class SharedIncomingPayload(
    val uri: Uri? = null,
    val text: String? = null,
    val fileName: String = "shared_file"
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var projectRepository: com.localhost.core.data.repository.ProjectRepository

    private var sharedPayload by mutableStateOf<SharedIncomingPayload?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleShareIntent(intent)

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
            val secondaryHex by settingsRepository.secondaryColorHex.collectAsState(initial = "#181C28")
            val successHex by settingsRepository.successColorHex.collectAsState(initial = "#10B981")
            val failHex by settingsRepository.failColorHex.collectAsState(initial = "#F43F5E")
            val warnHex by settingsRepository.warnColorHex.collectAsState(initial = "#F59E0B")
            val themeMode by settingsRepository.themeMode.collectAsState(initial = "DARK")

            val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val isDark = when (themeMode.uppercase()) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemDark
            }

            androidx.compose.runtime.SideEffect {
                val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }

            LocalhostTheme(
                accentName = accentName,
                secondaryHex = secondaryHex,
                successHex = successHex,
                failHex = failHex,
                warnHex = warnHex,
                themeMode = themeMode
            ) {
                AppWithSplash(sharedPayload = sharedPayload, onDismissShared = { sharedPayload = null }, projectRepository = projectRepository)
            }
        }
    }

    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        if (action == Intent.ACTION_SEND || action == Intent.ACTION_SEND_MULTIPLE) {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            val text: String? = intent.getStringExtra(Intent.EXTRA_TEXT)
            var fName: String? = null
            if (uri != null) {
                try {
                    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (idx != -1 && cursor.moveToFirst()) {
                            fName = cursor.getString(idx)
                        }
                    }
                } catch (_: Exception) {}
                if (fName == null) {
                    fName = uri.lastPathSegment ?: "shared_file"
                }
            } else if (text != null) {
                fName = "shared_snippet.txt"
            }
            if (uri != null || text != null) {
                sharedPayload = SharedIncomingPayload(uri = uri, text = text, fileName = fName ?: "shared_file")
            }
        }
    }


}

@Composable
fun AppWithSplash(sharedPayload: SharedIncomingPayload? = null, onDismissShared: () -> Unit = {}, projectRepository: com.localhost.core.data.repository.ProjectRepository? = null) {
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
        MainApp(sharedPayload = sharedPayload, onDismissShared = onDismissShared, projectRepository = projectRepository)

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
fun MainApp(sharedPayload: SharedIncomingPayload? = null, onDismissShared: () -> Unit = {}, projectRepository: com.localhost.core.data.repository.ProjectRepository? = null) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        Screen.Monitor,
        Screen.Projects,
        Screen.Databases,
        Screen.Terminal,
        Screen.Settings
    )

    val isImeVisible = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp
    val showBottomBar = bottomNavItems.any { it.route == currentRoute } && !isImeVisible

        if (sharedPayload != null && projectRepository != null) {
        IncomingShareDialog(
            payload = sharedPayload,
            projectRepository = projectRepository,
            navController = navController,
            onDismiss = onDismissShared
        )
    }

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
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        NavHost(
            navController = navController,
            startDestination = Screen.Monitor.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (showBottomBar) padding.calculateBottomPadding() else navBarBottom),
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

            composable(Screen.Databases.route) {
                DatabaseScreen()
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

@Composable
fun IncomingShareDialog(
    payload: SharedIncomingPayload,
    projectRepository: com.localhost.core.data.repository.ProjectRepository,
    navController: androidx.navigation.NavController,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val projects by projectRepository.observeAll().collectAsState(initial = emptyList())
    var selectedMode by remember { mutableStateOf<String?>(null) }

    val rawSuggestedName = remember(payload.fileName) {
        val s = payload.fileName.removeSuffix(".zip").removeSuffix(".tar.gz").removeSuffix(".tgz").replace(Regex("[^a-zA-Z0-9_-]"), "_").trim('_')
        if (s.isNotBlank()) s else "shared_project"
    }
    var newProjectName by remember(rawSuggestedName) { mutableStateOf(rawSuggestedName) }
    var isCreatingNew by remember { mutableStateOf(false) }

    val initialStack = remember(payload.fileName) {
        val lower = payload.fileName.lowercase()
        when {
            lower.endsWith(".py") || lower.contains("python") || lower.contains("flask") || lower.contains("django") || lower.contains("fastapi") -> com.localhost.core.model.RuntimeType.PYTHON
            lower.endsWith(".php") || lower.contains("php") || lower.contains("laravel") || lower.contains("wordpress") -> com.localhost.core.model.RuntimeType.PHP
            lower.endsWith(".js") || lower.endsWith(".ts") || lower.contains("node") || lower.contains("express") || lower.contains("react") || lower.contains("vue") -> com.localhost.core.model.RuntimeType.NODEJS
            lower.endsWith(".jar") || lower.endsWith(".war") || lower.contains("spring") -> com.localhost.core.model.RuntimeType.JAVA
            else -> com.localhost.core.model.RuntimeType.PYTHON
        }
    }
    var selectedRuntime by remember(initialStack) { mutableStateOf(initialStack) }
    var portText by remember(selectedRuntime) { mutableStateOf(selectedRuntime.defaultPort.toString()) }
    var runtimeDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceElevated,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Share, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(24.dp))
                Column {
                    Text("Shared File Received", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                    Text(payload.fileName, color = PrimaryAccent, fontSize = 12.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedMode == null) {
                    Text(
                        "Would you like to import this shared file into an existing project or create a new project?",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )

                    Button(
                        onClick = {
                            if (projects.isEmpty()) {
                                Toast.makeText(context, "No existing projects found. Create a new one.", Toast.LENGTH_SHORT).show()
                            } else {
                                selectedMode = "EXISTING"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccentContainer),
                        border = BorderStroke(1.dp, PrimaryAccent.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import into Existing Project", color = PrimaryAccent, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            selectedMode = "NEW"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = TextOnAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create New Project", color = TextOnAccent, fontWeight = FontWeight.Bold)
                    }
                } else if (selectedMode == "NEW") {
                    Text("Project Details", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

                    OutlinedTextField(
                        value = newProjectName,
                        onValueChange = { newProjectName = it.replace(" ", "_") },
                        label = { Text("Project Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryAccent,
                            unfocusedBorderColor = DarkBorderSubtle,
                            focusedContainerColor = DarkSurfaceElevated,
                            unfocusedContainerColor = DarkSurfaceElevated
                        )
                    )

                    Text("Runtime Stack", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DarkSurface,
                            border = BorderStroke(1.dp, DarkBorderSubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { runtimeDropdownExpanded = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(selectedRuntime.displayName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryAccent)
                                    Text("Default entry: ${selectedRuntime.defaultEntryFile}", fontSize = 11.sp, color = TextMuted)
                                }
                                Icon(Icons.Default.ExpandMore, contentDescription = null, tint = TextMuted)
                            }
                        }

                        DropdownMenu(
                            expanded = runtimeDropdownExpanded,
                            onDismissRequest = { runtimeDropdownExpanded = false },
                            modifier = Modifier.background(DarkSurfaceElevated).fillMaxWidth(0.75f)
                        ) {
                            com.localhost.core.model.RuntimeType.entries.forEach { r ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                r.displayName,
                                                fontWeight = if (r == selectedRuntime) FontWeight.Bold else FontWeight.Normal,
                                                color = if (r == selectedRuntime) PrimaryAccent else TextPrimary
                                            )
                                            Text("Port ${r.defaultPort} • ${r.defaultEntryFile}", fontSize = 11.sp, color = TextMuted)
                                        }
                                    },
                                    onClick = {
                                        selectedRuntime = r
                                        portText = r.defaultPort.toString()
                                        runtimeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Text("Port", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                    OutlinedTextField(
                        value = portText,
                        onValueChange = { portText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Port Number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryAccent,
                            unfocusedBorderColor = DarkBorderSubtle,
                            focusedContainerColor = DarkSurfaceElevated,
                            unfocusedContainerColor = DarkSurfaceElevated
                        )
                    )

                    Button(
                        onClick = {
                            val trimmed = newProjectName.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_").trim('_')
                            if (trimmed.isEmpty()) {
                                Toast.makeText(context, "Please enter a valid project name", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val targetPort = portText.toIntOrNull() ?: selectedRuntime.defaultPort
                            val chosenRuntime = selectedRuntime

                            isCreatingNew = true
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val projectsDir = java.io.File(context.filesDir, "projects").apply { mkdirs() }
                                    var finalName = trimmed
                                    var targetDir = java.io.File(projectsDir, finalName)
                                    var count = 1
                                    while (targetDir.exists()) {
                                        finalName = "${trimmed}_$count"
                                        targetDir = java.io.File(projectsDir, finalName)
                                        count++
                                    }
                                    targetDir.mkdirs()

                                    if (payload.uri != null) {
                                        if (payload.fileName.endsWith(".zip", ignoreCase = true)) {
                                            context.contentResolver.openInputStream(payload.uri)?.use { inStream ->
                                                com.localhost.core.common.FileUtils.unzip(inStream, targetDir)
                                            }
                                        } else {
                                            val destFile = java.io.File(targetDir, payload.fileName)
                                            context.contentResolver.openInputStream(payload.uri)?.use { inStream ->
                                                destFile.outputStream().use { outStream -> inStream.copyTo(outStream) }
                                            }
                                        }
                                    } else if (payload.text != null) {
                                        java.io.File(targetDir, "shared_snippet.txt").writeText(payload.text)
                                    }

                                    if (chosenRuntime == com.localhost.core.model.RuntimeType.PYTHON) {
                                        val venvDir = java.io.File(targetDir, ".venv")
                                        if (!venvDir.exists()) {
                                            venvDir.mkdirs()
                                            java.io.File(venvDir, "bin").mkdirs()
                                            java.io.File(venvDir, "lib/python3.11/site-packages").mkdirs()
                                            java.io.File(venvDir, "pyvenv.cfg").writeText("home = /system/bin\ninclude-system-site-packages = true\nversion = 3.11.9\n")
                                        }
                                    }

                                    val startupCmd = when (chosenRuntime) {
                                        com.localhost.core.model.RuntimeType.PYTHON -> {
                                            val entry = if (java.io.File(targetDir, "main.py").exists()) "main.py" else "app.py"
                                            "python3 $entry"
                                        }
                                        com.localhost.core.model.RuntimeType.NODEJS -> {
                                            val entry = if (java.io.File(targetDir, "index.js").exists()) "index.js" else "server.js"
                                            "node $entry"
                                        }
                                        com.localhost.core.model.RuntimeType.PHP -> "builtin"
                                        com.localhost.core.model.RuntimeType.STATIC -> "builtin"
                                        com.localhost.core.model.RuntimeType.JAVA -> "java -jar app.jar"
                                    }

                                    val newProj = com.localhost.core.model.Project(
                                        id = java.util.UUID.randomUUID().toString(),
                                        name = finalName,
                                        runtime = chosenRuntime,
                                        port = targetPort,
                                        workingDir = targetDir.absolutePath,
                                        startupCommand = startupCmd
                                    )
                                    projectRepository.save(newProj)

                                    withContext(Dispatchers.Main) {
                                        isCreatingNew = false
                                        Toast.makeText(context, "Created project '$finalName'", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                        navController.navigate("project_detail/${newProj.id}")
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        isCreatingNew = false
                                        Toast.makeText(context, "Creation failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isCreatingNew && newProjectName.isNotBlank() && portText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        if (isCreatingNew) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = TextOnAccent, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Extracting & Creating...", color = TextOnAccent, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Add, contentDescription = null, tint = TextOnAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create & Import Project", color = TextOnAccent, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = { selectedMode = null },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, DarkBorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Back", color = TextSecondary)
                    }
                } else {
                    Text("Select Target Project:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        projects.forEach { proj ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DarkSurface,
                                border = BorderStroke(1.dp, DarkBorderSubtle),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            try {
                                                val targetDir = java.io.File(proj.workingDir)
                                                if (!targetDir.exists()) targetDir.mkdirs()
                                                if (payload.uri != null) {
                                                    if (payload.fileName.endsWith(".zip", ignoreCase = true)) {
                                                        context.contentResolver.openInputStream(payload.uri)?.use { inStream ->
                                                            com.localhost.core.common.FileUtils.unzip(inStream, targetDir)
                                                        }
                                                    } else {
                                                        val destFile = java.io.File(targetDir, payload.fileName)
                                                        context.contentResolver.openInputStream(payload.uri)?.use { inStream ->
                                                            destFile.outputStream().use { outStream -> inStream.copyTo(outStream) }
                                                        }
                                                    }
                                                } else if (payload.text != null) {
                                                    java.io.File(targetDir, "shared_snippet.txt").writeText(payload.text)
                                                }
                                                if (proj.runtime == com.localhost.core.model.RuntimeType.PYTHON) {
                                                    val venvDir = java.io.File(targetDir, ".venv")
                                                    if (!venvDir.exists()) {
                                                        venvDir.mkdirs()
                                                        java.io.File(venvDir, "bin").mkdirs()
                                                        java.io.File(venvDir, "lib/python3.11/site-packages").mkdirs()
                                                        java.io.File(venvDir, "pyvenv.cfg").writeText("home = /system/bin\ninclude-system-site-packages = true\nversion = 3.11.9\n")
                                                    }
                                                }
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, "Imported '${payload.fileName}' to ${proj.name}", Toast.LENGTH_SHORT).show()
                                                    onDismiss()
                                                    navController.navigate("project_detail/${proj.id}")
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(proj.name, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                                        Text("${proj.runtime.displayName} (: ${proj.port})", color = TextMuted, fontSize = 11.sp)
                                    }
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { selectedMode = null },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, DarkBorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Back", color = TextSecondary)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = {
                if (selectedMode != null) selectedMode = null
                else onDismiss()
            }) {
                Text(if (selectedMode != null) "Back" else "Cancel", color = TextSecondary)
            }
        }
    )
}
