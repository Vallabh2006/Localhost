package com.localhost.feature.projects.database

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localhost.core.data.database.DatabaseClient
import com.localhost.core.model.ColumnInfo
import com.localhost.core.model.DatabaseConfig
import com.localhost.core.model.DatabaseEngine
import com.localhost.core.model.DatabaseUser
import com.localhost.core.model.QueryResult
import com.localhost.core.model.TableData
import com.localhost.runtime.manager.MariaDBManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class TableSchemaEntry(
    val tableName: String,
    val rowCount: Int,
    val columns: List<ColumnInfo>
)

@HiltViewModel
class DatabaseViewModel @Inject constructor(
    private val databaseClient: DatabaseClient,
    private val mariaDBManager: MariaDBManager
) : ViewModel() {

    companion object {
        private val _selectedEngine = MutableStateFlow(DatabaseEngine.MYSQL)
        private val _mysqlConfig = MutableStateFlow(DatabaseConfig(engine = DatabaseEngine.MYSQL, port = 3306, user = "root", password = "", database = ""))
        private val _isMysqlConnected = MutableStateFlow(false)
        private val _mysqlMessage = MutableStateFlow<String?>("MariaDB Service Stopped")
        private val _databaseLogs = MutableStateFlow<List<String>>(listOf("[SYSTEM] Database Studio initialized."))
    }

    val selectedEngine: StateFlow<DatabaseEngine> = _selectedEngine.asStateFlow()
    val mysqlConfig: StateFlow<DatabaseConfig> = _mysqlConfig.asStateFlow()
    val isMysqlConnected: StateFlow<Boolean> = _isMysqlConnected.asStateFlow()
    val mysqlMessage: StateFlow<String?> = _mysqlMessage.asStateFlow()
    private val _testConnectionResult = MutableStateFlow<String?>(null)
    val testConnectionResult: StateFlow<String?> = _testConnectionResult.asStateFlow()
    val databaseLogs: StateFlow<List<String>> = _databaseLogs.asStateFlow()

    private val _databases = MutableStateFlow<List<String>>(emptyList())
    val databases: StateFlow<List<String>> = _databases.asStateFlow()

    private val _selectedDatabase = MutableStateFlow("cats_db")
    val selectedDatabase: StateFlow<String> = _selectedDatabase.asStateFlow()

    private val _tables = MutableStateFlow<List<String>>(emptyList())
    val tables: StateFlow<List<String>> = _tables.asStateFlow()

    private val _selectedTable = MutableStateFlow<String?>("cats")
    val selectedTable: StateFlow<String?> = _selectedTable.asStateFlow()

    private val _tableSchema = MutableStateFlow<List<ColumnInfo>>(emptyList())
    val tableSchema: StateFlow<List<ColumnInfo>> = _tableSchema.asStateFlow()

    private val _tableData = MutableStateFlow<TableData?>(null)
    val tableData: StateFlow<TableData?> = _tableData.asStateFlow()

    private val _allTableSchemas = MutableStateFlow<List<TableSchemaEntry>>(emptyList())
    val allTableSchemas: StateFlow<List<TableSchemaEntry>> = _allTableSchemas.asStateFlow()

    private val _queryResult = MutableStateFlow<QueryResult?>(null)
    val queryResult: StateFlow<QueryResult?> = _queryResult.asStateFlow()

    private val _users = MutableStateFlow<List<DatabaseUser>>(emptyList())
    val users: StateFlow<List<DatabaseUser>> = _users.asStateFlow()

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()

    val isMariaDBDownloading: StateFlow<Boolean> = mariaDBManager.isDownloading
    val mariaDBProgress: StateFlow<Float> = mariaDBManager.downloadProgress
    val mariaDBStatusMessage: StateFlow<String?> = mariaDBManager.statusMessage

    init {
        checkServiceStatus()
    }

    fun isMariaDBInstalled(): Boolean = mariaDBManager.isInstalled()

    fun checkServiceStatus() {
        viewModelScope.launch {
            val isPortOpen = mariaDBManager.checkIsPortOpen()
            _isMysqlConnected.value = isPortOpen
            if (isPortOpen) {
                _mysqlMessage.value = "MariaDB Server is running on 127.0.0.1:3306"
                loadDatabases()
                loadUsers()
            } else {
                _mysqlMessage.value = "MariaDB Service Stopped"
            }
        }
    }

    val activeConfig: DatabaseConfig
        get() = _mysqlConfig.value

    val isConnected: Boolean
        get() = _isMysqlConnected.value

    fun addLog(msg: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        _databaseLogs.value = (_databaseLogs.value + "[$timestamp] $msg").takeLast(500)
    }

    fun clearLogs() {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        _databaseLogs.value = listOf("[$timestamp] [SYSTEM] Logs cleared")
    }

    fun updateConfig(host: String, port: Int, user: String, pass: String, db: String) {
        _mysqlConfig.value = _mysqlConfig.value.copy(
            host = host.trim(),
            port = port,
            user = user.trim(),
            password = pass,
            database = db.trim()
        )
        addLog("[CONFIG] Updated connection parameters for MariaDB")
    }

    fun installMariaDB() {
        viewModelScope.launch {
            _isExecuting.value = true
            addLog("[INSTALL] Starting MariaDB runtime installation...")
            val res = mariaDBManager.installMariaDB()
            if (res.isSuccess) {
                addLog("[INSTALL] MariaDB installed successfully")
                connect()
            } else {
                addLog("[ERROR] MariaDB install failed: ${res.exceptionOrNull()?.message}")
            }
            _isExecuting.value = false
        }
    }

    fun testConnection() {
        if (!_isMysqlConnected.value) {
            val err = "Failed: MariaDB service is stopped. Start the service first."
            _testConnectionResult.value = err
            addLog("[WARN] Connection test blocked: $err")
            return
        }
        viewModelScope.launch {
            _isExecuting.value = true
            val cfg = activeConfig
            val res = databaseClient.testConnection(cfg)
            _testConnectionResult.value = if (res.isSuccess) {
                val msg = "Success: " + res.getOrNull()
                addLog("[TEST] $msg")
                msg
            } else {
                val msg = "Failed: " + (res.exceptionOrNull()?.message ?: "Unknown error")
                addLog("[ERROR] $msg")
                msg
            }
            _isExecuting.value = false
        }
    }

    fun clearTestResult() {
        _testConnectionResult.value = null
    }

    fun connect() {
        viewModelScope.launch {
            _isExecuting.value = true
            addLog("[SERVICE] Starting MariaDB server daemon...")
            val startRes = mariaDBManager.startService()
            if (startRes.isSuccess) {
                _isMysqlConnected.value = true
                _mysqlMessage.value = "MariaDB running on 127.0.0.1:3306"
                addLog("[SERVICE] MariaDB service active on port 3306")
                loadDatabases()
                loadUsers()
            } else {
                _isMysqlConnected.value = false
                val err = startRes.exceptionOrNull()?.message ?: "Failed to start service"
                _mysqlMessage.value = err
                addLog("[ERROR] MariaDB start failed: $err")
            }
            _isExecuting.value = false
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            _isExecuting.value = true
            addLog("[SERVICE] Stopping MariaDB server daemon...")
            mariaDBManager.stopService()
            _isMysqlConnected.value = false
            _mysqlMessage.value = "MariaDB Service stopped"
            addLog("[SERVICE] MariaDB service stopped")
            _isExecuting.value = false
        }
    }

    fun restartService() {
        viewModelScope.launch {
            addLog("[SERVICE] Restarting MariaDB service...")
            disconnect()
            kotlinx.coroutines.delay(500)
            connect()
        }
    }

    fun loadDatabases() {
        viewModelScope.launch {
            val list = databaseClient.listDatabases(_selectedEngine.value)
            _databases.value = list
            if (list.isNotEmpty()) {
                if (_selectedDatabase.value !in list) {
                    val preferCat = if ("cats_db" in list) "cats_db" else list.first()
                    _selectedDatabase.value = preferCat
                }
                loadTables(_selectedDatabase.value)
                loadAllSchemas(_selectedDatabase.value)
            } else {
                _tables.value = emptyList()
                _allTableSchemas.value = emptyList()
            }
        }
    }

    fun selectDatabase(name: String) {
        _selectedDatabase.value = name
        _selectedTable.value = null
        addLog("[DATABASE] Switched to database '$name'")
        loadTables(name)
        loadAllSchemas(name)
    }

    fun createDatabase(name: String) {
        if (!_isMysqlConnected.value) {
            addLog("[ERROR] Cannot create database: MariaDB service is offline")
            return
        }
        viewModelScope.launch {
            val trimmed = name.trim().lowercase().replace(" ", "_")
            if (trimmed.isEmpty()) return@launch
            val res = databaseClient.executeQuery(_selectedEngine.value, "", "CREATE DATABASE IF NOT EXISTS `$trimmed`;")
            if (res.isSuccess) {
                addLog("[DATABASE] Created database '$trimmed'")
                loadDatabases()
                selectDatabase(trimmed)
            } else {
                addLog("[ERROR] Failed to create database: ${res.errorMessage}")
            }
        }
    }

    fun dropDatabase(name: String) {
        if (!_isMysqlConnected.value) {
            addLog("[ERROR] Cannot drop database: MariaDB service is offline")
            return
        }
        viewModelScope.launch {
            val res = databaseClient.executeQuery(_selectedEngine.value, "", "DROP DATABASE IF EXISTS `$name`;")
            if (res.isSuccess) {
                addLog("[DATABASE] Dropped database '$name'")
                loadDatabases()
            } else {
                addLog("[ERROR] Failed to drop database: ${res.errorMessage}")
            }
        }
    }

    fun loadTables(database: String = _selectedDatabase.value) {
        viewModelScope.launch {
            val list = databaseClient.listTables(_selectedEngine.value, database)
            _tables.value = list
            if (list.isNotEmpty()) {
                val current = _selectedTable.value
                val nextTable = if (current != null && current in list) current else list.first()
                _selectedTable.value = nextTable
                loadTableSchema(database, nextTable)
                loadTableData(database, nextTable)
            } else {
                _selectedTable.value = null
                _tableSchema.value = emptyList()
                _tableData.value = null
            }
        }
    }

    fun loadAllSchemas(database: String = _selectedDatabase.value) {
        viewModelScope.launch {
            val tables = databaseClient.listTables(_selectedEngine.value, database)
            val entries = mutableListOf<TableSchemaEntry>()
            for (tbl in tables) {
                val schema = databaseClient.getTableSchema(_selectedEngine.value, database, tbl)
                val data = databaseClient.getTableData(_selectedEngine.value, database, tbl)
                entries.add(TableSchemaEntry(tableName = tbl, rowCount = data.rows.size, columns = schema))
            }
            _allTableSchemas.value = entries
        }
    }

    fun selectTable(database: String, tableName: String) {
        _selectedDatabase.value = database
        selectTable(tableName)
    }

    fun selectTable(tableName: String) {
        _selectedTable.value = tableName
        addLog("[TABLE] Selected table '$tableName'")
        loadTableSchema(_selectedDatabase.value, tableName)
        loadTableData(_selectedDatabase.value, tableName)
    }

    fun loadTableSchema(database: String, table: String) {
        viewModelScope.launch {
            _tableSchema.value = databaseClient.getTableSchema(_selectedEngine.value, database, table)
        }
    }

    fun loadTableData(database: String, table: String) {
        viewModelScope.launch {
            _tableData.value = databaseClient.getTableData(_selectedEngine.value, database, table)
        }
    }

    fun executeQuery(sql: String) {
        if (!_isMysqlConnected.value) {
            val err = "Cannot execute query: MariaDB service is offline. Start the service first."
            _queryResult.value = QueryResult(isSuccess = false, errorMessage = err)
            addLog("[ERROR] $err")
            return
        }
        viewModelScope.launch {
            _isExecuting.value = true
            val res = databaseClient.executeQuery(_selectedEngine.value, _selectedDatabase.value, sql)
            _queryResult.value = res
            val preview = sql.lines().firstOrNull() ?: sql
            if (res.isSuccess) {
                addLog("[QUERY] OK (${res.executionTimeMs}ms) - $preview")
            } else {
                addLog("[ERROR] (${res.executionTimeMs}ms) ${res.errorMessage} - $preview")
            }
            _isExecuting.value = false
            loadTables(_selectedDatabase.value)
            loadAllSchemas(_selectedDatabase.value)
        }
    }

    fun loadUsers() {
        viewModelScope.launch {
            _users.value = databaseClient.listUsers(_selectedEngine.value)
        }
    }

    fun createUser(username: String, host: String, pass: String) {
        if (!_isMysqlConnected.value) {
            addLog("[ERROR] Cannot create user: MariaDB service is offline")
            return
        }
        viewModelScope.launch {
            databaseClient.createUser(_selectedEngine.value, username, host, pass)
            _users.value = databaseClient.listUsers(_selectedEngine.value)
            addLog("[USER] Created user '$username'@'$host'")
        }
    }

    fun changePassword(username: String, newPass: String) {
        if (!_isMysqlConnected.value) {
            addLog("[ERROR] Cannot change password: MariaDB service is offline")
            return
        }
        viewModelScope.launch {
            databaseClient.changeUserPassword(_selectedEngine.value, username, newPass)
            _users.value = databaseClient.listUsers(_selectedEngine.value)
            addLog("[USER] Changed password for '$username'")
        }
    }

    fun deleteUser(username: String) {
        if (!_isMysqlConnected.value) {
            addLog("[ERROR] Cannot delete user: MariaDB service is offline")
            return
        }
        viewModelScope.launch {
            databaseClient.deleteUser(_selectedEngine.value, username)
            _users.value = databaseClient.listUsers(_selectedEngine.value)
            addLog("[USER] Deleted user '$username'")
        }
    }

    fun grantPrivileges(username: String, host: String, db: String, perms: List<String>) {
        viewModelScope.launch {
            databaseClient.grantPermissions(_selectedEngine.value, username, host, db, perms)
            _users.value = databaseClient.listUsers(_selectedEngine.value)
            addLog("[USER] Granted privileges ${perms.joinToString(", ")} to '$username'@'$host'")
        }
    }

    fun revokePrivileges(username: String, host: String, db: String) {
        viewModelScope.launch {
            databaseClient.revokePermissions(_selectedEngine.value, username, host, db)
            _users.value = databaseClient.listUsers(_selectedEngine.value)
            addLog("[USER] Revoked privileges from '$username'@'$host'")
        }
    }

    fun generateSqlExport(database: String, includeData: Boolean): String {
        val sb = StringBuilder()
        sb.append("-- --------------------------------------------------------\n")
        sb.append("-- Localhost MariaDB SQL Dump\n")
        sb.append("-- Target Database: $database\n")
        sb.append("-- Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}\n")
        sb.append("-- --------------------------------------------------------\n\n")

        sb.append("CREATE DATABASE IF NOT EXISTS `$database`;\n")
        sb.append("USE `$database`;\n\n")

        val entries = _allTableSchemas.value
        for (entry in entries) {
            val tbl = entry.tableName
            sb.append("-- Table structure for table `$tbl`\n")
            sb.append("DROP TABLE IF EXISTS `$tbl`;\n")
            sb.append("CREATE TABLE `$tbl` (\n")
            val colDefs = entry.columns.map { c ->
                val pkStr = if (c.isPrimaryKey) " PRIMARY KEY" else ""
                val nullStr = if (!c.isNullable) " NOT NULL" else ""
                "    `${c.name}` ${c.type}$pkStr$nullStr"
            }
            sb.append(colDefs.joinToString(",\n"))
            sb.append("\n);\n\n")

            if (includeData) {
                viewModelScope.launch {
                    val data = databaseClient.getTableData(DatabaseEngine.MYSQL, database, tbl)
                    if (data.rows.isNotEmpty()) {
                        sb.append("-- Dumping data for table `$tbl`\n")
                        for (row in data.rows) {
                            val vals = row.map { v -> if (v == "NULL") "NULL" else "'${v.replace("'", "''")}'" }
                            sb.append("INSERT INTO `$tbl` (${data.columns.joinToString(", ") { "`$it`" }}) VALUES (${vals.joinToString(", ")});\n")
                        }
                        sb.append("\n")
                    }
                }
            }
        }
        return sb.toString()
    }

    fun importSqlFile(context: Context, uri: Uri) {
        if (!_isMysqlConnected.value) {
            Toast.makeText(context, "MariaDB service is stopped. Start service first.", Toast.LENGTH_SHORT).show()
            addLog("[ERROR] Cannot import SQL: MariaDB service is offline")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isExecuting.value = true
                var executedCount = 0
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val sqlContent = inputStream.bufferedReader().use { it.readText() }
                    val statements = sqlContent.split(";")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() && !it.startsWith("--") && !it.startsWith("/*") }

                    for (stmt in statements) {
                        databaseClient.executeQuery(_selectedEngine.value, _selectedDatabase.value, stmt)
                        executedCount++
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Imported SQL: $executedCount statements executed successfully", Toast.LENGTH_LONG).show()
                }
                addLog("[IMPORT] Imported SQL: $executedCount statements executed")
                loadDatabases()
                loadAllSchemas(_selectedDatabase.value)
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "SQL Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
                addLog("[ERROR] SQL Import failed: ${e.message}")
            } finally {
                _isExecuting.value = false
            }
        }
    }

    fun exportSqlToFile(context: Context, database: String, sqlContent: String, share: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fileName = "${database}_dump_${System.currentTimeMillis()}.sql"
                val dir = File(context.cacheDir, "sql_exports").apply { mkdirs() }
                val file = File(dir, fileName)
                file.writeText(sqlContent)

                if (share) {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/sql"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    withContext(Dispatchers.Main) {
                        context.startActivity(Intent.createChooser(intent, "Share SQL Dump ($fileName)"))
                    }
                } else {
                    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                    val dest = File(downloadsDir, fileName)
                    file.copyTo(dest, overwrite = true)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Saved to Downloads: ${dest.name}", Toast.LENGTH_LONG).show()
                    }
                }
                addLog("[EXPORT] Exported SQL dump for database '$database'")
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                addLog("[ERROR] Export error: ${e.message}")
            }
        }
    }
}
