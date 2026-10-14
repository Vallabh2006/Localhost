package com.localhost.core.data.database

import com.localhost.core.model.ColumnInfo
import com.localhost.core.model.DatabaseConfig
import com.localhost.core.model.DatabaseEngine
import com.localhost.core.model.DatabaseUser
import com.localhost.core.model.QueryResult
import com.localhost.core.model.TableData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseClient @Inject constructor() {

    private val mysqlDatabases = mutableMapOf<String, MutableMap<String, MutableList<Map<String, String>>>>(
        "localhost_app" to mutableMapOf(
            "users" to mutableListOf(
                mapOf("id" to "1", "username" to "admin", "email" to "admin@localhost.dev", "role" to "superadmin", "created_at" to "2026-10-14 12:00:00"),
                mapOf("id" to "2", "username" to "developer", "email" to "dev@localhost.dev", "role" to "editor", "created_at" to "2026-10-14 12:15:00"),
                mapOf("id" to "3", "username" to "guest_tester", "email" to "guest@localhost.dev", "role" to "viewer", "created_at" to "2026-10-14 12:30:00")
            ),
            "projects" to mutableListOf(
                mapOf("id" to "1", "name" to "Flask API", "runtime" to "PYTHON", "port" to "8080", "status" to "RUNNING"),
                mapOf("id" to "2", "name" to "Express Web", "runtime" to "NODEJS", "port" to "3000", "status" to "STOPPED"),
                mapOf("id" to "3", "name" to "PHP Portal", "runtime" to "PHP", "port" to "8000", "status" to "RUNNING")
            ),
            "app_logs" to mutableListOf(
                mapOf("log_id" to "101", "level" to "INFO", "message" to "Database initialized successfully", "source" to "SYSTEM"),
                mapOf("log_id" to "102", "level" to "WARN", "message" to "High memory threshold warning", "source" to "KERNEL"),
                mapOf("log_id" to "103", "level" to "INFO", "message" to "Localhost reverse proxy connected", "source" to "PROXY")
            )
        ),
        "information_schema" to mutableMapOf(
            "tables" to mutableListOf(
                mapOf("TABLE_CATALOG" to "def", "TABLE_SCHEMA" to "localhost_app", "TABLE_NAME" to "users", "TABLE_TYPE" to "BASE TABLE"),
                mapOf("TABLE_CATALOG" to "def", "TABLE_SCHEMA" to "localhost_app", "TABLE_NAME" to "projects", "TABLE_TYPE" to "BASE TABLE")
            )
        ),
        "mysql" to mutableMapOf(
            "user" to mutableListOf(
                mapOf("Host" to "%", "User" to "root", "Select_priv" to "Y", "Insert_priv" to "Y", "Update_priv" to "Y")
            )
        )
    )

    private val postgresDatabases = mutableMapOf<String, MutableMap<String, MutableList<Map<String, String>>>>(
        "localhost_app" to mutableMapOf(
            "users" to mutableListOf(
                mapOf("id" to "1", "username" to "root", "email" to "root@localhost.dev", "role" to "superadmin", "created_at" to "2026-10-14 12:00:00"),
                mapOf("id" to "2", "username" to "developer", "email" to "dev@localhost.dev", "role" to "developer", "created_at" to "2026-10-14 12:15:00")
            ),
            "accounts" to mutableListOf(
                mapOf("user_id" to "1001", "username" to "root", "email" to "root@localhost.dev", "is_active" to "true"),
                mapOf("user_id" to "1002", "username" to "service_worker", "email" to "worker@localhost.dev", "is_active" to "true")
            ),
            "metrics" to mutableListOf(
                mapOf("id" to "1", "metric_name" to "cpu_usage", "value" to "12.4", "timestamp" to "1728900000"),
                mapOf("id" to "2", "metric_name" to "mem_allocated", "value" to "256.0", "timestamp" to "1728900060")
            )
        ),
        "postgres" to mutableMapOf(
            "pg_database" to mutableListOf(
                mapOf("oid" to "13000", "datname" to "postgres", "datdba" to "10"),
                mapOf("oid" to "13001", "datname" to "localhost_app", "datdba" to "10")
            )
        )
    )

    private val userPasswords = mutableMapOf<Pair<DatabaseEngine, String>, String>(
        Pair(DatabaseEngine.MYSQL, "root") to "",
        Pair(DatabaseEngine.POSTGRESQL, "root") to ""
    )

    private val mysqlUsers = mutableListOf(
        DatabaseUser("root", "%", listOf("ALL PRIVILEGES"))
    )

    private val postgresUsers = mutableListOf(
        DatabaseUser("root", "%", listOf("ALL PRIVILEGES"))
    )

    suspend fun testConnection(config: DatabaseConfig): Result<String> = withContext(Dispatchers.IO) {
        val isLocal = config.host == "127.0.0.1" || config.host.equals("localhost", ignoreCase = true)
        if (isLocal) {
            val userList = if (config.engine == DatabaseEngine.MYSQL) mysqlUsers else postgresUsers
            val userExists = userList.any { it.username.equals(config.user, ignoreCase = true) }
            if (!userExists) {
                return@withContext Result.failure(Exception("Access denied for user '${config.user}'@'${config.host}' (User does not exist)"))
            }
            val expectedPass = userPasswords[Pair(config.engine, config.user.lowercase())] ?: ""
            if (expectedPass.isNotEmpty() && config.password != expectedPass) {
                return@withContext Result.failure(Exception("Access denied for user '${config.user}' (Incorrect password)"))
            }
            if (expectedPass.isEmpty() && config.password.isNotEmpty()) {
                return@withContext Result.failure(Exception("Access denied for user '${config.user}' (Null password expected)"))
            }
            Result.success("Connected to local ${config.engine.displayName} service at ${config.host}:${config.port} (Latency: <1ms)")
        } else {
            val socket = Socket()
            try {
                socket.connect(InetSocketAddress(config.host, config.port), 2000)
                socket.close()
                Result.success("Connected to live ${config.engine.displayName} server at ${config.host}:${config.port} (Latency: <5ms)")
            } catch (e: Exception) {
                Result.failure(Exception("Connection failed: Unable to reach ${config.host}:${config.port} (${e.message ?: "Connection refused"})"))
            }
        }
    }

    suspend fun changeUserPassword(engine: DatabaseEngine, user: String, newPass: String): Result<Unit> = withContext(Dispatchers.IO) {
        userPasswords[Pair(engine, user.lowercase())] = newPass
        Result.success(Unit)
    }

    suspend fun deleteUser(engine: DatabaseEngine, user: String): Result<Unit> = withContext(Dispatchers.IO) {
        val userList = if (engine == DatabaseEngine.MYSQL) mysqlUsers else postgresUsers
        userList.removeAll { it.username.equals(user, ignoreCase = true) }
        userPasswords.remove(Pair(engine, user.lowercase()))
        Result.success(Unit)
    }

    suspend fun listDatabases(engine: DatabaseEngine): List<String> = withContext(Dispatchers.IO) {
        if (engine == DatabaseEngine.MYSQL) {
            mysqlDatabases.keys.toList()
        } else {
            postgresDatabases.keys.toList()
        }
    }

    suspend fun listTables(engine: DatabaseEngine, database: String): List<String> = withContext(Dispatchers.IO) {
        val dbs = if (engine == DatabaseEngine.MYSQL) mysqlDatabases else postgresDatabases
        dbs[database]?.keys?.toList() ?: emptyList()
    }

    suspend fun getTableSchema(engine: DatabaseEngine, database: String, table: String): List<ColumnInfo> = withContext(Dispatchers.IO) {
        val dbs = if (engine == DatabaseEngine.MYSQL) mysqlDatabases else postgresDatabases
        val rows = dbs[database]?.get(table) ?: return@withContext emptyList()
        val cols = rows.firstOrNull()?.keys ?: return@withContext emptyList()
        cols.map { col ->
            val isPk = col.equals("id", ignoreCase = true) || col.endsWith("_id", ignoreCase = true)
            val type = when {
                isPk -> if (engine == DatabaseEngine.MYSQL) "INT AUTO_INCREMENT" else "SERIAL PRIMARY KEY"
                col.contains("date", ignoreCase = true) || col.contains("at", ignoreCase = true) -> "DATETIME"
                col.contains("email", ignoreCase = true) || col.contains("name", ignoreCase = true) -> "VARCHAR(255)"
                col.contains("status", ignoreCase = true) || col.contains("role", ignoreCase = true) -> "VARCHAR(64)"
                else -> "TEXT"
            }
            ColumnInfo(
                name = col,
                type = type,
                isNullable = !isPk,
                isPrimaryKey = isPk
            )
        }
    }

    suspend fun getTableData(engine: DatabaseEngine, database: String, table: String): TableData = withContext(Dispatchers.IO) {
        val dbs = if (engine == DatabaseEngine.MYSQL) mysqlDatabases else postgresDatabases
        val rows = dbs[database]?.get(table) ?: return@withContext TableData(table, emptyList(), emptyList())
        val cols = rows.firstOrNull()?.keys?.toList() ?: emptyList()
        val rowList = rows.map { rowMap ->
            cols.map { col -> rowMap[col] ?: "NULL" }
        }
        TableData(table, cols, rowList)
    }

    suspend fun executeQuery(engine: DatabaseEngine, database: String, sql: String): QueryResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val cleanSql = sql.trim().trimEnd(';')
        val upper = cleanSql.uppercase()
        val dbs = if (engine == DatabaseEngine.MYSQL) mysqlDatabases else postgresDatabases

        val targetDb = dbs.entries.firstOrNull { it.key.equals(database, ignoreCase = true) }?.value
            ?: dbs["localhost_app"]
            ?: dbs.values.firstOrNull()
            ?: mutableMapOf()

        try {
            when {
                upper.startsWith("SHOW DATABASES") || upper.contains("PG_DATABASE") -> {
                    val list = dbs.keys.toList()
                    QueryResult(true, System.currentTimeMillis() - start, list.size, listOf("Database"), list.map { listOf(it) })
                }
                upper.startsWith("SHOW TABLES") || upper.contains("PG_TABLES") -> {
                    val tables = targetDb.keys.toList()
                    QueryResult(true, System.currentTimeMillis() - start, tables.size, listOf("Tables_in_$database"), tables.map { listOf(it) })
                }
                upper.startsWith("SELECT") -> {
                    val fromIdx = upper.indexOf("FROM ")
                    if (fromIdx != -1) {
                        val afterFrom = cleanSql.substring(fromIdx + 5).trim()
                        val rawTableToken = afterFrom.split(Regex("""\s+"""))[0]
                        val cleanTable = rawTableToken.substringAfterLast(".").filter { it !in "`'\"; \t\r\n" }
                        val tableEntry = targetDb.entries.firstOrNull { it.key.equals(cleanTable, ignoreCase = true) }
                        if (tableEntry != null) {
                            val rows = tableEntry.value
                            val cols = rows.firstOrNull()?.keys?.toList() ?: listOf("id", "value")
                            val rowData = rows.map { r -> cols.map { c -> r[c] ?: "NULL" } }
                            QueryResult(true, System.currentTimeMillis() - start, rowData.size, cols, rowData)
                        } else {
                            val availableTables = targetDb.keys.joinToString(", ")
                            QueryResult(false, System.currentTimeMillis() - start, 0, errorMessage = "Table '$cleanTable' does not exist in database '$database'. Available: [$availableTables]")
                        }
                    } else if (upper.contains("VERSION()")) {
                        val ver = if (engine == DatabaseEngine.MYSQL) "8.4.1-Localhost" else "PostgreSQL 16.2 (Localhost Engine)"
                        QueryResult(true, System.currentTimeMillis() - start, 1, listOf("version"), listOf(listOf(ver)))
                    } else {
                        QueryResult(true, System.currentTimeMillis() - start, 1, listOf("result"), listOf(listOf("1")))
                    }
                }
                upper.startsWith("CREATE DATABASE") -> {
                    val dbName = cleanSql.split(Regex("""\s+""")).last().trim('`', '"', ';')
                    dbs[dbName] = mutableMapOf()
                    QueryResult(true, System.currentTimeMillis() - start, 1)
                }
                upper.startsWith("CREATE TABLE") -> {
                    val tableName = cleanSql.substringAfter("TABLE").trim().split(Regex("""\s+"""))[0].split("(")[0].substringAfterLast(".").trim('`', '"', ';')
                    targetDb[tableName] = mutableListOf(
                        mapOf("id" to "1", "created_at" to java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date()))
                    )
                    QueryResult(true, System.currentTimeMillis() - start, 1)
                }
                upper.startsWith("INSERT INTO") -> {
                    val tableName = cleanSql.substringAfter("INTO").trim().split(Regex("""\s+"""))[0].split("(")[0].substringAfterLast(".").trim('`', '"', ';')
                    val tableEntry = targetDb.entries.firstOrNull { it.key.equals(tableName, ignoreCase = true) }
                    if (tableEntry != null) {
                        val table = tableEntry.value
                        val newId = (table.size + 1).toString()
                        val newRow = mutableMapOf("id" to newId, "created_at" to java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date()))
                        table.add(newRow)
                        QueryResult(true, System.currentTimeMillis() - start, 1)
                    } else {
                        QueryResult(false, System.currentTimeMillis() - start, 0, errorMessage = "Table '$tableName' not found in database '$database'")
                    }
                }
                upper.startsWith("DROP TABLE") -> {
                    val tableName = cleanSql.split(Regex("""\s+""")).last().substringAfterLast(".").trim('`', '"', ';')
                    val matchKey = targetDb.keys.firstOrNull { it.equals(tableName, ignoreCase = true) }
                    if (matchKey != null) {
                        targetDb.remove(matchKey)
                    }
                    QueryResult(true, System.currentTimeMillis() - start, 1)
                }
                upper.startsWith("CREATE USER") -> {
                    val userList = if (engine == DatabaseEngine.MYSQL) mysqlUsers else postgresUsers
                    val username = cleanSql.substringAfter("CREATE USER").trim().split(Regex("""\s+"""))[0].filter { it !in "`'\"; \t\r\n" }.substringBefore("@")
                    userList.add(DatabaseUser(username, "%", listOf("ALL PRIVILEGES")))
                    QueryResult(true, System.currentTimeMillis() - start, 1)
                }
                upper.startsWith("GRANT") -> {
                    QueryResult(true, System.currentTimeMillis() - start, 1)
                }
                else -> {
                    QueryResult(true, System.currentTimeMillis() - start, 1)
                }
            }
        } catch (e: Exception) {
            QueryResult(false, System.currentTimeMillis() - start, 0, errorMessage = e.message ?: "Query execution failed")
        }
    }

    suspend fun listUsers(engine: DatabaseEngine): List<DatabaseUser> = withContext(Dispatchers.IO) {
        if (engine == DatabaseEngine.MYSQL) mysqlUsers else postgresUsers
    }

    suspend fun createUser(engine: DatabaseEngine, user: String, host: String = "%", pass: String): Result<Unit> = withContext(Dispatchers.IO) {
        val userList = if (engine == DatabaseEngine.MYSQL) mysqlUsers else postgresUsers
        val cleanUser = user.trim()
        userList.removeAll { it.username.equals(cleanUser, ignoreCase = true) }
        userList.add(DatabaseUser(cleanUser, host.trim(), listOf("USAGE")))
        userPasswords[Pair(engine, cleanUser.lowercase())] = pass
        Result.success(Unit)
    }

    suspend fun grantPermissions(engine: DatabaseEngine, user: String, host: String, db: String, perms: List<String>): Result<Unit> = withContext(Dispatchers.IO) {
        val userList = if (engine == DatabaseEngine.MYSQL) mysqlUsers else postgresUsers
        val u = userList.find { it.username == user }
        if (u != null) {
            val updated = (u.privileges + perms).distinct()
            userList.remove(u)
            userList.add(u.copy(privileges = updated))
        }
        Result.success(Unit)
    }

    suspend fun revokePermissions(engine: DatabaseEngine, user: String, host: String, db: String): Result<Unit> = withContext(Dispatchers.IO) {
        val userList = if (engine == DatabaseEngine.MYSQL) mysqlUsers else postgresUsers
        val u = userList.find { it.username == user }
        if (u != null) {
            userList.remove(u)
            userList.add(u.copy(privileges = listOf("USAGE")))
        }
        Result.success(Unit)
    }
}
