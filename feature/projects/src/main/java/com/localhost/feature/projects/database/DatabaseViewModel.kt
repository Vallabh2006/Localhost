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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class TableSchemaEntry(
    val tableName: String,
    val rowCount: Int,
    val columns: List<ColumnInfo>
)

@HiltViewModel
class DatabaseViewModel @Inject constructor(
    private val databaseClient: DatabaseClient
) : ViewModel() {

    private val _selectedEngine = MutableStateFlow(DatabaseEngine.MYSQL)
    val selectedEngine: StateFlow<DatabaseEngine> = _selectedEngine.asStateFlow()

    private val _mysqlConfig = MutableStateFlow(DatabaseConfig(engine = DatabaseEngine.MYSQL, port = 3306, user = "root", password = ""))
    val mysqlConfig: StateFlow<DatabaseConfig> = _mysqlConfig.asStateFlow()

    private val _postgresConfig = MutableStateFlow(DatabaseConfig(engine = DatabaseEngine.POSTGRESQL, port = 5432, user = "root", password = ""))
    val postgresConfig: StateFlow<DatabaseConfig> = _postgresConfig.asStateFlow()

    private val _isMysqlConnected = MutableStateFlow(true)
    val isMysqlConnected: StateFlow<Boolean> = _isMysqlConnected.asStateFlow()

    private val _isPostgresConnected = MutableStateFlow(true)
    val isPostgresConnected: StateFlow<Boolean> = _isPostgresConnected.asStateFlow()

    private val _mysqlMessage = MutableStateFlow<String?>("MySQL Service ready on 127.0.0.1:3306")
    val mysqlMessage: StateFlow<String?> = _mysqlMessage.asStateFlow()

    private val _postgresMessage = MutableStateFlow<String?>("PostgreSQL Service ready on 127.0.0.1:5432")
    val postgresMessage: StateFlow<String?> = _postgresMessage.asStateFlow()

    private val _testConnectionResult = MutableStateFlow<String?>(null)
    val testConnectionResult: StateFlow<String?> = _testConnectionResult.asStateFlow()

    private val _databases = MutableStateFlow<List<String>>(emptyList())
    val databases: StateFlow<List<String>> = _databases.asStateFlow()

    private val _selectedDatabase = MutableStateFlow("localhost_app")
    val selectedDatabase: StateFlow<String> = _selectedDatabase.asStateFlow()

    private val _tables = MutableStateFlow<List<String>>(emptyList())
    val tables: StateFlow<List<String>> = _tables.asStateFlow()

    private val _selectedTable = MutableStateFlow<String?>("users")
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

    init {
        loadDatabases()
        loadUsers()
    }

    val activeConfig: DatabaseConfig
        get() = if (_selectedEngine.value == DatabaseEngine.MYSQL) _mysqlConfig.value else _postgresConfig.value

    val isConnected: Boolean
        get() = if (_selectedEngine.value == DatabaseEngine.MYSQL) _isMysqlConnected.value else _isPostgresConnected.value

    fun selectService(engine: DatabaseEngine) {
        _selectedEngine.value = engine
        loadDatabases()
        loadUsers()
    }

    fun updateConfig(host: String, port: Int, user: String, pass: String, db: String) {
        if (_selectedEngine.value == DatabaseEngine.MYSQL) {
            _mysqlConfig.value = _mysqlConfig.value.copy(
                host = host.trim(),
                port = port,
                user = user.trim(),
                password = pass,
                database = db.trim()
            )
        } else {
            _postgresConfig.value = _postgresConfig.value.copy(
                host = host.trim(),
                port = port,
                user = user.trim(),
                password = pass,
                database = db.trim()
            )
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _isExecuting.value = true
            val cfg = activeConfig
            val res = databaseClient.testConnection(cfg)
            _testConnectionResult.value = if (res.isSuccess) {
                "Success: " + res.getOrNull()
            } else {
                "Failed: " + (res.exceptionOrNull()?.message ?: "Unknown error")
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
            val eng = _selectedEngine.value
            val cfg = activeConfig
            val res = databaseClient.testConnection(cfg)
            if (res.isSuccess) {
                if (eng == DatabaseEngine.MYSQL) {
                    _isMysqlConnected.value = true
                    _mysqlMessage.value = res.getOrNull()
                } else {
                    _isPostgresConnected.value = true
                    _postgresMessage.value = res.getOrNull()
                }
                loadDatabases()
            } else {
                val err = res.exceptionOrNull()?.message ?: "Connection refused"
                if (eng == DatabaseEngine.MYSQL) {
                    _isMysqlConnected.value = false
                    _mysqlMessage.value = err
                } else {
                    _isPostgresConnected.value = false
                    _postgresMessage.value = err
                }
            }
            _isExecuting.value = false
        }
    }

    fun disconnect() {
        if (_selectedEngine.value == DatabaseEngine.MYSQL) {
            _isMysqlConnected.value = false
            _mysqlMessage.value = "Disconnected from MySQL Service"
        } else {
            _isPostgresConnected.value = false
            _postgresMessage.value = "Disconnected from PostgreSQL Service"
        }
    }

    fun loadDatabases() {
        viewModelScope.launch {
            val list = databaseClient.listDatabases(_selectedEngine.value)
            _databases.value = list
            if (list.isNotEmpty()) {
                val db = if (list.contains(_selectedDatabase.value)) _selectedDatabase.value else list.first()
                _selectedDatabase.value = db
                loadTables(db)
                loadAllSchemas(db)
            }
        }
    }

    fun selectDatabase(db: String) {
        _selectedDatabase.value = db
        loadTables(db)
        loadAllSchemas(db)
    }

    fun loadTables(database: String) {
        viewModelScope.launch {
            val tbls = databaseClient.listTables(_selectedEngine.value, database)
            _tables.value = tbls
            if (tbls.isNotEmpty()) {
                val t = if (tbls.contains(_selectedTable.value)) _selectedTable.value ?: tbls.first() else tbls.first()
                _selectedTable.value = t
                selectTable(database, t)
            } else {
                _selectedTable.value = null
                _tableSchema.value = emptyList()
                _tableData.value = null
            }
        }
    }

    fun selectTable(database: String, table: String) {
        _selectedTable.value = table
        viewModelScope.launch {
            _tableSchema.value = databaseClient.getTableSchema(_selectedEngine.value, database, table)
            _tableData.value = databaseClient.getTableData(_selectedEngine.value, database, table)
        }
    }

    fun loadAllSchemas(database: String) {
        viewModelScope.launch {
            val eng = _selectedEngine.value
            val tbls = databaseClient.listTables(eng, database)
            val entries = mutableListOf<TableSchemaEntry>()
            for (t in tbls) {
                val cols = databaseClient.getTableSchema(eng, database, t)
                val data = databaseClient.getTableData(eng, database, t)
                entries.add(TableSchemaEntry(t, data.rows.size, cols))
            }
            _allTableSchemas.value = entries
        }
    }

    fun executeQuery(sql: String) {
        if (sql.isBlank()) return
        viewModelScope.launch {
            _isExecuting.value = true
            val res = databaseClient.executeQuery(_selectedEngine.value, _selectedDatabase.value, sql)
            _queryResult.value = res
            _isExecuting.value = false
            loadDatabases()
            _selectedTable.value?.let { selectTable(_selectedDatabase.value, it) }
            loadUsers()
        }
    }

    fun loadUsers() {
        viewModelScope.launch {
            _users.value = databaseClient.listUsers(_selectedEngine.value)
        }
    }

    fun createUser(username: String, host: String = "%", pass: String) {
        if (username.isBlank()) return
        viewModelScope.launch {
            databaseClient.createUser(_selectedEngine.value, username, host, pass)
            loadUsers()
        }
    }

    fun changePassword(username: String, newPass: String) {
        viewModelScope.launch {
            databaseClient.changeUserPassword(_selectedEngine.value, username, newPass)
            loadUsers()
        }
    }

    fun deleteUser(username: String) {
        viewModelScope.launch {
            databaseClient.deleteUser(_selectedEngine.value, username)
            loadUsers()
        }
    }

    fun grantPrivileges(username: String, host: String, db: String, perms: List<String>) {
        viewModelScope.launch {
            databaseClient.grantPermissions(_selectedEngine.value, username, host, db, perms)
            loadUsers()
        }
    }

    fun revokePrivileges(username: String, host: String, db: String) {
        viewModelScope.launch {
            databaseClient.revokePermissions(_selectedEngine.value, username, host, db)
            loadUsers()
        }
    }

    fun generateSqlExport(database: String, includeData: Boolean): String {
        val eng = _selectedEngine.value
        val sb = StringBuilder()
        sb.append("-- --------------------------------------------------------\n")
        sb.append("-- Localhost SQL Dump\n")
        sb.append("-- Service Engine: ${eng.displayName}\n")
        sb.append("-- Target Database: $database\n")
        sb.append("-- Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}\n")
        sb.append("-- --------------------------------------------------------\n\n")

        val quote = if (eng == DatabaseEngine.MYSQL) "`" else "\""
        if (eng == DatabaseEngine.MYSQL) {
            sb.append("CREATE DATABASE IF NOT EXISTS $quote$database$quote;\n")
            sb.append("USE $quote$database$quote;\n\n")
        } else {
            sb.append("-- CREATE DATABASE $quote$database$quote;\n")
            sb.append("\\c $database;\n\n")
        }

        val entries = _allTableSchemas.value
        for (entry in entries) {
            val tbl = entry.tableName
            sb.append("-- Table structure for table $quote$tbl$quote\n")
            sb.append("DROP TABLE IF EXISTS $quote$tbl$quote;\n")
            sb.append("CREATE TABLE $quote$tbl$quote (\n")
            val colDefs = entry.columns.map { c ->
                val pkStr = if (c.isPrimaryKey) " PRIMARY KEY" else ""
                val nullStr = if (!c.isNullable) " NOT NULL" else ""
                "    $quote${c.name}$quote ${c.type}$pkStr$nullStr"
            }
            sb.append(colDefs.joinToString(",\n"))
            sb.append("\n);\n\n")

            if (includeData) {
                viewModelScope.launch {
                    val data = databaseClient.getTableData(eng, database, tbl)
                    if (data.rows.isNotEmpty()) {
                        sb.append("-- Dumping data for table $quote$tbl$quote\n")
                        for (row in data.rows) {
                            val vals = row.map { v -> if (v == "NULL") "NULL" else "'${v.replace("'", "''")}'" }
                            sb.append("INSERT INTO $quote$tbl$quote (${data.columns.joinToString(", ") { "$quote$it$quote" }}) VALUES (${vals.joinToString(", ")});\n")
                        }
                        sb.append("\n")
                    }
                }
            }
        }
        return sb.toString()
    }

    fun importSqlFile(context: Context, uri: Uri) {
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
                loadDatabases()
                loadAllSchemas(_selectedDatabase.value)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "SQL Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
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
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
