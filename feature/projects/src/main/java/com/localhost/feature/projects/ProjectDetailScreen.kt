package com.localhost.feature.projects

import kotlinx.coroutines.launch

import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.graphics.asImageBitmap
import com.localhost.core.common.NetworkUtils
import com.localhost.core.common.QrCodeGenerator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.localhost.core.designsystem.component.StatusChip
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
import java.util.Date
import java.util.Locale

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
    val project = projects.firstOrNull { it.id == projectId }
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var selectedTab by remember { mutableIntStateOf(0) }
    var qrDialogUrl by remember { mutableStateOf<String?>(null) }
    val tabs = listOf("Overview", "Snapshots", "Packages", "Env Vars", "Control")
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    fun shareExportedZip() {
        if (project == null) return
        scope.launch {
            isExporting = true
            Toast.makeText(context, "Exporting ${project.name} ZIP...", Toast.LENGTH_SHORT).show()
            try {
                val zipFile = viewModel.exportProjectZip(project, context)
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

    if (project == null) {
        Scaffold(
            containerColor = DarkBackground,
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(project.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                        Text(project.runtime.displayName, fontSize = 12.sp, color = TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    if (project.status == ProjectStatus.RUNNING) {
                        IconButton(onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            val ip = NetworkUtils.getLocalIpAddress() ?: "127.0.0.1"
                            qrDialogUrl = "http://$ip:${project.port}"
                        }) {
                            Icon(Icons.Default.QrCode, contentDescription = "QR Code & Link", tint = PrimaryAccent)
                        }
                    }
                    IconButton(onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        showEditDialog = true
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Project", tint = TextPrimary)
                    }
                    IconButton(
                        onClick = {
                            if (!isExporting) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                shareExportedZip()
                            }
                        },
                        enabled = !isExporting
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = PrimaryAccent,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Share, contentDescription = "Export ZIP", tint = PrimaryAccent)
                        }
                    }
                    IconButton(onClick = { onOpenFiles(project.id) }) {
                        Icon(Icons.Default.Folder, contentDescription = "Files", tint = TextSecondary)
                    }
                    IconButton(onClick = { onOpenLogs(project.id) }) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Logs", tint = TextSecondary)
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
                        onOpenLogs = onOpenLogs,
                        onOpenFiles = onOpenFiles,
                        onExportZip = { shareExportedZip() }
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
                            .pointerInput(selectedTab) {
                                var accumulated = 0f
                                detectHorizontalDragGestures(
                                    onDragStart = { accumulated = 0f },
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        accumulated += dragAmount
                                    },
                                    onDragEnd = {
                                        if (accumulated < -60f && selectedTab < tabs.size - 1) {
                                            selectedTab++
                                        } else if (accumulated > 60f && selectedTab > 0) {
                                            selectedTab--
                                        }
                                    }
                                )
                            }
                    ) {
                        when (selectedTab) {
                            0 -> OverviewTab(project = project, onOpenLogs = onOpenLogs, onOpenFiles = onOpenFiles, onExportZip = { shareExportedZip() })
                            1 -> SnapshotsTab(project = project, viewModel = viewModel)
                            2 -> PackagesTab(project = project, viewModel = viewModel)
                            3 -> EnvironmentTab(project = project, viewModel = viewModel)
                            4 -> ControlTab(project = project, viewModel = viewModel, onBackClick = onBackClick)
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .pointerInput(selectedTab) {
                        var accumulated = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { accumulated = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                accumulated += dragAmount
                            },
                            onDragEnd = {
                                if (accumulated < -70f && selectedTab < tabs.size - 1) {
                                    selectedTab++
                                } else if (accumulated > 70f) {
                                    if (selectedTab > 0) {
                                        selectedTab--
                                    } else {
                                        onBackClick()
                                    }
                                }
                            }
                        )
                    }
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
                                    fontSize = 14.sp
                                )
                            }
                        )
                    }
                }

                when (selectedTab) {
                    0 -> OverviewTab(project = project, onOpenLogs = onOpenLogs, onOpenFiles = onOpenFiles, onExportZip = { shareExportedZip() })
                    1 -> SnapshotsTab(project = project, viewModel = viewModel)
                    2 -> PackagesTab(project = project, viewModel = viewModel)
                    3 -> EnvironmentTab(project = project, viewModel = viewModel)
                    4 -> ControlTab(project = project, viewModel = viewModel, onBackClick = onBackClick)
                }
            }
        }
    }

    if (showEditDialog && project != null) {
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
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
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
}

