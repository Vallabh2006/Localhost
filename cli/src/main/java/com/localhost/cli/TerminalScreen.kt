package com.localhost.cli
import androidx.compose.foundation.layout.WindowInsets

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localhost.core.designsystem.theme.CodeBackground
import com.localhost.core.designsystem.theme.DarkBackground
import com.localhost.core.designsystem.theme.DarkBorderSubtle
import com.localhost.core.designsystem.theme.DarkSurface
import com.localhost.core.designsystem.theme.DarkSurfaceElevated
import com.localhost.core.designsystem.theme.PrimaryAccent
import com.localhost.core.designsystem.theme.StatusGreen
import com.localhost.core.designsystem.theme.StatusRed
import com.localhost.core.designsystem.theme.StatusYellow
import com.localhost.core.designsystem.theme.TextMuted
import com.localhost.core.designsystem.theme.TextPrimary
import com.localhost.core.designsystem.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel
) {
    val history by viewModel.history.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()

    var input by remember { mutableStateOf("") }
    var historyIndex by remember { mutableIntStateOf(-1) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }

    val accessoryKeys = listOf("Tab", "Ctrl+C", "|", "-", "/", "~", "ls -la", "cd ..", "pwd", "clear")
    val allCommands = listOf("ls", "cd", "pwd", "cat", "head", "tail", "mkdir", "rmdir", "touch", "cp", "mv", "rm", "grep", "find", "curl", "chmod", "source", "activate", "deactivate", "composer", "python3", "pip", "node", "npm", "php", "java", "ps", "status", "start", "stop", "restart", "help", "clear")

    val matchingSuggestions = remember(input) {
        val trimmed = input.trim()
        if (trimmed.isNotEmpty() && !trimmed.contains(" ")) {
            allCommands.filter { it.startsWith(trimmed, ignoreCase = true) && it != trimmed }.take(5)
        } else {
            emptyList()
        }
    }

    val lastOutputLength = history.lastOrNull()?.output?.length ?: 0
    LaunchedEffect(history.size, lastOutputLength) {
        if (history.isNotEmpty()) {
            listState.scrollToItem(history.size - 1, 10000)
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Terminal", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        val venvName = viewModel.getActiveVenvName()
                        val headerPath = if (venvName != null) "($venvName) $currentPath" else currentPath
                        Text(
                            text = headerPath,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = StatusYellow,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    if (isRunning) {
                        IconButton(onClick = { viewModel.stopRunningCommand() }) {
                            Icon(Icons.Default.Stop, contentDescription = "Interrupt", tint = StatusRed)
                        }
                    }
                    IconButton(onClick = { viewModel.clear() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Terminal", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CodeBackground)
                    .border(1.dp, DarkBorderSubtle, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                SelectionContainer {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            Text(
                                text = "Localhost Environment Shell (Type 'help' for commands)",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }

                        items(history) { item ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val text = if (item.command.isNotBlank()) "$ ${item.command}\n${item.output}" else item.output
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Terminal Output", text)
                                        clipboard.setPrimaryClip(clip)
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Copied command output")
                                        }
                                    }
                            ) {
                                if (item.command.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val promptDisplay = if (item.cwd.startsWith("(")) {
                                            val v = item.cwd.substringBefore(")") + ")"
                                            val p = item.cwd.substringAfter(")").trim()
                                            "$v user@localhost:$p$ "
                                        } else if (item.cwd.isNotBlank()) {
                                            "user@localhost:${item.cwd}$ "
                                        } else {
                                            "$ "
                                        }
                                        Text(
                                            text = promptDisplay,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryAccent
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
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                        color = if (isError) StatusRed else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (matchingSuggestions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    matchingSuggestions.forEach { sug ->
                        Surface(
                            color = DarkSurfaceElevated,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.clickable {
                                input = sug + " "
                            }
                        ) {
                            Text(sug, fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = PrimaryAccent, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = DarkSurface,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, DarkBorderSubtle),
                    modifier = Modifier.clickable {
                        val commands = history.map { it.command }.filter { it.isNotBlank() }
                        if (commands.isNotEmpty()) {
                            if (historyIndex < commands.size - 1) {
                                historyIndex++
                                input = commands[commands.size - 1 - historyIndex]
                            }
                        }
                    }
                ) {
                    Text("↑", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
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
                    Text("↓", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                }

                accessoryKeys.forEach { key ->
                    Surface(
                        color = DarkSurface,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, DarkBorderSubtle),
                        modifier = Modifier.clickable {
                            when (key) {
                                "Tab" -> {
                                    val firstSug = matchingSuggestions.firstOrNull()
                                    if (firstSug != null) input = firstSug + " "
                                }
                                "Ctrl+C" -> {
                                    viewModel.stopRunningCommand()
                                    input = ""
                                }
                                "clear" -> viewModel.clear()
                                "ls -la" -> viewModel.runCommand("ls -la")
                                "cd .." -> viewModel.runCommand("cd ..")
                                "pwd" -> viewModel.runCommand("pwd")
                                else -> input += key
                            }
                        }
                    ) {
                        Text(
                            text = key,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
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
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val activePrompt = viewModel.getActiveVenvName()?.let { "($it) $ " } ?: "$ "
                    Text(
                        text = activePrompt,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatusGreen
                    )

                    BasicTextField(
                        value = input,
                        onValueChange = { input = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = TextPrimary
                        ),
                        cursorBrush = SolidColor(PrimaryAccent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (input.isNotBlank()) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.runCommand(input)
                                input = ""
                                historyIndex = -1
                                scope.launch {
                                    if (history.isNotEmpty()) {
                                        listState.scrollToItem(history.size - 1, 10000)
                                    }
                                }
                            }
                        }),
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = {
                            if (input.isNotBlank()) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.runCommand(input)
                                input = ""
                                historyIndex = -1
                                scope.launch {
                                    if (history.isNotEmpty()) {
                                        listState.scrollToItem(history.size - 1, 10000)
                                    }
                                }
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Run",
                            tint = if (input.isNotBlank()) PrimaryAccent else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
