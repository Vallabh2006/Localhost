package com.localhost.core.data.database

import android.content.Context
import android.util.Base64
import com.localhost.core.model.ColumnInfo
import com.localhost.core.model.DatabaseConfig
import com.localhost.core.model.DatabaseEngine
import com.localhost.core.model.DatabaseUser
import com.localhost.core.model.QueryResult
import com.localhost.core.model.TableData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseClient @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scriptB64 = "aW1wb3J0IHN5cywgb3MsIGpzb24sIHB5bXlzcWwKCmRlZiBydW4oKToKICAgIHRyeToKICAgICAgICByYXdfaW5wdXQgPSBzeXMuc3RkaW4ucmVhZCgpLnN0cmlwKCkKICAgICAgICBpZiBub3QgcmF3X2lucHV0OgogICAgICAgICAgICBwcmludChqc29uLmR1bXBzKHsic3VjY2VzcyI6IEZhbHNlLCAiZXJyb3IiOiAiRW1wdHkgcGF5bG9hZCJ9KSkKICAgICAgICAgICAgcmV0dXJuCiAgICAgICAgcGF5bG9hZCA9IGpzb24ubG9hZHMocmF3X2lucHV0KQogICAgICAgIGFjdGlvbiA9IHBheWxvYWQuZ2V0KCJhY3Rpb24iLCAiIikKICAgICAgICBkYiA9IHBheWxvYWQuZ2V0KCJkYiIsICIiKQogICAgICAgIHNxbCA9IHBheWxvYWQuZ2V0KCJzcWwiLCAiIikKICAgICAgICBob3N0ID0gcGF5bG9hZC5nZXQoImhvc3QiLCAiMTI3LjAuMC4xIikKICAgICAgICBwb3J0ID0gaW50KHBheWxvYWQuZ2V0KCJwb3J0IiwgMzMwNikpCiAgICAgICAgdXNlciA9IHBheWxvYWQuZ2V0KCJ1c2VyIiwgInJvb3QiKQogICAgICAgIHBhc3N3b3JkID0gcGF5bG9hZC5nZXQoInBhc3N3b3JkIiwgIiIpCgogICAgICAgIHNvY2sgPSBwYXlsb2FkLmdldCgic29jayIsICIiKQogICAgICAgIGlmIG5vdCBzb2NrIG9yIG5vdCBvcy5wYXRoLmV4aXN0cyhzb2NrKToKICAgICAgICAgICAgc29jayA9IG9zLnBhdGguam9pbihvcy5wYXRoLmRpcm5hbWUob3MucGF0aC5hYnNwYXRoKF9fZmlsZV9fKSksICJtYXJpYWRiLnNvY2siKQogICAgICAgIGlmIG5vdCBvcy5wYXRoLmV4aXN0cyhzb2NrKToKICAgICAgICAgICAgc29jayA9ICIvZGF0YS91c2VyLzAvY29tLmxvY2FsaG9zdC5hcHAvZmlsZXMvcnVudGltZXMvbWFyaWFkYi9tYXJpYWRiLnNvY2siCiAgICAgICAgdXNlX3NvY2sgPSBvcy5wYXRoLmV4aXN0cyhzb2NrKSBhbmQgKGhvc3QgaW4gKCIxMjcuMC4wLjEiLCAibG9jYWxob3N0IiwgIiIpKQoKICAgICAgICBjb25uID0gTm9uZQogICAgICAgIGlmIHVzZV9zb2NrOgogICAgICAgICAgICB0cnk6CiAgICAgICAgICAgICAgICBjb25uID0gcHlteXNxbC5jb25uZWN0KAogICAgICAgICAgICAgICAgICAgIHVuaXhfc29ja2V0PXNvY2ssCiAgICAgICAgICAgICAgICAgICAgdXNlcj11c2VyLAogICAgICAgICAgICAgICAgICAgIHBhc3N3b3JkPXBhc3N3b3JkLAogICAgICAgICAgICAgICAgICAgIGRhdGFiYXNlPWRiIGlmIGRiIGVsc2UgTm9uZSwKICAgICAgICAgICAgICAgICAgICBhdXRvY29tbWl0PVRydWUsCiAgICAgICAgICAgICAgICAgICAgY29ubmVjdF90aW1lb3V0PTMsCiAgICAgICAgICAgICAgICAgICAgY3Vyc29yY2xhc3M9cHlteXNxbC5jdXJzb3JzLkRpY3RDdXJzb3IKICAgICAgICAgICAgICAgICkKICAgICAgICAgICAgZXhjZXB0IEV4Y2VwdGlvbjoKICAgICAgICAgICAgICAgIGNvbm4gPSBOb25lCgogICAgICAgIGlmIGNvbm4gaXMgTm9uZToKICAgICAgICAgICAgY29ubiA9IHB5bXlzcWwuY29ubmVjdCgKICAgICAgICAgICAgICAgIGhvc3Q9IjEyNy4wLjAuMSIgaWYgaG9zdCBpbiAoIiIsICJsb2NhbGhvc3QiKSBlbHNlIGhvc3QsCiAgICAgICAgICAgICAgICBwb3J0PXBvcnQsCiAgICAgICAgICAgICAgICB1c2VyPXVzZXIsCiAgICAgICAgICAgICAgICBwYXNzd29yZD1wYXNzd29yZCwKICAgICAgICAgICAgICAgIGRhdGFiYXNlPWRiIGlmIGRiIGVsc2UgTm9uZSwKICAgICAgICAgICAgICAgIGF1dG9jb21taXQ9VHJ1ZSwKICAgICAgICAgICAgICAgIGNvbm5lY3RfdGltZW91dD0zLAogICAgICAgICAgICAgICAgY3Vyc29yY2xhc3M9cHlteXNxbC5jdXJzb3JzLkRpY3RDdXJzb3IKICAgICAgICAgICAgKQoKICAgICAgICB3aXRoIGNvbm4uY3Vyc29yKCkgYXMgY3VyOgogICAgICAgICAgICBpZiBkYjoKICAgICAgICAgICAgICAgIHRyeToKICAgICAgICAgICAgICAgICAgICBjdXIuZXhlY3V0ZShmIlVTRSBge2RifWA7IikKICAgICAgICAgICAgICAgIGV4Y2VwdCBFeGNlcHRpb246CiAgICAgICAgICAgICAgICAgICAgcGFzcwogICAgICAgICAgICBpZiBhY3Rpb24gPT0gInRlc3QiOgogICAgICAgICAgICAgICAgY3VyLmV4ZWN1dGUoIlNFTEVDVCBWRVJTSU9OKCk7IikKICAgICAgICAgICAgICAgIHYgPSBjdXIuZmV0Y2hvbmUoKQogICAgICAgICAgICAgICAgdmVyID0gbGlzdCh2LnZhbHVlcygpKVswXSBpZiB2IGVsc2UgIjEzLjAiCiAgICAgICAgICAgICAgICBwcmludChqc29uLmR1bXBzKHsic3VjY2VzcyI6IFRydWUsICJtZXNzYWdlIjogZiJDb25uZWN0ZWQgdG8gTWFyaWFEQiB7dmVyfSJ9KSkKICAgICAgICAgICAgICAgIHJldHVybgogICAgICAgICAgICBlbGlmIGFjdGlvbiA9PSAibGlzdF9kYnMiOgogICAgICAgICAgICAgICAgY3VyLmV4ZWN1dGUoIlNIT1cgREFUQUJBU0VTOyIpCiAgICAgICAgICAgICAgICByb3dzID0gY3VyLmZldGNoYWxsKCkKICAgICAgICAgICAgICAgIGRicyA9IFtyW2xpc3Qoci5rZXlzKCkpWzBdXSBmb3IgciBpbiByb3dzIGlmIHJbbGlzdChyLmtleXMoKSlbMF1dIG5vdCBpbiAoImluZm9ybWF0aW9uX3NjaGVtYSIsICJwZXJmb3JtYW5jZV9zY2hlbWEiLCAic3lzIildCiAgICAgICAgICAgICAgICBwcmludChqc29uLmR1bXBzKHsic3VjY2VzcyI6IFRydWUsICJkYXRhYmFzZXMiOiBkYnN9KSkKICAgICAgICAgICAgICAgIHJldHVybgogICAgICAgICAgICBlbGlmIGFjdGlvbiA9PSAibGlzdF90YWJsZXMiOgogICAgICAgICAgICAgICAgaWYgZGI6CiAgICAgICAgICAgICAgICAgICAgY3VyLmV4ZWN1dGUoZiJTSE9XIFRBQkxFUyBGUk9NIGB7ZGJ9YDsiKQogICAgICAgICAgICAgICAgZWxzZToKICAgICAgICAgICAgICAgICAgICBjdXIuZXhlY3V0ZSgiU0hPVyBUQUJMRVM7IikKICAgICAgICAgICAgICAgIHJvd3MgPSBjdXIuZmV0Y2hhbGwoKQogICAgICAgICAgICAgICAgdGFibGVzID0gW3JbbGlzdChyLmtleXMoKSlbMF1dIGZvciByIGluIHJvd3NdCiAgICAgICAgICAgICAgICBwcmludChqc29uLmR1bXBzKHsic3VjY2VzcyI6IFRydWUsICJ0YWJsZXMiOiB0YWJsZXN9KSkKICAgICAgICAgICAgICAgIHJldHVybgogICAgICAgICAgICBlbGlmIGFjdGlvbiA9PSAic2NoZW1hIjoKICAgICAgICAgICAgICAgIHRibCA9IHBheWxvYWQuZ2V0KCJ0YWJsZSIsICIiKQogICAgICAgICAgICAgICAgaWYgZGI6CiAgICAgICAgICAgICAgICAgICAgY3VyLmV4ZWN1dGUoZiJERVNDUklCRSBge2RifWAuYHt0Ymx9YDsiKQogICAgICAgICAgICAgICAgZWxzZToKICAgICAgICAgICAgICAgICAgICBjdXIuZXhlY3V0ZShmIkRFU0NSSUJFIGB7dGJsfWA7IikKICAgICAgICAgICAgICAgIHJvd3MgPSBjdXIuZmV0Y2hhbGwoKQogICAgICAgICAgICAgICAgY29scyA9IFtdCiAgICAgICAgICAgICAgICBmb3IgciBpbiByb3dzOgogICAgICAgICAgICAgICAgICAgIGNvbHMuYXBwZW5kKHsKICAgICAgICAgICAgICAgICAgICAgICAgIm5hbWUiOiByLmdldCgiRmllbGQiLCAiIiksCiAgICAgICAgICAgICAgICAgICAgICAgICJ0eXBlIjogci5nZXQoIlR5cGUiLCAiIiksCiAgICAgICAgICAgICAgICAgICAgICAgICJpc051bGxhYmxlIjogci5nZXQoIk51bGwiLCAiWUVTIikgPT0gIllFUyIsCiAgICAgICAgICAgICAgICAgICAgICAgICJpc1ByaW1hcnlLZXkiOiByLmdldCgiS2V5IiwgIiIpID09ICJQUkkiLAogICAgICAgICAgICAgICAgICAgICAgICAiZGVmYXVsdFZhbHVlIjogci5nZXQoIkRlZmF1bHQiKQogICAgICAgICAgICAgICAgICAgIH0pCiAgICAgICAgICAgICAgICBwcmludChqc29uLmR1bXBzKHsic3VjY2VzcyI6IFRydWUsICJjb2x1bW5zIjogY29sc30pKQogICAgICAgICAgICAgICAgcmV0dXJuCiAgICAgICAgICAgIGVsaWYgYWN0aW9uID09ICJ0YWJsZV9kYXRhIjoKICAgICAgICAgICAgICAgIHRibCA9IHBheWxvYWQuZ2V0KCJ0YWJsZSIsICIiKQogICAgICAgICAgICAgICAgaWYgZGI6CiAgICAgICAgICAgICAgICAgICAgY3VyLmV4ZWN1dGUoZiJTRUxFQ1QgKiBGUk9NIGB7ZGJ9YC5ge3RibH1gIExJTUlUIDIwMDsiKQogICAgICAgICAgICAgICAgZWxzZToKICAgICAgICAgICAgICAgICAgICBjdXIuZXhlY3V0ZShmIlNFTEVDVCAqIEZST00gYHt0Ymx9YCBMSU1JVCAyMDA7IikKICAgICAgICAgICAgICAgIHJvd3MgPSBjdXIuZmV0Y2hhbGwoKQogICAgICAgICAgICAgICAgY29scyA9IGxpc3Qocm93c1swXS5rZXlzKCkpIGlmIHJvd3MgZWxzZSBbXQogICAgICAgICAgICAgICAgcm93X3ZhbHMgPSBbW3N0cihyLmdldChjLCAiIikpIGlmIHIuZ2V0KGMpIGlzIG5vdCBOb25lIGVsc2UgIk5VTEwiIGZvciBjIGluIGNvbHNdIGZvciByIGluIHJvd3NdCiAgICAgICAgICAgICAgICBwcmludChqc29uLmR1bXBzKHsic3VjY2VzcyI6IFRydWUsICJjb2x1bW5zIjogY29scywgInJvd3MiOiByb3dfdmFsc30pKQogICAgICAgICAgICAgICAgcmV0dXJuCiAgICAgICAgICAgIGVsaWYgYWN0aW9uID09ICJxdWVyeSI6CiAgICAgICAgICAgICAgICBjdXIuZXhlY3V0ZShzcWwpCiAgICAgICAgICAgICAgICBjb25uLmNvbW1pdCgpCiAgICAgICAgICAgICAgICBpZiBjdXIuZGVzY3JpcHRpb246CiAgICAgICAgICAgICAgICAgICAgY29scyA9IFtkWzBdIGZvciBkIGluIGN1ci5kZXNjcmlwdGlvbl0KICAgICAgICAgICAgICAgICAgICByb3dzID0gY3VyLmZldGNoYWxsKCkKICAgICAgICAgICAgICAgICAgICByb3dfdmFscyA9IFtbc3RyKHIuZ2V0KGMsICIiKSkgaWYgci5nZXQoYykgaXMgbm90IE5vbmUgZWxzZSAiTlVMTCIgZm9yIGMgaW4gY29sc10gZm9yIHIgaW4gcm93c10KICAgICAgICAgICAgICAgICAgICBwcmludChqc29uLmR1bXBzKHsic3VjY2VzcyI6IFRydWUsICJjb2x1bW5zIjogY29scywgInJvd3MiOiByb3dfdmFscywgImFmZmVjdGVkIjogbGVuKHJvd192YWxzKX0pKQogICAgICAgICAgICAgICAgZWxzZToKICAgICAgICAgICAgICAgICAgICBwcmludChqc29uLmR1bXBzKHsic3VjY2VzcyI6IFRydWUsICJjb2x1bW5zIjogW10sICJyb3dzIjogW10sICJhZmZlY3RlZCI6IGN1ci5yb3djb3VudH0pKQogICAgICAgICAgICAgICAgcmV0dXJuCiAgICAgICAgICAgIGVsaWYgYWN0aW9uID09ICJsaXN0X3VzZXJzIjoKICAgICAgICAgICAgICAgIGN1ci5leGVjdXRlKCJTRUxFQ1QgVXNlciwgSG9zdCBGUk9NIG15c3FsLnVzZXI7IikKICAgICAgICAgICAgICAgIHJvd3MgPSBjdXIuZmV0Y2hhbGwoKQogICAgICAgICAgICAgICAgdXNlcnMgPSBbeyJ1c2VybmFtZSI6IHIuZ2V0KCJVc2VyIiwgIiIpLCAiaG9zdCI6IHIuZ2V0KCJIb3N0IiwgIiUiKSwgInByaXZpbGVnZXMiOiBbIkFMTCBQUklWSUxFR0VTIl19IGZvciByIGluIHJvd3MgaWYgci5nZXQoIlVzZXIiKV0KICAgICAgICAgICAgICAgIHByaW50KGpzb24uZHVtcHMoeyJzdWNjZXNzIjogVHJ1ZSwgInVzZXJzIjogdXNlcnN9KSkKICAgICAgICAgICAgICAgIHJldHVybgogICAgZXhjZXB0IEV4Y2VwdGlvbiBhcyBlOgogICAgICAgIHByaW50KGpzb24uZHVtcHMoeyJzdWNjZXNzIjogRmFsc2UsICJlcnJvciI6IHN0cihlKX0pKQoKaWYgX19uYW1lX18gPT0gIl9fbWFpbl9fIjoKICAgIHJ1bigp"

    private fun executeQueryRunner(payload: JSONObject): JSONObject? {
        return try {
            val pythonBin = File(context.filesDir, "runtimes/python/bin/python3")
            val mariadbDir = File(context.filesDir, "runtimes/mariadb").apply { mkdirs() }
            val queryScript = File(mariadbDir, "db_query.py")
            if (!pythonBin.exists()) return null
            val decoded = String(Base64.decode(scriptB64, Base64.DEFAULT), StandardCharsets.UTF_8)
            if (!queryScript.exists() || queryScript.readText() != decoded) {
                queryScript.writeText(decoded)
                queryScript.setExecutable(true, false)
            }
            payload.put("sock", File(context.filesDir, "runtimes/mariadb/mariadb.sock").absolutePath)
            val pb = ProcessBuilder(pythonBin.absolutePath, queryScript.absolutePath)
            val env = pb.environment()
            env["LD_LIBRARY_PATH"] = "${context.filesDir.absolutePath}/runtimes/python/lib:${context.filesDir.absolutePath}/runtimes/mariadb/lib"
            env["PYTHONPATH"] = "${context.filesDir.absolutePath}/runtimes/python/lib/python3.14/site-packages"
            val proc = pb.start()
            proc.outputStream.bufferedWriter().use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }
            val output = proc.inputStream.bufferedReader().use { it.readText() }
            val jsonLine = output.lines().lastOrNull { it.trim().startsWith("{") } ?: return null
            JSONObject(jsonLine)
        } catch (_: Throwable) {
            null
        }
    }

    suspend fun testConnection(config: DatabaseConfig): Result<String> = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("action", "test")
            put("host", config.host)
            put("port", config.port)
            put("user", config.user)
            put("password", config.password)
            put("db", config.database)
        }
        val res = executeQueryRunner(payload)
        if (res != null && res.optBoolean("success")) {
            Result.success(res.optString("message", "Connected to MariaDB on ${config.host}:${config.port}"))
        } else {
            val err = res?.optString("error") ?: "Cannot connect to MariaDB on ${config.host}:${config.port}. Start service first."
            Result.failure(Exception(err))
        }
    }

    suspend fun listDatabases(engine: DatabaseEngine): List<String> = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("action", "list_dbs")
        }
        val res = executeQueryRunner(payload)
        if (res != null && res.optBoolean("success")) {
            val arr = res.optJSONArray("databases")
            if (arr != null) {
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    list.add(arr.getString(i))
                }
                return@withContext list
            }
        }
        emptyList()
    }

    suspend fun listTables(engine: DatabaseEngine, database: String): List<String> = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("action", "list_tables")
            put("db", database)
        }
        val res = executeQueryRunner(payload)
        if (res != null && res.optBoolean("success")) {
            val arr = res.optJSONArray("tables")
            if (arr != null) {
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    list.add(arr.getString(i))
                }
                return@withContext list
            }
        }
        emptyList()
    }

    suspend fun getTableSchema(engine: DatabaseEngine, database: String, table: String): List<ColumnInfo> = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("action", "schema")
            put("db", database)
            put("table", table)
        }
        val res = executeQueryRunner(payload)
        if (res != null && res.optBoolean("success")) {
            val arr = res.optJSONArray("columns")
            if (arr != null) {
                val list = mutableListOf<ColumnInfo>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        ColumnInfo(
                            name = obj.optString("name"),
                            type = obj.optString("type"),
                            isNullable = obj.optBoolean("isNullable", true),
                            isPrimaryKey = obj.optBoolean("isPrimaryKey", false),
                            defaultValue = if (obj.isNull("defaultValue")) null else obj.optString("defaultValue")
                        )
                    )
                }
                return@withContext list
            }
        }
        emptyList()
    }

    suspend fun getTableData(engine: DatabaseEngine, database: String, table: String): TableData = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("action", "table_data")
            put("db", database)
            put("table", table)
        }
        val res = executeQueryRunner(payload)
        if (res != null && res.optBoolean("success")) {
            val colsArr = res.optJSONArray("columns") ?: org.json.JSONArray()
            val rowsArr = res.optJSONArray("rows") ?: org.json.JSONArray()
            val cols = mutableListOf<String>()
            for (i in 0 until colsArr.length()) {
                cols.add(colsArr.getString(i))
            }
            val rows = mutableListOf<List<String>>()
            for (i in 0 until rowsArr.length()) {
                val rArr = rowsArr.getJSONArray(i)
                val row = mutableListOf<String>()
                for (j in 0 until rArr.length()) {
                    row.add(rArr.getString(j))
                }
                rows.add(row)
            }
            return@withContext TableData(tableName = table, columns = cols, rows = rows)
        }
        TableData(tableName = table, columns = emptyList(), rows = emptyList())
    }

    suspend fun executeQuery(engine: DatabaseEngine, database: String, sql: String): QueryResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val payload = JSONObject().apply {
            put("action", "query")
            put("db", database)
            put("sql", sql)
        }
        val res = executeQueryRunner(payload)
        val elapsed = System.currentTimeMillis() - start
        if (res != null && res.optBoolean("success")) {
            val colsArr = res.optJSONArray("columns") ?: org.json.JSONArray()
            val rowsArr = res.optJSONArray("rows") ?: org.json.JSONArray()
            val cols = mutableListOf<String>()
            for (i in 0 until colsArr.length()) {
                cols.add(colsArr.getString(i))
            }
            val rows = mutableListOf<List<String>>()
            for (i in 0 until rowsArr.length()) {
                val rArr = rowsArr.getJSONArray(i)
                val row = mutableListOf<String>()
                for (j in 0 until rArr.length()) {
                    row.add(rArr.getString(j))
                }
                rows.add(row)
            }
            QueryResult(
                isSuccess = true,
                executionTimeMs = elapsed,
                affectedRows = res.optInt("affected", rows.size),
                columns = cols,
                rows = rows
            )
        } else {
            QueryResult(
                isSuccess = false,
                executionTimeMs = elapsed,
                affectedRows = 0,
                errorMessage = res?.optString("error") ?: "MariaDB service offline or query failed"
            )
        }
    }

    suspend fun listUsers(engine: DatabaseEngine): List<DatabaseUser> = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("action", "list_users")
        }
        val res = executeQueryRunner(payload)
        if (res != null && res.optBoolean("success")) {
            val arr = res.optJSONArray("users")
            if (arr != null) {
                val list = mutableListOf<DatabaseUser>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        DatabaseUser(
                            username = obj.optString("username"),
                            host = obj.optString("host", "%"),
                            privileges = listOf("ALL PRIVILEGES")
                        )
                    )
                }
                return@withContext list
            }
        }
        listOf(DatabaseUser(username = "root", host = "%", privileges = listOf("ALL PRIVILEGES")))
    }

    suspend fun createUser(engine: DatabaseEngine, user: String, host: String = "%", pass: String): Result<Unit> = withContext(Dispatchers.IO) {
        val res = executeQuery(engine, "", "CREATE USER '$user'@'$host' IDENTIFIED BY '$pass'; FLUSH PRIVILEGES;")
        if (res.isSuccess) Result.success(Unit) else Result.failure(Exception(res.errorMessage))
    }

    suspend fun changeUserPassword(engine: DatabaseEngine, user: String, newPass: String): Result<Unit> = withContext(Dispatchers.IO) {
        val res = executeQuery(engine, "", "ALTER USER '$user'@'%' IDENTIFIED BY '$newPass'; FLUSH PRIVILEGES;")
        if (res.isSuccess) Result.success(Unit) else Result.failure(Exception(res.errorMessage))
    }

    suspend fun deleteUser(engine: DatabaseEngine, user: String): Result<Unit> = withContext(Dispatchers.IO) {
        executeQuery(engine, "", "DROP USER '$user'@'%';")
        executeQuery(engine, "", "DROP USER '$user'@'localhost';")
        val res = executeQuery(engine, "", "FLUSH PRIVILEGES;")
        if (res.isSuccess) Result.success(Unit) else Result.failure(Exception(res.errorMessage))
    }

    suspend fun grantPermissions(engine: DatabaseEngine, user: String, host: String, db: String, perms: List<String>): Result<Unit> = withContext(Dispatchers.IO) {
        val permStr = if (perms.isEmpty() || "ALL" in perms) "ALL PRIVILEGES" else perms.joinToString(", ")
        val targetDb = if (db.isBlank() || db == "*") "*.*" else "`$db`.*"
        val res = executeQuery(engine, "", "GRANT $permStr ON $targetDb TO '$user'@'$host'; FLUSH PRIVILEGES;")
        if (res.isSuccess) Result.success(Unit) else Result.failure(Exception(res.errorMessage))
    }

    suspend fun revokePermissions(engine: DatabaseEngine, user: String, host: String, db: String): Result<Unit> = withContext(Dispatchers.IO) {
        val targetDb = if (db.isBlank() || db == "*") "*.*" else "`$db`.*"
        val res = executeQuery(engine, "", "REVOKE ALL PRIVILEGES ON $targetDb FROM '$user'@'$host'; FLUSH PRIVILEGES;")
        if (res.isSuccess) Result.success(Unit) else Result.failure(Exception(res.errorMessage))
    }
}
