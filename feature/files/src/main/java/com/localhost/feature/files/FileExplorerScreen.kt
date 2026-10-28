package com.localhost.feature.files

import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.ScrollState

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.platform.LocalDensity

import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.FileOpen
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts


import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFolderUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
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
import androidx.activity.compose.BackHandler
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import com.localhost.core.designsystem.theme.StatusGreen
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileExplorerScreen(
    initialProjectId: String? = null,
    projectId: String? = initialProjectId,
    viewModel: FilesViewModel,
    onBackClick: () -> Unit = {}
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
    val clipboard by viewModel.clipboard.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var dropdownExpanded by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var isCreatingFolder by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var showVcsSheet by remember { mutableStateOf(false) }
    val recentPaths by viewModel.recentPaths.collectAsState()
    var renamingFile by remember { mutableStateOf<File?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var detailsFile by remember { mutableStateOf<File?>(null) }
    var showCommitDialog by remember { mutableStateOf(false) }
    var commitMessageInput by remember { mutableStateOf("") }
    var snapshotToRollback by remember { mutableStateOf<ProjectSnapshot?>(null) }
    var fileToDelete by remember { mutableStateOf<UiFile?>(null) }

    LaunchedEffect(initialProjectId, projects) {
        if (selectedId == null) {
            val target = projectId ?: initialProjectId ?: projects.firstOrNull()?.id
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

    
    val multipleFilesPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importMultipleFiles(context, uris)
        }
    }

    val deviceFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importFileFromDevice(context, it) }
    }
    val fileListState = rememberLazyListState()

    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importZip(context, it) }
    }

    BackHandler(enabled = editingFile == null) {
        if (showVcsSheet) {
            showVcsSheet = false
        } else if (showCreateDialog) {
            showCreateDialog = false
        } else if (renamingFile != null) {
            renamingFile = null
        } else if (detailsFile != null) {
            detailsFile = null
        } else if (fileToDelete != null) {
            fileToDelete = null
        } else {
            val rootPath = currentProject?.let { File(it.workingDir).canonicalPath }
            if (currentDir != null && rootPath != null && currentDir?.canonicalPath != rootPath) {
                viewModel.navigateUp()
            } else {
                onBackClick()
            }
        }
    }


    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text("File Explorer", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = {
                        val rootPath = currentProject?.let { File(it.workingDir).canonicalPath }
                        if (currentDir != null && rootPath != null && currentDir?.canonicalPath != rootPath) {
                            viewModel.navigateUp()
                        } else {
                            onBackClick()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    if (currentProject != null) {
                        IconButton(onClick = { multipleFilesPickerLauncher.launch(arrayOf("*/*")) }) {
                            Icon(Icons.Default.FileOpen, contentDescription = "Import Files / Media", tint = PrimaryAccent)
                        }
                        IconButton(onClick = { currentDir?.let { viewModel.downloadItem(context, it) } }) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Export Folder as ZIP", tint = PrimaryAccent)
                        }
                        IconButton(onClick = { zipPickerLauncher.launch("*/*") }) {
                            Icon(Icons.Default.DriveFolderUpload, contentDescription = "Import ZIP", tint = TextSecondary)
                        }
                        IconButton(onClick = { showVcsSheet = true }) {
                            Icon(Icons.Default.History, contentDescription = "Version Control", tint = TextSecondary)
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
                        onClick = { multipleFilesPickerLauncher.launch(arrayOf("*/*")) },
                        containerColor = DarkSurfaceElevated,
                        contentColor = PrimaryAccent,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = "Import Media / Files")
                    }

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
                        .weight(0.40f)
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
                        AddressBar(
                            currentDir = currentDir,
                            projectDir = currentProject.workingDir,
                            onNavigateTo = { viewModel.navigateTo(it) },
                            onNavigateUp = { viewModel.navigateUp() },
                            onNavigateToPath = { viewModel.navigateToPath(it) },
                            onRefresh = { viewModel.refreshCurrentDir() }
                        )

                        if (clipboard != null) {
                            ClipboardStatusBar(
                                clipboard = clipboard!!,
                                onPaste = { viewModel.pasteToCurrentDir() },
                                onClear = { viewModel.clearClipboard() }
                            )
                        }

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
                            OutlinedButton(
                                onClick = { multipleFilesPickerLauncher.launch(arrayOf("*/*")) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, PrimaryAccent.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.FileOpen, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Import", color = PrimaryAccent, fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { currentDir?.let { viewModel.downloadItem(context, it) } },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, DarkBorderSubtle),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Export", color = TextPrimary, fontSize = 12.sp)
                            }
                        }

                        FileListContent(
                            files = files,
                            listState = fileListState,
                            onFileClick = { f ->
                                if (f.isDirectory) viewModel.navigateTo(f.file)
                                else viewModel.openFile(f.file)
                            },
                            onRenameClick = { f ->
                                renamingFile = f.file
                                renameInput = f.name
                            },
                            onDetailsClick = { f ->
                                detailsFile = f.file
                            },
                            onShareClick = { f ->
                                viewModel.shareFile(context, f.file)
                            },
                            onCutClick = { f -> viewModel.cutFile(f.file) },
                            onCopyClick = { f -> viewModel.copyFile(f.file) },
                            onDownloadClick = { f -> viewModel.downloadItem(context, f.file) },
                            onDeleteClick = { f -> fileToDelete = f },
                            onExtractClick = { f -> viewModel.extractArchiveFile(f.file) },
                            clipboard = clipboard,
                            onPasteClick = { target ->
                                if (target.isDirectory) viewModel.pasteToDir(target.file)
                                else viewModel.pasteToCurrentDir()
                            }
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
                        .weight(0.60f)
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
                    AddressBar(
                        currentDir = currentDir,
                        projectDir = currentProject.workingDir,
                        onNavigateTo = { viewModel.navigateTo(it) },
                        onNavigateUp = { viewModel.navigateUp() },
                        onNavigateToPath = { viewModel.navigateToPath(it) },
                        onRefresh = { viewModel.refreshCurrentDir() }
                    )

                    if (clipboard != null) {
                        ClipboardStatusBar(
                            clipboard = clipboard!!,
                            onPaste = { viewModel.pasteToCurrentDir() },
                            onClear = { viewModel.clearClipboard() }
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = PrimaryAccentContainer,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, PrimaryAccent.copy(alpha = 0.5f)),
                            modifier = Modifier.clickable { multipleFilesPickerLauncher.launch(arrayOf("*/*")) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.FileOpen, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(15.dp))
                                Text("Import Files / Media", color = PrimaryAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Surface(
                            color = DarkSurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, DarkBorderSubtle),
                            modifier = Modifier.clickable { currentDir?.let { viewModel.downloadItem(context, it) } }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(15.dp))
                                Text("Export Folder (ZIP)", color = TextPrimary, fontSize = 11.sp)
                            }
                        }

                        Surface(
                            color = DarkSurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, DarkBorderSubtle),
                            modifier = Modifier.clickable { zipPickerLauncher.launch("*/*") }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.DriveFolderUpload, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(15.dp))
                                Text("Import ZIP", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }

                    FileListContent(
                        files = files,
                        onFileClick = { f ->
                            if (f.isDirectory) viewModel.navigateTo(f.file)
                            else viewModel.openFile(f.file)
                        },
                        onRenameClick = { f ->
                            renamingFile = f.file
                            renameInput = f.name
                        },
                        onDetailsClick = { f ->
                            detailsFile = f.file
                        },
                        onShareClick = { f ->
                            viewModel.shareFile(context, f.file)
                        },
                        onCutClick = { f -> viewModel.cutFile(f.file) },
                        onCopyClick = { f -> viewModel.copyFile(f.file) },
                        onDownloadClick = { f -> viewModel.downloadItem(context, f.file) },
                        onDeleteClick = { f -> fileToDelete = f },
                        onExtractClick = { f -> viewModel.extractArchiveFile(f.file) },
                        clipboard = clipboard,
                        onPasteClick = { target ->
                            if (target.isDirectory) viewModel.pasteToDir(target.file)
                            else viewModel.pasteToCurrentDir()
                        }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Please select or create a project first", color = TextMuted, fontSize = 14.sp)
                    }
                }
            }
        }
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

    
    renamingFile?.let { file ->
        AlertDialog(
            onDismissRequest = { renamingFile = null },
            containerColor = DarkSurfaceElevated,
            title = { Text("Rename '${file.name}'", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    placeholder = { Text("New name", color = TextMuted) },
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
                Button(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            viewModel.renameFile(file, renameInput.trim())
                            renamingFile = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                ) {
                    Text("Rename", color = TextOnAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingFile = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    detailsFile?.let { file ->
        val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
        AlertDialog(
            onDismissRequest = { detailsFile = null },
            containerColor = DarkSurfaceElevated,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("File Details", fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Name", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                    Text(file.name, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)

                    Text("Type", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                    Text(if (file.isDirectory) "Directory" else file.extension.uppercase().ifEmpty { "File" }, fontSize = 13.sp, color = TextPrimary)

                    Text("Size", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                    Text(if (file.isDirectory) "Folder" else "${formatFileSize(file.length())} (${file.length()} bytes)", fontSize = 13.sp, color = TextPrimary)

                    Text("Path", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                    Text(file.absolutePath, fontSize = 11.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)

                    Text("Last Modified", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                    Text(dateFormat.format(Date(file.lastModified())), fontSize = 13.sp, color = TextPrimary)

                    Text("Permissions", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                    val r = if (file.canRead()) "r" else "-"
                    val w = if (file.canWrite()) "w" else "-"
                    val x = if (file.canExecute()) "x" else "-"
                    Text("$r$w$x", fontSize = 12.sp, color = StatusGreen, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(
                    onClick = { detailsFile = null },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                ) {
                    Text("Close", color = TextOnAccent)
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
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Version History (Local VCS)", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                    Button(
                        onClick = { showCommitDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = TextOnAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Snapshot", color = TextOnAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (snapshots.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No snapshots recorded yet. Create one to protect your code state.", color = TextMuted, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(snapshots, key = { it.id }) { item ->
                            val dateStr = remember(item.timestamp) { dateFormat.format(Date(item.timestamp)) }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                border = BorderStroke(1.dp, DarkBorderSubtle)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("#${item.id}", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryAccent)
                                            Spacer(modifier = Modifier.width(8.dp))
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
private fun AddressBar(
    currentDir: File?,
    projectDir: String,
    recentPaths: List<String> = emptyList(),
    onNavigateTo: (File) -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateToPath: (String) -> Unit,
    onRefresh: () -> Unit
) {
    val rootDir = remember(projectDir) { File(projectDir) }
    val isRoot = currentDir?.canonicalPath == rootDir.canonicalPath
    val relativePath = remember(currentDir, projectDir) {
        if (currentDir == null) "/"
        else {
            val rel = currentDir.canonicalPath.removePrefix(rootDir.canonicalPath).trimStart('/')
            if (rel.isEmpty()) "/" else "/$rel"
        }
    }

    var isEditMode by remember { mutableStateOf(false) }
    var textInput by remember(relativePath) { mutableStateOf(relativePath) }

    Surface(
        color = DarkSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, DarkBorderSubtle)
    ) {
        if (isEditMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    singleLine = true,
                    placeholder = { Text("/path/to/folder", color = TextMuted, fontSize = 12.sp) },
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = TextPrimary
                    ),
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = {
                        onNavigateToPath(textInput)
                        isEditMode = false
                    }),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryAccent,
                        unfocusedBorderColor = DarkBorderSubtle,
                        cursorColor = PrimaryAccent,
                        focusedContainerColor = DarkBackground,
                        unfocusedContainerColor = DarkBackground
                    )
                )

                IconButton(
                    onClick = {
                        onNavigateToPath(textInput)
                        isEditMode = false
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Go", tint = StatusGreen, modifier = Modifier.size(18.dp))
                }

                IconButton(
                    onClick = {
                        textInput = relativePath
                        isEditMode = false
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
            if (recentPaths.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recents:", fontSize = 11.sp, color = TextMuted)
                    recentPaths.forEach { rPath ->
                        Surface(
                            color = DarkSurfaceElevated,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.clickable {
                                textInput = rPath
                                onNavigateToPath(rPath)
                                isEditMode = false
                            }
                        ) {
                            Text(rPath, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = PrimaryAccent, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onNavigateTo(rootDir) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Home, contentDescription = "Root", tint = if (isRoot) TextMuted else PrimaryAccent, modifier = Modifier.size(18.dp))
                }

                IconButton(
                    onClick = onNavigateUp,
                    enabled = !isRoot,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = "Up",
                        tint = if (!isRoot) PrimaryAccent else TextMuted.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val segments = remember(relativePath) {
                        if (relativePath == "/") emptyList()
                        else relativePath.split("/").filter { it.isNotEmpty() }
                    }

                    Surface(
                        color = if (segments.isEmpty()) PrimaryAccentContainer else DarkSurfaceElevated,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable { onNavigateTo(rootDir) }
                    ) {
                        Text(
                            text = "~",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (segments.isEmpty()) PrimaryAccent else TextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    var accumulatedPath = rootDir
                    segments.forEachIndexed { index, segment ->
                        accumulatedPath = File(accumulatedPath, segment)
                        val targetDir = accumulatedPath
                        val isLast = index == segments.size - 1

                        Text("/", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 2.dp))

                        Surface(
                            color = if (isLast) PrimaryAccentContainer else DarkSurfaceElevated,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.clickable { onNavigateTo(targetDir) }
                        ) {
                            Text(
                                text = segment,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp,
                                color = if (isLast) PrimaryAccent else TextPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = {
                        textInput = relativePath
                        isEditMode = true
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Path", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ClipboardStatusBar(
    clipboard: FileClipboard,
    onPaste: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = BorderStroke(1.dp, PrimaryAccent.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (clipboard.isCut) Icons.Default.ContentCut else Icons.Default.ContentCopy,
                    contentDescription = null,
                    tint = PrimaryAccent,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = if (clipboard.isCut) "Cut: ${clipboard.file.name}" else "Copied: ${clipboard.file.name}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text("Ready to paste into current folder", fontSize = 10.sp, color = TextMuted)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onPaste,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, tint = TextOnAccent, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Paste", color = TextOnAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(onClick = onClear, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Clear clipboard", tint = TextMuted, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun FileListContent(
    files: List<UiFile>,
    listState: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState(),
    onFileClick: (UiFile) -> Unit,
    onRenameClick: (UiFile) -> Unit = {},
    onDetailsClick: (UiFile) -> Unit = {},
    onShareClick: (UiFile) -> Unit = {},
    onCutClick: (UiFile) -> Unit,
    onCopyClick: (UiFile) -> Unit,
    onDownloadClick: (UiFile) -> Unit,
    onDeleteClick: (UiFile) -> Unit,
    onExtractClick: (UiFile) -> Unit,
    clipboard: FileClipboard? = null,
    onPasteClick: (UiFile) -> Unit = {}
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    if (files.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Folder, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("Directory is empty", color = TextMuted, fontSize = 14.sp)
            }
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
        ) {
            items(files, key = { it.file.absolutePath }) { fileItem ->
                var menuExpanded by remember { mutableStateOf(false) }
                val isDir = fileItem.isDirectory
                val sizeStr = remember(fileItem.size, isDir) {
                    if (isDir) "Folder" else formatFileSize(fileItem.size)
                }
                val modDate = remember(fileItem.lastModified) {
                    dateFormat.format(Date(fileItem.lastModified))
                }

                val haptics = LocalHapticFeedback.current
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onFileClick(fileItem) },
                            onLongClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuExpanded = true
                            }
                        ),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
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
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(fileItem.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                                Text("$sizeStr - $modDate", fontSize = 11.sp, color = TextMuted)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box {
                                IconButton(
                                    onClick = { menuExpanded = true },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More actions", tint = TextSecondary, modifier = Modifier.size(18.dp))
                                }

                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false },
                                    modifier = Modifier.background(DarkSurfaceElevated)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Copy", color = TextPrimary) },
                                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(18.dp)) },
                                        onClick = {
                                            menuExpanded = false
                                            onCopyClick(fileItem)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Cut", color = TextPrimary) },
                                        leadingIcon = { Icon(Icons.Default.ContentCut, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(18.dp)) },
                                        onClick = {
                                            menuExpanded = false
                                            onCutClick(fileItem)
                                        }
                                    )
                                    if (clipboard != null) {
                                        DropdownMenuItem(
                                            text = { Text("Paste", color = PrimaryAccent, fontWeight = FontWeight.Bold) },
                                            leadingIcon = { Icon(Icons.Default.ContentPaste, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(18.dp)) },
                                            onClick = {
                                                menuExpanded = false
                                                onPasteClick(fileItem)
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Rename", color = TextPrimary) },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(18.dp)) },
                                        onClick = {
                                            menuExpanded = false
                                            onRenameClick(fileItem)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (fileItem.isDirectory) "Export to Downloads (ZIP)" else "Export to Downloads", color = TextPrimary) },
                                        leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(18.dp)) },
                                        onClick = {
                                            menuExpanded = false
                                            onDownloadClick(fileItem)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (fileItem.isDirectory) "Share (ZIP)" else "Share", color = TextPrimary) },
                                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(18.dp)) },
                                        onClick = {
                                            menuExpanded = false
                                            onShareClick(fileItem)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Details", color = TextPrimary) },
                                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(18.dp)) },
                                        onClick = {
                                            menuExpanded = false
                                            onDetailsClick(fileItem)
                                        }
                                    )
                                    if (fileItem.fileType == FileType.ARCHIVE) {
                                        DropdownMenuItem(
                                            text = { Text("Extract Archive", color = TextPrimary) },
                                            leadingIcon = { Icon(Icons.Default.DriveFolderUpload, contentDescription = null, tint = StatusYellow, modifier = Modifier.size(18.dp)) },
                                            onClick = {
                                                menuExpanded = false
                                                onExtractClick(fileItem)
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Delete", color = StatusRed) },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StatusRed, modifier = Modifier.size(18.dp)) },
                                        onClick = {
                                            menuExpanded = false
                                            onDeleteClick(fileItem)
                                        }
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
    var markdownPreviewMode by remember { mutableStateOf(false) }
    var isSavedRecently by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val editorSnackbarHostState = remember { SnackbarHostState() }
    var showUnsavedDialog by remember { mutableStateOf(false) }

    val syntaxIssues = remember(textInput, file.extension) {
        if (fileType == FileType.TEXT) SyntaxValidator.validate(textInput, file.extension) else emptyList()
    }

    val attemptClose = {
        if (fileType == FileType.TEXT && textInput != content) {
            showUnsavedDialog = true
        } else {
            onClose()
        }
    }

    BackHandler {
        attemptClose()
    }

    Scaffold(
        containerColor = CodeBackground,
        snackbarHost = { SnackbarHost(editorSnackbarHostState) },
        topBar = {
            Surface(
                color = DarkSurface,
                border = BorderStroke(1.dp, DarkBorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    Row(modifier = Modifier.weight(1f).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { attemptClose() }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close", tint = TextPrimary)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(file.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (fileType == FileType.TEXT && textInput.isNotBlank() && syntaxIssues.isNotEmpty()) {
                                    val first = syntaxIssues.first()
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = StatusRed.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = "⚠ L${first.line}: ${first.message}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = StatusRed,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text("${fileType.name} • ${formatFileSize(file.length())}${if (textInput != content && fileType == FileType.TEXT) " • Modified" else ""}", fontSize = 11.sp, color = if (textInput != content && fileType == FileType.TEXT) StatusYellow else TextMuted)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (fileType == FileType.SVG) {
                            TextButton(onClick = { svgPreviewMode = !svgPreviewMode }) {
                                Text(if (svgPreviewMode) "Source" else "Preview", color = PrimaryAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (file.extension.equals("md", ignoreCase = true)) {
                            TextButton(onClick = { markdownPreviewMode = !markdownPreviewMode }) {
                                Text(if (markdownPreviewMode) "Edit" else "Preview", color = PrimaryAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (fileType == FileType.TEXT || (fileType == FileType.SVG && !svgPreviewMode)) {
                            Button(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSave(textInput)
                                    isSavedRecently = true
                                    coroutineScope.launch {
                                        editorSnackbarHostState.showSnackbar("Saved ${file.name}")
                                        delay(2000)
                                        isSavedRecently = false
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSavedRecently) StatusGreen else PrimaryAccent
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSavedRecently) Icons.Default.Check else Icons.Default.Save,
                                    contentDescription = null,
                                    tint = TextOnAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isSavedRecently) "Saved" else "Save",
                                    color = TextOnAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                if (fileType == FileType.TEXT && syntaxIssues.isNotEmpty()) {
                    Surface(
                        color = StatusRed.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = StatusRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                syntaxIssues.take(2).forEach { issue ->
                                    Text(
                                        text = "Line ${issue.line}: ${issue.message}",
                                        color = StatusRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
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
                .navigationBarsPadding()
                .imePadding()
        ) {
            when (fileType) {
                FileType.TEXT -> {
                    if (file.extension.equals("md", ignoreCase = true) && markdownPreviewMode) {
                        MarkdownPreviewView(content = textInput)
                    } else {
                        CodeTextEditor(
                            text = textInput,
                            fileExtension = file.extension,
                            onTextChange = { textInput = it }
                        )
                    }
                }
                FileType.IMAGE -> {
                    ImageViewer(file = file, bitmap = imageBitmap)
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

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            containerColor = DarkSurfaceElevated,
            title = { Text("Changes Detected", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("You have unsaved changes in '${file.name}'. Would you like to save before leaving?", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        onSave(textInput)
                        showUnsavedDialog = false
                        onClose()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                ) {
                    Text("Save & Exit", color = TextOnAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        showUnsavedDialog = false
                        onClose()
                    }) {
                        Text("Discard", color = StatusRed)
                    }
                    TextButton(onClick = { showUnsavedDialog = false }) {
                        Text("Cancel", color = TextMuted)
                    }
                }
            }
        )
    }
}


@Composable
private fun MarkdownPreviewView(content: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val lines = content.lines()
        var inCodeBlock = false
        val codeBlockLines = mutableListOf<String>()

        lines.forEach { line ->
            when {
                line.startsWith("```") -> {
                    if (inCodeBlock) {
                        Surface(
                            color = CodeBackground,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, DarkBorderSubtle),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = codeBlockLines.joinToString("\n"),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = TextPrimary,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        codeBlockLines.clear()
                        inCodeBlock = false
                    } else {
                        inCodeBlock = true
                    }
                }
                inCodeBlock -> {
                    codeBlockLines.add(line)
                }
                line.startsWith("# ") -> {
                    Text(
                        text = line.removePrefix("# ").trim(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryAccent,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                line.startsWith("## ") -> {
                    Text(
                        text = line.removePrefix("## ").trim(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }
                line.startsWith("### ") -> {
                    Text(
                        text = line.removePrefix("### ").trim(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryAccent,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    Row(modifier = Modifier.padding(start = 8.dp)) {
                        Text("• ", color = PrimaryAccent, fontWeight = FontWeight.Bold)
                        Text(line.substring(2).trim(), color = TextPrimary, fontSize = 13.sp)
                    }
                }
                line.startsWith("> ") -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(DarkSurfaceElevated)
                            .border(BorderStroke(2.dp, PrimaryAccent), RoundedCornerShape(4.dp))
                            .padding(8.dp)
                    ) {
                        Text(line.removePrefix("> ").trim(), color = TextSecondary, fontSize = 13.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    }
                }
                line.isBlank() -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                else -> {
                    Text(line, color = TextPrimary, fontSize = 13.sp, lineHeight = 20.sp)
                }
            }
        }
    }
}

@Composable
private fun CodeTextEditor(
    text: String,
    fileExtension: String = "",
    onTextChange: (String) -> Unit
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val lineHeightPx = with(density) { 20.sp.toPx() }
    var viewportHeightPx by remember { mutableStateOf(0) }

    val lines = remember(text) { text.lines().size.coerceAtLeast(1) }
    val lineNumbersText = remember(lines) { (1..lines).joinToString("\n") }
    val verticalScrollState = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }
    val horizontalScrollState = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }
    val syntaxHighlighter = remember(fileExtension) { SyntaxHighlighter(fileExtension) }

    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = text, selection = TextRange(text.length)))
    }

    var isUserTyping by remember { mutableStateOf(false) }

    fun getCursorY(tfv: TextFieldValue): Float {
        val cursorIndex = tfv.selection.end.coerceIn(0, tfv.text.length)
        val lineIndex = tfv.text.take(cursorIndex).count { it == '\n' }
        return lineIndex * lineHeightPx
    }

    fun ensureCursorVisible(tfv: TextFieldValue, animated: Boolean = false) {
        if (viewportHeightPx <= 0) return
        val cursorY = getCursorY(tfv)
        val currentScroll = verticalScrollState.value
        val visibleBottom = currentScroll + viewportHeightPx

        val marginPx = (lineHeightPx * 2).toInt()
        val cursorYInt = cursorY.toInt()
        if (cursorYInt < currentScroll + marginPx || cursorYInt > visibleBottom - marginPx) {
            val targetScroll = (cursorYInt - (viewportHeightPx / 3)).coerceIn(0, verticalScrollState.maxValue)
            coroutineScope.launch {
                if (animated) {
                    verticalScrollState.animateScrollTo(targetScroll)
                } else {
                    verticalScrollState.scrollTo(targetScroll)
                }
            }
        }
    }

    val isImeVisible = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp
    LaunchedEffect(isImeVisible) {
        if (isImeVisible) {
            kotlinx.coroutines.delay(120)
            ensureCursorVisible(textFieldValue, animated = true)
        }
    }

    LaunchedEffect(text) {
        if (!isUserTyping && textFieldValue.text != text) {
            textFieldValue = textFieldValue.copy(text = text)
        }
    }

    val tokens = remember(text) { SyntaxHighlighter.extractTokens(text) }
    var currentWord by remember { mutableStateOf("") }

    val suggestions = remember(currentWord, tokens) {
        if (currentWord.length >= 2) {
            tokens.filter { it.startsWith(currentWord, ignoreCase = true) && !it.equals(currentWord, ignoreCase = true) }
                .take(6)
        } else {
            emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CodeBackground)
            .onSizeChanged { viewportHeightPx = it.height }
    ) {
        if (suggestions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurfaceElevated)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Suggestions:", fontSize = 11.sp, color = TextMuted)
                suggestions.forEach { suggestion ->
                    Surface(
                        color = PrimaryAccentContainer,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable {
                            val lastIdx = textFieldValue.text.lastIndexOf(currentWord)
                            if (lastIdx != -1) {
                                val newText = textFieldValue.text.substring(0, lastIdx) + suggestion + textFieldValue.text.substring(lastIdx + currentWord.length)
                                textFieldValue = TextFieldValue(text = newText, selection = TextRange(lastIdx + suggestion.length))
                                onTextChange(newText)
                                currentWord = ""
                            }
                        }
                    ) {
                        Text(
                            text = suggestion,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = PrimaryAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
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

            BasicTextField(
                value = textFieldValue,
                onValueChange = { tfv ->
                    isUserTyping = true
                    textFieldValue = tfv
                    if (tfv.text != text) {
                        onTextChange(tfv.text)
                        val lastWord = tfv.text.takeLastWhile { it.isLetterOrDigit() || it == '_' }
                        currentWord = lastWord
                    }
                    ensureCursorVisible(tfv, animated = false)
                    isUserTyping = false
                },
                visualTransformation = syntaxHighlighter,
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
}

@Composable
private fun ImageViewer(file: File, bitmap: Bitmap?) {
    val isGif = remember(file) { file.extension.equals("gif", ignoreCase = true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isGif) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        setBackgroundColor(0xFF161B22.toInt())
                        settings.javaScriptEnabled = false
                        settings.allowFileAccess = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                    }
                },
                update = { webView ->
                    val html = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <style>
                          html, body {
                            height: 100%;
                            width: 100%;
                            margin: 0;
                            padding: 0;
                            background-color: #161B22;
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            overflow: hidden;
                          }
                          img {
                            max-width: 95%;
                            max-height: 95%;
                            object-fit: contain;
                            border-radius: 8px;
                            display: block;
                            margin: auto;
                          }
                        </style>
                        </head>
                        <body>
                          <img src="file://${file.absolutePath}" alt="GIF animation" />
                        </body>
                        </html>
                    """.trimIndent()
                    webView.loadDataWithBaseURL("file://${file.parentFile?.absolutePath ?: ""}/", html, "text/html", "UTF-8", null)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
            )
        } else if (bitmap != null) {
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