@Composable
private fun OverviewTab(
    project: Project,
    onOpenLogs: (String) -> Unit,
    onOpenFiles: (String) -> Unit,
    onExportZip: () -> Unit
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
            onOpenLogs = onOpenLogs,
            onOpenFiles = onOpenFiles,
            onExportZip = onExportZip
        )
    }
}

@Composable
private fun OverviewTabContent(
    project: Project,
    onOpenLogs: (String) -> Unit,
    onOpenFiles: (String) -> Unit,
    onExportZip: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val localIp = remember(project.id) { NetworkUtils.getLocalIpAddress() ?: "127.0.0.1" }
    val lanUrl = "http://$localIp:${project.port}"
    val loopbackUrl = "http://127.0.0.1:${project.port}"
    var showFullQrModal by remember { mutableStateOf(false) }

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
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(lanUrl)).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
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

            DetailRow(label = "Local URL", value = "http://127.0.0.1:${project.port}")
            DetailRow(label = "Port", value = "${project.port}")
            DetailRow(label = "Runtime", value = project.runtime.displayName)
            DetailRow(label = "Directory", value = project.workingDir)
            DetailRow(label = "Command", value = project.startupCommand)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://127.0.0.1:${project.port}"))
                context.startActivity(intent)
            },
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, DarkBorder),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Browser", color = TextPrimary, fontSize = 12.sp, maxLines = 1, softWrap = false)
        }

        OutlinedButton(
            onClick = { onOpenFiles(project.id) },
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, DarkBorder),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Folder, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Files", color = TextPrimary, fontSize = 12.sp, maxLines = 1, softWrap = false)
        }

        OutlinedButton(
            onClick = { onOpenLogs(project.id) },
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, DarkBorder),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Logs", color = TextPrimary, fontSize = 12.sp, maxLines = 1, softWrap = false)
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
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Project (.zip)", color = PrimaryAccent, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SnapshotsTab(
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
private fun PackagesTab(
    project: Project,
    viewModel: ProjectsViewModel
) {
    val isPython = project.runtime == RuntimeType.PYTHON
    val isNode = project.runtime == RuntimeType.NODEJS
    val isInstalling by viewModel.isInstallingDeps.collectAsState()
    var customPackageInput by remember { mutableStateOf("") }
    var requirementsFileInput by remember { mutableStateOf("requirements.txt") }
    var packageToDelete by remember { mutableStateOf<String?>(null) }
    val haptics = LocalHapticFeedback.current

    val requirementsFiles = remember(project.id) {
        if (isPython) viewModel.getRequirementsFiles(project) else emptyList()
    }

    val popularPackages = remember(project.runtime) {
        when (project.runtime) {
            RuntimeType.PYTHON -> listOf("flask", "requests", "fastapi", "uvicorn", "pydantic", "django", "pytest", "python-dotenv")
            RuntimeType.NODEJS -> listOf("express", "cors", "dotenv", "ws", "axios", "mongoose", "nodemon", "jsonwebtoken")
            else -> emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!isPython && !isNode) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${project.runtime.displayName} projects use built-in execution. Custom package managers are active for Python (pip) and Node.js (npm).",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            if (isPython) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Install from Requirements (.txt)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                        }

                        Text(
                            "Validate and install dependencies from a requirements text file via pip.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )

                        if (requirementsFiles.isNotEmpty()) {
                            Text("Available in project:", fontSize = 11.sp, color = TextMuted)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                requirementsFiles.forEach { file ->
                                    val isSelected = requirementsFileInput.equals(file, ignoreCase = true)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) PrimaryAccentContainer else DarkSurfaceElevated,
                                        border = BorderStroke(1.dp, if (isSelected) PrimaryAccent else DarkBorderSubtle),
                                        onClick = {
                                            requirementsFileInput = file
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isSelected) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                            }
                                            Text(file, fontSize = 11.sp, color = if (isSelected) PrimaryAccent else TextPrimary, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = requirementsFileInput,
                                onValueChange = { requirementsFileInput = it },
                                placeholder = { Text("e.g. requirements.txt", color = TextMuted) },
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
                                    if (requirementsFileInput.isNotBlank()) {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.installPipRequirements(project, requirementsFileInput.trim())
                                    }
                                },
                                enabled = !isInstalling && requirementsFileInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (isInstalling) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = TextOnAccent, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Download, contentDescription = null, tint = TextOnAccent, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Install .txt", fontWeight = FontWeight.Bold, color = TextOnAccent, fontSize = 13.sp)
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
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isPython) "Install Python Package (pip)" else "Install Node.js Package (npm)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customPackageInput,
                            onValueChange = { customPackageInput = it },
                            placeholder = { Text(if (isPython) "e.g. beautifulsoup4" else "e.g. lodash", color = TextMuted) },
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
                                if (customPackageInput.isNotBlank()) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.installCustomPackage(project, customPackageInput)
                                    customPackageInput = ""
                                }
                            },
                            enabled = !isInstalling && customPackageInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isInstalling) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = TextOnAccent, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Download, contentDescription = null, tint = TextOnAccent, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            if (popularPackages.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Popular Libraries", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            popularPackages.forEach { pkg ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = DarkSurfaceElevated,
                                    border = BorderStroke(1.dp, DarkBorderSubtle),
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.installCustomPackage(project, pkg)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(pkg, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isNode) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Manifest Dependencies", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        Text(
                            "Install all packages declared in package.json",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.installCustomPackage(project, "")
                            },
                            enabled = !isInstalling,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                            border = BorderStroke(1.dp, DarkBorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Run npm install", color = PrimaryAccent, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    packageToDelete?.let { pkg ->
        AlertDialog(
            onDismissRequest = { packageToDelete = null },
            containerColor = DarkSurfaceElevated,
            title = { Text("Uninstall '$pkg'?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("This will remove $pkg from the environment.", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.uninstallCustomPackage(project, pkg)
                        packageToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = StatusRed)
                ) {
                    Text("Uninstall", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { packageToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun EnvironmentTab(
    project: Project,
    viewModel: ProjectsViewModel
) {
    var newKey by remember { mutableStateOf("") }
    var newValue by remember { mutableStateOf("") }
    var isSecret by remember { mutableStateOf(false) }
    var showRawEnvDialog by remember { mutableStateOf(false) }
    var rawEnvText by remember { mutableStateOf("") }
    var visibleKeys by remember { mutableStateOf(setOf<String>()) }
    val haptics = LocalHapticFeedback.current

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
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Add Variable", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)

                OutlinedTextField(
                    value = newKey,
                    onValueChange = { newKey = it },
                    placeholder = { Text("KEY (e.g. API_KEY)", color = TextMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
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

                OutlinedTextField(
                    value = newValue,
                    onValueChange = { newValue = it },
                    placeholder = { Text("VALUE", color = TextMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = isSecret,
                            onCheckedChange = { isSecret = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextOnAccent,
                                checkedTrackColor = PrimaryAccent,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkSurfaceElevated
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mask Value (Secret)", color = TextSecondary, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            if (newKey.isNotBlank()) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.addEnvironmentVar(project.id, newKey, newValue, isSecret)
                                newKey = ""
                                newValue = ""
                                isSecret = false
                            }
                        },
                        enabled = newKey.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                    ) {
                        Text("Add", color = TextOnAccent, fontWeight = FontWeight.Bold)
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
                text = "Environment Variables (${project.envVars.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimary,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
            OutlinedButton(
                onClick = { showRawEnvDialog = true },
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, DarkBorderSubtle),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Import .env", color = TextPrimary, fontSize = 12.sp)
            }
        }

        if (project.envVars.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No environment variables configured", color = TextMuted, fontSize = 13.sp)
            }
        } else {
            project.envVars.forEach { v ->
                val isMasked = v.isSecret && !visibleKeys.contains(v.key)
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = BorderStroke(1.dp, DarkBorderSubtle)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(v.key, fontWeight = FontWeight.Bold, color = PrimaryAccent, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                if (isMasked) "************" else v.value,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (v.isSecret) {
                                IconButton(
                                    onClick = {
                                        visibleKeys = if (visibleKeys.contains(v.key)) visibleKeys - v.key else visibleKeys + v.key
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        if (visibleKeys.contains(v.key)) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle Visibility",
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.removeEnvironmentVar(project.id, v.key)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRawEnvDialog) {
        AlertDialog(
            onDismissRequest = { showRawEnvDialog = false },
            containerColor = DarkSurfaceElevated,
            title = { Text("Import .env File Content", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Paste lines in KEY=VALUE format:", color = TextSecondary, fontSize = 12.sp)
                    OutlinedTextField(
                        value = rawEnvText,
                        onValueChange = { rawEnvText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        placeholder = { Text("PORT=8000\nDATABASE_URL=postgres://...\nDEBUG=true", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryAccent,
                            unfocusedBorderColor = DarkBorderSubtle,
                            cursorColor = PrimaryAccent,
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.importRawEnv(project.id, rawEnvText)
                        rawEnvText = ""
                        showRawEnvDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = PrimaryAccent)
                ) {
                    Text("Import", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRawEnvDialog = false }) {
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
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var namedTokenInput by remember { mutableStateOf("") }

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
                    "Expose http://127.0.0.1:${project.port} to the public internet securely with a free Cloudflare TryCloudflare tunnel.",
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
                                Text("Public URL", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
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
                }

                tunnelError?.let { err ->
                    Text(err, color = StatusRed, fontSize = 12.sp)
                }

                if (tunnelStatus == TunnelStatus.CONNECTED || tunnelStatus == TunnelStatus.CONNECTING) {
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
                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.startQuickTunnelForProject(project)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Start Quick Tunnel (Free)", color = TextOnAccent, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = namedTokenInput,
                            onValueChange = { namedTokenInput = it },
                            placeholder = { Text("Cloudflare Tunnel Token", color = TextMuted, fontSize = 12.sp) },
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
                                if (namedTokenInput.isNotBlank()) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.startNamedTunnel(namedTokenInput.trim())
                                }
                            },
                            enabled = namedTokenInput.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccentContainer)
                        ) {
                            Text("Connect", color = PrimaryAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
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
                Text("Process Control", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)

                if (project.status == ProjectStatus.RUNNING) {
                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.stopProject(project.id)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusYellow),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Stop Server", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.startProject(project)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start Server", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.restartProject(project.id)
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = TextPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restart Server", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Danger Zone", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = StatusRed)
                Text("Deleting this project will terminate any running processes and remove configuration from Localhost.", color = TextSecondary, fontSize = 12.sp)

                Button(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = StatusRed)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete Project", color = StatusRed, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = DarkSurfaceElevated,
            title = { Text("Delete '${project.name}'?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("This will permanently remove the project configuration and stop processes. Directory files will be cleaned.", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.deleteProject(project.id)
                        showDeleteConfirm = false
                        onBackClick()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = StatusRed)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            fontFamily = if (value.startsWith("http") || value.startsWith("/") || value.all { it.isDigit() }) FontFamily.Monospace else FontFamily.Default
        )
    }
}
