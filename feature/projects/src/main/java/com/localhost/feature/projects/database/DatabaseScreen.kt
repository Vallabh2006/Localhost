package com.localhost.feature.projects.database

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.localhost.core.designsystem.theme.StatusYellow
import com.localhost.core.designsystem.theme.TextOnAccent
import com.localhost.core.designsystem.theme.TextPrimary
import com.localhost.core.designsystem.theme.TextSecondary
import com.localhost.core.designsystem.theme.TextMuted


import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import android.content.Context
import androidx.compose.foundation.layout.PaddingValues

import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.localhost.core.designsystem.theme.TextMuted
import com.localhost.core.designsystem.theme.TextOnAccent
import com.localhost.core.designsystem.theme.TextPrimary
import com.localhost.core.designsystem.theme.TextSecondary
import com.localhost.core.model.DatabaseEngine

@Composable
fun DatabaseScreen(
    viewModel: DatabaseViewModel = hiltViewModel()
) {
    val selectedEngine by viewModel.selectedEngine.collectAsState()
    val mysqlConfig by viewModel.mysqlConfig.collectAsState()
    val isMysqlConnected by viewModel.isMysqlConnected.collectAsState()
    val mysqlMessage by viewModel.mysqlMessage.collectAsState()
    val isMariaDBDownloading by viewModel.isMariaDBDownloading.collectAsState()
    val mariaDBProgress by viewModel.mariaDBProgress.collectAsState()
    val mariaDBStatusMessage by viewModel.mariaDBStatusMessage.collectAsState()
    val testResult by viewModel.testConnectionResult.collectAsState()

    val databases by viewModel.databases.collectAsState()
    val selectedDb by viewModel.selectedDatabase.collectAsState()
    val tables by viewModel.tables.collectAsState()
    val selectedTable by viewModel.selectedTable.collectAsState()
    val tableSchema by viewModel.tableSchema.collectAsState()
    val tableData by viewModel.tableData.collectAsState()
    val allTableSchemas by viewModel.allTableSchemas.collectAsState()
    val queryResult by viewModel.queryResult.collectAsState()
    val users by viewModel.users.collectAsState()
    val databaseLogs by viewModel.databaseLogs.collectAsState()
    val isExecuting by viewModel.isExecuting.collectAsState()

    val context = LocalContext.current
    val importSqlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.importSqlFile(context, it) }
    }
    var selectedTab by remember { mutableIntStateOf(0) }
    val dbTerminalHistory by viewModel.dbTerminalHistory.collectAsState()
    val dbTerminalRunning by viewModel.dbTerminalRunning.collectAsState()
    val tabs = listOf("Controls & Settings", "Explorer", "Visualize", "Terminal (CLI)", "Query Console", "Users & Perms", "Export Database", "Logs")

    LaunchedEffect(Unit) {
        viewModel.checkServiceStatus()
    }

    LaunchedEffect(selectedTab) {
        viewModel.checkServiceStatus()
        if (selectedTab == 1) {
            viewModel.loadDatabases()
            if (selectedDb.isNotBlank()) {
                viewModel.loadTables(selectedDb)
                val curTable = selectedTable
                if (curTable != null) {
                    viewModel.loadTableSchema(selectedDb, curTable)
                    viewModel.loadTableData(selectedDb, curTable)
                }
            }
        } else if (selectedTab == 2) {
            viewModel.loadDatabases()
            if (selectedDb.isNotBlank()) {
                viewModel.loadAllSchemas(selectedDb)
            }
        }
    }

    var sqlQueryInput by remember { mutableStateOf("") }
    var showCreateUserDialog by remember { mutableStateOf(false) }
    var showChangePassDialog by remember { mutableStateOf<String?>(null) }
    var showDeleteUserDialog by remember { mutableStateOf<String?>(null) }
    var showGrantDialog by remember { mutableStateOf<String?>(null) }

    var newUsername by remember { mutableStateOf("") }
    var newHost by remember { mutableStateOf("%") }
    var newPassword by remember { mutableStateOf("") }
    var changePasswordInput by remember { mutableStateOf("") }

    val activeConfig = mysqlConfig
    val activeConnected = isMysqlConnected

    LaunchedEffect(Unit) {
        viewModel.checkServiceStatus()
    }

    LaunchedEffect(selectedTab) {
        viewModel.checkServiceStatus()
        if (selectedTab == 1) {
            viewModel.loadDatabases()
            if (selectedDb.isNotBlank()) {
                viewModel.loadTables(selectedDb)
            }
        } else if (selectedTab == 2) {
            viewModel.loadDatabases()
            if (selectedDb.isNotBlank()) {
                viewModel.loadAllSchemas(selectedDb)
            }
        }
    }
    val activeMessage = mysqlMessage

    Scaffold(
        containerColor = DarkBackground,
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Storage, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(20.dp))
                    Text("Database", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }

            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, if (isMysqlConnected) StatusGreen.copy(alpha = 0.5f) else DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column {
                            Text("MariaDB Server 13.0 (Port 3306)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                            Text(
                                if (isMysqlConnected) "Native Server Active (127.0.0.1:3306)" else (mariaDBStatusMessage ?: "Server Daemon Stopped"),
                                fontSize = 11.sp,
                                color = if (isMysqlConnected) StatusGreen else TextMuted
                            )
                        }
                    }
                    if (!viewModel.isMariaDBInstalled()) {
                        Button(
                            onClick = { viewModel.installMariaDB() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            enabled = !isMariaDBDownloading
                        ) {
                            Text(if (isMariaDBDownloading) "Installing..." else "Install MariaDB", fontSize = 11.sp, color = TextOnAccent)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tabs.forEachIndexed { idx, title ->
                    val isSelected = selectedTab == idx
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) PrimaryAccentContainer else DarkSurfaceElevated,
                        border = BorderStroke(1.dp, if (isSelected) PrimaryAccent else DarkBorderSubtle),
                        modifier = Modifier.clickable { selectedTab = idx }
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) PrimaryAccent else TextMuted,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (selectedTab) {
                    0 -> {
                        DatabaseDashboardTab(
                            engine = selectedEngine,
                            config = activeConfig,
                            isConnected = activeConnected,
                            statusMessage = activeMessage,
                            databases = databases,
                            users = users,
                            onUpdateConfig = { h, p, u, pwd, db -> viewModel.updateConfig(h, p, u, pwd, db) },
                            onStart = { viewModel.connect() },
                            onStop = { viewModel.disconnect() },
                            onRestart = { viewModel.restartService() },
                            onTest = { viewModel.testConnection() }
                        )
                    }
                    1 -> {
                        ExplorerTab(
                            databases = databases,
                            selectedDb = selectedDb,
                            tables = tables,
                            selectedTable = selectedTable,
                            tableSchema = tableSchema,
                            tableData = tableData,
                            isConnected = activeConnected,
                            onStartService = { viewModel.connect() },
                            onSelectDb = { viewModel.selectDatabase(it) },
                            onSelectTable = { db, tbl -> viewModel.selectTable(db, tbl) },
                            onRefresh = {
                                viewModel.loadDatabases()
                                if (selectedDb.isNotBlank()) {
                                    viewModel.loadTables(selectedDb)
                                }
                            }
                        )
                    }
                    2 -> {
                        VisualizeTab(
                            databases = databases,
                            selectedDb = selectedDb,
                            allSchemas = allTableSchemas,
                            isConnected = activeConnected,
                            onStartService = { viewModel.connect() },
                            onSelectDb = { viewModel.selectDatabase(it) },
                            onRefresh = {
                                viewModel.loadDatabases()
                                if (selectedDb.isNotBlank()) {
                                    viewModel.loadAllSchemas(selectedDb)
                                }
                            }
                        )
                    }
                    3 -> {
                        DatabaseCliTerminalTab(
                            terminalHistory = dbTerminalHistory,
                            isRunning = dbTerminalRunning,
                            isConnected = activeConnected,
                            onStartService = { viewModel.connect() },
                            onRunCommand = { viewModel.runDbTerminalCommand(it) },
                            onClear = { viewModel.clearDbTerminal() }
                        )
                    }
                    4 -> {
                        QueryConsoleTab(
                            query = sqlQueryInput,
                            onQueryChange = { sqlQueryInput = it },
                            onExecute = { viewModel.executeQuery(sqlQueryInput) },
                            queryResult = queryResult,
                            isExecuting = isExecuting,
                            isConnected = activeConnected,
                            engine = selectedEngine
                        )
                    }
                    5 -> {
                        UsersTab(
                            users = users,
                            onCreateUser = { showCreateUserDialog = true },
                            onChangePassword = { showChangePassDialog = it },
                            onDeleteUser = { showDeleteUserDialog = it },
                            onGrant = { showGrantDialog = it }
                        )
                    }
                    6 -> {
                        ExportDatabaseTab(
                            databases = databases,
                            selectedDb = selectedDb,
                            onSelectDb = { viewModel.selectDatabase(it) },
                            onExport = { db, includeData, share ->
                                val sql = viewModel.generateSqlExport(db, includeData)
                                viewModel.exportSqlToFile(context, db, sql, share)
                            },
                            onImport = { importSqlLauncher.launch("*/*") }
                        )
                    }
                    7 -> {
                        DatabaseLogsTab(
                            logs = databaseLogs,
                            onClearLogs = { viewModel.clearLogs() }
                        )
                    }
                }
            }
        }
    }

    testResult?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearTestResult() },
            containerColor = DarkSurfaceElevated,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val isOk = msg.startsWith("Success")
                    Icon(
                        if (isOk) Icons.Default.Check else Icons.Default.Security,
                        contentDescription = null,
                        tint = if (isOk) StatusGreen else StatusRed
                    )
                    Text(if (isOk) "Test Succeeded" else "Test Failed", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                }
            },
            text = {
                Text(msg, color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp, fontFamily = FontFamily.Monospace)
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearTestResult() },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("OK", color = TextOnAccent)
                }
            }
        )
    }

    if (showCreateUserDialog) {
        AlertDialog(
            onDismissRequest = { showCreateUserDialog = false },
            containerColor = DarkSurfaceElevated,
            title = { Text("Create Database User", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newUsername,
                        onValueChange = { newUsername = it },
                        label = { Text("Username", fontSize = 11.sp, color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedContainerColor = DarkSurface, unfocusedContainerColor = DarkSurface)
                    )
                    OutlinedTextField(
                        value = newHost,
                        onValueChange = { newHost = it },
                        label = { Text("Host (% for any)", fontSize = 11.sp, color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedContainerColor = DarkSurface, unfocusedContainerColor = DarkSurface)
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Password (optional)", fontSize = 11.sp, color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedContainerColor = DarkSurface, unfocusedContainerColor = DarkSurface)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newUsername.isNotBlank()) {
                            viewModel.createUser(newUsername.trim(), newHost.trim(), newPassword)
                            newUsername = ""
                            newPassword = ""
                            showCreateUserDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Create", color = TextOnAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateUserDialog = false }) { Text("Cancel", color = TextMuted) }
            }
        )
    }

    showChangePassDialog?.let { targetUser ->
        AlertDialog(
            onDismissRequest = { showChangePassDialog = null },
            containerColor = DarkSurfaceElevated,
            title = { Text("Change Password for '$targetUser'", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                OutlinedTextField(
                    value = changePasswordInput,
                    onValueChange = { changePasswordInput = it },
                    label = { Text("New Password", fontSize = 11.sp, color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedContainerColor = DarkSurface, unfocusedContainerColor = DarkSurface)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.changePassword(targetUser, changePasswordInput)
                        changePasswordInput = ""
                        showChangePassDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Save", color = TextOnAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePassDialog = null }) { Text("Cancel", color = TextMuted) }
            }
        )
    }

    showDeleteUserDialog?.let { targetUser ->
        AlertDialog(
            onDismissRequest = { showDeleteUserDialog = null },
            containerColor = DarkSurfaceElevated,
            title = { Text("Delete User '$targetUser'?", color = StatusRed, fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Text("This will permanently remove the database user and revoke all privileges.", color = TextSecondary, fontSize = 13.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteUser(targetUser)
                        showDeleteUserDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete", color = TextOnAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteUserDialog = null }) { Text("Cancel", color = TextMuted) }
            }
        )
    }

    showGrantDialog?.let { targetUser ->
        val currentPrivs = users.firstOrNull { it.username.equals(targetUser, ignoreCase = true) }?.privileges
        var selPrivileges by remember(targetUser) {
            mutableStateOf(
                if (targetUser.equals("root", ignoreCase = true)) listOf("ALL PRIVILEGES")
                else currentPrivs ?: listOf("ALL PRIVILEGES")
            )
        }
        val allPrivs = listOf("SELECT", "INSERT", "UPDATE", "DELETE", "CREATE", "DROP", "ALL PRIVILEGES")
        AlertDialog(
            onDismissRequest = { showGrantDialog = null },
            containerColor = DarkSurfaceElevated,
            title = { Text("Grant Privileges to '$targetUser'", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    allPrivs.forEach { priv ->
                        val checked = selPrivileges.contains(priv)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable {
                            selPrivileges = if (checked) selPrivileges - priv else selPrivileges + priv
                        }) {
                            Checkbox(checked = checked, onCheckedChange = { isC ->
                                selPrivileges = if (isC) selPrivileges + priv else selPrivileges - priv
                            }, colors = CheckboxDefaults.colors(checkedColor = PrimaryAccent))
                            Text(priv, fontSize = 12.sp, color = TextPrimary)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.grantPrivileges(targetUser, "%", selectedDb, selPrivileges)
                        showGrantDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Apply", color = TextOnAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGrantDialog = null }) { Text("Cancel", color = TextMuted) }
            }
        )
    }
}

@Composable
private fun DatabaseDashboardTab(
    engine: DatabaseEngine,
    config: com.localhost.core.model.DatabaseConfig,
    isConnected: Boolean,
    statusMessage: String?,
    databases: List<String>,
    users: List<com.localhost.core.model.DatabaseUser>,
    onUpdateConfig: (String, Int, String, String, String) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onTest: () -> Unit
) {
    val haptics = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card 1: Server Process Control (styled identically to Project Process Control)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, if (isConnected) StatusGreen.copy(alpha = 0.4f) else DarkBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Server Process Control", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isConnected) StatusGreen.copy(alpha = 0.15f) else TextMuted.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, if (isConnected) StatusGreen.copy(alpha = 0.4f) else DarkBorderSubtle)
                    ) {
                        Text(
                            text = if (isConnected) "ONLINE (: ${config.port})" else "STOPPED",
                            color = if (isConnected) StatusGreen else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                if (isConnected) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onStop()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusYellow),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Stop Service", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onRestart()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, tint = TextOnAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restart Service", color = TextOnAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onStart()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start Service", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Card 2: Connection & Engine Settings
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
                        Icon(Icons.Default.Storage, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connection & Settings", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    }

                    Button(
                        onClick = onTest,
                        enabled = isConnected,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated, disabledContainerColor = DarkSurfaceElevated.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, if (isConnected) PrimaryAccent.copy(alpha = 0.5f) else DarkBorderSubtle),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = if (isConnected) PrimaryAccent else TextMuted, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test Connection", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (isConnected) TextPrimary else TextMuted)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = config.host,
                        onValueChange = { onUpdateConfig(it, config.port, config.user, config.password, config.database) },
                        label = { Text("Host", fontSize = 11.sp, color = TextMuted) },
                        singleLine = true,
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryAccent,
                            unfocusedBorderColor = DarkBorderSubtle,
                            focusedContainerColor = DarkSurfaceElevated,
                            unfocusedContainerColor = DarkSurfaceElevated
                        )
                    )
                    OutlinedTextField(
                        value = config.port.toString(),
                        onValueChange = { p -> p.toIntOrNull()?.let { onUpdateConfig(config.host, it, config.user, config.password, config.database) } },
                        label = { Text("Port", fontSize = 11.sp, color = TextMuted) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryAccent,
                            unfocusedBorderColor = DarkBorderSubtle,
                            focusedContainerColor = DarkSurfaceElevated,
                            unfocusedContainerColor = DarkSurfaceElevated
                        )
                    )
                }

                var dbPasswordVisible by remember { mutableStateOf(false) }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = config.user,
                        onValueChange = { onUpdateConfig(config.host, config.port, it, config.password, config.database) },
                        label = { Text("User", fontSize = 11.sp, color = TextMuted) },
                        placeholder = { Text("root", color = TextMuted, fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryAccent,
                            unfocusedBorderColor = DarkBorderSubtle,
                            focusedContainerColor = DarkSurfaceElevated,
                            unfocusedContainerColor = DarkSurfaceElevated
                        )
                    )
                    OutlinedTextField(
                        value = config.password,
                        onValueChange = { onUpdateConfig(config.host, config.port, config.user, it, config.database) },
                        label = { Text("Password", fontSize = 11.sp, color = TextMuted) },
                        placeholder = { Text("None (null)", color = TextMuted, fontSize = 12.sp) },
                        singleLine = true,
                        visualTransformation = if (dbPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(
                                onClick = { dbPasswordVisible = !dbPasswordVisible },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (dbPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (dbPasswordVisible) "Hide password" else "Show password",
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryAccent,
                            unfocusedBorderColor = DarkBorderSubtle,
                            focusedContainerColor = DarkSurfaceElevated,
                            unfocusedContainerColor = DarkSurfaceElevated
                        )
                    )
                }

                statusMessage?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurfaceElevated,
                        border = BorderStroke(1.dp, if (isConnected) StatusGreen.copy(alpha = 0.3f) else StatusRed.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            fontSize = 11.sp,
                            color = if (isConnected) StatusGreen else StatusRed,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        // Card 3: Service Telemetry & Stats (Styled identically to Project Stats)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Service Telemetry & Stats", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkSurfaceElevated,
                        border = BorderStroke(1.dp, DarkBorderSubtle),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Databases", fontSize = 11.sp, color = TextMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${databases.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryAccent)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkSurfaceElevated,
                        border = BorderStroke(1.dp, DarkBorderSubtle),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Users", fontSize = 11.sp, color = TextMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${users.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkSurfaceElevated,
                        border = BorderStroke(1.dp, DarkBorderSubtle),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Port", fontSize = 11.sp, color = TextMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${config.port}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = StatusGreen)
                        }
                    }
                }
            }
        }

        // Card 4: Danger Zone
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Danger Zone", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = StatusRed)
                Text(
                    "Service operations and maintenance controls for MariaDB daemon and process files.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onRestart()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, tint = TextOnAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restart MariaDB Daemon", color = TextOnAccent, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ExplorerTab(
    databases: List<String>,
    selectedDb: String,
    tables: List<String>,
    selectedTable: String?,
    tableSchema: List<com.localhost.core.model.ColumnInfo>,
    tableData: com.localhost.core.model.TableData?,
    isConnected: Boolean,
    onStartService: () -> Unit,
    onSelectDb: (String) -> Unit,
    onSelectTable: (String, String) -> Unit,
    onRefresh: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!isConnected) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = StatusOrange.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, StatusOrange.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = StatusOrange, modifier = Modifier.size(16.dp))
                        Text(
                            text = "MariaDB service is offline",
                            color = StatusOrange,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Button(
                        onClick = onStartService,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Start Service", color = androidx.compose.ui.graphics.Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var dbExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkSurfaceElevated,
                    border = BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth().clickable {
                        onRefresh()
                        dbExpanded = true
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Database", fontSize = 9.sp, color = TextMuted)
                            Text(selectedDb.ifBlank { "Select Database" }, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1)
                        }
                        Icon(Icons.Default.ExpandMore, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                    }
                }
                DropdownMenu(
                    expanded = dbExpanded,
                    onDismissRequest = { dbExpanded = false },
                    modifier = Modifier.background(DarkSurfaceElevated).heightIn(max = 280.dp)
                ) {
                    if (databases.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No databases found (tap to refresh)", fontSize = 12.sp, color = TextMuted) },
                            onClick = {
                                onRefresh()
                                dbExpanded = false
                            }
                        )
                    } else {
                        databases.forEach { db ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = db,
                                        fontSize = 12.sp,
                                        fontWeight = if (db == selectedDb) FontWeight.Bold else FontWeight.Normal,
                                        color = if (db == selectedDb) PrimaryAccent else TextPrimary
                                    )
                                },
                                onClick = {
                                    onSelectDb(db)
                                    dbExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            var tableExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkSurfaceElevated,
                    border = BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth().clickable { tableExpanded = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Table", fontSize = 9.sp, color = TextMuted)
                            Text(selectedTable ?: "Select Table", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selectedTable != null) PrimaryAccent else TextMuted, maxLines = 1)
                        }
                        Icon(Icons.Default.ExpandMore, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                    }
                }
                DropdownMenu(
                    expanded = tableExpanded,
                    onDismissRequest = { tableExpanded = false },
                    modifier = Modifier.background(DarkSurfaceElevated).heightIn(max = 280.dp)
                ) {
                    if (tables.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No tables found in '${selectedDb.ifBlank { "database" }}'", fontSize = 12.sp, color = TextMuted) },
                            onClick = { tableExpanded = false }
                        )
                    } else {
                        tables.forEach { tbl ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = tbl,
                                        fontSize = 12.sp,
                                        fontWeight = if (tbl == selectedTable) FontWeight.Bold else FontWeight.Normal,
                                        color = if (tbl == selectedTable) PrimaryAccent else TextPrimary
                                    )
                                },
                                onClick = {
                                    onSelectTable(selectedDb, tbl)
                                    tableExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = onRefresh,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = PrimaryAccent, modifier = Modifier.size(18.dp))
            }
        }

        HorizontalDivider(color = DarkBorderSubtle)

        if (tableData != null && tableData.columns.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${selectedTable} (${tableData.rows.size} rows, ${tableData.columns.size} cols)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = TextPrimary
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(8.dp))
                    .border(BorderStroke(1.dp, DarkBorder), RoundedCornerShape(8.dp))
                    .horizontalScroll(rememberScrollState())
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                    item {
                        Row(
                            modifier = Modifier
                                .background(PrimaryAccentContainer, RoundedCornerShape(6.dp))
                                .padding(vertical = 6.dp, horizontal = 8.dp)
                        ) {
                            tableData.columns.forEach { col ->
                                Text(
                                    text = col,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = PrimaryAccent,
                                    modifier = Modifier.width(130.dp)
                                )
                            }
                        }
                    }
                    items(tableData.rows) { row ->
                        Row(
                            modifier = Modifier
                                .padding(vertical = 4.dp, horizontal = 8.dp)
                        ) {
                            row.forEach { cell ->
                                Text(
                                    text = cell,
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.width(130.dp),
                                    maxLines = 1
                                )
                            }
                        }
                        HorizontalDivider(color = DarkBorderSubtle.copy(alpha = 0.5f))
                    }
                }
            }
        } else if (selectedTable != null) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Schema for '${selectedTable}':",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = TextPrimary
                )
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(tableSchema) { col ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkSurface,
                            border = BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (col.isPrimaryKey) {
                                        Icon(Icons.Default.Key, contentDescription = "PK", tint = StatusOrange, modifier = Modifier.size(14.dp))
                                    }
                                    Column {
                                        Text(col.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                        Text(col.type, fontSize = 10.sp, color = TextMuted)
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (col.isNullable) {
                                        Surface(color = DarkSurfaceElevated, shape = RoundedCornerShape(4.dp)) {
                                            Text("NULL", fontSize = 9.sp, color = TextMuted, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                        }
                                    }
                                    if (col.defaultValue != null) {
                                        Surface(color = DarkSurfaceElevated, shape = RoundedCornerShape(4.dp)) {
                                            Text("default: ${col.defaultValue}", fontSize = 9.sp, color = TextMuted, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Select a database and table above to inspect schema and live rows", color = TextMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun VisualizeTab(
    databases: List<String>,
    selectedDb: String,
    allSchemas: List<TableSchemaEntry>,
    isConnected: Boolean,
    onStartService: () -> Unit,
    onSelectDb: (String) -> Unit,
    onRefresh: () -> Unit
) {
    var showForeignKeys by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!isConnected) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = StatusOrange.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, StatusOrange.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = StatusOrange, modifier = Modifier.size(24.dp))
                    Text(
                        "MariaDB Service is Offline",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                    Text(
                        "Start MariaDB to visualize tables, ER relations, and live column schemas.",
                        fontSize = 11.sp,
                        color = TextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Button(
                        onClick = onStartService,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Start Service", color = androidx.compose.ui.graphics.Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            var dbExpanded by remember { mutableStateOf(false) }
            Box {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkSurfaceElevated,
                    border = BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.clickable {
                        onRefresh()
                        dbExpanded = true
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Database: ${selectedDb.ifBlank { "Select Database" }}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryAccent)
                        Icon(Icons.Default.ExpandMore, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
                DropdownMenu(
                    expanded = dbExpanded,
                    onDismissRequest = { dbExpanded = false },
                    modifier = Modifier.background(DarkSurfaceElevated).heightIn(max = 280.dp)
                ) {
                    if (databases.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No databases found (tap to refresh)", fontSize = 12.sp, color = TextMuted) },
                            onClick = {
                                onRefresh()
                                dbExpanded = false
                            }
                        )
                    } else {
                        databases.forEach { db ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = db,
                                        fontSize = 12.sp,
                                        fontWeight = if (db == selectedDb) FontWeight.Bold else FontWeight.Normal,
                                        color = if (db == selectedDb) PrimaryAccent else TextPrimary
                                    )
                                },
                                onClick = {
                                    onSelectDb(db)
                                    dbExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("FK Lines", fontSize = 10.sp, color = TextMuted)
                Switch(
                    checked = showForeignKeys,
                    onCheckedChange = { showForeignKeys = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = StatusOrange, checkedTrackColor = StatusOrange.copy(alpha = 0.3f))
                )
                IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = PrimaryAccent, modifier = Modifier.size(16.dp))
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(DarkSurface, RoundedCornerShape(8.dp))
                .border(BorderStroke(1.dp, DarkBorder), RoundedCornerShape(8.dp))
        ) {
            if (allSchemas.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No schema data available for '$selectedDb'.", color = TextMuted, fontSize = 12.sp)
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(rememberScrollState())
                        .verticalScroll(rememberScrollState())
                ) {
                    val cardWidth = 200.dp
                    val cardMargin = 20.dp
                    val totalCards = allSchemas.size
                    val totalCols = if (totalCards <= 2) totalCards else 2
                    val totalRows = (totalCards + totalCols - 1) / totalCols
                    val totalCanvasWidth = ((cardWidth + cardMargin) * totalCols) + 40.dp
                    val totalCanvasHeight = (280.dp * totalRows) + 80.dp

                    Box(modifier = Modifier.size(totalCanvasWidth, totalCanvasHeight)) {
                        if (showForeignKeys) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val colW = (cardWidth + cardMargin).toPx()
                                val rowH = 280.dp.toPx()

                                allSchemas.forEachIndexed { i, source ->
                                    val srcCol = i % totalCols
                                    val srcRow = i / totalCols
                                    val srcX = 20.dp.toPx() + srcCol * colW + cardWidth.toPx()
                                    val srcY = 20.dp.toPx() + srcRow * rowH + 40.dp.toPx()

                                    allSchemas.forEachIndexed { j, target ->
                                        if (i != j) {
                                            val hasFk = source.columns.any { sc ->
                                                target.columns.any { tc ->
                                                    tc.isPrimaryKey && (sc.name.equals("${target.tableName}_id", ignoreCase = true) || (sc.name.equals("user_id", ignoreCase = true) && target.tableName.equals("users", ignoreCase = true)))
                                                }
                                            }

                                            if (hasFk) {
                                                val tgtCol = j % totalCols
                                                val tgtRow = j / totalCols
                                                val tgtX = 20.dp.toPx() + tgtCol * colW
                                                val tgtY = 20.dp.toPx() + tgtRow * rowH + 40.dp.toPx()

                                                drawLine(
                                                    color = StatusOrange,
                                                    start = Offset(srcX, srcY),
                                                    end = Offset(tgtX, tgtY),
                                                    strokeWidth = 2.5f,
                                                    cap = StrokeCap.Round,
                                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                                                )
                                                drawCircle(
                                                    color = StatusOrange,
                                                    radius = 4f,
                                                    center = Offset(tgtX, tgtY)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        allSchemas.forEachIndexed { idx, schemaEntry ->
                            val col = idx % totalCols
                            val row = idx / totalCols
                            val leftPad = 20.dp + (cardWidth + cardMargin) * col
                            val topPad = 20.dp + 280.dp * row

                            Box(modifier = Modifier.padding(start = leftPad, top = topPad)) {
                                TableEntityCard(schemaEntry = schemaEntry, modifier = Modifier.width(cardWidth))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TableEntityCard(schemaEntry: TableSchemaEntry, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = BorderStroke(1.dp, DarkBorderSubtle)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PrimaryAccentContainer)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.TableChart, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(15.dp))
                Text(
                    text = schemaEntry.tableName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = PrimaryAccent,
                    maxLines = 1
                )
            }

            HorizontalDivider(color = DarkBorderSubtle)

            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                schemaEntry.columns.forEach { col ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (col.isPrimaryKey) {
                                Icon(Icons.Default.Key, contentDescription = "PK", tint = StatusOrange, modifier = Modifier.size(11.dp))
                            }
                            Text(
                                text = col.name,
                                fontSize = 11.sp,
                                fontWeight = if (col.isPrimaryKey) FontWeight.Bold else FontWeight.Normal,
                                color = if (col.isPrimaryKey) TextPrimary else TextSecondary,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = col.type.lowercase(),
                            fontSize = 9.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QueryConsoleTab(
    query: String,
    onQueryChange: (String) -> Unit,
    onExecute: () -> Unit,
    queryResult: com.localhost.core.model.QueryResult?,
    isExecuting: Boolean,
    isConnected: Boolean,
    engine: DatabaseEngine
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!isConnected) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = StatusOrange.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, StatusOrange.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = StatusOrange, modifier = Modifier.size(16.dp))
                    Text(
                        text = "${engine.displayName} service is stopped. Start the service from Dashboard to run queries.",
                        color = StatusOrange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val quickQueries = listOf(
                "SELECT current_timestamp;",
                "SELECT * FROM users LIMIT 20;",
                "SELECT * FROM accounts;",
                "SELECT version();",
                "SHOW TABLES;"
            )
            quickQueries.forEach { q ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = DarkSurfaceElevated,
                    border = BorderStroke(1.dp, DarkBorderSubtle),
                    modifier = Modifier.clickable { onQueryChange(q) }
                ) {
                    Text(
                        text = q,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Enter SQL query (e.g. SELECT * FROM users)...", color = TextMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace) },
            modifier = Modifier.fillMaxWidth().height(90.dp),
            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = TextPrimary),
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

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(
                onClick = onExecute,
                enabled = isConnected && !isExecuting,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent, disabledContainerColor = DarkSurfaceElevated)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = if (isConnected) TextOnAccent else TextMuted, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = when {
                        isExecuting -> "Running..."
                        !isConnected -> "Service Stopped"
                        else -> "Run Query"
                    },
                    color = if (isConnected) TextOnAccent else TextMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        queryResult?.let { res ->
            HorizontalDivider(color = DarkBorderSubtle)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = if (res.isSuccess) "Success (${res.executionTimeMs}ms, ${res.affectedRows} affected)" else "Error",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = if (res.isSuccess) StatusGreen else StatusRed
                )
            }
            if (res.isSuccess && res.columns.isNotEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().background(DarkSurfaceElevated, RoundedCornerShape(8.dp)).horizontalScroll(rememberScrollState())
                ) {
                    LazyColumn(modifier = Modifier.padding(6.dp)) {
                        item {
                            Row(modifier = Modifier.background(PrimaryAccentContainer).padding(4.dp)) {
                                res.columns.forEach { c ->
                                    Text(c, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = PrimaryAccent, modifier = Modifier.width(180.dp))
                                }
                            }
                        }
                        items(res.rows) { r ->
                            Row(modifier = Modifier.padding(vertical = 3.dp, horizontal = 4.dp)) {
                                r.forEach { cell ->
                                    Text(cell, fontSize = 10.sp, color = TextSecondary, modifier = Modifier.width(180.dp))
                                }
                            }
                        }
                    }
                }
            } else if (!res.isSuccess) {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusRed.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Query Execution Error", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = StatusRed)
                        Text(
                            text = res.errorMessage ?: "Query failed",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UsersTab(
    users: List<com.localhost.core.model.DatabaseUser>,
    onCreateUser: () -> Unit,
    onChangePassword: (String) -> Unit,
    onDeleteUser: (String) -> Unit,
    onGrant: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Database Users (${users.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
            Button(
                onClick = onCreateUser,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = TextOnAccent, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New User", color = TextOnAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(users) { u ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkSurface,
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(u.username, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                Surface(shape = RoundedCornerShape(4.dp), color = DarkSurfaceElevated) {
                                    Text("@${u.host}", fontSize = 9.sp, color = TextMuted, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                }
                            }
                            Text(
                                text = "Privileges: ${u.privileges.joinToString(", ")}",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { onGrant(u.username) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Security, contentDescription = "Grant", tint = PrimaryAccent, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { onChangePassword(u.username) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Key, contentDescription = "Password", tint = TextMuted, modifier = Modifier.size(16.dp))
                            }
                            if (u.username != "root") {
                                IconButton(onClick = { onDeleteUser(u.username) }, modifier = Modifier.size(32.dp)) {
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

@Composable
private fun ExportDatabaseTab(
    databases: List<String>,
    selectedDb: String,
    onSelectDb: (String) -> Unit,
    onExport: (String, Boolean, Boolean) -> Unit,
    onImport: () -> Unit
) {
    var includeData by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Export Database", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                    "Generate a standard SQL dump file (.sql) containing table schemas and record data for backup or external database restoration.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )

                Text("Select Target Database:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                var dbExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurfaceElevated,
                        border = BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.fillMaxWidth().clickable { dbExpanded = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Target Database", fontSize = 9.sp, color = TextMuted)
                                Text(selectedDb, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryAccent)
                            }
                            Icon(Icons.Default.ExpandMore, contentDescription = null, tint = TextMuted)
                        }
                    }
                    DropdownMenu(
                        expanded = dbExpanded,
                        onDismissRequest = { dbExpanded = false },
                        modifier = Modifier.background(DarkSurfaceElevated).heightIn(max = 280.dp)
                    ) {
                        if (databases.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No databases found", fontSize = 12.sp, color = TextMuted) },
                                onClick = { dbExpanded = false }
                            )
                        } else {
                            databases.forEach { db ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = db,
                                            fontSize = 12.sp,
                                            fontWeight = if (db == selectedDb) FontWeight.Bold else FontWeight.Normal,
                                            color = if (db == selectedDb) PrimaryAccent else TextPrimary
                                        )
                                    },
                                    onClick = {
                                        onSelectDb(db)
                                        dbExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Include Table Data (INSERTs)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(if (includeData) "Exports both CREATE TABLE and INSERT statements" else "Schema only (CREATE TABLE)", fontSize = 10.sp, color = TextMuted)
                    }
                    Switch(
                        checked = includeData,
                        onCheckedChange = { includeData = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryAccent, checkedTrackColor = PrimaryAccentContainer)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { onExport(selectedDb, includeData, false) },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, PrimaryAccent),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Locally", color = PrimaryAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = { onExport(selectedDb, includeData, true) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = TextOnAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share SQL", color = TextOnAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                HorizontalDivider(color = DarkBorderSubtle)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Import Database SQL File", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Import and execute an existing SQL dump (.sql) into '$selectedDb'.", fontSize = 11.sp, color = TextSecondary)
                    OutlinedButton(
                        onClick = onImport,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, StatusGreen),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import .sql File into $selectedDb", color = StatusGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DatabaseLogsTab(
    logs: List<String>,
    onClearLogs: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Terminal, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(16.dp))
                    Text("Database Activity Logs", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = PrimaryAccentContainer
                    ) {
                        Text("${logs.size}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryAccent, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Database Logs", logs.joinToString("\n"))
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Logs copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, DarkBorder),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Copy", fontSize = 11.sp, color = TextPrimary)
                    }

                    OutlinedButton(
                        onClick = onClearLogs,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, StatusRed.copy(alpha = 0.4f)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Clear", fontSize = 11.sp, color = StatusRed)
                    }
                }
            }

            Surface(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = com.localhost.core.designsystem.theme.CodeBackground,
                border = BorderStroke(1.dp, DarkBorderSubtle)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    items(logs) { logLine ->
                        val logColor = when {
                            logLine.contains("[ERROR]") || logLine.contains("[SQL-ERROR]") -> StatusRed
                            logLine.contains("[WARN]") -> StatusOrange
                            logLine.contains("[SQL]") -> PrimaryAccent
                            logLine.contains("[SQL-RESULT]") || logLine.contains("[TEST]") || logLine.contains("SUCCESS") -> StatusGreen
                            logLine.contains("[USER]") -> androidx.compose.ui.graphics.Color(0xFF67E8F9)
                            else -> TextSecondary
                        }
                        SelectionContainer {
                            Text(
                                text = logLine,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = logColor
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun DatabaseCliTerminalTab(
    terminalHistory: List<DbTerminalLine>,
    isRunning: Boolean,
    isConnected: Boolean,
    onStartService: () -> Unit,
    onRunCommand: (String) -> Unit,
    onClear: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    var historyIndex by remember { mutableIntStateOf(-1) }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val accessoryKeys = listOf("SHOW DATABASES;", "SHOW TABLES;", "STATUS;", "SELECT * FROM ", "DESCRIBE ", "Ctrl+C", "clear")

    val lastOutputLength = terminalHistory.lastOrNull()?.output?.length ?: 0
    LaunchedEffect(terminalHistory.size, lastOutputLength) {
        if (terminalHistory.isNotEmpty()) {
            listState.scrollToItem(terminalHistory.size - 1, 10000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!isConnected) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = StatusOrange.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, StatusOrange.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = StatusOrange, modifier = Modifier.size(15.dp))
                        Text(
                            text = "MariaDB service is offline",
                            color = StatusOrange,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Button(
                        onClick = onStartService,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("Start Service", color = androidx.compose.ui.graphics.Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
            }
        }
        Surface(
            color = DarkSurface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, DarkBorder),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(terminalHistory) { item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val text = if (item.command.isNotBlank()) "mariadb> ${item.command}\n${item.output}" else item.output
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Terminal Output", text)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Output copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        if (item.command.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "mariadb> ",
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
                            Text(
                                text = item.output,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = if (item.isError) StatusRed else TextSecondary
                            )
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
            val commands = terminalHistory.map { it.command }.filter { it.isNotBlank() }
            Surface(
                color = DarkSurface,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, DarkBorderSubtle),
                modifier = Modifier.clickable {
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
                            "clear" -> onClear()
                            "Ctrl+C" -> input = ""
                            else -> input += if (input.isEmpty()) key else " $key"
                        }
                    }
                ) {
                    Text(
                        text = key,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
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
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "mariadb> ",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
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
                        val toSend = input.trim()
                        if (toSend.isNotBlank()) {
                            onRunCommand(toSend)
                            input = ""
                            historyIndex = -1
                        }
                    }),
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        val toSend = input.trim()
                        if (toSend.isNotBlank()) {
                            onRunCommand(toSend)
                            input = ""
                            historyIndex = -1
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Execute",
                        tint = PrimaryAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
