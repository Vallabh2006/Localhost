package com.localhost.feature.files

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.DriveFolderUpload
import androidx.compose.ui.platform.LocalContext

import android.content.res.Configuration
import android.graphics.Bitmap
import android.webkit.WebView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.localhost.core.designsystem.theme.CodeBackground
import com.localhost.core.designsystem.theme.DarkBackground
import com.localhost.core.designsystem.theme.DarkBorder
import com.localhost.core.designsystem.theme.DarkBorderSubtle
import com.localhost.core.designsystem.theme.DarkSurface
import com.localhost.core.designsystem.theme.DarkSurfaceElevated
import com.localhost.core.designsystem.theme.PrimaryAccent
import com.localhost.core.designsystem.theme.PrimaryAccentContainer
import com.localhost.core.designsystem.theme.StatusRed
import com.localhost.core.designsystem.theme.StatusYellow
import com.localhost.core.designsystem.theme.TextMuted
import com.localhost.core.designsystem.theme.TextOnAccent
import com.localhost.core.designsystem.theme.TextPrimary
import com.localhost.core.designsystem.theme.TextSecondary
import com.localhost.core.model.ProjectSnapshot
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileExplorerScreen(
    viewModel: FilesViewModel,
    initialProjectId: String? = null
) {
    val projects by viewModel.projects.collectAsState()
    val selectedId by viewModel.selectedProjectId.collectAsState()
    val currentDir by viewModel.currentDir.collectAsState()
    val files by viewModel.files.collectAsState()
    val editingFile by viewModel.editingFile.collectAsState()
    val editingFileType by viewModel.editingFileType.collectAsState()
    val fileContent by viewModel.fileContent.collectAsState()
    val imageBitmap by viewModel.imageBitmap.collectAsState()
    val pdfPageBitmap by viewModel.pdfPageBitmap.collectAsState()
    val pdfPageCount by viewModel.pdfPageCount.collectAsState()
    val currentPdfPage by viewModel.currentPdfPage.collectAsState()
    val snapshots by viewModel.snapshots.collectAsState()
    val message by viewModel.message.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var dropdownExpanded by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var isCreatingFolder by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var showVcsSheet by remember { mutableStateOf(false) }
    var showCommitDialog by remember { mutableStateOf(false) }
    var commitMessageInput by remember { mutableStateOf("") }
    var snapshotToRollback by remember { mutableStateOf<ProjectSnapshot?>(null) }
    var fileToDelete by remember { mutableStateOf<UiFile?>(null) }

    LaunchedEffect(initialProjectId, projects) {
        if (selectedId == null) {
            val target = initialProjectId ?: projects.firstOrNull()?.id
            target?.let { viewModel.selectProject(it) }
        }
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val currentProject = projects.find { it.id == selectedId }
    val context = LocalContext.current

    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importZip(context, it) }
    }

    if (!isLandscape && editingFile != null) {
        FileEditorAndViewerScreen(
            file = editingFile!!,
            fileType = editingFileType,
            content = fileContent,
            imageBitmap = imageBitmap,
            pdfPageBitmap = pdfPageBitmap,
            pdfPageCount = pdfPageCount,
            currentPdfPage = currentPdfPage,
            onClose = { viewModel.closeEditor() },
            onSave = { viewModel.saveContent(it) },
            onExtractArchive = { viewModel.extractArchiveFile(editingFile!!); viewModel.closeEditor() },
            onNextPdfPage = { viewModel.nextPdfPage() },
            onPrevPdfPage = { viewModel.previousPdfPage() }
        )
        return
    }

    Scaffold(
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text("File Explorer", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextPrimary)
                },
                actions = {
                    if (currentProject != null) {
                        IconButton(onClick = { zipPickerLauncher.launch("*/*") }) {
                            Icon(Icons.Default.DriveFolderUpload, contentDescription = "Import ZIP", tint = PrimaryAccent)
                        }
                        IconButton(onClick = { showVcsSheet = true }) {
                            Icon(Icons.Default.History, contentDescription = "Version Control", tint = PrimaryAccent)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        floatingActionButton = {
            if (currentProject != null && !isLandscape) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FloatingActionButton(
                        onClick = {
                            isCreatingFolder = true
                            newFileName = ""
                            showCreateDialog = true
                        },
                        containerColor = DarkSurfaceElevated,
                        contentColor = TextPrimary,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder")
                    }

                    FloatingActionButton(
                        onClick = {
                            isCreatingFolder = false
                            newFileName = ""
                            showCreateDialog = true
                        },
                        containerColor = PrimaryAccent,
                        contentColor = TextOnAccent,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New File")
                    }
                }
            }
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
                        .weight(0.38f)
                        .fillMaxHeight()
                        .background(DarkSurface)
                ) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        ProjectSelectorHeader(
                            projects = projects,
                            selectedId = selectedId,
                            dropdownExpanded = dropdownExpanded,
                            onExpandChange = { dropdownExpanded = it },
                            onSelectProject = {
                                viewModel.selectProject(it)
                                dropdownExpanded = false
                            }
                        )
                    }

                    if (currentProject != null) {
                        BreadcrumbPathBar(
                            currentDir = currentDir,
                            projectDir = currentProject.workingDir,
                            onNavigateUp = { viewModel.navigateUp() }
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    isCreatingFolder = true
                                    newFileName = ""
                                    showCreateDialog = true
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, DarkBorderSubtle),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Folder", color = TextPrimary, fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    isCreatingFolder = false
                                    newFileName = ""
                                    showCreateDialog = true
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, DarkBorderSubtle),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("File", color = TextPrimary, fontSize = 12.sp)
                            }
                        }

                        FileListContent(
                            files = files,
                            onFileClick = { f ->
                                if (f.isDirectory) viewModel.navigateTo(f.file)
                                else viewModel.openFile(f.file)
                            },
                            onDeleteClick = { f -> fileToDelete = f }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(DarkBorder)
                )

                Box(
                    modifier = Modifier
                        .weight(0.62f)
                        .fillMaxHeight()
                ) {
                    if (editingFile != null) {
                        FileEditorAndViewerScreen(
                            file = editingFile!!,
                            fileType = editingFileType,
                            content = fileContent,
                            imageBitmap = imageBitmap,
                            pdfPageBitmap = pdfPageBitmap,
                            pdfPageCount = pdfPageCount,
                            currentPdfPage = currentPdfPage,
                            onClose = { viewModel.closeEditor() },
                            onSave = { viewModel.saveContent(it) },
                            onExtractArchive = { viewModel.extractArchiveFile(editingFile!!); viewModel.closeEditor() },
                            onNextPdfPage = { viewModel.nextPdfPage() },
                            onPrevPdfPage = { viewModel.previousPdfPage() }
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Code, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Select a file from the explorer to view or edit", color = TextMuted, fontSize = 14.sp)
                            }
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
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    ProjectSelectorHeader(
                        projects = projects,
                        selectedId = selectedId,
                        dropdownExpanded = dropdownExpanded,
                        onExpandChange = { dropdownExpanded = it },
                        onSelectProject = {
                            viewModel.selectProject(it)
                            dropdownExpanded = false
                        }
                    )
                }

                if (currentProject != null) {
                    BreadcrumbPathBar(
                        currentDir = currentDir,
                        projectDir = currentProject.workingDir,
                        onNavigateUp = { viewModel.navigateUp() }
                    )

                    FileListContent(
                        files = files,
                        onFileClick = { f ->
                            if (f.isDirectory) viewModel.navigateTo(f.file)
                            else viewModel.openFile(f.file)
                        },
                        onDeleteClick = { f -> fileToDelete = f }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Please select or create a project first", color = TextMuted, fontSize = 14.sp)
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = DarkSurfaceElevated,
            title = { Text(if (isCreatingFolder) "Create Directory" else "Create File", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = newFileName,
                    onValueChange = { newFileName = it },
                    placeholder = { Text(if (isCreatingFolder) "Folder name" else "filename.ext", color = TextMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
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
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newFileName.isNotBlank()) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.createFile(newFileName.trim(), isCreatingFolder)
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = PrimaryAccent)
                ) {
                    Text("Create", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    fileToDelete?.let { uiFile ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            containerColor = DarkSurfaceElevated,
            title = { Text("Delete '${uiFile.name}'?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("Are you sure you want to permanently delete this ${if (uiFile.isDirectory) "folder" else "file"}?", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.deleteFile(uiFile.file)
                        fileToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = StatusRed)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showVcsSheet && currentProject != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()) }

        ModalBottomSheet(
            onDismissRequest = { showVcsSheet = false },
            sheetState = sheetState,
            containerColor = DarkSurfaceElevated
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = PrimaryAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Snapshots & History", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                    }

                    Button(
                        onClick = { showCommitDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = TextOnAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Snapshot", color = TextOnAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                if (snapshots.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No snapshot checkpoints saved yet.", color = TextMuted, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(320.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(snapshots, key = { it.id }) { item ->
                            val dateStr = dateFormat.format(Date(item.timestamp))
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                border = BorderStroke(1.dp, DarkBorderSubtle)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(PrimaryAccentContainer)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(item.id, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = PrimaryAccent, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(item.message, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("$dateStr - ${item.fileCount} files", fontSize = 11.sp, color = TextMuted)
                                    }

                                    Row {
                                        IconButton(onClick = { snapshotToRollback = item }) {
                                            Icon(Icons.Default.Restore, contentDescription = "Rollback", tint = StatusYellow, modifier = Modifier.size(20.dp))
                                        }
                                        IconButton(onClick = { viewModel.deleteCommit(item) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showCommitDialog && currentProject != null) {
        AlertDialog(
            onDismissRequest = { showCommitDialog = false },
            containerColor = DarkSurfaceElevated,
            title = { Text("Create Version Snapshot", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = commitMessageInput,
                    onValueChange = { commitMessageInput = it },
                    placeholder = { Text("e.g. Added user authentication endpoints", color = TextMuted) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
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
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.createCommit(commitMessageInput.trim())
                        commitMessageInput = ""
                        showCommitDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = PrimaryAccent)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCommitDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    snapshotToRollback?.let { snap ->
        AlertDialog(
            onDismissRequest = { snapshotToRollback = null },
            containerColor = DarkSurfaceElevated,
            title = { Text("Rollback to ${snap.id}?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("This will restore project files to snapshot '${snap.message}'. Current modifications will be replaced.", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.rollbackToCommit(snap)
                        snapshotToRollback = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = StatusYellow)
                ) {
                    Text("Rollback", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { snapshotToRollback = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectSelectorHeader(
    projects: List<com.localhost.core.model.Project>,
    selectedId: String?,
    dropdownExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onSelectProject: (String) -> Unit
) {
    val currentProject = projects.find { it.id == selectedId }
    val context = LocalContext.current

    ExposedDropdownMenuBox(
        expanded = dropdownExpanded,
        onExpandedChange = onExpandChange,
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = currentProject?.let { "${it.name} (${it.runtime.displayName})" } ?: "Select a project",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = PrimaryAccent,
                unfocusedBorderColor = DarkBorderSubtle,
                focusedContainerColor = DarkSurfaceElevated,
                unfocusedContainerColor = DarkSurfaceElevated
            )
        )
        ExposedDropdownMenu(
            expanded = dropdownExpanded,
            onDismissRequest = { onExpandChange(false) },
            modifier = Modifier.background(DarkSurfaceElevated)
        ) {
            projects.forEach { p ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(p.name, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(p.runtime.displayName, fontSize = 11.sp, color = TextSecondary)
                        }
                    },
                    onClick = { onSelectProject(p.id) }
                )
            }
        }
    }
}

@Composable
private fun BreadcrumbPathBar(
    currentDir: File?,
    projectDir: String,
    onNavigateUp: () -> Unit
) {
    val isRoot = currentDir?.absolutePath == projectDir
    val relativePath = currentDir?.let {
        val rel = it.absolutePath.removePrefix(projectDir).trimStart('/')
        if (rel.isEmpty()) "/" else "/$rel"
    } ?: "/"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isRoot) {
            IconButton(onClick = onNavigateUp, modifier = Modifier.size(32.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Up", tint = PrimaryAccent, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = relativePath,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = PrimaryAccent,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FileListContent(
    files: List<UiFile>,
    onFileClick: (UiFile) -> Unit,
    onDeleteClick: (UiFile) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    if (files.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("Directory is empty", color = TextMuted, fontSize = 13.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(files, key = { it.file.absolutePath }) { fileItem ->
                val isDir = fileItem.isDirectory
                val modDate = dateFormat.format(Date(fileItem.lastModified))
                val sizeStr = if (isDir) "${fileItem.file.listFiles()?.size ?: 0} items" else formatFileSize(fileItem.size)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFileClick(fileItem) },
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    border = BorderStroke(1.dp, DarkBorderSubtle)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (fileItem.fileType) {
                                    FileType.IMAGE -> Icons.Default.Image
                                    FileType.SVG -> Icons.Default.Visibility
                                    FileType.PDF -> Icons.Default.PictureAsPdf
                                    FileType.ARCHIVE -> Icons.Default.DriveFolderUpload
                                    FileType.BINARY -> Icons.AutoMirrored.Filled.InsertDriveFile
                                    FileType.TEXT -> if (isDir) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile
                                },
                                contentDescription = null,
                                tint = if (isDir) PrimaryAccent else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(fileItem.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                                Text("$sizeStr - $modDate", fontSize = 11.sp, color = TextMuted)
                            }
                        }

                        IconButton(onClick = { onDeleteClick(fileItem) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FileEditorAndViewerScreen(
    file: File,
    fileType: FileType,
    content: String,
    imageBitmap: Bitmap?,
    pdfPageBitmap: Bitmap?,
    pdfPageCount: Int,
    currentPdfPage: Int,
    onClose: () -> Unit,
    onSave: (String) -> Unit,
    onExtractArchive: () -> Unit = {},
    onNextPdfPage: () -> Unit,
    onPrevPdfPage: () -> Unit
) {
    var textInput by remember(content) { mutableStateOf(content) }
    var svgPreviewMode by remember { mutableStateOf(true) }
    val haptics = LocalHapticFeedback.current

    Scaffold(
        containerColor = CodeBackground,
        topBar = {
            Surface(color = DarkSurface, border = BorderStroke(1.dp, DarkBorderSubtle)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", tint = TextPrimary)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(file.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                            Text("${fileType.name} - ${formatFileSize(file.length())}", fontSize = 11.sp, color = TextMuted)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (fileType == FileType.SVG) {
                            TextButton(onClick = { svgPreviewMode = !svgPreviewMode }) {
                                Text(if (svgPreviewMode) "Source" else "Preview", color = PrimaryAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (fileType == FileType.TEXT || (fileType == FileType.SVG && !svgPreviewMode)) {
                            Button(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSave(textInput)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, tint = TextOnAccent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save", color = TextOnAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (fileType) {
                FileType.TEXT -> {
                    CodeTextEditor(
                        text = textInput,
                        onTextChange = { textInput = it }
                    )
                }
                FileType.IMAGE -> {
                    ImageViewer(imageBitmap)
                }
                FileType.SVG -> {
                    if (svgPreviewMode) {
                        SvgViewer(content)
                    } else {
                        CodeTextEditor(
                            text = textInput,
                            onTextChange = { textInput = it }
                        )
                    }
                }
                FileType.PDF -> {
                    PdfViewer(
                        bitmap = pdfPageBitmap,
                        pageIndex = currentPdfPage,
                        pageCount = pdfPageCount,
                        onPrev = onPrevPdfPage,
                        onNext = onNextPdfPage
                    )
                }
                FileType.ARCHIVE -> {
                    ArchiveViewer(file, onExtract = onExtractArchive)
                }
                FileType.BINARY -> {
                    BinaryViewer(file)
                }
            }
        }
    }
}

@Composable
private fun CodeTextEditor(
    text: String,
    onTextChange: (String) -> Unit
) {
    val lines = remember(text) { text.lines().size.coerceAtLeast(1) }
    val lineNumbersText = remember(lines) { (1..lines).joinToString("\n") }
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(CodeBackground)
            .verticalScroll(verticalScrollState)
            .padding(top = 10.dp, bottom = 16.dp, start = 8.dp, end = 8.dp)
    ) {
        Text(
            text = lineNumbersText,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = TextMuted.copy(alpha = 0.45f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier
                .width(42.dp)
                .padding(end = 10.dp)
        )

        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(DarkBorderSubtle)
        )

        Spacer(modifier = Modifier.width(10.dp))

        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = TextPrimary
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(PrimaryAccent),
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(horizontalScrollState)
        )
    }
}

@Composable
private fun ImageViewer(bitmap: Bitmap?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Image preview",
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
            )
        } else {
            CircularProgressIndicator(color = PrimaryAccent)
        }
    }
}

@Composable
private fun SvgViewer(svgContent: String) {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(0xFF161B22.toInt())
                settings.javaScriptEnabled = false
            }
        },
        update = { webView ->
            val html = """
                <!DOCTYPE html>
                <html>
                <head>
                <style>
                  body { margin: 0; background: #161B22; display: flex; justify-content: center; align-items: center; height: 100vh; }
                  svg { max-width: 90%; max-height: 90%; }
                </style>
                </head>
                <body>
                  $svgContent
                </body>
                </html>
            """.trimIndent()
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun PdfViewer(
    bitmap: Bitmap?,
    pageIndex: Int,
    pageCount: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "PDF Page",
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, DarkBorder, RoundedCornerShape(6.dp))
                )
            } else {
                CircularProgressIndicator(color = PrimaryAccent)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPrev,
                enabled = pageIndex > 0
            ) {
                Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = "Previous Page", tint = if (pageIndex > 0) PrimaryAccent else TextMuted)
            }

            Text(
                text = "Page ${pageIndex + 1} of ${pageCount.coerceAtLeast(1)}",
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            IconButton(
                onClick = onNext,
                enabled = pageIndex < pageCount - 1
            ) {
                Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "Next Page", tint = if (pageIndex < pageCount - 1) PrimaryAccent else TextMuted)
            }
        }
    }
}

@Composable
private fun BinaryViewer(file: File) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(file.name, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Binary File (${formatFileSize(file.length())})", color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Path: ${file.absolutePath}", color = TextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
    val pre = "KMGTPE"[exp - 1]
    return String.format(Locale.US, "%.1f %sB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
}

@Composable
private fun ArchiveViewer(file: File, onExtract: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, PrimaryAccent.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(PrimaryAccentContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.DriveFolderUpload, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(32.dp))
                }
                Text(file.name, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                Text("Archive (" + formatFileSize(file.length()) + ")", color = TextSecondary, fontSize = 13.sp)
                Text("Path: " + file.absolutePath, color = TextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onExtract,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.DriveFolderUpload, contentDescription = null, tint = TextOnAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Extract Archive Here", color = TextOnAccent, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
