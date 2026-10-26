package com.localhost.feature.projects

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Check

import androidx.compose.foundation.layout.WindowInsets

import com.localhost.core.designsystem.theme.StatusGreen
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Cloud
import com.localhost.core.model.TunnelStatus
import com.localhost.core.designsystem.theme.IsDarkTheme
import com.localhost.core.designsystem.theme.StatusRed
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DriveFolderUpload
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localhost.core.common.NetworkUtils
import com.localhost.core.common.QrCodeGenerator
import com.localhost.core.designsystem.component.AppLogo
import com.localhost.core.designsystem.component.EmptyStateView
import com.localhost.core.designsystem.component.StatusChip
import com.localhost.core.designsystem.theme.DarkBackground
import com.localhost.core.designsystem.theme.DarkBorder
import com.localhost.core.designsystem.theme.DarkBorderSubtle
import com.localhost.core.designsystem.theme.DarkSurface
import com.localhost.core.designsystem.theme.DarkSurfaceElevated
import com.localhost.core.designsystem.theme.PrimaryAccent
import com.localhost.core.designsystem.theme.PrimaryAccentContainer
import com.localhost.core.designsystem.theme.TextMuted
import com.localhost.core.designsystem.theme.TextOnAccent
import com.localhost.core.designsystem.theme.TextPrimary
import com.localhost.core.designsystem.theme.TextSecondary
import com.localhost.core.model.Project
import com.localhost.core.model.ProjectStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    viewModel: ProjectsViewModel,
    onCreateClick: () -> Unit,
    onProjectClick: (String) -> Unit
) {
    val projects by viewModel.projects.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var qrDialogUrl by remember { mutableStateOf<String?>(null) }
    var browserPreviewUrl by remember { mutableStateOf<String?>(null) }
    val forwardingStatus by viewModel.forwardingStatus.collectAsState()
    val forwardingActiveUrl by viewModel.forwardingActiveUrl.collectAsState()
    val forwardedPort by viewModel.forwardedPort.collectAsState()
    val tokenTunnelStatus by viewModel.tokenTunnelStatus.collectAsState()
    val tokenActiveUrl by viewModel.tokenActiveUrl.collectAsState()
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var newProjectSource by remember { mutableStateOf("EMPTY") }
    var searchQuery by remember { mutableStateOf("") }

    val zipImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.importProjectZip(context, it)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.syncProjectsFromFilesystem(context.filesDir)
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val runningCount = projects.count { it.status == ProjectStatus.RUNNING }
    val filteredProjects = remember(projects, searchQuery) {
        if (searchQuery.isBlank()) projects
        else projects.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.runtime.displayName.contains(searchQuery, ignoreCase = true) ||
            it.port.toString().contains(searchQuery)
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AppLogo(size = 22.dp)
                        Text(
                            text = "Projects",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        if (runningCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PrimaryAccentContainer)
                                    .border(1.dp, PrimaryAccent.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$runningCount running",
                                    color = PrimaryAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = TextPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    newProjectSource = "EMPTY"
                    showNewProjectDialog = true
                },
                containerColor = PrimaryAccent,
                contentColor = TextOnAccent,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Project")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (projects.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search projects, runtime or port...", color = TextMuted, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextMuted) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryAccent,
                            unfocusedBorderColor = DarkBorderSubtle,
                            cursorColor = PrimaryAccent,
                            focusedContainerColor = DarkSurfaceElevated,
                            unfocusedContainerColor = DarkSurfaceElevated
                        )
                    )
                }
            }

            if (projects.isEmpty()) {
                EmptyStateView(
                    title = "No Projects Yet",
                    description = "Create a project or tap the import icon above to load a ZIP archive.",
                    actionLabel = "Create Project",
                    onActionClick = {
                        newProjectSource = "EMPTY"
                        showNewProjectDialog = true
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (filteredProjects.isEmpty()) {
                EmptyStateView(
                    title = "No Matching Projects",
                    description = "No projects match '$searchQuery'",
                    actionLabel = "Clear Search",
                    onActionClick = { searchQuery = "" },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (isLandscape) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(span = { GridItemSpan(2) }) {
                        HeaderStatusRow(projectsCount = filteredProjects.size)
                    }

                    items(filteredProjects, key = { it.id }) { project ->
                        val isPortForwarded = forwardingStatus == TunnelStatus.CONNECTED &&
                            forwardingActiveUrl.isNotBlank() &&
                            (forwardedPort == project.port || (forwardedPort == null && project.status == ProjectStatus.RUNNING))

                        val isTokenActive = tokenTunnelStatus == TunnelStatus.CONNECTED &&
                            tokenActiveUrl.isNotBlank() &&
                            project.status == ProjectStatus.RUNNING

                        ProjectCardItem(
                            project = project,
                            portForwardUrl = if (isPortForwarded) forwardingActiveUrl else null,
                            tokenTunnelUrl = if (isTokenActive) tokenActiveUrl else null,
                            onCardClick = { onProjectClick(project.id) },
                            onStartClick = { viewModel.startProject(project) },
                            onStopClick = { viewModel.stopProject(project.id) },
                            onRestartClick = { viewModel.restartProject(project.id) },
                            onQrClick = {
                                val ip = NetworkUtils.getLocalIpAddress() ?: "127.0.0.1"
                                qrDialogUrl = "http://$ip:${project.port}"
                            },
                            onOpenBrowser = {
                                val ip = NetworkUtils.getLocalIpAddress() ?: "127.0.0.1"
                                browserPreviewUrl = "http://$ip:${project.port}"
                            },
                            onOpenUrl = { url ->
                                browserPreviewUrl = url
                            },
                            onShowQrUrl = { url ->
                                qrDialogUrl = url
                            }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        HeaderStatusRow(projectsCount = filteredProjects.size)
                    }

                    items(filteredProjects, key = { it.id }) { project ->
                        val isPortForwarded = forwardingStatus == TunnelStatus.CONNECTED &&
                            forwardingActiveUrl.isNotBlank() &&
                            (forwardedPort == project.port || (forwardedPort == null && project.status == ProjectStatus.RUNNING))

                        val isTokenActive = tokenTunnelStatus == TunnelStatus.CONNECTED &&
                            tokenActiveUrl.isNotBlank() &&
                            project.status == ProjectStatus.RUNNING

                        ProjectCardItem(
                            project = project,
                            portForwardUrl = if (isPortForwarded) forwardingActiveUrl else null,
                            tokenTunnelUrl = if (isTokenActive) tokenActiveUrl else null,
                            onCardClick = { onProjectClick(project.id) },
                            onStartClick = { viewModel.startProject(project) },
                            onStopClick = { viewModel.stopProject(project.id) },
                            onRestartClick = { viewModel.restartProject(project.id) },
                            onQrClick = {
                                val ip = NetworkUtils.getLocalIpAddress() ?: "127.0.0.1"
                                qrDialogUrl = "http://$ip:${project.port}"
                            },
                            onOpenBrowser = {
                                val ip = NetworkUtils.getLocalIpAddress() ?: "127.0.0.1"
                                browserPreviewUrl = "http://$ip:${project.port}"
                            },
                            onOpenUrl = { url ->
                                browserPreviewUrl = url
                            },
                            onShowQrUrl = { url ->
                                qrDialogUrl = url
                            }
                        )
                    }
                }
            }
        }
    }

    if (showNewProjectDialog) {
        var step by remember { mutableIntStateOf(1) }
        var source by remember { mutableStateOf(newProjectSource) }
        var name by remember { mutableStateOf("") }
        var portStr by remember { mutableStateOf("8081") }
        var userChangedPort by remember { mutableStateOf(false) }
        var selectedRuntime by remember { mutableStateOf(com.localhost.core.model.RuntimeType.STATIC) }
        var githubUrl by remember { mutableStateOf("") }
        var selectedZipUri by remember { mutableStateOf<Uri?>(null) }
        var selectedZipName by remember { mutableStateOf<String?>(null) }

        val newProjectZipLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                selectedZipUri = it
                try {
                    context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1 && cursor.moveToFirst()) {
                            val fName = cursor.getString(nameIndex)
                            selectedZipName = fName
                            if (name.isBlank()) {
                                name = fName.substringBeforeLast(".").filter { ch -> ch.isLetterOrDigit() || ch == '_' }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        AlertDialog(
            onDismissRequest = { showNewProjectDialog = false },
            containerColor = DarkSurfaceElevated,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (step == 1) Icons.Default.Code else when (source) {
                            "GITHUB" -> Icons.Default.CloudDownload
                            "ZIP" -> Icons.Default.DriveFolderUpload
                            else -> Icons.Default.Add
                        },
                        contentDescription = null,
                        tint = PrimaryAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = if (step == 1) "Select Stack (1/2)" else "Configure Project (2/2)",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 17.sp
                        )
                        Text(
                            text = if (step == 1) "Step 1: Choose project runtime" else "Step 2: Source & unique project name",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (step == 1) {
                        Text("Select Technology Stack / Runtime", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            com.localhost.core.model.RuntimeType.entries.forEach { runtime ->
                                val isSelected = selectedRuntime == runtime
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) PrimaryAccentContainer else DarkSurface,
                                    border = BorderStroke(1.dp, if (isSelected) PrimaryAccent else DarkBorderSubtle),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedRuntime = runtime
                                            if (!userChangedPort) {
                                                portStr = runtime.defaultPort.toString()
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) PrimaryAccent else DarkSurfaceElevated)
                                                    .border(1.dp, if (isSelected) PrimaryAccent else DarkBorder, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = TextOnAccent, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                            Column {
                                                Text(
                                                    text = runtime.displayName,
                                                    fontSize = 14.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) PrimaryAccent else TextPrimary
                                                )
                                                Text(
                                                    text = when (runtime) {
                                                        com.localhost.core.model.RuntimeType.PYTHON -> "Python 3, Flask, FastAPI, Django, pip"
                                                        com.localhost.core.model.RuntimeType.NODEJS -> "Node.js runtime, npm package server"
                                                        com.localhost.core.model.RuntimeType.PHP -> "PHP built-in server with Composer"
                                                        com.localhost.core.model.RuntimeType.JAVA -> "OpenJDK embedded runtime and JARs"
                                                        com.localhost.core.model.RuntimeType.STATIC -> "HTML5, CSS3, JavaScript web assets"
                                                    },
                                                    fontSize = 11.sp,
                                                    color = TextMuted
                                                )
                                            }
                                        }
                                        Text(
                                            text = ":${runtime.defaultPort}",
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold,
                                            color = PrimaryAccent
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text("Select Project Source", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                Triple("EMPTY", "Empty", Icons.Default.Code),
                                Triple("ZIP", "ZIP File", Icons.Default.DriveFolderUpload),
                                Triple("GITHUB", "Git / GitHub", Icons.Default.CloudDownload)
                            ).forEach { (src, label, icon) ->
                                val isSelected = source == src
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) PrimaryAccentContainer else DarkSurface,
                                    border = BorderStroke(1.dp, if (isSelected) PrimaryAccent else DarkBorderSubtle),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { source = src }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            icon,
                                            contentDescription = null,
                                            tint = if (isSelected) PrimaryAccent else TextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) PrimaryAccent else TextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        if (source == "GITHUB") {
                            OutlinedTextField(
                                value = githubUrl,
                                onValueChange = {
                                    githubUrl = it
                                    if (name.isBlank() && it.contains("/")) {
                                        val candidate = it.trim().removeSuffix("/").removeSuffix(".git").substringAfterLast("/")
                                        if (candidate.isNotBlank()) {
                                            name = candidate.filter { ch -> ch.isLetterOrDigit() || ch == '_' }
                                        }
                                    }
                                },
                                label = { Text("Git Repository URL", fontSize = 12.sp) },
                                placeholder = { Text("https://github.com/user/repo", color = TextMuted, fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = PrimaryAccent,
                                    unfocusedBorderColor = DarkBorderSubtle,
                                    focusedContainerColor = DarkSurface,
                                    unfocusedContainerColor = DarkSurface
                                )
                            )
                        } else if (source == "ZIP") {
                            Button(
                                onClick = { newProjectZipLauncher.launch("*/*") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedZipUri != null) PrimaryAccentContainer else DarkSurface
                                ),
                                border = BorderStroke(1.dp, if (selectedZipUri != null) PrimaryAccent else DarkBorderSubtle)
                            ) {
                                Icon(
                                    Icons.Default.DriveFolderUpload,
                                    contentDescription = null,
                                    tint = if (selectedZipUri != null) PrimaryAccent else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = selectedZipName ?: "Choose .zip Archive",
                                    color = if (selectedZipUri != null) PrimaryAccent else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        val isDuplicateName = name.isNotBlank() && projects.any { it.name.equals(name.trim(), ignoreCase = true) }
                        val isInvalidFormat = name.isNotBlank() && !Regex("^[a-zA-Z0-9_]+$").matches(name.trim())

                        OutlinedTextField(
                            value = name,
                            onValueChange = { input ->
                                name = input.filter { ch -> ch.isLetterOrDigit() || ch == '_' }
                            },
                            label = { Text("Project Name (alphanumeric, _)", fontSize = 12.sp) },
                            placeholder = { Text("e.g. my_project_1", color = TextMuted, fontSize = 12.sp) },
                            singleLine = true,
                            isError = isDuplicateName || isInvalidFormat,
                            supportingText = {
                                if (isDuplicateName) {
                                    Text("A project with this name already exists", color = StatusRed, fontSize = 11.sp)
                                } else if (isInvalidFormat) {
                                    Text("Only letters, numbers, and underscores (_) are allowed", color = StatusRed, fontSize = 11.sp)
                                } else {
                                    Text("Must be unique, alphanumeric and underscores only", color = TextMuted, fontSize = 10.sp)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = PrimaryAccent,
                                unfocusedBorderColor = DarkBorderSubtle,
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface,
                                errorBorderColor = StatusRed
                            )
                        )

                        OutlinedTextField(
                            value = portStr,
                            onValueChange = {
                                portStr = it.filter { ch -> ch.isDigit() }
                                userChangedPort = true
                            },
                            label = { Text("Server Port (1024 - 65535)", fontSize = 12.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = PrimaryAccent,
                                unfocusedBorderColor = DarkBorderSubtle,
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface
                            )
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkSurface,
                            border = BorderStroke(1.dp, DarkBorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Runtime: ${selectedRuntime.displayName}", color = TextSecondary, fontSize = 12.sp)
                                TextButton(onClick = { step = 1 }, contentPadding = PaddingValues(0.dp)) {
                                    Text("Change Stack", color = PrimaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (step == 1) {
                    Button(
                        onClick = {
                            if (!userChangedPort) {
                                portStr = selectedRuntime.defaultPort.toString()
                            }
                            step = 2
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Next", color = TextOnAccent, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            val trimmedName = name.trim()
                            if (trimmedName.isBlank()) {
                                Toast.makeText(context, "Project name cannot be empty", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (!Regex("^[a-zA-Z0-9_]+$").matches(trimmedName)) {
                                Toast.makeText(context, "Only alphanumeric characters and _ allowed", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (projects.any { it.name.equals(trimmedName, ignoreCase = true) }) {
                                Toast.makeText(context, "Project name '$trimmedName' already exists", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val p = portStr.toIntOrNull() ?: selectedRuntime.defaultPort
                            if (p !in 1024..65535) {
                                Toast.makeText(context, "Port must be between 1024 and 65535", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            when (source) {
                                "EMPTY" -> {
                                    val created = viewModel.createProject(trimmedName, selectedRuntime, p, context.filesDir)
                                    if (created) {
                                        showNewProjectDialog = false
                                        Toast.makeText(context, "Created project '$trimmedName'", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                "ZIP" -> {
                                    if (selectedZipUri == null) {
                                        Toast.makeText(context, "Please choose a ZIP file", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    viewModel.importProjectZip(context, selectedZipUri!!, customName = trimmedName, customPort = p, customRuntime = selectedRuntime)
                                    showNewProjectDialog = false
                                }
                                "GITHUB" -> {
                                    if (githubUrl.isBlank()) {
                                        Toast.makeText(context, "Please enter a Git repository URL", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    viewModel.importProjectFromGithub(context, githubUrl, customName = trimmedName, customPort = p, customRuntime = selectedRuntime)
                                    showNewProjectDialog = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            if (source == "EMPTY") "Create Project" else "Import Project",
                            color = TextOnAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            dismissButton = {
                if (step == 1) {
                    TextButton(onClick = { showNewProjectDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                } else {
                    TextButton(onClick = { step = 1 }) {
                        Text("Back", color = TextSecondary)
                    }
                }
            }
        )
    }

    qrDialogUrl?.let { url ->
        val qrBitmap = remember(url) { QrCodeGenerator.generate(url, 400) }
        AlertDialog(
            onDismissRequest = { qrDialogUrl = null },
            containerColor = DarkSurfaceElevated,
            title = {
                val dialogTitle = if (url.contains("trycloudflare.com")) {
                    "Scan Port Forward URL"
                } else if (url.contains("cloudflare") || url.startsWith("https://")) {
                    "Scan Cloudflare Tunnel"
                } else {
                    "Scan to Open Local Server"
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QrCode, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = dialogTitle, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .background(androidx.compose.ui.graphics.Color.White, RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    ) {
                        qrBitmap?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "QR Code",
                                modifier = Modifier
                                    .size(200.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurface, RoundedCornerShape(8.dp))
                            .border(1.dp, DarkBorderSubtle, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = url,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = PrimaryAccent,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Local Server Link", url)
                                    clipboard.setPrimaryClip(clip)
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    Toast.makeText(context, "Copied link: $url", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Link", tint = TextPrimary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Local Server Link", url)
                            clipboard.setPrimaryClip(clip)
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            Toast.makeText(context, "Copied: $url", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, DarkBorderSubtle)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", color = TextPrimary, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val target = qrDialogUrl ?: url
                            qrDialogUrl = null
                            browserPreviewUrl = target
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = TextOnAccent, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open", color = TextOnAccent, fontSize = 12.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { qrDialogUrl = null }
                ) {
                    Text("Close", color = TextSecondary)
                }
            }
        )
    }

    browserPreviewUrl?.let { url ->
        InAppBrowserDialog(
            initialUrl = url,
            onDismiss = { browserPreviewUrl = null }
        )
    }
}

@Composable
private fun HeaderStatusRow(projectsCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Hosted Projects ($projectsCount)",
            style = MaterialTheme.typography.titleSmall,
            color = TextSecondary,
            fontWeight = FontWeight.SemiBold
        )
        val localIp = NetworkUtils.getLocalIpAddress() ?: "127.0.0.1"
        Text(
            text = "IP: $localIp",
            style = MaterialTheme.typography.labelSmall,
            color = PrimaryAccent,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ProjectCardItem(
    project: Project,
    portForwardUrl: String? = null,
    tokenTunnelUrl: String? = null,
    onCardClick: () -> Unit,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onRestartClick: () -> Unit,
    onQrClick: () -> Unit,
    onOpenBrowser: () -> Unit,
    onOpenUrl: (String) -> Unit = {},
    onShowQrUrl: (String) -> Unit = {}
) {
    val isRunning = project.status == ProjectStatus.RUNNING
    val borderColor = if (isRunning) PrimaryAccent.copy(alpha = 0.5f) else DarkBorder

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = project.runtime.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                StatusChip(status = project.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkSurface)
                            .border(1.dp, DarkBorderSubtle, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = ":${project.port}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryAccent
                        )
                    }

                    if (isRunning) {
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = onOpenBrowser, modifier = Modifier.size(38.dp)) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open in Browser", tint = PrimaryAccent, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = onQrClick, modifier = Modifier.size(38.dp)) {
                            Icon(Icons.Default.QrCode, contentDescription = "QR Code", tint = TextSecondary, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isRunning) {
                        Button(
                            onClick = onStopClick,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusRed.copy(alpha = 0.25f)),
                            border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, tint = StatusRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Stop", color = StatusRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onRestartClick,
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, DarkBorderSubtle),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Restart", tint = PrimaryAccent, modifier = Modifier.size(20.dp))
                        }
                    } else {
                        Button(
                            onClick = onStartClick,
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (IsDarkTheme) StatusGreen.copy(alpha = 0.3f) else androidx.compose.ui.graphics.Color(0xFFD1FAE5)
                            ),
                            border = BorderStroke(1.dp, if (IsDarkTheme) StatusGreen.copy(alpha = 0.8f) else androidx.compose.ui.graphics.Color(0xFF059669)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            val greenTint = if (IsDarkTheme) StatusGreen else androidx.compose.ui.graphics.Color(0xFF047857)
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = greenTint, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start Server", color = greenTint, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (portForwardUrl != null || tokenTunnelUrl != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (portForwardUrl != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = StatusGreen.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, StatusGreen.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Public,
                                        contentDescription = null,
                                        tint = StatusGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            "Port Forward (Public)",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = StatusGreen
                                        )
                                        Text(
                                            text = portForwardUrl,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onOpenUrl(portForwardUrl) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.OpenInNew,
                                            contentDescription = "Open in Browser",
                                            tint = StatusGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onShowQrUrl(portForwardUrl) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.QrCode,
                                            contentDescription = "QR Code",
                                            tint = StatusGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (tokenTunnelUrl != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PrimaryAccent.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, PrimaryAccent.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Cloud,
                                        contentDescription = null,
                                        tint = PrimaryAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            "Cloudflare Tunnel",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryAccent
                                        )
                                        Text(
                                            text = tokenTunnelUrl,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onOpenUrl(tokenTunnelUrl) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.OpenInNew,
                                            contentDescription = "Open in Browser",
                                            tint = PrimaryAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onShowQrUrl(tokenTunnelUrl) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.QrCode,
                                            contentDescription = "QR Code",
                                            tint = PrimaryAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
