package com.localhost.feature.logs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localhost.core.designsystem.theme.CodeBackground
import com.localhost.core.designsystem.theme.DarkBackground
import com.localhost.core.designsystem.theme.DarkBorder
import com.localhost.core.designsystem.theme.DarkSurface
import com.localhost.core.designsystem.theme.PrimaryAccent
import com.localhost.core.designsystem.theme.SecondaryTeal
import com.localhost.core.designsystem.theme.StatusGreen
import com.localhost.core.designsystem.theme.StatusRed
import com.localhost.core.designsystem.theme.StatusYellow
import com.localhost.core.designsystem.theme.TextMuted
import com.localhost.core.designsystem.theme.TextPrimary
import com.localhost.core.designsystem.theme.TextSecondary
import com.localhost.core.model.LogEntry
import com.localhost.core.model.LogStream
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    viewModel: LogsViewModel,
    initialProjectId: String? = null
) {
    val context = LocalContext.current
    val projects by viewModel.projects.collectAsState()
    val selectedId by viewModel.selectedProjectId.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val filterQuery by viewModel.filterQuery.collectAsState()

    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var isLiveStreaming by remember { mutableStateOf(true) }

    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedLogIds = remember { mutableStateListOf<Long>() }
    var menuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(initialProjectId, projects) {
        if (!initialProjectId.isNullOrBlank()) {
            viewModel.selectProject(initialProjectId)
        } else if (selectedId == null && projects.isNotEmpty()) {
            viewModel.selectProject(projects.first().id)
        }
    }

    val filteredLogs = remember(logs, filterQuery) {
        if (filterQuery.isBlank()) logs
        else logs.filter { it.message.contains(filterQuery, ignoreCase = true) }
    }

    LaunchedEffect(filteredLogs.size, isLiveStreaming) {
        if (isLiveStreaming && !isSelectionMode && filteredLogs.isNotEmpty() && !listState.isScrollInProgress) {
            try {
                listState.scrollToItem(filteredLogs.size - 1)
            } catch (_: Exception) {}
        }
    }

    var dropdownExpanded by remember { mutableStateOf(false) }
    val currentProject = projects.firstOrNull { it.id == selectedId }
    val timeFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()) }

    fun copyTextToClipboard(label: String, text: String, count: Int = 1) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        scope.launch {
            val msg = if (count > 1) "$count log lines copied to clipboard" else "Copied to clipboard"
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = {
                        Text("${selectedLogIds.size} Selected", fontWeight = FontWeight.Bold)
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSelectionMode = false
                            selectedLogIds.clear()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit Selection Mode")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            if (selectedLogIds.size == filteredLogs.size) {
                                selectedLogIds.clear()
                            } else {
                                selectedLogIds.clear()
                                selectedLogIds.addAll(filteredLogs.map { it.id })
                            }
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                        }
                        IconButton(
                            onClick = {
                                if (selectedLogIds.isNotEmpty()) {
                                    val selectedItems = filteredLogs.filter { it.id in selectedLogIds }
                                    val text = selectedItems.joinToString("\n") { log ->
                                        "[${timeFormatter.format(Date(log.timestamp))}] [${log.stream.name}] ${log.message}"
                                    }
                                    copyTextToClipboard("Selected Logs", text, selectedItems.size)
                                    isSelectionMode = false
                                    selectedLogIds.clear()
                                }
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Selected", tint = PrimaryAccent)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
                )
            } else {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Live Logs", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isLiveStreaming) StatusGreen else StatusYellow)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { isLiveStreaming = !isLiveStreaming }) {
                            Icon(
                                imageVector = if (isLiveStreaming) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isLiveStreaming) "Pause Stream" else "Resume Stream",
                                tint = if (isLiveStreaming) StatusYellow else StatusGreen
                            )
                        }
                        IconButton(
                            onClick = {
                                if (filteredLogs.isNotEmpty()) {
                                    val allLogsText = filteredLogs.joinToString("\n") { log ->
                                        "[${timeFormatter.format(Date(log.timestamp))}] [${log.stream.name}] ${log.message}"
                                    }
                                    copyTextToClipboard("All Logs", allLogsText, filteredLogs.size)
                                }
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy All Logs", tint = TextSecondary)
                        }
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = TextSecondary)
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Select Multiple Lines") },
                                    leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        isSelectionMode = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Copy Last 50 Lines") },
                                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        val slice = filteredLogs.takeLast(50)
                                        val text = slice.joinToString("\n") { log ->
                                            "[${timeFormatter.format(Date(log.timestamp))}] [${log.stream.name}] ${log.message}"
                                        }
                                        copyTextToClipboard("Recent Logs", text, slice.size)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Copy Errors Only (STDERR)") },
                                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = StatusRed) },
                                    onClick = {
                                        menuExpanded = false
                                        val errors = filteredLogs.filter { it.stream == LogStream.STDERR }
                                        if (errors.isNotEmpty()) {
                                            val text = errors.joinToString("\n") { log ->
                                                "[${timeFormatter.format(Date(log.timestamp))}] [${log.stream.name}] ${log.message}"
                                            }
                                            copyTextToClipboard("Error Logs", text, errors.size)
                                        } else {
                                            scope.launch { snackbarHostState.showSnackbar("No STDERR errors recorded") }
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Clear All Logs") },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StatusRed) },
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.clearLogs()
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = currentProject?.name ?: "Select Project",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    projects.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p.name) },
                            onClick = {
                                viewModel.selectProject(p.id)
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = filterQuery,
                onValueChange = { viewModel.setFilterQuery(it) },
                placeholder = { Text("Filter logs...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                trailingIcon = {
                    if (filterQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setFilterQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CodeBackground)
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                if (filteredLogs.isEmpty()) {
                    Text(
                        text = "No log output recorded.",
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredLogs, key = { it.id }) { log ->
                            val isSelected = log.id in selectedLogIds
                            LogItemRow(
                                log = log,
                                isSelectionMode = isSelectionMode,
                                isSelected = isSelected,
                                onToggleSelect = {
                                    if (isSelected) selectedLogIds.remove(log.id)
                                    else selectedLogIds.add(log.id)
                                },
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        isSelectionMode = true
                                        selectedLogIds.add(log.id)
                                    }
                                },
                                onCopyLine = { line ->
                                    if (isSelectionMode) {
                                        if (isSelected) selectedLogIds.remove(log.id)
                                        else selectedLogIds.add(log.id)
                                    } else {
                                        copyTextToClipboard("Log Line", line, 1)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LogItemRow(
    log: LogEntry,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onLongClick: () -> Unit,
    onCopyLine: (String) -> Unit
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }
    val timeStr = remember(log.timestamp) { timeFormatter.format(Date(log.timestamp)) }

    val textColor = when (log.stream) {
        LogStream.STDOUT -> TextPrimary
        LogStream.STDERR -> StatusRed
        LogStream.SYSTEM -> SecondaryTeal
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) PrimaryAccent.copy(alpha = 0.18f) else CodeBackground)
            .combinedClickable(
                onClick = { onCopyLine("[$timeStr] [${log.stream.name}] ${log.message}") },
                onLongClick = onLongClick
            )
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() },
                modifier = Modifier.size(24.dp).padding(end = 4.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = PrimaryAccent,
                    uncheckedColor = TextMuted
                )
            )
            Spacer(modifier = Modifier.width(4.dp))
        }

        Text(
            text = timeStr,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = TextMuted,
            modifier = Modifier.width(88.dp)
        )
        Text(
            text = "[${log.stream.name}] ",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (log.stream == LogStream.STDERR) StatusRed else PrimaryAccent,
            modifier = Modifier.width(68.dp)
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
