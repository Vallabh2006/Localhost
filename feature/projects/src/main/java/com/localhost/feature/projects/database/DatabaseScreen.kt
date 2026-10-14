package com.localhost.feature.projects.database

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
    val postgresConfig by viewModel.postgresConfig.collectAsState()
    val isMysqlConnected by viewModel.isMysqlConnected.collectAsState()
    val isPostgresConnected by viewModel.isPostgresConnected.collectAsState()
    val mysqlMessage by viewModel.mysqlMessage.collectAsState()
    val postgresMessage by viewModel.postgresMessage.collectAsState()
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
    val isExecuting by viewModel.isExecuting.collectAsState()

    val context = LocalContext.current
    val importSqlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.importSqlFile(context, it) }
    }
    var selectedTab by remember { mutableIntStateOf(0) }
    var isConfigExpanded by remember { mutableStateOf(false) }
    val tabs = listOf("Explorer", "Visualize", "Query Console", "Users & Perms", "Export Database")

    var sqlQueryInput by remember { mutableStateOf("SELECT * FROM users LIMIT 20;") }
    var showCreateUserDialog by remember { mutableStateOf(false) }
    var showChangePassDialog by remember { mutableStateOf<String?>(null) }
    var showDeleteUserDialog by remember { mutableStateOf<String?>(null) }
    var showGrantDialog by remember { mutableStateOf<String?>(null) }

    var newUsername by remember { mutableStateOf("") }
    var newHost by remember { mutableStateOf("%") }
    var newPassword by remember { mutableStateOf("") }
    var changePasswordInput by remember { mutableStateOf("") }

    val activeConfig = if (selectedEngine == DatabaseEngine.MYSQL) mysqlConfig else postgresConfig
    val activeConnected = if (selectedEngine == DatabaseEngine.MYSQL) isMysqlConnected else isPostgresConnected
    val activeMessage = if (selectedEngine == DatabaseEngine.MYSQL) mysqlMessage else postgresMessage

    Scaffold(
        containerColor = DarkBackground,
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Storage, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(24.dp))
                    Column {
                        Text("Database Studio", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Separate MySQL & PostgreSQL Services", fontSize = 11.sp, color = TextMuted)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (activeConnected) StatusGreen.copy(alpha = 0.15f) else StatusRed.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, if (activeConnected) StatusGreen else StatusRed)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (activeConnected) StatusGreen else StatusRed)
                        )
                        Text(
                            text = if (activeConnected) "Service Online" else "Offline",
                            color = if (activeConnected) StatusGreen else StatusRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val isMySqlSel = selectedEngine == DatabaseEngine.MYSQL
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isMySqlSel) PrimaryAccentContainer else DarkSurface,
                    border = BorderStroke(1.dp, if (isMySqlSel) PrimaryAccent else DarkBorder),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.selectService(DatabaseEngine.MYSQL) }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isMysqlConnected) StatusGreen else StatusRed)
                        )
                        Column {
                            Text("MySQL Service", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isMySqlSel) PrimaryAccent else TextPrimary)
                            Text("Port 3306 | ${if (isMysqlConnected) "Active" else "Offline"}", fontSize = 10.sp, color = TextMuted)
                        }
                    }
                }

                val isPgSel = selectedEngine == DatabaseEngine.POSTGRESQL
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isPgSel) PrimaryAccentContainer else DarkSurface,
                    border = BorderStroke(1.dp, if (isPgSel) PrimaryAccent else DarkBorder),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.selectService(DatabaseEngine.POSTGRESQL) }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isPostgresConnected) StatusGreen else StatusRed)
                        )
                        Column {
                            Text("PostgreSQL Service", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isPgSel) PrimaryAccent else TextPrimary)
                            Text("Port 5432 | ${if (isPostgresConnected) "Active" else "Offline"}", fontSize = 10.sp, color = TextMuted)
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, if (activeConnected) PrimaryAccent.copy(alpha = 0.3f) else DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.clickable { isConfigExpanded = !isConfigExpanded }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (activeConnected) StatusGreen else StatusRed)
                            )
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "${activeConfig.host}:${activeConfig.port}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = DarkSurfaceElevated,
                                        border = BorderStroke(1.dp, DarkBorderSubtle)
                                    ) {
                                        Text(
                                            text = activeConfig.user.ifBlank { "root" },
                                            fontSize = 10.sp,
                                            color = PrimaryAccent,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (isConfigExpanded) "Tap to hide configuration" else "Tap to edit credentials",
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.testConnection() },
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, DarkBorder),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Test", fontSize = 11.sp, color = TextPrimary)
                            }

                            Button(
                                onClick = {
                                    if (activeConnected) viewModel.disconnect() else viewModel.connect()
                                },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (activeConnected) DarkSurfaceElevated else PrimaryAccent
                                ),
                                border = if (activeConnected) BorderStroke(1.dp, StatusRed) else null,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(
                                    if (activeConnected) "Disconnect" else "Connect",
                                    color = if (activeConnected) StatusRed else TextOnAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }

                            IconButton(
                                onClick = { isConfigExpanded = !isConfigExpanded },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (isConfigExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Toggle Config",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (isConfigExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = DarkBorderSubtle)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = activeConfig.host,
                                onValueChange = { viewModel.updateConfig(it, activeConfig.port, activeConfig.user, activeConfig.password, activeConfig.database) },
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
                                value = activeConfig.port.toString(),
                                onValueChange = { p -> p.toIntOrNull()?.let { viewModel.updateConfig(activeConfig.host, it, activeConfig.user, activeConfig.password, activeConfig.database) } },
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
                                value = activeConfig.user,
                                onValueChange = { viewModel.updateConfig(activeConfig.host, activeConfig.port, it, activeConfig.password, activeConfig.database) },
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
                                value = activeConfig.password,
                                onValueChange = { viewModel.updateConfig(activeConfig.host, activeConfig.port, activeConfig.user, it, activeConfig.database) },
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
                    }

                    activeMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = msg,
                            fontSize = 10.sp,
                            color = if (activeConnected) StatusGreen else StatusRed,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 13.sp,
                            maxLines = if (isConfigExpanded) 4 else 1
                        )
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
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (selectedTab) {
                0 -> {
                    ExplorerTab(
                        databases = databases,
                        selectedDb = selectedDb,
                        tables = tables,
                        selectedTable = selectedTable,
                        tableSchema = tableSchema,
                        tableData = tableData,
                        onSelectDb = { viewModel.selectDatabase(it) },
                        onSelectTable = { db, tbl -> viewModel.selectTable(db, tbl) },
                        onRefresh = { viewModel.loadDatabases() }
                    )
                }
                1 -> {
                    VisualizeTab(
                        databases = databases,
                        selectedDb = selectedDb,
                        allSchemas = allTableSchemas,
                        onSelectDb = { viewModel.selectDatabase(it) },
                        onRefresh = { viewModel.loadAllSchemas(selectedDb) }
                    )
                }
                2 -> {
                    QueryConsoleTab(
                        query = sqlQueryInput,
                        onQueryChange = { sqlQueryInput = it },
                        onExecute = { viewModel.executeQuery(sqlQueryInput) },
                        queryResult = queryResult,
                        isExecuting = isExecuting
                    )
                }
                3 -> {
                    UsersTab(
                        users = users,
                        onCreateUser = { showCreateUserDialog = true },
                        onChangePassword = { showChangePassDialog = it },
                        onDeleteUser = { showDeleteUserDialog = it },
                        onGrant = { showGrantDialog = it }
                    )
                }
                4 -> {
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
                        label = { Text("Host (%)", fontSize = 11.sp, color = TextMuted) },
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
                            viewModel.createUser(newUsername, newHost, newPassword)
                            showCreateUserDialog = false
                            newUsername = ""
                            newPassword = ""
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter new password for $targetUser (leave empty for null password):", fontSize = 12.sp, color = TextSecondary)
                    OutlinedTextField(
                        value = changePasswordInput,
                        onValueChange = { changePasswordInput = it },
                        label = { Text("New Password", fontSize = 11.sp, color = TextMuted) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedContainerColor = DarkSurface, unfocusedContainerColor = DarkSurface)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.changePassword(targetUser, changePasswordInput)
                        showChangePassDialog = null
                        changePasswordInput = ""
                        Toast.makeText(context, "Password updated for $targetUser", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Save Password", color = TextOnAccent, fontWeight = FontWeight.Bold)
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
            title = { Text("Delete User '$targetUser'", color = StatusRed, fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Text("Are you sure you want to permanently drop user '$targetUser' and revoke all permissions?", color = TextSecondary, fontSize = 13.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteUser(targetUser)
                        showDeleteUserDialog = null
                        Toast.makeText(context, "User '$targetUser' dropped", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete User", color = TextOnAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteUserDialog = null }) { Text("Cancel", color = TextMuted) }
            }
        )
    }

    showGrantDialog?.let { targetUser ->
        var selPrivileges by remember { mutableStateOf(listOf("SELECT", "INSERT")) }
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
private fun ExplorerTab(
    databases: List<String>,
    selectedDb: String,
    tables: List<String>,
    selectedTable: String?,
    tableSchema: List<com.localhost.core.model.ColumnInfo>,
    tableData: com.localhost.core.model.TableData?,
    onSelectDb: (String) -> Unit,
    onSelectTable: (String, String) -> Unit,
    onRefresh: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Databases:", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
            databases.forEach { db ->
                val isSel = db == selectedDb
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSel) PrimaryAccent else DarkSurfaceElevated,
                    border = BorderStroke(1.dp, if (isSel) PrimaryAccent else DarkBorder),
                    modifier = Modifier.clickable { onSelectDb(db) }
                ) {
                    Text(
                        text = db,
                        fontSize = 11.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSel) TextOnAccent else TextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Tables:", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
            if (tables.isEmpty()) {
                Text("No tables found", fontSize = 11.sp, color = TextMuted)
            } else {
                tables.forEach { tbl ->
                    val isSel = tbl == selectedTable
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSel) PrimaryAccentContainer else DarkSurface,
                        border = BorderStroke(1.dp, if (isSel) PrimaryAccent else DarkBorder),
                        modifier = Modifier.clickable { onSelectTable(selectedDb, tbl) }
                    ) {
                        Text(
                            text = tbl,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) PrimaryAccent else TextPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
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
                IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = PrimaryAccent, modifier = Modifier.size(16.dp))
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(DarkSurfaceElevated, RoundedCornerShape(8.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                    .horizontalScroll(rememberScrollState())
            ) {
                LazyColumn(modifier = Modifier.padding(6.dp)) {
                    item {
                        Row(modifier = Modifier.background(PrimaryAccentContainer, RoundedCornerShape(4.dp)).padding(6.dp)) {
                            tableData.columns.forEach { col ->
                                Text(
                                    text = col,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryAccent,
                                    modifier = Modifier.width(130.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    items(tableData.rows) { row ->
                        Row(modifier = Modifier.padding(vertical = 4.dp, horizontal = 6.dp)) {
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
        } else {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Select a table to view schema & records", color = TextMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun VisualizeTab(
    databases: List<String>,
    selectedDb: String,
    allSchemas: List<TableSchemaEntry>,
    onSelectDb: (String) -> Unit,
    onRefresh: () -> Unit
) {
    var showForeignKeys by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Database:", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
            databases.forEach { db ->
                val isSel = db == selectedDb
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSel) PrimaryAccent else DarkSurfaceElevated,
                    border = BorderStroke(1.dp, if (isSel) PrimaryAccent else DarkBorder),
                    modifier = Modifier.clickable { onSelectDb(db) }
                ) {
                    Text(
                        text = db,
                        fontSize = 11.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSel) TextOnAccent else TextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("FK Lines", fontSize = 10.sp, color = TextMuted)
                Switch(
                    checked = showForeignKeys,
                    onCheckedChange = { showForeignKeys = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = StatusOrange, checkedTrackColor = StatusOrange.copy(alpha = 0.3f))
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(16.dp, 3.dp).background(PrimaryAccent.copy(alpha = 0.6f)))
                Text("DB Structure Line", fontSize = 10.sp, color = TextMuted)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(16.dp, 3.dp).background(StatusOrange))
                Text("Foreign Key Relation", fontSize = 10.sp, color = StatusOrange, fontWeight = FontWeight.Bold)
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(DarkSurfaceElevated, RoundedCornerShape(10.dp))
                .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PrimaryAccentContainer,
                    border = BorderStroke(1.5.dp, PrimaryAccent),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(20.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(selectedDb, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryAccent)
                            Text("${allSchemas.size} Tables Total", fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                }

                if (allSchemas.isEmpty()) {
                    Box(modifier = Modifier.height(150.dp), contentAlignment = Alignment.Center) {
                        Text("No tables in '$selectedDb' to visualize", color = TextMuted, fontSize = 12.sp)
                    }
                } else {
                    val chunks = allSchemas.chunked(2)
                    chunks.forEach { rowTables ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowTables.forEach { tblEntry ->
                                TableEntityCard(
                                    entry = tblEntry,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowTables.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TableEntityCard(
    entry: TableSchemaEntry,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.TableChart, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(14.dp))
                    Text(entry.tableName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = DarkSurfaceElevated
                ) {
                    Text("${entry.rowCount} rows", fontSize = 9.sp, color = TextMuted, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                }
            }

            HorizontalDivider(color = DarkBorderSubtle)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                entry.columns.forEach { col ->
                    val isFk = col.name.endsWith("_id", ignoreCase = true) && !col.isPrimaryKey
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (col.isPrimaryKey) {
                                Text("PK", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = StatusOrange)
                            } else if (isFk) {
                                Text("FK", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = StatusGreen)
                            }
                            Text(
                                text = col.name,
                                fontSize = 10.sp,
                                fontWeight = if (col.isPrimaryKey) FontWeight.Bold else FontWeight.Normal,
                                color = if (col.isPrimaryKey) PrimaryAccent else if (isFk) StatusGreen else TextPrimary
                            )
                        }
                        Text(
                            text = col.type.take(14),
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted
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
    isExecuting: Boolean
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val quickQueries = listOf(
                "SELECT * FROM users LIMIT 20;",
                "SELECT * FROM accounts;",
                "SELECT * FROM metrics;",
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
                enabled = !isExecuting,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = TextOnAccent, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isExecuting) "Running..." else "Run Query", color = TextOnAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                                    Text(c, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = PrimaryAccent, modifier = Modifier.width(120.dp))
                                }
                            }
                        }
                        items(res.rows) { r ->
                            Row(modifier = Modifier.padding(vertical = 3.dp, horizontal = 4.dp)) {
                                r.forEach { cell ->
                                    Text(cell, fontSize = 10.sp, color = TextSecondary, modifier = Modifier.width(120.dp))
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
                Icon(Icons.Default.Add, contentDescription = null, tint = TextOnAccent, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add User", fontSize = 11.sp, color = TextOnAccent, fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(users) { u ->
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
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
                                    Text(u.host, fontSize = 10.sp, color = TextMuted, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                }
                            }
                            Text(
                                text = "Privileges: " + u.privileges.joinToString(", "),
                                fontSize = 10.sp,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { onChangePassword(u.username) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Key, contentDescription = "Change Password", tint = PrimaryAccent, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { onGrant(u.username) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Security, contentDescription = "Edit Permissions", tint = StatusOrange, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { onDeleteUser(u.username) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete User", tint = StatusRed, modifier = Modifier.size(16.dp))
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
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    databases.forEach { db ->
                        val isSel = db == selectedDb
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) PrimaryAccentContainer else DarkSurfaceElevated,
                            border = BorderStroke(1.dp, if (isSel) PrimaryAccent else DarkBorder),
                            modifier = Modifier.clickable { onSelectDb(db) }
                        ) {
                            Text(
                                text = db,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) PrimaryAccent else TextSecondary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
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
