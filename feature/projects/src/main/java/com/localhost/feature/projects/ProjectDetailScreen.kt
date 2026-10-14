package com.localhost.feature.projects

import java.util.Date
import com.localhost.core.designsystem.theme.SecondaryTeal
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.text.input.KeyboardType


import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage


import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.localhost.core.common.NetworkUtils
import com.localhost.core.common.QrCodeGenerator
import com.localhost.core.designsystem.component.StatusChip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import com.localhost.core.designsystem.theme.CodeBackground
import com.localhost.core.designsystem.theme.DarkBackground
import com.localhost.core.designsystem.theme.DarkBorder
import com.localhost.core.designsystem.theme.DarkBorderSubtle
import com.localhost.core.designsystem.theme.DarkSurface
import com.localhost.core.designsystem.theme.DarkSurfaceElevated
import com.localhost.core.designsystem.theme.PrimaryAccent
import com.localhost.core.designsystem.theme.PrimaryAccentContainer
import com.localhost.core.designsystem.theme.StatusGreen
import com.localhost.core.designsystem.theme.StatusOrange
import com.localhost.core.designsystem.theme.StatusRed
import com.localhost.core.designsystem.theme.StatusYellow
import com.localhost.core.designsystem.theme.TextMuted
import com.localhost.core.designsystem.theme.TextOnAccent
import com.localhost.core.designsystem.theme.TextPrimary
import com.localhost.core.designsystem.theme.TextSecondary
import com.localhost.core.model.EnvironmentVar
import com.localhost.core.model.Project
import com.localhost.core.model.ProjectSnapshot
import com.localhost.core.model.ProjectStatus
import com.localhost.core.model.RuntimeType
import com.localhost.core.model.TunnelStatus
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectId: String,
    viewModel: ProjectsViewModel,
    onBackClick: () -> Unit,
    onOpenLogs: (String) -> Unit,
    onOpenFiles: (String) -> Unit
) {
    val projects by viewModel.projects.collectAsState()
    val message by viewModel.message.collectAsState()
    val tunnelStatus by viewModel.tunnelStatus.collectAsState()
    val activeTunnelUrl by viewModel.activeTunnelUrl.collectAsState()
    val tunnelError by viewModel.tunnelError.collectAsState()
    val project = projects.firstOrNull { it.id == projectId }
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var selectedTab by remember { mutableIntStateOf(0) }
    var qrDialogUrl by remember { mutableStateOf<String?>(null) }
    var browserPreviewUrl by remember { mutableStateOf<String?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var showDotEnvExportPrompt by remember { mutableStateOf(false) }

    val tabs = listOf("Dashboard", "Configs", "Terminal", "Log", "Commits", "Stats", "Settings")
    val tabHistory = remember { mutableStateListOf(0) }
    BackHandler {
        if (browserPreviewUrl != null) {
            browserPreviewUrl = null
        } else if (showEditDialog) {
            showEditDialog = false
        } else if (showDeleteDialog) {
            showDeleteDialog = false
        } else if (qrDialogUrl != null) {
            qrDialogUrl = null
        } else if (tabHistory.size > 1) {
            tabHistory.removeAt(tabHistory.lastIndex)
            selectedTab = tabHistory.last()
        } else {
            onBackClick()
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    fun doExportZip(includeDotEnv: Boolean) {
        if (project == null) return
        scope.launch {
            isExporting = true
            Toast.makeText(context, "Exporting ${project.name} ZIP...", Toast.LENGTH_SHORT).show()
            try {
                val zipFile = viewModel.exportProjectZip(project, context, includeDotEnv = includeDotEnv)
                if (zipFile != null && zipFile.exists()) {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zipFile)
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/zip"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Export ${project.name} ZIP"))
                } else {
                    Toast.makeText(context, "Failed to create project ZIP", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Export share error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isExporting = false
            }
        }
    }

    fun shareExportedZip() {
        if (project == null) return
        val hasDotEnv = java.io.File(project.workingDir, ".env").exists()
        if (hasDotEnv) {
            showDotEnvExportPrompt = true
        } else {
            doExportZip(includeDotEnv = false)
        }
    }

    if (showDotEnvExportPrompt && project != null) {
        AlertDialog(
            onDismissRequest = { showDotEnvExportPrompt = false },
            containerColor = DarkSurfaceElevated,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = StatusOrange)
                    Text(".env Detected", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    "This project contains a .env file with environment variables. Do you want to include it in the exported archive or exclude it for security?",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDotEnvExportPrompt = false
                        doExportZip(includeDotEnv = true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Include .env", color = TextOnAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showDotEnvExportPrompt = false }) {
                        Text("Cancel", color = TextMuted)
                    }
                    OutlinedButton(
                        onClick = {
                            showDotEnvExportPrompt = false
                            doExportZip(includeDotEnv = false)
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, PrimaryAccent)
                    ) {
                        Text("Exclude (Recommended)", color = PrimaryAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    }

    if (project == null) {
        Scaffold(
            containerColor = DarkBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    title = { Text("Project", color = TextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Project not found", color = TextMuted)
            }
        }
        return
    }

    Scaffold(
        containerColor = DarkBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            project.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(project.runtime.displayName, fontSize = 12.sp, color = TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedTab > 0) selectedTab = 0 else onBackClick()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    if (project.status == ProjectStatus.RUNNING) {
                        IconButton(onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.stopProject(project.id)
                        }) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop", tint = StatusRed)
                        }
                        IconButton(onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.restartProject(project.id)
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Restart", tint = PrimaryAccent)
                        }
                    } else {
                        IconButton(onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.startProject(project)
                        }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Start", tint = StatusGreen)
                        }
                    }

                    IconButton(onClick = { onOpenFiles(project.id) }) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Open Files", tint = PrimaryAccent)
                    }
                    var showDetailMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showDetailMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = TextPrimary)
                        }
                        DropdownMenu(
                            expanded = showDetailMenu,
                            onDismissRequest = { showDetailMenu = false },
                            modifier = Modifier.background(DarkSurfaceElevated)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Project", color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = PrimaryAccent) },
                                onClick = {
                                    showDetailMenu = false
                                    showEditDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export ZIP", color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = PrimaryAccent) },
                                onClick = {
                                    showDetailMenu = false
                                    shareExportedZip()
                                }
                            )
                            if (project.status == ProjectStatus.RUNNING) {
                                DropdownMenuItem(
                                    text = { Text("QR Code & Link", color = TextPrimary) },
                                    leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null, tint = PrimaryAccent) },
                                    onClick = {
                                        showDetailMenu = false
                                        val ip = NetworkUtils.getLocalIpAddress() ?: "127.0.0.1"
                                        qrDialogUrl = "http://$ip:${project.port}"
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Files Explorer", color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, tint = TextSecondary) },
                                onClick = {
                                    showDetailMenu = false
                                    onOpenFiles(project.id)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Project", color = StatusRed) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StatusRed) },
                                onClick = {
                                    showDetailMenu = false
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        }
    ) { padding ->
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxHeight()
                        .background(DarkSurface)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OverviewTabContent(
                        project = project,
                        tunnelStatus = tunnelStatus,
                        activeTunnelUrl = activeTunnelUrl,
                        tunnelError = tunnelError,
                        viewModel = viewModel,
                        onStartTunnel = { viewModel.startQuickTunnelForProject(project) },
                        onStopTunnel = { viewModel.stopTunnel() },
                        onStart = { viewModel.startProject(project) },
                        onStop = { viewModel.stopProject(project.id) },
                        onRestart = { viewModel.restartProject(project.id) },
                        onOpenLogs = onOpenLogs,
                        onOpenFiles = onOpenFiles,
                        onExportZip = { shareExportedZip() },
                        onOpenBrowser = { browserPreviewUrl = it }
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(DarkBorder)
                )

                Column(
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxHeight()
                ) {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = DarkSurface,
                        contentColor = PrimaryAccent,
                        edgePadding = 16.dp,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = PrimaryAccent
                            )
                        },
                        divider = {}
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = {
                                    Text(
                                        text = title,
                                        color = if (selectedTab == index) PrimaryAccent else TextMuted,
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                }
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            
                    ) {
                        when (selectedTab) {
                            0 -> OverviewTab(
                                project = project,
                                tunnelStatus = tunnelStatus,
                                activeTunnelUrl = activeTunnelUrl,
                                tunnelError = tunnelError,
                                viewModel = viewModel,
                                onStartTunnel = { viewModel.startQuickTunnelForProject(project) },
                                onStopTunnel = { viewModel.stopTunnel() },
                                onStart = { viewModel.startProject(project) },
                                onStop = { viewModel.stopProject(project.id) },
                                onRestart = { viewModel.restartProject(project.id) },
                                onOpenLogs = {
                                    selectedTab = 3
                                    tabHistory.add(3)
                                },
                                onOpenFiles = onOpenFiles,
                                onExportZip = { shareExportedZip() },
                                onOpenBrowser = { browserPreviewUrl = it }
                            )
                            1 -> ConfigurationsTab(project = project, viewModel = viewModel)
                            2 -> ProjectTerminalTab(project = project, viewModel = viewModel)
                            3 -> ProjectLiveLogsView(project = project, viewModel = viewModel)
                            4 -> VersionsTab(project = project, viewModel = viewModel)
                            5 -> AnalyticsTab(project = project, viewModel = viewModel)
                            6 -> ControlTab(project = project, viewModel = viewModel, onBackClick = onBackClick)
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    
            ) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DarkSurface,
                    contentColor = PrimaryAccent,
                    edgePadding = 16.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = PrimaryAccent
                        )
                    },
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = {
                                if (selectedTab != index) {
                                    selectedTab = index
                                    tabHistory.add(index)
                                }
                            },
                            text = {
                                Text(
                                    text = title,
                                    color = if (selectedTab == index) PrimaryAccent else TextMuted,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            }
                        )
                    }
                }

                when (selectedTab) {
                    0 -> OverviewTab(
                            project = project,
                            tunnelStatus = tunnelStatus,
                            activeTunnelUrl = activeTunnelUrl,
                            tunnelError = tunnelError,
                            viewModel = viewModel,
                            onStartTunnel = { viewModel.startQuickTunnelForProject(project) },
                            onStopTunnel = { viewModel.stopTunnel() },
                            onStart = { viewModel.startProject(project) },
                            onStop = { viewModel.stopProject(project.id) },
                            onRestart = { viewModel.restartProject(project.id) },
                            onOpenLogs = {
                                selectedTab = 3
                                tabHistory.add(3)
                            },
                            onOpenFiles = onOpenFiles,
                            onExportZip = { shareExportedZip() },
                            onOpenBrowser = { browserPreviewUrl = it }
                        )
                    1 -> ConfigurationsTab(project = project, viewModel = viewModel)
                    2 -> ProjectTerminalTab(project = project, viewModel = viewModel)
                    3 -> ProjectLiveLogsView(project = project, viewModel = viewModel)
                    4 -> VersionsTab(project = project, viewModel = viewModel)
                    5 -> AnalyticsTab(project = project, viewModel = viewModel)
                    6 -> ControlTab(project = project, viewModel = viewModel, onBackClick = onBackClick)
                }
            }
        }
    }

    if (showDeleteDialog && project != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete '${project.name}'?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("This will permanently delete this project and all its files.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProject(project.id)
                        showDeleteDialog = false
                        onBackClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showEditDialog) {
        var editName by remember(project.name) { mutableStateOf(project.name) }
        var editPort by remember(project.port) { mutableStateOf(project.port.toString()) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text("Edit Project", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Project Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryAccent,
                            unfocusedBorderColor = DarkBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editPort,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) editPort = it },
                        label = { Text("Port (1024 - 65535)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryAccent,
                            unfocusedBorderColor = DarkBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val portInt = editPort.toIntOrNull()
                        if (portInt != null && portInt in 1024..65535 && editName.isNotBlank()) {
                            viewModel.updateProjectDetails(project, editName.trim(), portInt)
                            showEditDialog = false
                        } else {
                            Toast.makeText(context, "Please enter a valid name and port (1024-65535)", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                ) {
                    Text("Save", color = DarkBackground)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    qrDialogUrl?.let { url ->
        val qrBitmap = remember(url) { QrCodeGenerator.generate(url, 400) }
        AlertDialog(
            onDismissRequest = { qrDialogUrl = null },
            containerColor = DarkSurfaceElevated,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QrCode, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan to Open Local Server", fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        qrBitmap?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "QR Code",
                                modifier = Modifier
                                    .size(220.dp)
                                    .padding(8.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurface,
                        border = BorderStroke(1.dp, DarkBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = url,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = PrimaryAccent,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Local Link", url)
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
                            val clip = ClipData.newPlainText("Local Link", url)
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
                TextButton(onClick = { qrDialogUrl = null }) {
                    Text("Close", color = TextSecondary)
                }
            }
        )
    }

    browserPreviewUrl?.let { url ->
        InAppBrowserDialog(
            initialUrl = url,
            onDismiss = { browserPreviewUrl = null },
            onVisitRecorded = { recordedUrl -> viewModel.recordUrlVisit(project.id, recordedUrl) }
        )
    }
}

@Composable
private fun OverviewTab(
    project: Project,
    tunnelStatus: TunnelStatus,
    activeTunnelUrl: String,
    tunnelError: String?,
    viewModel: ProjectsViewModel,
    onStartTunnel: () -> Unit,
    onStopTunnel: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onOpenLogs: (String) -> Unit,
    onOpenFiles: (String) -> Unit,
    onExportZip: () -> Unit,
    onOpenBrowser: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OverviewTabContent(
            project = project,
            tunnelStatus = tunnelStatus,
            activeTunnelUrl = activeTunnelUrl,
            tunnelError = tunnelError,
            viewModel = viewModel,
            onStartTunnel = onStartTunnel,
            onStopTunnel = onStopTunnel,
            onStart = onStart,
            onStop = onStop,
            onRestart = onRestart,
            onOpenLogs = onOpenLogs,
            onOpenFiles = onOpenFiles,
            onExportZip = onExportZip,
            onOpenBrowser = onOpenBrowser
        )
    }
}

@Composable
private fun OverviewTabContent(
    project: Project,
    tunnelStatus: TunnelStatus,
    activeTunnelUrl: String,
    tunnelError: String?,
    viewModel: ProjectsViewModel,
    onStartTunnel: () -> Unit,
    onStopTunnel: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onOpenLogs: (String) -> Unit,
    onOpenFiles: (String) -> Unit,
    onExportZip: () -> Unit,
    onOpenBrowser: (String) -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val localIp = remember(project.id) { NetworkUtils.getLocalIpAddress() ?: "127.0.0.1" }
    val lanUrl = "http://$localIp:${project.port}"
    val loopbackUrl = "http://127.0.0.1:${project.port}"
    var showFullQrModal by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.5.dp, if (project.status == ProjectStatus.RUNNING) StatusGreen.copy(alpha = 0.5f) else DarkBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (project.status == ProjectStatus.RUNNING) StatusGreen else TextMuted)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (project.status == ProjectStatus.RUNNING) "Server Online" else "Server Stopped",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (project.status == ProjectStatus.RUNNING) StatusGreen else TextMuted
                    )
                }
                StatusChip(status = project.status)
            }

            if (project.status == ProjectStatus.RUNNING) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onStop,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusRed.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, tint = StatusRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Stop Server", color = StatusRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    OutlinedButton(
                        onClick = onRestart,
                        border = BorderStroke(1.dp, PrimaryAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restart", color = PrimaryAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            } else {
                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = TextOnAccent, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Start Server", color = TextOnAccent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = { onOpenBrowser("http://127.0.0.1:${project.port}") },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccentContainer),
            border = BorderStroke(1.dp, PrimaryAccent),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
        ) {
            Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open", color = PrimaryAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = { onOpenLogs(project.id) },
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, DarkBorder),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Log", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }

    if (activeTunnelUrl.isNotBlank() || tunnelStatus == com.localhost.core.model.TunnelStatus.CONNECTED) {
        val displayUrl = if (activeTunnelUrl.isNotBlank()) activeTunnelUrl else "https://tunnel.localhost"
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.5.dp, StatusGreen.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(50))
                                .background(StatusGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Cloudflare Tunnel Active",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = StatusGreen
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = StatusGreen.copy(alpha = 0.15f)
                    ) {
                        Text(
                            "Live Public",
                            color = StatusGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DarkSurfaceElevated,
                    border = BorderStroke(1.dp, DarkBorderSubtle)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = displayUrl,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = PrimaryAccent,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Tunnel URL", displayUrl)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied Tunnel URL", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = PrimaryAccent, modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = { onOpenBrowser(displayUrl) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = "Open", tint = TextPrimary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }



    if (project.status == ProjectStatus.RUNNING) {
        val qrBitmap = remember(lanUrl) { QrCodeGenerator.generate(lanUrl, 400) }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.5.dp, PrimaryAccent.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(50))
                                .background(StatusGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Server Online & Live",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = StatusGreen
                        )
                    }
                    StatusChip(status = project.status)
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceElevated,
                    border = BorderStroke(1.dp, DarkBorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            onClick = { showFullQrModal = true },
                            modifier = Modifier.size(96.dp)
                        ) {
                            qrBitmap?.let { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Scan QR Code",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(4.dp)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "Local Network Access",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                            Text(
                                "Scan QR code or open link from any device on Wi-Fi / LAN.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 15.sp
                            )
                            OutlinedButton(
                                onClick = { showFullQrModal = true },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, DarkBorder),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Default.QrCode, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Expand QR", fontSize = 11.sp, color = TextPrimary)
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkSurfaceElevated,
                        border = BorderStroke(1.dp, DarkBorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("LAN URL (Wi-Fi Devices)", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                                Text(
                                    text = lanUrl,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    color = PrimaryAccent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("LAN URL", lanUrl)
                                        clipboard.setPrimaryClip(clip)
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        Toast.makeText(context, "Copied LAN URL: $lanUrl", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy LAN URL", tint = PrimaryAccent, modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = {
                                        onOpenBrowser(lanUrl)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in Browser", tint = TextPrimary, modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = {
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, lanUrl)
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Share Project URL"))
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share Link", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkSurfaceElevated,
                        border = BorderStroke(1.dp, DarkBorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Localhost URL (This Device)", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                                Text(
                                    text = loopbackUrl,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Local URL", loopbackUrl)
                                    clipboard.setPrimaryClip(clip)
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    Toast.makeText(context, "Copied: $loopbackUrl", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Localhost URL", tint = TextMuted, modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }
            }
        }

        if (showFullQrModal) {
            AlertDialog(
                onDismissRequest = { showFullQrModal = false },
                containerColor = DarkSurfaceElevated,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.QrCode, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scan to Open Local Server", fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            qrBitmap?.let { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "QR Code",
                                    modifier = Modifier
                                        .size(220.dp)
                                        .padding(8.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = lanUrl,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = PrimaryAccent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("LAN URL", lanUrl)
                            clipboard.setPrimaryClip(clip)
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            Toast.makeText(context, "Copied: $lanUrl", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = TextOnAccent, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy Link", color = TextOnAccent, fontSize = 12.sp)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFullQrModal = false }) {
                        Text("Close", color = TextSecondary)
                    }
                }
            )
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Server Details", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                StatusChip(status = project.status)
            }

            val recentRequests by viewModel.observeRequests(project.id, 0L).collectAsState(initial = emptyList())
            val callCount = recentRequests.size
            val successCount = recentRequests.count { it.statusCode in 200..299 }
            val errorCount = recentRequests.count { it.statusCode >= 400 }
            val successRate = if (callCount > 0) (successCount * 100 / callCount) else 100

            DetailRow(label = "Local URL", value = "http://127.0.0.1:${project.port}")
            DetailRow(label = "Port", value = "${project.port}")
            DetailRow(label = "Runtime", value = project.runtime.displayName)
            DetailRow(label = "Command", value = project.startupCommand)
            DetailRow(label = "Total Calls", value = "$callCount requests")
            DetailRow(label = "Success Rate", value = "$successRate%")
            DetailRow(label = "Errors", value = "$errorCount errors")
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Export & Backup", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
            Text("Export entire project as a standalone ZIP archive for backup or distribution.", color = TextSecondary, fontSize = 12.sp)

            Button(
                onClick = onExportZip,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccentContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Project (.zip)", color = PrimaryAccent, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun VersionsTab(
    project: Project,
    viewModel: ProjectsViewModel
) {
    val snapshots by viewModel.getProjectSnapshots(project.id).collectAsState(initial = emptyList())
    var commitMessage by remember { mutableStateOf("") }
    var rollbackCandidate by remember { mutableStateOf<ProjectSnapshot?>(null) }
    var deleteCandidate by remember { mutableStateOf<ProjectSnapshot?>(null) }
    val haptics = LocalHapticFeedback.current
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Version Snapshot", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = commitMessage,
                        onValueChange = { commitMessage = it },
                        placeholder = { Text("Commit message...", color = TextMuted, fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
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

                    Button(
                        onClick = {
                            if (commitMessage.isNotBlank()) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.createCommit(project, commitMessage.trim())
                                commitMessage = ""
                            }
                        },
                        enabled = commitMessage.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                    ) {
                        Text("Commit", fontWeight = FontWeight.Bold, color = TextOnAccent)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Version History (${snapshots.size})",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = TextSecondary
            )
        }

        if (snapshots.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.History, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No snapshots recorded yet", color = TextMuted, fontSize = 14.sp)
                    Text("Take a snapshot above to enable rollbacks anytime", color = DarkBorder, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(snapshots, key = { it.id }) { item ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                        border = BorderStroke(1.dp, DarkBorderSubtle)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(PrimaryAccentContainer)
                                            .border(1.dp, PrimaryAccent.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = item.id,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryAccent
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = item.message,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { rollbackCandidate = item },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Restore, contentDescription = "Rollback", tint = StatusYellow, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { deleteCandidate = item },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dateFormat.format(Date(item.timestamp)),
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                                Text(
                                    text = "${item.fileCount} files - ${(item.totalSizeBytes / 1024).coerceAtLeast(1)} KB",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    rollbackCandidate?.let { snap ->
        AlertDialog(
            onDismissRequest = { rollbackCandidate = null },
            containerColor = DarkSurfaceElevated,
            title = { Text("Rollback to ${snap.id}?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("This will restore project files to the state captured at ${dateFormat.format(Date(snap.timestamp))}. Any uncommitted current changes will be overwritten.", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.rollbackToCommit(project, snap)
                        rollbackCandidate = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = StatusYellow)
                ) {
                    Text("Rollback", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { rollbackCandidate = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    deleteCandidate?.let { snap ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            containerColor = DarkSurfaceElevated,
            title = { Text("Delete snapshot ${snap.id}?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("This snapshot archive will be removed permanently.", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.deleteCommit(snap)
                        deleteCandidate = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = StatusRed)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConfigurationsTab(
    project: Project,
    viewModel: ProjectsViewModel
) {
    val isInstalling by viewModel.isInstallingDeps.collectAsState()
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current

    var envVars by remember(project.id) { mutableStateOf(viewModel.loadDotEnv(project).toMutableMap()) }
    var newEnvKey by remember { mutableStateOf("") }
    var newEnvValue by remember { mutableStateOf("") }

    var showFreezeDialog by remember { mutableStateOf(false) }
    var freezeFileName by remember { mutableStateOf("requirements.txt") }
    var freezeFilePath by remember { mutableStateOf("./") }

    var mainFileName by remember(project.id) {
        val defaultMain = when (project.runtime) {
            RuntimeType.PYTHON -> "app.py"
            RuntimeType.PHP -> "index.php"
            RuntimeType.NODEJS -> "server.js"
            RuntimeType.JAVA -> "Main"
            RuntimeType.STATIC -> "index.html"
        }
        mutableStateOf(defaultMain)
    }

    val workDir = remember(project.workingDir) { java.io.File(project.workingDir) }
    val dirSize = remember(project.workingDir, envVars.size) {
        var bytes = 0L
        workDir.walkTopDown().forEach { if (it.isFile) bytes += it.length() }
        bytes
    }
    val usedMb = dirSize / (1024.0 * 1024.0)
    val minQuota = maxOf(50f, Math.ceil(usedMb + 5.0).toFloat()).coerceAtMost(1024f)
    var quotaMb by remember(project.id) { mutableFloatStateOf(maxOf(250f, minQuota)) }
    LaunchedEffect(minQuota) {
        if (quotaMb < minQuota) {
            quotaMb = minQuota
        }
    }
    val quotaProgress = (usedMb / quotaMb).coerceIn(0.0, 1.0).toFloat()

    var activeLang by remember(project.runtime) { mutableStateOf(project.runtime.name) }
    val languages = listOf("PYTHON" to "Python", "PHP" to "PHP", "NODEJS" to "Node.js", "JAVA" to "Java", "STATIC" to "Static")

    var customPackageInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Project Storage Quota", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                    }
                    Text(
                        String.format(java.util.Locale.US, "%.1f MB / %.0f MB", usedMb, quotaMb),
                        fontSize = 12.sp,
                        color = if (quotaProgress > 0.85f) StatusRed else TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                LinearProgressIndicator(
                    progress = { quotaProgress },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = if (quotaProgress > 0.85f) StatusRed else PrimaryAccent,
                    trackColor = DarkSurfaceElevated
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Quota Limit: ${quotaMb.toInt()} MB (Min: ${minQuota.toInt()} MB, Max: 1024 MB)", fontSize = 11.sp, color = TextMuted)
                }

                Slider(
                    value = quotaMb.coerceIn(minQuota, 1024f),
                    onValueChange = { quotaMb = it.coerceIn(minQuota, 1024f) },
                    valueRange = minQuota..1024f,
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryAccent,
                        activeTrackColor = PrimaryAccent,
                        inactiveTrackColor = DarkSurfaceElevated
                    )
                )
            }
        }

        Text("Language Configurations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            languages.forEach { (langKey, label) ->
                val isSelected = activeLang == langKey
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) PrimaryAccentContainer else DarkSurfaceElevated,
                    border = BorderStroke(1.dp, if (isSelected) PrimaryAccent else DarkBorderSubtle),
                    modifier = Modifier.clickable { activeLang = langKey }
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) PrimaryAccent else TextMuted,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        when (activeLang) {
            "PYTHON" -> {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Python Main Entry File", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        Text("Default entrypoint is app.py, followed by main.py. Specify the primary file to execute.", fontSize = 11.sp, color = TextSecondary)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = mainFileName,
                                onValueChange = { mainFileName = it },
                                placeholder = { Text("app.py", color = TextMuted) },
                                singleLine = true,
                                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = DarkSurfaceElevated,
                                    unfocusedContainerColor = DarkSurfaceElevated
                                )
                            )
                            Button(
                                onClick = {
                                    val trimmed = mainFileName.trim()
                                    if (trimmed.isNotBlank()) {
                                        Toast.makeText(context, "Saved primary file: $trimmed", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                            ) {
                                Text("Save", color = TextOnAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Virtual Environment & Requirements", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        Text("Strictly isolated into .venv. Use Regenerate to recreate, or Freeze to export dependencies.", fontSize = 11.sp, color = TextSecondary)

                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.regenerateVenv(project)
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                            border = BorderStroke(1.dp, PrimaryAccent.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Regenerate Environment", color = PrimaryAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                showFreezeDialog = true
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                            border = BorderStroke(1.dp, StatusGreen.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Freeze Requirements", color = StatusGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Environment (.env)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                            Text("${envVars.size} variables", fontSize = 11.sp, color = TextMuted)
                        }
                        Text("Stored as .env in project root. Automatically loaded when starting the server.", fontSize = 11.sp, color = TextSecondary)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = newEnvKey,
                                onValueChange = { newEnvKey = it },
                                placeholder = { Text("KEY", color = TextMuted, fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedContainerColor = DarkSurfaceElevated, unfocusedContainerColor = DarkSurfaceElevated)
                            )
                            OutlinedTextField(
                                value = newEnvValue,
                                onValueChange = { newEnvValue = it },
                                placeholder = { Text("VALUE", color = TextMuted, fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedContainerColor = DarkSurfaceElevated, unfocusedContainerColor = DarkSurfaceElevated)
                            )
                            Button(
                                onClick = {
                                    if (newEnvKey.isNotBlank()) {
                                        val updated = envVars.toMutableMap()
                                        updated[newEnvKey.trim()] = newEnvValue.trim()
                                        envVars = updated
                                        viewModel.saveDotEnv(project, updated)
                                        newEnvKey = ""
                                        newEnvValue = ""
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccentContainer)
                            ) {
                                Text("Add", color = PrimaryAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        if (envVars.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                envVars.forEach { (k, v) ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = DarkSurfaceElevated,
                                        border = BorderStroke(1.dp, DarkBorderSubtle),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("$k = $v", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                IconButton(
                                                    onClick = {
                                                        newEnvKey = k
                                                        newEnvValue = v
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Tune, contentDescription = "Edit", tint = PrimaryAccent, modifier = Modifier.size(16.dp))
                                                }
                                                IconButton(
                                                    onClick = {
                                                        val updated = envVars.toMutableMap()
                                                        updated.remove(k)
                                                        envVars = updated
                                                        viewModel.saveDotEnv(project, updated)
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusRed, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Python Version", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        Text("Select target Python runtime version for project execution.", fontSize = 11.sp, color = TextSecondary)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Python 3.11 (Default)", "Python 3.10", "Python 3.9").forEach { v ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (v.contains("3.11")) PrimaryAccentContainer else DarkSurfaceElevated,
                                    border = BorderStroke(1.dp, if (v.contains("3.11")) PrimaryAccent else DarkBorderSubtle),
                                    modifier = Modifier.clickable {
                                        Toast.makeText(context, "Selected $v", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text(v, fontSize = 11.sp, color = if (v.contains("3.11")) PrimaryAccent else TextMuted, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }

            "PHP" -> {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("PHP Main Entry File", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        Text("Default entry file is index.php. Specify custom startup script if required.", fontSize = 11.sp, color = TextSecondary)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = mainFileName,
                                onValueChange = { mainFileName = it },
                                placeholder = { Text("index.php", color = TextMuted) },
                                singleLine = true,
                                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = DarkSurfaceElevated,
                                    unfocusedContainerColor = DarkSurfaceElevated
                                )
                            )
                            Button(
                                onClick = {
                                    Toast.makeText(context, "Saved PHP entry: ${mainFileName.trim()}", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                            ) {
                                Text("Save", color = TextOnAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PHP Composer Management", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        }
                        Text("Composer dependency manager for PHP packages (composer.json / composer.lock).", fontSize = 11.sp, color = TextSecondary)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    viewModel.runProjectTerminalCommand(project, "composer install")
                                    Toast.makeText(context, "Running composer install...", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                                border = BorderStroke(1.dp, PrimaryAccent.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("composer install", color = PrimaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    viewModel.runProjectTerminalCommand(project, "composer update")
                                    Toast.makeText(context, "Running composer update...", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                                border = BorderStroke(1.dp, StatusGreen.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("composer update", color = StatusGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = customPackageInput,
                                onValueChange = { customPackageInput = it },
                                placeholder = { Text("require package (e.g. guzzlehttp/guzzle)", color = TextMuted, fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedContainerColor = DarkSurfaceElevated, unfocusedContainerColor = DarkSurfaceElevated)
                            )
                            Button(
                                onClick = {
                                    if (customPackageInput.isNotBlank()) {
                                        viewModel.runProjectTerminalCommand(project, "composer require ${customPackageInput.trim()}")
                                        customPackageInput = ""
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                            ) {
                                Text("Require", color = TextOnAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            "NODEJS" -> {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Node.js & NPM Settings", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        Text("Entrypoint default is server.js or index.js.", fontSize = 11.sp, color = TextSecondary)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = mainFileName,
                                onValueChange = { mainFileName = it },
                                placeholder = { Text("server.js", color = TextMuted) },
                                singleLine = true,
                                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = DarkSurfaceElevated,
                                    unfocusedContainerColor = DarkSurfaceElevated
                                )
                            )
                            Button(
                                onClick = {
                                    Toast.makeText(context, "Saved Node.js main: ${mainFileName.trim()}", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                            ) {
                                Text("Save", color = TextOnAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    viewModel.runProjectTerminalCommand(project, "npm install")
                                    Toast.makeText(context, "Running npm install...", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                                border = BorderStroke(1.dp, PrimaryAccent.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("npm install", color = PrimaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    viewModel.runProjectTerminalCommand(project, "npm run dev")
                                    Toast.makeText(context, "Running npm run dev...", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                                border = BorderStroke(1.dp, StatusGreen.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("npm run dev", color = StatusGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            "JAVA" -> {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Java Runtime & Build Configuration", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        Text("Configure main class or JAR file with build tools.", fontSize = 11.sp, color = TextSecondary)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = mainFileName,
                                onValueChange = { mainFileName = it },
                                placeholder = { Text("Main or App.jar", color = TextMuted) },
                                singleLine = true,
                                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = DarkSurfaceElevated,
                                    unfocusedContainerColor = DarkSurfaceElevated
                                )
                            )
                            Button(
                                onClick = {
                                    Toast.makeText(context, "Saved Java class: ${mainFileName.trim()}", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                            ) {
                                Text("Save", color = TextOnAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    viewModel.runProjectTerminalCommand(project, "javac *.java")
                                    Toast.makeText(context, "Compiling Java sources...", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                                border = BorderStroke(1.dp, PrimaryAccent.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("javac compile", color = PrimaryAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    viewModel.runProjectTerminalCommand(project, "jar cvfe App.jar Main *.class")
                                    Toast.makeText(context, "Packaging JAR...", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                                border = BorderStroke(1.dp, StatusGreen.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("jar package", color = StatusGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            "STATIC" -> {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Static HTML & Web Server", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        Text("Embedded Nginx/SAPI static document server.", fontSize = 11.sp, color = TextSecondary)

                        OutlinedTextField(
                            value = mainFileName,
                            onValueChange = { mainFileName = it },
                            placeholder = { Text("index.html", color = TextMuted) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = DarkSurfaceElevated,
                                unfocusedContainerColor = DarkSurfaceElevated
                            )
                        )
                    }
                }
            }
        }
    }

    if (showFreezeDialog) {
        AlertDialog(
            onDismissRequest = { showFreezeDialog = false },
            containerColor = DarkSurfaceElevated,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Freeze Requirements", fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Export installed dependencies into a requirements file.", fontSize = 12.sp, color = TextSecondary)
                    OutlinedTextField(
                        value = freezeFileName,
                        onValueChange = { freezeFileName = it },
                        label = { Text("Output File Name") },
                        placeholder = { Text("requirements.txt") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )
                    OutlinedTextField(
                        value = freezeFilePath,
                        onValueChange = { freezeFilePath = it },
                        label = { Text("Output Directory Path") },
                        placeholder = { Text("./ (project root)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val file = if (freezeFileName.isBlank()) "requirements.txt" else freezeFileName.trim()
                        val path = if (freezeFilePath.isBlank() || freezeFilePath.trim() == "./") file else "${freezeFilePath.trim().trimEnd('/')}/$file"
                        viewModel.freezeRequirements(project, path)
                        showFreezeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                ) {
                    Text("Freeze", color = TextOnAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFreezeDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun ControlTab(
    project: Project,
    viewModel: ProjectsViewModel,
    onBackClick: () -> Unit
) {
    val tunnelStatus by viewModel.tunnelStatus.collectAsState()
    val activeTunnelUrl by viewModel.activeTunnelUrl.collectAsState()
    val tunnelError by viewModel.tunnelError.collectAsState()
    val vpnDisabled by viewModel.isVpnDisabled(project.id).collectAsState(initial = false)
    val vpnBlockedMsgFlow by viewModel.getVpnBlockedMessage(project.id).collectAsState(initial = "VPN Detected, Please turn it off. If you think it is a mistake, contact the Developer.")
    var vpnBlockedMsg by remember(vpnBlockedMsgFlow) { mutableStateOf(vpnBlockedMsgFlow) }
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var namedTokenInput by remember { mutableStateOf("") }

    var showForwardPortDialog by remember { mutableStateOf(false) }
    var forwardPortInput by remember(project.port) { mutableStateOf(project.port.toString()) }
    val forwardedPort by viewModel.forwardedPort.collectAsState()
    val isForwarding = (forwardedPort != null && tunnelStatus == TunnelStatus.CONNECTED && activeTunnelUrl.isNotBlank())
    val isConnecting = (forwardedPort != null && tunnelStatus == TunnelStatus.CONNECTING)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Server Process Control", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    val isRunning = project.status == ProjectStatus.RUNNING
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isRunning) StatusGreen.copy(alpha = 0.15f) else TextMuted.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, if (isRunning) StatusGreen.copy(alpha = 0.4f) else DarkBorderSubtle)
                    ) {
                        Text(
                            text = if (isRunning) "ONLINE (: ${project.port})" else "STOPPED",
                            color = if (isRunning) StatusGreen else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                if (project.status == ProjectStatus.RUNNING) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.stopProject(project.id)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusYellow),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Stop Server", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.restartProject(project.id)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, tint = TextOnAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restart Server", color = TextOnAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.startProject(project)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start Server", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, if (isForwarding) StatusGreen.copy(alpha = 0.5f) else if (isConnecting) StatusOrange.copy(alpha = 0.5f) else DarkBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Port Forwarding", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    }

                    if (isForwarding) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StatusGreen.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, StatusGreen)
                        ) {
                            Text(
                                text = "Worldwide HTTPS Active",
                                color = StatusGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    } else if (isConnecting) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StatusOrange.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, StatusOrange)
                        ) {
                            Text(
                                text = "Connecting...",
                                color = StatusOrange,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                if (isForwarding) {
                    Text(
                        "Port $forwardedPort is actively forwarded with a public worldwide HTTPS link. Anyone around the world can access it.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkSurfaceElevated,
                        border = BorderStroke(1.dp, DarkBorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column {
                                Text("Worldwide HTTPS Link (Public Internet):", fontSize = 10.sp, color = TextMuted)
                                Text(
                                    text = activeTunnelUrl,
                                    fontSize = 12.sp,
                                    color = PrimaryAccent,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Forwarded URL", activeTunnelUrl)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Copied Worldwide HTTPS link", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, DarkBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy", fontSize = 12.sp, color = TextPrimary)
                                }

                                Button(
                                    onClick = {
                                        try {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(activeTunnelUrl))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Cannot open browser", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = TextOnAccent, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Open", fontSize = 12.sp, color = TextOnAccent)
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.stopPortForwarding() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRed),
                        border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Stop Port Forwarding", color = StatusRed, fontWeight = FontWeight.Bold)
                    }
                } else if (isConnecting) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = PrimaryAccent)
                    Text("Creating public worldwide HTTPS tunnel for port $forwardedPort...", color = TextSecondary, fontSize = 12.sp)

                    OutlinedButton(
                        onClick = { viewModel.stopPortForwarding() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRed),
                        border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel", color = StatusRed, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text("Forward any local port to a public worldwide HTTPS URL (like VS Code port forwarding) so anyone around the world can access it.", color = TextSecondary, fontSize = 12.sp)

                    Button(
                        onClick = {
                            forwardPortInput = project.port.toString()
                            showForwardPortDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                        border = BorderStroke(1.dp, PrimaryAccent.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Forward a Port...", color = PrimaryAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Cloud, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cloudflare Tunnel", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    }

                    val (tunnelLabel, tunnelColor) = when (tunnelStatus) {
                        TunnelStatus.DISCONNECTED -> Pair("Disconnected", TextMuted)
                        TunnelStatus.CONNECTING -> Pair("Connecting...", StatusOrange)
                        TunnelStatus.CONNECTED -> Pair("Connected", StatusGreen)
                        TunnelStatus.ERROR -> Pair("Error", StatusRed)
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = tunnelColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, tunnelColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = tunnelLabel,
                            color = tunnelColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Text(
                    "Expose your local project server securely to the public internet using your Cloudflare Tunnel Token.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                if (tunnelStatus == TunnelStatus.CONNECTED && activeTunnelUrl.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkSurfaceElevated,
                        border = BorderStroke(1.dp, StatusGreen.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Public Tunnel URL", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = activeTunnelUrl,
                                    color = StatusGreen,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Tunnel URL", activeTunnelUrl))
                                Toast.makeText(context, "URL copied to clipboard", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = StatusGreen, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.stopTunnel()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusRed.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Stop Tunnel", color = StatusRed, fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedTextField(
                        value = namedTokenInput,
                        onValueChange = { namedTokenInput = it },
                        label = { Text("Cloudflare Tunnel Token", fontSize = 12.sp) },
                        placeholder = { Text("eyJhIjoi...", color = TextMuted, fontSize = 12.sp) },
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

                    Button(
                        onClick = {
                            val token = namedTokenInput.trim()
                            if (token.isBlank()) {
                                Toast.makeText(context, "Please enter your Cloudflare tunnel token", Toast.LENGTH_SHORT).show()
                            } else {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.startNamedTunnel(token)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Cloud, contentDescription = null, tint = TextOnAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Connect Cloudflare Tunnel", color = TextOnAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Disable VPN Visits", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Block incoming server requests when active VPN connections are detected.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                    Switch(
                        checked = vpnDisabled,
                        onCheckedChange = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setVpnDisabled(project.id, it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PrimaryAccent,
                            checkedTrackColor = PrimaryAccentContainer,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkSurfaceElevated
                        )
                    )
                }

                HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(vertical = 4.dp))

                Text("VPN Error Message", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                Text(
                    "Custom error message served to flagged VPN visitors. Displayed on responsive block page alongside visitor IP & terminal error code.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                OutlinedTextField(
                    value = vpnBlockedMsg,
                    onValueChange = { vpnBlockedMsg = it },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(10.dp),
                    textStyle = TextStyle(fontSize = 13.sp, color = TextPrimary),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryAccent,
                        unfocusedBorderColor = DarkBorder,
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated,
                        cursorColor = PrimaryAccent
                    ),
                    placeholder = {
                        Text("VPN Detected, Please turn it off. If you think it is a mistake, contact the Developer.", fontSize = 12.sp, color = TextMuted)
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setVpnBlockedMessage(project.id, vpnBlockedMsg.trim())
                            Toast.makeText(context, "VPN Error Message Saved", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Error Message", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Danger Zone", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = StatusRed)
                Text(
                    "Permanently delete this project and erase all associated files and logs from storage.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Button(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = TextOnAccent, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete Project", color = TextOnAccent, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showForwardPortDialog) {
        AlertDialog(
            onDismissRequest = { showForwardPortDialog = false },
            containerColor = DarkSurfaceElevated,
            title = { Text("Forward a Port", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter the local port you want to tunnel to a public worldwide HTTPS URL (like VS Code port forwarding):", color = TextSecondary, fontSize = 13.sp)
                    OutlinedTextField(
                        value = forwardPortInput,
                        onValueChange = { forwardPortInput = it },
                        label = { Text("Port to Forward", fontSize = 11.sp, color = TextMuted) },
                        placeholder = { Text("${project.port}", color = TextMuted, fontSize = 12.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedContainerColor = DarkSurface, unfocusedContainerColor = DarkSurface)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = forwardPortInput.toIntOrNull() ?: project.port
                        showForwardPortDialog = false
                        viewModel.startPortForwarding(p)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                ) {
                    Text("Forward Port", color = TextOnAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForwardPortDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = DarkSurfaceElevated,
            title = { Text("Delete '${project.name}'?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("This will permanently remove '${project.name}' and delete all files in ${project.workingDir}. This action cannot be undone.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteProject(project.id)
                        onBackClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) {
                    Text("Permanently Delete", color = TextOnAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}

@Composable
private fun LogsAndTerminalTab(
    project: Project,
    viewModel: ProjectsViewModel
) {
    var subTab by remember { mutableIntStateOf(0) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurfaceElevated, RoundedCornerShape(10.dp))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (subTab == 0) PrimaryAccent else Color.Transparent)
                    .clickable { subTab = 0 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Terminal",
                    color = if (subTab == 0) TextOnAccent else TextMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (subTab == 1) PrimaryAccent else Color.Transparent)
                    .clickable { subTab = 1 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Log",
                    color = if (subTab == 1) TextOnAccent else TextMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        if (subTab == 0) {
            ProjectTerminalTab(project = project, viewModel = viewModel)
        } else {
            ProjectLiveLogsView(project = project, viewModel = viewModel)
        }
    }
}

@Composable
private fun ProjectLiveLogsView(
    project: Project,
    viewModel: ProjectsViewModel
) {
    val logs by viewModel.observeLogs(project.id).collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var streamFilter by remember { mutableStateOf<com.localhost.core.model.LogStream?>(null) }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }

    val filteredLogs = remember(logs, searchQuery, streamFilter) {
        logs.filter { entry ->
            (streamFilter == null || entry.stream == streamFilter) &&
            (searchQuery.isBlank() || entry.message.contains(searchQuery, ignoreCase = true))
        }
    }

    LaunchedEffect(filteredLogs.size) {
        if (filteredLogs.isNotEmpty()) {
            listState.scrollToItem(filteredLogs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter logs...", color = TextMuted, fontSize = 13.sp) },
                singleLine = true,
                textStyle = TextStyle(fontSize = 13.sp, color = TextPrimary),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = PrimaryAccent,
                    unfocusedBorderColor = DarkBorderSubtle,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedPlaceholderColor = TextMuted,
                    unfocusedPlaceholderColor = TextMuted
                )
            )

            IconButton(
                onClick = {
                    val allText = filteredLogs.joinToString("\n") { "[${timeFormatter.format(Date(it.timestamp))}] [${it.stream.name}] ${it.message}" }
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Logs", allText))
                    Toast.makeText(context, "Copied ${filteredLogs.size} lines", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy logs", tint = TextSecondary, modifier = Modifier.size(18.dp))
            }

            IconButton(
                onClick = {
                    val allText = filteredLogs.joinToString("\n") { "[${timeFormatter.format(Date(it.timestamp))}] [${it.stream.name}] ${it.message}" }
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "${project.name} Logs")
                        putExtra(Intent.EXTRA_TEXT, allText)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Logs"))
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Export logs", tint = PrimaryAccent, modifier = Modifier.size(18.dp))
            }

            IconButton(
                onClick = { viewModel.clearLogs(project.id) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Clear logs", tint = TextMuted, modifier = Modifier.size(18.dp))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val filters = listOf(
                null to "ALL",
                com.localhost.core.model.LogStream.STDOUT to "STDOUT",
                com.localhost.core.model.LogStream.STDERR to "STDERR",
                com.localhost.core.model.LogStream.SYSTEM to "SYSTEM"
            )
            filters.forEach { (stream, label) ->
                val active = streamFilter == stream
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (active) PrimaryAccentContainer else DarkSurface,
                    border = BorderStroke(1.dp, if (active) PrimaryAccent else DarkBorderSubtle),
                    onClick = { streamFilter = stream },
                    modifier = Modifier.height(28.dp)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                        Text(label, fontSize = 11.sp, color = if (active) PrimaryAccent else TextMuted, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = CodeBackground,
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            if (filteredLogs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No log entries recorded", color = TextMuted, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredLogs, key = { it.id }) { log ->
                        val timeStr = remember(log.timestamp) { timeFormatter.format(Date(log.timestamp)) }
                        val textColor = when (log.stream) {
                            com.localhost.core.model.LogStream.STDOUT -> TextPrimary
                            com.localhost.core.model.LogStream.STDERR -> StatusRed
                            com.localhost.core.model.LogStream.SYSTEM -> SecondaryTeal
                        }
                        val streamColor = when (log.stream) {
                            com.localhost.core.model.LogStream.STDERR -> StatusRed
                            com.localhost.core.model.LogStream.SYSTEM -> SecondaryTeal
                            else -> PrimaryAccent
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(CodeBackground)
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Log Line", "[$timeStr] [${log.stream.name}] ${log.message}"))
                                    Toast.makeText(context, "Copied log line", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 4.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = timeStr,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = TextMuted,
                                modifier = Modifier.width(88.dp)
                            )
                            Text(
                                text = "[${log.stream.name}]",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = streamColor,
                                modifier = Modifier.width(72.dp)
                            )
                            Text(
                                text = log.message,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = textColor,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsTab(
    project: Project,
    viewModel: ProjectsViewModel
) {
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    var showOnlyErrors by remember { mutableStateOf(false) }
    val filterLabels = listOf("Past 3 hr", "Today (12 am)", "This week", "This month", "All time")

    val sinceTimestamp = remember(selectedFilterIndex) {
        val current = System.currentTimeMillis()
        when (selectedFilterIndex) {
            0 -> current - (3 * 3600 * 1000L)
            1 -> {
                val cal = java.util.Calendar.getInstance()
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            2 -> current - (7 * 86400 * 1000L)
            3 -> current - (30 * 86400 * 1000L)
            else -> 0L
        }
    }

    val requests by viewModel.observeRequests(project.id, sinceTimestamp).collectAsState(initial = emptyList())
    val totalRequests = requests.size
    val totalBytes = requests.sumOf { it.responseBytes }
    val visitsCount = requests.count { it.method == "GET" && (it.path == "/" || it.path.endsWith(".html") || !it.path.contains(".")) }
    val success2xx = requests.count { it.statusCode in 200..299 }
    val redirect3xx = requests.count { it.statusCode in 300..399 }
    val client4xx = requests.count { it.statusCode in 400..499 }
    val server5xx = requests.count { it.statusCode in 500..599 }
    val totalErrors = requests.count { it.statusCode >= 400 || it.method == "ERR" }
    val successRate = if (totalRequests > 0) (success2xx * 100 / totalRequests) else 100

    val getCount = requests.count { it.method == "GET" }
    val postCount = requests.count { it.method == "POST" }
    val putCount = requests.count { it.method == "PUT" }
    val deleteCount = requests.count { it.method == "DELETE" }
    val patchCount = requests.count { it.method == "PATCH" }
    val errCount = requests.count { it.method == "ERR" }

    val dataTransferredText = when {
        totalBytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", totalBytes / (1024.0 * 1024.0))
        totalBytes >= 1024 -> String.format(Locale.US, "%.1f KB", totalBytes / 1024.0)
        else -> "$totalBytes B"
    }

    val displayRequests = remember(requests, showOnlyErrors) {
        if (showOnlyErrors) requests.filter { it.statusCode >= 400 || it.method == "ERR" }
        else requests
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filterLabels.forEachIndexed { index, label ->
                val active = selectedFilterIndex == index
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (active) PrimaryAccentContainer else DarkSurface,
                    border = BorderStroke(1.dp, if (active) PrimaryAccent else DarkBorder),
                    onClick = { selectedFilterIndex = index },
                    modifier = Modifier.height(34.dp)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            color = if (active) PrimaryAccent else TextMuted
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Total Calls", color = TextMuted, fontSize = 11.sp)
                    Text("$totalRequests", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("requests", color = TextSecondary, fontSize = 10.sp)
                }
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Visits", color = TextMuted, fontSize = 11.sp)
                    Text("$visitsCount", color = PrimaryAccent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("page hits", color = TextSecondary, fontSize = 10.sp)
                }
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Success", color = TextMuted, fontSize = 11.sp)
                    Text("$successRate%", color = StatusGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("$success2xx OK", color = TextSecondary, fontSize = 10.sp)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, if (totalErrors > 0) StatusRed.copy(alpha = 0.5f) else DarkBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Errors", color = TextMuted, fontSize = 11.sp)
                    Text("$totalErrors", color = if (totalErrors > 0) StatusRed else TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("4xx/5xx logs", color = TextSecondary, fontSize = 10.sp)
                }
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Data Usage", color = TextMuted, fontSize = 11.sp)
                    Text(dataTransferredText, color = PrimaryAccent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("bandwidth", color = TextSecondary, fontSize = 10.sp)
                }
            }
        }

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Request Volume Analytics (Interactive)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)

                val (lineCounts, lineTimeLabels) = remember(requests, sinceTimestamp) {
                    val count = 12
                    val current = System.currentTimeMillis()
                    val span = (current - sinceTimestamp).coerceAtLeast(1000L)
                    val buckets = IntArray(count)
                    val stepTime = span / count
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val labels = (0 until count).map { i ->
                        timeFormat.format(Date(sinceTimestamp + i * stepTime))
                    }
                    requests.forEach { r ->
                        val offset = r.timestamp - sinceTimestamp
                        if (offset in 0 until span) {
                            val bucketIdx = ((offset * count) / span).toInt().coerceIn(0, count - 1)
                            buckets[bucketIdx]++
                        }
                    }
                    Pair(buckets.toList(), labels)
                }

                InteractiveLineGraph(
                    counts = lineCounts,
                    timeLabels = lineTimeLabels,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("HTTP Methods Distribution", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)

                if (totalRequests > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                    ) {
                        val methodColors = listOf(
                            Pair(getCount, Color(0xFF10B981)),
                            Pair(postCount, Color(0xFFF59E0B)),
                            Pair(putCount, Color(0xFF6366F1)),
                            Pair(deleteCount, Color(0xFFF43F5E)),
                            Pair(patchCount, Color(0xFF06B6D4)),
                            Pair(errCount, Color(0xFFE11D48))
                        )
                        methodColors.forEach { (count, color) ->
                            if (count > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(count.toFloat())
                                        .fillMaxHeight()
                                        .background(color)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MethodBadgeItem("GET", getCount, totalRequests, Color(0xFF10B981))
                    MethodBadgeItem("POST", postCount, totalRequests, Color(0xFFF59E0B))
                    MethodBadgeItem("PUT", putCount, totalRequests, Color(0xFF6366F1))
                    MethodBadgeItem("DEL", deleteCount, totalRequests, Color(0xFFF43F5E))
                    MethodBadgeItem("ERR", errCount, totalRequests, Color(0xFFE11D48))
                }
            }
        }

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("HTTP Status Codes Breakdown", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)

                StatusBreakdownRow(label = "2xx Success", count = success2xx, total = totalRequests, color = StatusGreen)
                StatusBreakdownRow(label = "3xx Redirection", count = redirect3xx, total = totalRequests, color = PrimaryAccent)
                StatusBreakdownRow(label = "4xx Client Error", count = client4xx, total = totalRequests, color = StatusOrange)
                StatusBreakdownRow(label = "5xx Server Error", count = server5xx, total = totalRequests, color = StatusRed)
            }
        }

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Persistent Request & Error History", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        Text("${displayRequests.size} recorded entries", fontSize = 11.sp, color = TextMuted)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (showOnlyErrors) StatusRed.copy(alpha = 0.2f) else DarkSurfaceElevated,
                            border = BorderStroke(1.dp, if (showOnlyErrors) StatusRed else DarkBorderSubtle),
                            modifier = Modifier.clickable { showOnlyErrors = !showOnlyErrors }
                        ) {
                            Text(
                                "Errors Only",
                                fontSize = 11.sp,
                                color = if (showOnlyErrors) StatusRed else TextSecondary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.clearRequests(project.id) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear History", tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                if (displayRequests.isEmpty()) {
                    Text("No requests captured in this time window.", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
                } else {
                    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
                    displayRequests.take(60).forEach { r ->
                        val statusColor = when (r.statusCode) {
                            in 200..299 -> StatusGreen
                            in 300..399 -> PrimaryAccent
                            in 400..499 -> StatusOrange
                            else -> StatusRed
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkSurfaceElevated,
                            border = BorderStroke(1.dp, DarkBorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = statusColor.copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                                        ) {
                                            Text(
                                                "${r.statusCode}",
                                                color = statusColor,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(r.method, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PrimaryAccent)
                                        Text(r.path, fontSize = 12.sp, color = TextPrimary, fontFamily = FontFamily.Monospace)
                                    }
                                    Text(dateFormat.format(Date(r.timestamp)), fontSize = 10.sp, color = TextMuted)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${r.durationMs}ms latency", fontSize = 10.sp, color = TextMuted)
                                    if (r.responseBytes > 0) {
                                        Text("${r.responseBytes} bytes", fontSize = 10.sp, color = TextMuted)
                                    }
                                }
                                r.errorMessage?.let { err ->
                                    Text(err, color = StatusRed, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MethodBadgeItem(method: String, count: Int, total: Int, color: Color) {
    val pct = if (total > 0) (count * 100 / total) else 0
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = color.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
        ) {
            Text(method, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text("$count", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text("$pct%", fontSize = 9.sp, color = TextMuted)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextSecondary, fontSize = 13.sp)
        Text(text = value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StatusBreakdownRow(label: String, count: Int, total: Int, color: Color) {
    val percent = if (total > 0) (count * 100 / total) else 0
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
            Text("$count ($percent%)", fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(DarkSurfaceElevated)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (total > 0) (count.toFloat() / total).coerceIn(0f, 1f) else 0f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun ProjectTerminalTab(
    project: Project,
    viewModel: ProjectsViewModel
) {
    val terminalsMap by viewModel.projectTerminals.collectAsState()
    val runningMap by viewModel.projectTerminalRunning.collectAsState()
    val isRunning = runningMap[project.id] == true
    val history = remember(terminalsMap, project.id) {
        terminalsMap[project.id] ?: listOf(ProjectTerminalLine("", "Project Terminal Ready\n"))
    }

    var input by remember { mutableStateOf("") }
    var historyIndex by remember { mutableIntStateOf(-1) }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val isPython = project.runtime == RuntimeType.PYTHON

    val pipQuickActions = listOf(
        "install -r reqs" to "pip install -r requirements.txt",
        "pip list" to "pip list",
        "pip check" to "pip check",
        "pip outdated" to "pip list --outdated",
        "rebuild venv" to "python3 -m venv --system-site-packages .venv"
    )

    val accessoryKeys = if (isPython) {
        listOf("Tab", "Ctrl+C", "clear", "ls -la", "pwd", "pip list", "python app.py")
    } else {
        listOf("Tab", "Ctrl+C", "clear", "ls -la", "pwd", "cat", "rmdir")
    }

    val lastOutputLength = history.lastOrNull()?.output?.length ?: 0
    LaunchedEffect(history.size, lastOutputLength) {
        if (history.isNotEmpty()) {
            listState.scrollToItem(history.size - 1, 10000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isPython) {
            Surface(
                color = DarkSurface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, DarkBorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "PIP OPTIONS",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = PrimaryAccent
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        pipQuickActions.forEach { (label, command) ->
                            Surface(
                                color = DarkSurfaceElevated,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, PrimaryAccent.copy(alpha = 0.4f)),
                                modifier = Modifier.clickable {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.runProjectTerminalCommand(project, command)
                                }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(CodeBackground)
                .border(1.dp, DarkBorderSubtle, RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "user@localhost:/projects/${project.name}$",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = StatusYellow,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (isRunning) {
                            Surface(
                                color = StatusRed.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.clickable { viewModel.stopProjectTerminalCommand(project.id) }
                            ) {
                                Text(
                                    "STOP",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusRed,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Surface(
                            color = DarkSurfaceElevated,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.clickable { viewModel.clearProjectTerminal(project.id) }
                        ) {
                            Text(
                                "CLEAR",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                SelectionContainer(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(history) { item ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val text = if (item.command.isNotBlank()) "$ ${item.command}\n${item.output}" else item.output
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Terminal Output", text)
                                        clipboard.setPrimaryClip(clip)
                                    }
                            ) {
                                if (item.command.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "$ ",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = StatusGreen
                                        )
                                        Text(
                                            text = item.command,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                    }
                                }
                                if (item.output.isNotBlank()) {
                                    val isError = item.output.contains("Error", ignoreCase = true) || item.output.contains("Failed", ignoreCase = true)
                                    Text(
                                        text = item.output,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        color = if (isError) StatusRed else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = DarkSurface,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, DarkBorderSubtle),
                modifier = Modifier.clickable {
                    val commands = history.map { it.command }.filter { it.isNotBlank() }
                    if (commands.isNotEmpty() && historyIndex < commands.size - 1) {
                        historyIndex++
                        input = commands[commands.size - 1 - historyIndex]
                    }
                }
            ) {
                Text("↑", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
            }

            Surface(
                color = DarkSurface,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, DarkBorderSubtle),
                modifier = Modifier.clickable {
                    val commands = history.map { it.command }.filter { it.isNotBlank() }
                    if (commands.isNotEmpty() && historyIndex > 0) {
                        historyIndex--
                        input = commands[commands.size - 1 - historyIndex]
                    } else if (historyIndex == 0) {
                        historyIndex = -1
                        input = ""
                    }
                }
            ) {
                Text("↓", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
            }

            accessoryKeys.forEach { key ->
                Surface(
                    color = DarkSurface,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, DarkBorderSubtle),
                    modifier = Modifier.clickable {
                        when (key) {
                            "Ctrl+C" -> {
                                viewModel.stopProjectTerminalCommand(project.id)
                                input = ""
                            }
                            "clear" -> viewModel.clearProjectTerminal(project.id)
                            "ls -la" -> viewModel.runProjectTerminalCommand(project, "ls -la")
                            "pwd" -> viewModel.runProjectTerminalCommand(project, "pwd")
                            "pip list" -> viewModel.runProjectTerminalCommand(project, "pip list")
                            else -> input += key
                        }
                    }
                ) {
                    Text(
                        text = key,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Surface(
            color = DarkSurface,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, DarkBorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$ ",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = StatusGreen
                )

                BasicTextField(
                    value = input,
                    onValueChange = { input = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = TextPrimary
                    ),
                    cursorBrush = SolidColor(PrimaryAccent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (input.isNotBlank()) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.runProjectTerminalCommand(project, input)
                            input = ""
                            historyIndex = -1
                        }
                    }),
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        if (input.isNotBlank()) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.runProjectTerminalCommand(project, input)
                            input = ""
                            historyIndex = -1
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Run",
                        tint = if (input.isNotBlank()) PrimaryAccent else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InteractiveLineGraph(
    counts: List<Int>,
    timeLabels: List<String>,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val maxCount = (counts.maxOrNull() ?: 1).coerceAtLeast(1)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (selectedIndex != null && selectedIndex!! in counts.indices) {
            val idx = selectedIndex!!
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(PrimaryAccentContainer)
                    .border(1.dp, PrimaryAccent.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Time: ${timeLabels.getOrElse(idx) { "" }}",
                    fontSize = 11.sp,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${counts[idx]} requests",
                    fontSize = 11.sp,
                    color = PrimaryAccent,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Text("Tap or drag across line graph to view exact request counts", fontSize = 11.sp, color = TextMuted)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(counts) {
                        detectTapGestures { offset ->
                            if (counts.isNotEmpty()) {
                                val stepX = size.width / (counts.size - 1).coerceAtLeast(1)
                                val idx = Math.round(offset.x / stepX).toInt().coerceIn(0, counts.size - 1)
                                selectedIndex = idx
                            }
                        }
                    }
                    .pointerInput(counts) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            if (counts.isNotEmpty()) {
                                val stepX = size.width / (counts.size - 1).coerceAtLeast(1)
                                val idx = Math.round(change.position.x / stepX).toInt().coerceIn(0, counts.size - 1)
                                selectedIndex = idx
                            }
                        }
                    }
            ) {
                val width = size.width
                val height = size.height
                val pointCount = counts.size
                if (pointCount < 2) return@Canvas

                val stepX = width / (pointCount - 1)
                val topPadding = 12.dp.toPx()
                val bottomPadding = 12.dp.toPx()
                val usableHeight = height - topPadding - bottomPadding

                listOf(0f, 0.5f, 1f).forEach { fraction ->
                    val y = topPadding + usableHeight * fraction
                    drawLine(
                        color = Color.White.copy(alpha = 0.08f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val points = counts.mapIndexed { i, c ->
                    val x = i * stepX
                    val y = topPadding + usableHeight * (1f - (c.toFloat() / maxCount))
                    Offset(x, y)
                }

                val fillPath = Path().apply {
                    moveTo(0f, height)
                    lineTo(points.first().x, points.first().y)
                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val cx = (p0.x + p1.x) / 2f
                        cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                    }
                    lineTo(width, height)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(PrimaryAccent.copy(alpha = 0.35f), Color.Transparent),
                        startY = topPadding,
                        endY = height
                    )
                )

                val strokePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val cx = (p0.x + p1.x) / 2f
                        cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                    }
                }

                drawPath(
                    path = strokePath,
                    color = PrimaryAccent,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                points.forEachIndexed { i, pt ->
                    val isSel = selectedIndex == i
                    drawCircle(
                        color = if (isSel) Color.White else PrimaryAccent,
                        radius = if (isSel) 6.dp.toPx() else 3.5.dp.toPx(),
                        center = pt
                    )
                    if (isSel) {
                        drawCircle(
                            color = PrimaryAccent,
                            radius = 3.5.dp.toPx(),
                            center = pt
                        )
                        drawLine(
                            color = PrimaryAccent.copy(alpha = 0.5f),
                            start = Offset(pt.x, topPadding),
                            end = Offset(pt.x, height),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            timeLabels.filterIndexed { index, _ -> index % 3 == 0 || index == timeLabels.lastIndex }.forEach { label ->
                Text(label, fontSize = 10.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
            }
        }
    }
}
