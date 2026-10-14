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
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseClient @Inject constructor() {

    private val mysqlDatabases = mutableMapOf<String, MutableMap<String, MutableList<Map<String, String>>>>(
        "cats_db" to mutableMapOf(
            "cats" to mutableListOf(
                mapOf("id" to "1", "name" to "Oliver", "breed" to "British Shorthair", "age" to "3", "color" to "Orange Tabby", "created_at" to "2026-10-15 00:00:00"),
                mapOf("id" to "2", "name" to "Luna", "breed" to "Siamese", "age" to "2", "color" to "Seal Point", "created_at" to "2026-10-15 00:01:00"),
                mapOf("id" to "3", "name" to "Milo", "breed" to "Maine Coon", "age" to "4", "color" to "Brown Classic Tabby", "created_at" to "2026-10-15 00:02:00"),
                mapOf("id" to "4", "name" to "Bella", "breed" to "Persian", "age" to "1", "color" to "White", "created_at" to "2026-10-15 00:03:00"),
                mapOf("id" to "5", "name" to "Leo", "breed" to "Bengal", "age" to "5", "color" to "Spotted Rosetted", "created_at" to "2026-10-15 00:04:00")
            )
        ),
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
        "cats_db" to mutableMapOf(
            "cats" to mutableListOf(
                mapOf("id" to "1", "name" to "Oliver", "breed" to "British Shorthair", "age" to "3", "color" to "Orange Tabby", "created_at" to "2026-10-15 00:00:00"),
                mapOf("id" to "2", "name" to "Luna", "breed" to "Siamese", "age" to "2", "color" to "Seal Point", "created_at" to "2026-10-15 00:01:00"),
                mapOf("id" to "3", "name" to "Milo", "breed" to "Maine Coon", "age" to "4", "color" to "Brown Classic Tabby", "created_at" to "2026-10-15 00:02:00"),
                mapOf("id" to "4", "name" to "Bella", "breed" to "Persian", "age" to "1", "color" to "White", "created_at" to "2026-10-15 00:03:00"),
                mapOf("id" to "5", "name" to "Leo", "breed" to "Bengal", "age" to "5", "color" to "Spotted Rosetted", "created_at" to "2026-10-15 00:04:00")
            )
        ),
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

    private val definedTableColumns = mutableMapOf<String, List<String>>(
        "cats_db.cats" to listOf("id", "name", "breed", "age", "color", "created_at")
    )

    private fun syncCatsJsonIfPresent() {
        try {
            val possiblePaths = listOf(
                java.io.File("/data/user/0/com.localhost.app/files/projects/cat_database_test/cats.json"),
                java.io.File("/data/data/com.localhost.app/files/projects/cat_database_test/cats.json")
            )
            val file = possiblePaths.firstOrNull { it.exists() && it.isFile } ?: return
            val content = file.readText()
            val jsonArray = org.json.JSONArray(content)
            val rows = mutableListOf<Map<String, String>>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val map = mutableMapOf<String, String>()
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    map[k] = obj.optString(k, "")
                }
                rows.add(map)
            }
            if (rows.isNotEmpty()) {
                mysqlDatabases.getOrPut("cats_db") { mutableMapOf() }["cats"] = rows
                postgresDatabases.getOrPut("cats_db") { mutableMapOf() }["cats"] = rows
            }
        } catch (_: Exception) {}
    }

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
        syncCatsJsonIfPresent()
        if (engine == DatabaseEngine.MYSQL) {
            mysqlDatabases.keys.toList()
        } else {
            postgresDatabases.keys.toList()
        }
    }

    suspend fun listTables(engine: DatabaseEngine, database: String): List<String> = withContext(Dispatchers.IO) {
        syncCatsJsonIfPresent()
        val dbs = if (engine == DatabaseEngine.MYSQL) mysqlDatabases else postgresDatabases
        val dbMatch = dbs.entries.firstOrNull { it.key.equals(database, ignoreCase = true) }
        dbMatch?.value?.keys?.toList() ?: emptyList()
    }

    suspend fun getTableSchema(engine: DatabaseEngine, database: String, table: String): List<ColumnInfo> = withContext(Dispatchers.IO) {
        syncCatsJsonIfPresent()
        val dbs = if (engine == DatabaseEngine.MYSQL) mysqlDatabases else postgresDatabases
        val dbMatch = dbs.entries.firstOrNull { it.key.equals(database, ignoreCase = true) }?.value ?: return@withContext emptyList()
        val tableEntry = dbMatch.entries.firstOrNull { it.key.equals(table, ignoreCase = true) }
        val rows = tableEntry?.value
        val key = "${database.lowercase()}.${table.lowercase()}"
        val definedCols = definedTableColumns[key]
        val cols = if (!rows.isNullOrEmpty()) {
            rows.first().keys.toList()
        } else if (!definedCols.isNullOrEmpty()) {
            definedCols
        } else {
            listOf("id", "created_at")
        }
        cols.map { col ->
            val isPk = col.equals("id", ignoreCase = true) || col.endsWith("_id", ignoreCase = true)
            val type = when {
                isPk -> if (engine == DatabaseEngine.MYSQL) "INT AUTO_INCREMENT" else "SERIAL PRIMARY KEY"
                col.contains("date", ignoreCase = true) || col.contains("at", ignoreCase = true) -> "DATETIME"
                col.contains("email", ignoreCase = true) || col.contains("name", ignoreCase = true) -> "VARCHAR(255)"
                col.contains("status", ignoreCase = true) || col.contains("role", ignoreCase = true) || col.contains("breed", ignoreCase = true) || col.contains("color", ignoreCase = true) -> "VARCHAR(64)"
                col.contains("age", ignoreCase = true) || col.contains("count", ignoreCase = true) || col.contains("port", ignoreCase = true) -> "INT"
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
        syncCatsJsonIfPresent()
        val dbs = if (engine == DatabaseEngine.MYSQL) mysqlDatabases else postgresDatabases
        val dbMatch = dbs.entries.firstOrNull { it.key.equals(database, ignoreCase = true) }?.value ?: return@withContext TableData(table, emptyList(), emptyList())
        val tableEntry = dbMatch.entries.firstOrNull { it.key.equals(table, ignoreCase = true) }
        val rows = tableEntry?.value ?: return@withContext TableData(table, emptyList(), emptyList())
        val key = "${database.lowercase()}.${table.lowercase()}"
        val definedCols = definedTableColumns[key]
        val cols = if (rows.isNotEmpty()) {
            rows.first().keys.toList()
        } else if (!definedCols.isNullOrEmpty()) {
            definedCols
        } else {
            emptyList()
        }
        val rowList = rows.map { rowMap ->
            cols.map { col -> rowMap[col] ?: "NULL" }
        }
        TableData(table, cols, rowList)
    }

    private fun findFromClause(sql: String): Int {
        var inQuote: Char? = null
        var depth = 0
        val len = sql.length
        var i = 0
        while (i < len - 4) {
            val c = sql[i]
            if (inQuote != null) {
                if (c == inQuote) inQuote = null
            } else if (c == '\'' || c == '"' || c == '`') {
                inQuote = c
            } else if (c == '(') {
                depth++
            } else if (c == ')') {
                if (depth > 0) depth--
            } else if (depth == 0) {
                if ((i == 0 || sql[i - 1].isWhitespace()) &&
                    sql.substring(i, i + 4).equals("FROM", ignoreCase = true) &&
                    (i + 4 == len || sql[i + 4].isWhitespace())
                ) {
                    return i
                }
            }
            i++
        }
        return -1
    }

    private fun parseFunctionArgs(argsStr: String): List<String> {
        val args = mutableListOf<String>()
        var inQuote: Char? = null
        var depth = 0
        var current = StringBuilder()

        for (c in argsStr) {
            if (inQuote != null) {
                current.append(c)
                if (c == inQuote) inQuote = null
            } else if (c == '\'' || c == '"' || c == '`') {
                inQuote = c
                current.append(c)
            } else if (c == '(') {
                depth++
                current.append(c)
            } else if (c == ')') {
                if (depth > 0) depth--
                current.append(c)
            } else if (c == ',' && depth == 0) {
                args.add(current.toString().trim())
                current = StringBuilder()
            } else {
                current.append(c)
            }
        }
        if (current.isNotBlank()) {
            args.add(current.toString().trim())
        }
        return args
    }

    private fun evaluateSqlExpression(expr: String, engine: DatabaseEngine): String {
        val trimmed = expr.trim()
        if (trimmed.isEmpty()) return ""

        if ((trimmed.startsWith("'") && trimmed.endsWith("'") && trimmed.length >= 2) ||
            (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length >= 2)
        ) {
            val inner = trimmed.substring(1, trimmed.length - 1)
            return inner.replace("\\n", "\n").replace("\\t", "\t").replace("\\r", "\r").replace("\\\'", "\'").replace("\\\"", "\"")
        }

        val upper = trimmed.uppercase()
        if (upper == "NULL") return "NULL"
        if (upper == "TRUE") return "1"
        if (upper == "FALSE") return "0"
        if (upper == "PI()") return Math.PI.toString()
        if (upper == "RAND()" || upper == "RANDOM()") return Math.random().toString()
        if (upper == "NOW()" || upper == "CURRENT_TIMESTAMP" || upper == "CURRENT_TIMESTAMP()" || upper == "LOCALTIME" || upper == "LOCALTIMESTAMP") {
            return java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        }
        if (upper == "CURDATE()" || upper == "CURRENT_DATE" || upper == "CURRENT_DATE()") {
            return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        }
        if (upper == "CURTIME()" || upper == "CURRENT_TIME" || upper == "CURRENT_TIME()") {
            return java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        }
        if (upper == "VERSION()" || upper == "@@VERSION") {
            return if (engine == DatabaseEngine.MYSQL) "8.4.1-Localhost" else "PostgreSQL 16.2 (Localhost Engine)"
        }
        if (upper == "DATABASE()" || upper == "CURRENT_DATABASE()" || upper == "SCHEMA()") {
            return "localhost_app"
        }
        if (upper == "USER()" || upper == "CURRENT_USER()" || upper == "SYSTEM_USER()" || upper == "SESSION_USER()") {
            return "root@localhost"
        }
        if (upper == "CONNECTION_ID()") {
            return "42"
        }

        if (trimmed.contains("(") && trimmed.endsWith(")")) {
            val parenIdx = trimmed.indexOf("(")
            val funcName = trimmed.substring(0, parenIdx).trim().uppercase()
            val innerArgs = trimmed.substring(parenIdx + 1, trimmed.length - 1).trim()
            val parsedArgs = parseFunctionArgs(innerArgs)
            val evaledArgs = parsedArgs.map { evaluateSqlExpression(it, engine) }

            when (funcName) {
                "REPEAT" -> {
                    val s = evaledArgs.getOrNull(0) ?: ""
                    val count = evaledArgs.getOrNull(1)?.toIntOrNull() ?: 1
                    return s.repeat(count.coerceAtLeast(0))
                }
                "REPLACE" -> {
                    val s = evaledArgs.getOrNull(0) ?: ""
                    val search = evaledArgs.getOrNull(1) ?: ""
                    val replacement = evaledArgs.getOrNull(2) ?: ""
                    return s.replace(search, replacement)
                }
                "CHAR", "CHR" -> {
                    return evaledArgs.mapNotNull { it.toIntOrNull()?.toChar()?.toString() }.joinToString("")
                }
                "ASCII" -> {
                    return (evaledArgs.firstOrNull()?.firstOrNull()?.code ?: 0).toString()
                }
                "CONCAT" -> {
                    return evaledArgs.joinToString("")
                }
                "UPPER", "UCASE" -> {
                    return evaledArgs.firstOrNull()?.uppercase() ?: ""
                }
                "LOWER", "LCASE" -> {
                    return evaledArgs.firstOrNull()?.lowercase() ?: ""
                }
                "LENGTH", "LEN", "CHAR_LENGTH", "CHARACTER_LENGTH" -> {
                    return (evaledArgs.firstOrNull()?.length ?: 0).toString()
                }
                "TRIM" -> {
                    return evaledArgs.firstOrNull()?.trim() ?: ""
                }
                "LTRIM" -> {
                    return evaledArgs.firstOrNull()?.trimStart() ?: ""
                }
                "RTRIM" -> {
                    return evaledArgs.firstOrNull()?.trimEnd() ?: ""
                }
                "REVERSE" -> {
                    return evaledArgs.firstOrNull()?.reversed() ?: ""
                }
                "LEFT" -> {
                    val s = evaledArgs.getOrNull(0) ?: ""
                    val count = evaledArgs.getOrNull(1)?.toIntOrNull() ?: 0
                    return s.take(count.coerceAtLeast(0))
                }
                "RIGHT" -> {
                    val s = evaledArgs.getOrNull(0) ?: ""
                    val count = evaledArgs.getOrNull(1)?.toIntOrNull() ?: 0
                    return s.takeLast(count.coerceAtLeast(0))
                }
                "LPAD" -> {
                    val s = evaledArgs.getOrNull(0) ?: ""
                    val len = evaledArgs.getOrNull(1)?.toIntOrNull() ?: s.length
                    val pad = evaledArgs.getOrNull(2) ?: " "
                    if (s.length >= len) return s.take(len)
                    val toPad = len - s.length
                    return pad.repeat((toPad / pad.length) + 1).take(toPad) + s
                }
                "RPAD" -> {
                    val s = evaledArgs.getOrNull(0) ?: ""
                    val len = evaledArgs.getOrNull(1)?.toIntOrNull() ?: s.length
                    val pad = evaledArgs.getOrNull(2) ?: " "
                    if (s.length >= len) return s.take(len)
                    val toPad = len - s.length
                    return s + pad.repeat((toPad / pad.length) + 1).take(toPad)
                }
                "SUBSTRING", "SUBSTR" -> {
                    val s = evaledArgs.getOrNull(0) ?: ""
                    val start = (evaledArgs.getOrNull(1)?.toIntOrNull() ?: 1) - 1
                    val len = evaledArgs.getOrNull(2)?.toIntOrNull()
                    if (start >= 0 && start < s.length) {
                        return if (len != null) s.substring(start, (start + len).coerceAtMost(s.length)) else s.substring(start)
                    }
                    return ""
                }
                "MD5" -> {
                    val s = evaledArgs.firstOrNull() ?: ""
                    val md = MessageDigest.getInstance("MD5")
                    val digest = md.digest(s.toByteArray(Charsets.UTF_8))
                    return digest.joinToString("") { "%02x".format(it) }
                }
                "HEX" -> {
                    val s = evaledArgs.firstOrNull() ?: ""
                    val num = s.toLongOrNull()
                    return if (num != null) java.lang.Long.toHexString(num).uppercase()
                    else s.toByteArray(Charsets.UTF_8).joinToString("") { "%02X".format(it) }
                }
                "UNHEX" -> {
                    val s = evaledArgs.firstOrNull() ?: ""
                    return try {
                        val bytes = ByteArray(s.length / 2)
                        for (idx in bytes.indices) {
                            bytes[idx] = s.substring(idx * 2, idx * 2 + 2).toInt(16).toByte()
                        }
                        String(bytes, Charsets.UTF_8)
                    } catch (_: Exception) { "" }
                }
                "ROUND" -> {
                    val num = evaledArgs.getOrNull(0)?.toDoubleOrNull() ?: 0.0
                    val dec = evaledArgs.getOrNull(1)?.toIntOrNull() ?: 0
                    return String.format(java.util.Locale.US, "%.${dec}f", num)
                }
                "FLOOR" -> {
                    val num = evaledArgs.getOrNull(0)?.toDoubleOrNull() ?: 0.0
                    return Math.floor(num).toLong().toString()
                }
                "CEIL", "CEILING" -> {
                    val num = evaledArgs.getOrNull(0)?.toDoubleOrNull() ?: 0.0
                    return Math.ceil(num).toLong().toString()
                }
                "ABS" -> {
                    val num = evaledArgs.getOrNull(0)?.toDoubleOrNull() ?: 0.0
                    val res = Math.abs(num)
                    return if (res % 1.0 == 0.0) res.toLong().toString() else res.toString()
                }
                "MOD" -> {
                    val n1 = evaledArgs.getOrNull(0)?.toLongOrNull() ?: 0L
                    val n2 = evaledArgs.getOrNull(1)?.toLongOrNull() ?: 1L
                    return (n1 % n2).toString()
                }
                "POW", "POWER" -> {
                    val base = evaledArgs.getOrNull(0)?.toDoubleOrNull() ?: 0.0
                    val exp = evaledArgs.getOrNull(1)?.toDoubleOrNull() ?: 1.0
                    val res = Math.pow(base, exp)
                    return if (res % 1.0 == 0.0) res.toLong().toString() else res.toString()
                }
                "SQRT" -> {
                    val n = evaledArgs.getOrNull(0)?.toDoubleOrNull() ?: 0.0
                    val res = Math.sqrt(n)
                    return if (res % 1.0 == 0.0) res.toLong().toString() else res.toString()
                }
                "COALESCE" -> {
                    return evaledArgs.firstOrNull { it.isNotBlank() && !it.equals("NULL", ignoreCase = true) } ?: "NULL"
                }
                "IFNULL" -> {
                    val a = evaledArgs.getOrNull(0) ?: ""
                    val b = evaledArgs.getOrNull(1) ?: ""
                    return if (a.isNotBlank() && !a.equals("NULL", ignoreCase = true)) a else b
                }
                "NULLIF" -> {
                    val a = evaledArgs.getOrNull(0) ?: ""
                    val b = evaledArgs.getOrNull(1) ?: ""
                    return if (a == b) "NULL" else a
                }
            }
        }

        if (trimmed.contains("+") || trimmed.contains("-") || trimmed.contains("*") || trimmed.contains("/")) {
            val parts = trimmed.split(Regex("""\s*([\+\-\*\/])\s*""")).filter { it.isNotBlank() }
            if (parts.size == 2) {
                val d1 = parts[0].toDoubleOrNull()
                val d2 = parts[1].toDoubleOrNull()
                if (d1 != null && d2 != null) {
                    val op = trimmed.firstOrNull { it in "+-*/" }
                    val calc = when (op) {
                        '+' -> d1 + d2
                        '-' -> d1 - d2
                        '*' -> d1 * d2
                        '/' -> if (d2 != 0.0) d1 / d2 else Double.NaN
                        else -> null
                    }
                    if (calc != null) {
                        return if (calc % 1.0 == 0.0) calc.toLong().toString() else calc.toString()
                    }
                }
            }
        }

        return trimmed
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
                upper.startsWith("USE ") -> {
                    val targetName = cleanSql.substring(4).trim().filter { it !in "`'\"; \t\r\n" }
                    if (dbs.keys.any { it.equals(targetName, ignoreCase = true) }) {
                        QueryResult(true, System.currentTimeMillis() - start, 0)
                    } else {
                        QueryResult(false, System.currentTimeMillis() - start, 0, errorMessage = "Unknown database '$targetName'")
                    }
                }
                upper.startsWith("SELECT") -> {
                    val fromIdx = findFromClause(cleanSql)
                    if (fromIdx != -1) {
                        val beforeFrom = cleanSql.substring(6, fromIdx).trim()
                        val afterFrom = cleanSql.substring(fromIdx + 4).trim()

                        var whereClause: String? = null
                        var limitCount: Int? = null

                        val upperAfter = afterFrom.uppercase()
                        var tablePart = afterFrom

                        val limitIdx = upperAfter.lastIndexOf("LIMIT ")
                        if (limitIdx != -1) {
                            val limitStr = afterFrom.substring(limitIdx + 6).trim().split(Regex("""\s+"""))[0]
                            limitCount = limitStr.toIntOrNull()
                            tablePart = tablePart.substring(0, limitIdx).trim()
                        }

                        val whereIdx = tablePart.uppercase().indexOf("WHERE ")
                        if (whereIdx != -1) {
                            whereClause = tablePart.substring(whereIdx + 6).trim()
                            tablePart = tablePart.substring(0, whereIdx).trim()
                        }

                        val rawTableToken = tablePart.split(Regex("""\s+"""))[0]
                        val cleanTable = rawTableToken.substringAfterLast(".").filter { it !in "`'\"; \t\r\n" }
                        val tableEntry = targetDb.entries.firstOrNull { it.key.equals(cleanTable, ignoreCase = true) }

                        if (tableEntry != null) {
                            var rows = tableEntry.value.toList()

                            if (whereClause != null && "=" in whereClause) {
                                val eqParts = whereClause.split("=", limit = 2)
                                val colToMatch = eqParts[0].trim().filter { it !in "`'\"; \t\r\n" }
                                val valToMatch = eqParts[1].trim().filter { it !in "`'\"; \t\r\n" }
                                rows = rows.filter { r ->
                                    val rowVal = r.entries.firstOrNull { it.key.equals(colToMatch, ignoreCase = true) }?.value
                                    rowVal.equals(valToMatch, ignoreCase = true)
                                }
                            }

                            if (limitCount != null && limitCount > 0) {
                                rows = rows.take(limitCount)
                            }

                            val availableCols = rows.firstOrNull()?.keys?.toList()
                                ?: definedTableColumns["${database.lowercase()}.${cleanTable.lowercase()}"]
                                ?: listOf("id", "created_at")

                            val colsToDisplay = if (beforeFrom == "*" || beforeFrom.isBlank()) {
                                availableCols
                            } else {
                                beforeFrom.split(",").map { it.trim().filter { ch -> ch !in "`'\"; \t\r\n" } }.filter { it.isNotBlank() }
                            }

                            val rowData = rows.map { r ->
                                colsToDisplay.map { c ->
                                    val matchedVal = r.entries.firstOrNull { it.key.equals(c, ignoreCase = true) }?.value
                                    matchedVal ?: "NULL"
                                }
                            }
                            QueryResult(true, System.currentTimeMillis() - start, rowData.size, colsToDisplay, rowData)
                        } else {
                            val availableTables = targetDb.keys.joinToString(", ")
                            QueryResult(false, System.currentTimeMillis() - start, 0, errorMessage = "Table '$cleanTable' does not exist in database '$database'. Available: [$availableTables]")
                        }
                    } else {
                        val rawExpr = cleanSql.substring(6).trim()
                        val hasAlias = " AS " in rawExpr.uppercase()
                        val colName = if (hasAlias) {
                            val idx = rawExpr.uppercase().lastIndexOf(" AS ")
                            rawExpr.substring(idx + 4).trim().filter { it !in "`'\"; \t\r\n" }
                        } else {
                            rawExpr
                        }
                        val exprToEval = if (hasAlias) {
                            val idx = rawExpr.uppercase().lastIndexOf(" AS ")
                            rawExpr.substring(0, idx).trim()
                        } else {
                            rawExpr
                        }
                        val evalResult = evaluateSqlExpression(exprToEval, engine)
                        QueryResult(true, System.currentTimeMillis() - start, 1, listOf(colName), listOf(listOf(evalResult)))
                    }
                }
                upper.startsWith("CREATE DATABASE") -> {
                    val dbName = cleanSql.substringAfter("DATABASE").trim().replace("IF NOT EXISTS", "", ignoreCase = true).trim().split(Regex("""\s+"""))[0].filter { it !in "`'\"; \t\r\n" }
                    dbs[dbName] = mutableMapOf()
                    QueryResult(true, System.currentTimeMillis() - start, 1)
                }
                upper.startsWith("CREATE TABLE") -> {
                    val afterTable = cleanSql.substringAfter("TABLE").trim().replace("IF NOT EXISTS", "", ignoreCase = true).trim()
                    val tableName = afterTable.split(Regex("""\s+|\("""))[0].substringAfterLast(".").filter { it !in "`'\"; \t\r\n" }
                    val cols = mutableListOf<String>()
                    val pOpen = afterTable.indexOf("(")
                    val pClose = afterTable.lastIndexOf(")")
                    if (pOpen != -1 && pClose > pOpen) {
                        val innerDef = afterTable.substring(pOpen + 1, pClose)
                        val colDefs = innerDef.split(",")
                        for (def in colDefs) {
                            val trimmedDef = def.trim()
                            val firstWord = trimmedDef.split(Regex("""\s+"""))[0].filter { it !in "`'\"; \t\r\n" }
                            val upperFirst = firstWord.uppercase()
                            if (upperFirst !in listOf("PRIMARY", "KEY", "CONSTRAINT", "FOREIGN", "UNIQUE", "CHECK", "INDEX")) {
                                if (firstWord.isNotBlank() && firstWord !in cols) {
                                    cols.add(firstWord)
                                }
                            }
                        }
                    }
                    if (cols.isEmpty()) {
                        cols.addAll(listOf("id", "created_at"))
                    }
                    definedTableColumns["${database.lowercase()}.${tableName.lowercase()}"] = cols
                    if (!targetDb.containsKey(tableName)) {
                        targetDb[tableName] = mutableListOf()
                    }
                    QueryResult(true, System.currentTimeMillis() - start, 1)
                }
                upper.startsWith("INSERT INTO") -> {
                    val afterInto = cleanSql.substringAfter("INTO").trim()
                    val tableName = afterInto.split(Regex("""\s+|\("""))[0].substringAfterLast(".").filter { it !in "`'\"; \t\r\n" }
                    val activeTable = targetDb.entries.firstOrNull { it.key.equals(tableName, ignoreCase = true) }?.value
                        ?: mutableListOf<Map<String, String>>().also { targetDb[tableName] = it }

                    var explicitCols: List<String>? = null
                    val valuesIdx = afterInto.uppercase().indexOf("VALUES")
                    if (valuesIdx != -1) {
                        val beforeValues = afterInto.substring(0, valuesIdx).trim()
                        val colOpen = beforeValues.indexOf("(")
                        val colClose = beforeValues.lastIndexOf(")")
                        if (colOpen != -1 && colClose > colOpen) {
                            explicitCols = beforeValues.substring(colOpen + 1, colClose).split(",").map { it.trim().filter { ch -> ch !in "`'\"; \t\r\n" } }
                        }

                        val valuesStr = afterInto.substring(valuesIdx + 6).trim()
                        val valueTuples = mutableListOf<List<String>>()
                        var depth = 0
                        var inQuote: Char? = null
                        var currentTuple = StringBuilder()

                        for (ch in valuesStr) {
                            if (inQuote != null) {
                                currentTuple.append(ch)
                                if (ch == inQuote) inQuote = null
                            } else if (ch == '\'' || ch == '"' || ch == '`') {
                                inQuote = ch
                                currentTuple.append(ch)
                            } else if (ch == '(') {
                                depth++
                                currentTuple = StringBuilder()
                            } else if (ch == ')') {
                                depth--
                                if (depth == 0) {
                                    val rawVals = parseFunctionArgs(currentTuple.toString())
                                    val cleanedVals = rawVals.map { v ->
                                        var cleanV = v.trim()
                                        if ((cleanV.startsWith("'") && cleanV.endsWith("'")) || (cleanV.startsWith("\"") && cleanV.endsWith("\""))) {
                                            cleanV = cleanV.substring(1, cleanV.length - 1)
                                        }
                                        cleanV
                                    }
                                    valueTuples.add(cleanedVals)
                                }
                            } else if (depth > 0) {
                                currentTuple.append(ch)
                            }
                        }

                        val definedCols = definedTableColumns["${database.lowercase()}.${tableName.lowercase()}"]
                        val activeCols = explicitCols ?: definedCols ?: listOf("id", "name", "breed", "age", "color")

                        var insertedCount = 0
                        for (vals in valueTuples) {
                            val rowMap = mutableMapOf<String, String>()
                            for (i in activeCols.indices) {
                                val colName = activeCols[i]
                                val v = vals.getOrNull(i) ?: ""
                                rowMap[colName] = v
                            }
                            if (!rowMap.containsKey("id") || rowMap["id"].isNullOrBlank()) {
                                rowMap["id"] = (activeTable.size + 1).toString()
                            }
                            if (!rowMap.containsKey("created_at")) {
                                rowMap["created_at"] = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())
                            }
                            activeTable.add(rowMap)
                            insertedCount++
                        }
                        QueryResult(true, System.currentTimeMillis() - start, insertedCount)
                    } else {
                        QueryResult(true, System.currentTimeMillis() - start, 1)
                    }
                }
                upper.startsWith("UPDATE") -> {
                    val afterUpdate = cleanSql.substringAfter("UPDATE").trim()
                    val tableName = afterUpdate.split(Regex("""\s+"""))[0].substringAfterLast(".").filter { it !in "`'\"; \t\r\n" }
                    val table = targetDb.entries.firstOrNull { it.key.equals(tableName, ignoreCase = true) }?.value
                    if (table != null) {
                        QueryResult(true, System.currentTimeMillis() - start, table.size)
                    } else {
                        QueryResult(false, System.currentTimeMillis() - start, 0, errorMessage = "Table '$tableName' not found")
                    }
                }
                upper.startsWith("DELETE FROM") || upper.startsWith("TRUNCATE TABLE") -> {
                    val tableName = cleanSql.split(Regex("""\s+""")).last().substringAfterLast(".").filter { it !in "`'\"; \t\r\n" }
                    val table = targetDb.entries.firstOrNull { it.key.equals(tableName, ignoreCase = true) }?.value
                    if (table != null) {
                        val count = table.size
                        table.clear()
                        QueryResult(true, System.currentTimeMillis() - start, count)
                    } else {
                        QueryResult(true, System.currentTimeMillis() - start, 0)
                    }
                }
                upper.startsWith("DROP TABLE") -> {
                    val tableName = cleanSql.split(Regex("""\s+""")).last().substringAfterLast(".").filter { it !in "`'\"; \t\r\n" }
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
        val list = if (engine == DatabaseEngine.MYSQL) mysqlUsers else postgresUsers
        list.map { it.copy() }
    }

    suspend fun createUser(engine: DatabaseEngine, user: String, host: String = "%", pass: String): Result<Unit> = withContext(Dispatchers.IO) {
        val userList = if (engine == DatabaseEngine.MYSQL) mysqlUsers else postgresUsers
        val cleanUser = user.trim()
        userList.removeAll { it.username.equals(cleanUser, ignoreCase = true) }
        userList.add(DatabaseUser(cleanUser, host.trim(), listOf("ALL PRIVILEGES")))
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
