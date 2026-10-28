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
    private val scriptB64 = "aW1wb3J0IHN5cywgb3MsIGpzb24sIHB5bXlzcWwKCmRlZiBydW4oKToKICAgIHRyeToKICAgICAgICByYXdfaW5wdXQgPSBzeXMuc3RkaW4ucmVhZCgpLnN0cmlwKCkKICAgICAgICBpZiBub3QgcmF3X2lucHV0OgogICAgICAgICAgICBwcmludChqc29uLmR1bXBzKHsic3VjY2VzcyI6IEZhbHNlLCAiZXJyb3IiOiAiRW1wdHkgcGF5bG9hZCJ9KSkKICAgICAgICAgICAgcmV0dXJuCiAgICAgICAgcGF5bG9hZCA9IGpzb24ubG9hZHMocmF3X2lucHV0KQogICAgICAgIGFjdGlvbiA9IHBheWxvYWQuZ2V0KCJhY3Rpb24iLCAiIikKICAgICAgICBkYiA9IHBheWxvYWQuZ2V0KCJkYiIsICIiKQogICAgICAgIHNxbCA9IHBheWxvYWQuZ2V0KCJzcWwiLCAiIikKICAgICAgICBob3N0ID0gcGF5bG9hZC5nZXQoImhvc3QiLCAiMTI3LjAuMC4xIikKICAgICAgICBwb3J0ID0gaW50KHBheWxvYWQuZ2V0KCJwb3J0IiwgMzMwNikpCiAgICAgICAgdXNlciA9IHBheWxvYWQuZ2V0KCJ1c2VyIiwgInJvb3QiKQogICAgICAgIHBhc3N3b3JkID0gcGF5bG9hZC5nZXQoInBhc3N3b3JkIiwgIiIpCgogICAgICAgIHNvY2sgPSBwYXlsb2FkLmdldCgic29jayIsICIiKQogICAgICAgIGlmIG5vdCBzb2NrIG9yIG5vdCBvcy5wYXRoLmV4aXN0cyhzb2NrKToKICAgICAgICAgICAgc29jayA9IG9zLnBhdGguam9pbihvcy5wYXRoLmRpcm5hbWUob3MucGF0aC5hYnNwYXRoKF9fZmlsZV9fKSksICJtYXJpYWRiLnNvY2siKQogICAgICAgIGlmIG5vdCBvcy5wYXRoLmV4aXN0cyhzb2NrKToKICAgICAgICAgICAgc29jayA9ICIvZGF0YS91c2VyLzAvY29tLmxvY2FsaG9zdC5hcHAvZmlsZXMvcnVudGltZXMvbWFyaWFkYi9tYXJpYWRiLnNvY2siCiAgICAgICAgdXNlX3NvY2sgPSBvcy5wYXRoLmV4aXN0cyhzb2NrKSBhbmQgKGhvc3QgaW4gKCIxMjcuMC4wLjEiLCAibG9jYWxob3N0IiwgIiIpKQoKICAgICAgICBjb25uID0gTm9uZQogICAgICAgIGlmIHVzZV9zb2NrOgogICAgICAgICAgICB0cnk6CiAgICAgICAgICAgICAgICBjb25uID0gcHlteXNxbC5jb25uZWN0KAogICAgICAgICAgICAgICAgICAgIHVuaXhfc29ja2V0PXNvY2ssCiAgICAgICAgICAgICAgICAgICAgdXNlcj11c2VyLAogICAgICAgICAgICAgICAgICAgIHBhc3N3b3JkPXBhc3N3b3JkLAogICAgICAgICAgICAgICAgICAgIGF1dG9jb21taXQ9VHJ1ZSwKICAgICAgICAgICAgICAgICAgICBjb25uZWN0X3RpbWVvdXQ9MywKICAgICAgICAgICAgICAgICAgICBjdXJzb3JjbGFzcz1weW15c3FsLmN1cnNvcnMuRGljdEN1cnNvcgogICAgICAgICAgICAgICAgKQogICAgICAgICAgICBleGNlcHQgRXhjZXB0aW9uOgogICAgICAgICAgICAgICAgY29ubiA9IE5vbmUKCiAgICAgICAgaWYgY29ubiBpcyBOb25lOgogICAgICAgICAgICBjb25uID0gcHlteXNxbC5jb25uZWN0KAogICAgICAgICAgICAgICAgaG9zdD0iMTI3LjAuMC4xIiBpZiBob3N0IGluICgiIiwgImxvY2FsaG9zdCIpIGVsc2UgaG9zdCwKICAgICAgICAgICAgICAgIHBvcnQ9cG9ydCwKICAgICAgICAgICAgICAgIHVzZXI9dXNlciwKICAgICAgICAgICAgICAgIHBhc3N3b3JkPXBhc3N3b3JkLAogICAgICAgICAgICAgICAgYXV0b2NvbW1pdD1UcnVlLAogICAgICAgICAgICAgICAgY29ubmVjdF90aW1lb3V0PTMsCiAgICAgICAgICAgICAgICBjdXJzb3JjbGFzcz1weW15c3FsLmN1cnNvcnMuRGljdEN1cnNvcgogICAgICAgICAgICApCgogICAgICAgIHdpdGggY29ubi5jdXJzb3IoKSBhcyBjdXI6CiAgICAgICAgICAgIGlmIGRiOgogICAgICAgICAgICAgICAgdHJ5OgogICAgICAgICAgICAgICAgICAgIGN1ci5leGVjdXRlKGYiVVNFIGB7ZGJ9YDsiKQogICAgICAgICAgICAgICAgZXhjZXB0IEV4Y2VwdGlvbjoKICAgICAgICAgICAgICAgICAgICBwYXNzCiAgICAgICAgICAgIGlmIGFjdGlvbiA9PSAidGVzdCI6CiAgICAgICAgICAgICAgICBjdXIuZXhlY3V0ZSgiU0VMRUNUIFZFUlNJT04oKTsiKQogICAgICAgICAgICAgICAgdiA9IGN1ci5mZXRjaG9uZSgpCiAgICAgICAgICAgICAgICB2ZXIgPSBsaXN0KHYudmFsdWVzKCkpWzBdIGlmIHYgZWxzZSAiMTMuMCIKICAgICAgICAgICAgICAgIHByaW50KGpzb24uZHVtcHMoeyJzdWNjZXNzIjogVHJ1ZSwgIm1lc3NhZ2UiOiBmIkNvbm5lY3RlZCB0byBNYXJpYURCIHt2ZXJ9In0pKQogICAgICAgICAgICAgICAgcmV0dXJuCiAgICAgICAgICAgIGVsaWYgYWN0aW9uID09ICJsaXN0X2RicyI6CiAgICAgICAgICAgICAgICBjdXIuZXhlY3V0ZSgiU0hPVyBEQVRBQkFTRVM7IikKICAgICAgICAgICAgICAgIHJvd3MgPSBjdXIuZmV0Y2hhbGwoKQogICAgICAgICAgICAgICAgZGJzID0gW3JbbGlzdChyLmtleXMoKSlbMF1dIGZvciByIGluIHJvd3MgaWYgcltsaXN0KHIua2V5cygpKVswXV0gbm90IGluICgiaW5mb3JtYXRpb25fc2NoZW1hIiwgInBlcmZvcm1hbmNlX3NjaGVtYSIsICJzeXMiKV0KICAgICAgICAgICAgICAgIHByaW50KGpzb24uZHVtcHMoeyJzdWNjZXNzIjogVHJ1ZSwgImRhdGFiYXNlcyI6IGRic30pKQogICAgICAgICAgICAgICAgcmV0dXJuCiAgICAgICAgICAgIGVsaWYgYWN0aW9uID09ICJsaXN0X3RhYmxlcyI6CiAgICAgICAgICAgICAgICBpZiBkYjoKICAgICAgICAgICAgICAgICAgICB0cnk6CiAgICAgICAgICAgICAgICAgICAgICAgIGN1ci5leGVjdXRlKGYiU0hPVyBUQUJMRVMgRlJPTSBge2RifWA7IikKICAgICAgICAgICAgICAgICAgICAgICAgcm93cyA9IGN1ci5mZXRjaGFsbCgpCiAgICAgICAgICAgICAgICAgICAgICAgIHRhYmxlcyA9IFtyW2xpc3Qoci5rZXlzKCkpWzBdXSBmb3IgciBpbiByb3dzXQogICAgICAgICAgICAgICAgICAgICAgICBwcmludChqc29uLmR1bXBzKHsic3VjY2VzcyI6IFRydWUsICJ0YWJsZXMiOiB0YWJsZXN9KSkKICAgICAgICAgICAgICAgICAgICAgICAgcmV0dXJuCiAgICAgICAgICAgICAgICAgICAgZXhjZXB0IEV4Y2VwdGlvbjoKICAgICAgICAgICAgICAgICAgICAgICAgcHJpbnQoanNvbi5kdW1wcyh7InN1Y2Nlc3MiOiBUcnVlLCAidGFibGVzIjogW119KSkKICAgICAgICAgICAgICAgICAgICAgICAgcmV0dXJuCiAgICAgICAgICAgICAgICBlbHNlOgogICAgICAgICAgICAgICAgICAgIGN1ci5leGVjdXRlKCJTSE9XIFRBQkxFUzsiKQogICAgICAgICAgICAgICAgICAgIHJvd3MgPSBjdXIuZmV0Y2hhbGwoKQogICAgICAgICAgICAgICAgICAgIHRhYmxlcyA9IFtyW2xpc3Qoci5rZXlzKCkpWzBdXSBmb3IgciBpbiByb3dzXQogICAgICAgICAgICAgICAgICAgIHByaW50KGpzb24uZHVtcHMoeyJzdWNjZXNzIjogVHJ1ZSwgInRhYmxlcyI6IHRhYmxlc30pKQogICAgICAgICAgICAgICAgICAgIHJldHVybgogICAgICAgICAgICBlbGlmIGFjdGlvbiA9PSAic2NoZW1hIjoKICAgICAgICAgICAgICAgIHRibCA9IHBheWxvYWQuZ2V0KCJ0YWJsZSIsICIiKQogICAgICAgICAgICAgICAgaWYgZGI6CiAgICAgICAgICAgICAgICAgICAgY3VyLmV4ZWN1dGUoZiJERVNDUklCRSBge2RifWAuYHt0Ymx9YDsiKQogICAgICAgICAgICAgICAgZWxzZToKICAgICAgICAgICAgICAgICAgICBjdXIuZXhlY3V0ZShmIkRFU0NSSUJFIGB7dGJsfWA7IikKICAgICAgICAgICAgICAgIHJvd3MgPSBjdXIuZmV0Y2hhbGwoKQogICAgICAgICAgICAgICAgY29scyA9IFtdCiAgICAgICAgICAgICAgICBmb3IgciBpbiByb3dzOgogICAgICAgICAgICAgICAgICAgIGNvbHMuYXBwZW5kKHsKICAgICAgICAgICAgICAgICAgICAgICAgIm5hbWUiOiByLmdldCgiRmllbGQiLCAiIiksCiAgICAgICAgICAgICAgICAgICAgICAgICJ0eXBlIjogci5nZXQoIlR5cGUiLCAiIiksCiAgICAgICAgICAgICAgICAgICAgICAgICJpc051bGxhYmxlIjogci5nZXQoIk51bGwiLCAiWUVTIikgPT0gIllFUyIsCiAgICAgICAgICAgICAgICAgICAgICAgICJpc1ByaW1hcnlLZXkiOiByLmdldCgiS2V5IiwgIiIpID09ICJQUkkiLAogICAgICAgICAgICAgICAgICAgICAgICAiZGVmYXVsdFZhbHVlIjogc3RyKHIuZ2V0KCJEZWZhdWx0IikpIGlmIHIuZ2V0KCJEZWZhdWx0IikgaXMgbm90IE5vbmUgZWxzZSBOb25lCiAgICAgICAgICAgICAgICAgICAgfSkKICAgICAgICAgICAgICAgIHByaW50KGpzb24uZHVtcHMoeyJzdWNjZXNzIjogVHJ1ZSwgImNvbHVtbnMiOiBjb2xzfSkpCiAgICAgICAgICAgICAgICByZXR1cm4KICAgICAgICAgICAgZWxpZiBhY3Rpb24gPT0gInRhYmxlX2RhdGEiOgogICAgICAgICAgICAgICAgdGJsID0gcGF5bG9hZC5nZXQoInRhYmxlIiwgIiIpCiAgICAgICAgICAgICAgICBpZiBkYjoKICAgICAgICAgICAgICAgICAgICBjdXIuZXhlY3V0ZShmIlNFTEVDVCAqIEZST00gYHtkYn1gLmB7dGJsfWAgTElNSVQgMjAwOyIpCiAgICAgICAgICAgICAgICBlbHNlOgogICAgICAgICAgICAgICAgICAgIGN1ci5leGVjdXRlKGYiU0VMRUNUICogRlJPTSBge3RibH1gIExJTUlUIDIwMDsiKQogICAgICAgICAgICAgICAgcm93cyA9IGN1ci5mZXRjaGFsbCgpCiAgICAgICAgICAgICAgICBjb2xzID0gbGlzdChyb3dzWzBdLmtleXMoKSkgaWYgcm93cyBlbHNlIFtdCiAgICAgICAgICAgICAgICByb3dfdmFscyA9IFtdCiAgICAgICAgICAgICAgICBmb3IgciBpbiByb3dzOgogICAgICAgICAgICAgICAgICAgIHJvdyA9IFtdCiAgICAgICAgICAgICAgICAgICAgZm9yIGMgaW4gY29sczoKICAgICAgICAgICAgICAgICAgICAgICAgdmFsID0gci5nZXQoYykKICAgICAgICAgICAgICAgICAgICAgICAgaWYgdmFsIGlzIE5vbmU6CiAgICAgICAgICAgICAgICAgICAgICAgICAgICByb3cuYXBwZW5kKCJOVUxMIikKICAgICAgICAgICAgICAgICAgICAgICAgZWxpZiBpc2luc3RhbmNlKHZhbCwgKGJ5dGVzLCBieXRlYXJyYXkpKToKICAgICAgICAgICAgICAgICAgICAgICAgICAgIHJvdy5hcHBlbmQodmFsLmRlY29kZSgidXRmLTgiLCBlcnJvcnM9InJlcGxhY2UiKSkKICAgICAgICAgICAgICAgICAgICAgICAgZWxzZToKICAgICAgICAgICAgICAgICAgICAgICAgICAgIHJvdy5hcHBlbmQoc3RyKHZhbCkpCiAgICAgICAgICAgICAgICAgICAgcm93X3ZhbHMuYXBwZW5kKHJvdykKICAgICAgICAgICAgICAgIHByaW50KGpzb24uZHVtcHMoeyJzdWNjZXNzIjogVHJ1ZSwgImNvbHVtbnMiOiBjb2xzLCAicm93cyI6IHJvd192YWxzfSkpCiAgICAgICAgICAgICAgICByZXR1cm4KICAgICAgICAgICAgZWxpZiBhY3Rpb24gPT0gInF1ZXJ5IjoKICAgICAgICAgICAgICAgIHRyaW1tZWRfc3FsID0gc3FsLnN0cmlwKCkKICAgICAgICAgICAgICAgIGlmIG5vdCB0cmltbWVkX3NxbDoKICAgICAgICAgICAgICAgICAgICBwcmludChqc29uLmR1bXBzKHsic3VjY2VzcyI6IFRydWUsICJjb2x1bW5zIjogW10sICJyb3dzIjogW10sICJhZmZlY3RlZCI6IDB9KSkKICAgICAgICAgICAgICAgICAgICByZXR1cm4KICAgICAgICAgICAgICAgIGN1ci5leGVjdXRlKHRyaW1tZWRfc3FsKQogICAgICAgICAgICAgICAgY29ubi5jb21taXQoKQogICAgICAgICAgICAgICAgaWYgY3VyLmRlc2NyaXB0aW9uOgogICAgICAgICAgICAgICAgICAgIGNvbHMgPSBbZFswXSBmb3IgZCBpbiBjdXIuZGVzY3JpcHRpb25dCiAgICAgICAgICAgICAgICAgICAgcm93cyA9IGN1ci5mZXRjaGFsbCgpCiAgICAgICAgICAgICAgICAgICAgcm93X3ZhbHMgPSBbXQogICAgICAgICAgICAgICAgICAgIGZvciByIGluIHJvd3M6CiAgICAgICAgICAgICAgICAgICAgICAgIHJvdyA9IFtdCiAgICAgICAgICAgICAgICAgICAgICAgIGZvciBjIGluIGNvbHM6CiAgICAgICAgICAgICAgICAgICAgICAgICAgICB2YWwgPSByLmdldChjKQogICAgICAgICAgICAgICAgICAgICAgICAgICAgaWYgdmFsIGlzIE5vbmU6CiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgcm93LmFwcGVuZCgiTlVMTCIpCiAgICAgICAgICAgICAgICAgICAgICAgICAgICBlbGlmIGlzaW5zdGFuY2UodmFsLCAoYnl0ZXMsIGJ5dGVhcnJheSkpOgogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHJvdy5hcHBlbmQodmFsLmRlY29kZSgidXRmLTgiLCBlcnJvcnM9InJlcGxhY2UiKSkKICAgICAgICAgICAgICAgICAgICAgICAgICAgIGVsc2U6CiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgcm93LmFwcGVuZChzdHIodmFsKSkKICAgICAgICAgICAgICAgICAgICAgICAgcm93X3ZhbHMuYXBwZW5kKHJvdykKICAgICAgICAgICAgICAgICAgICBwcmludChqc29uLmR1bXBzKHsic3VjY2VzcyI6IFRydWUsICJjb2x1bW5zIjogY29scywgInJvd3MiOiByb3dfdmFscywgImFmZmVjdGVkIjogbGVuKHJvd192YWxzKX0pKQogICAgICAgICAgICAgICAgZWxzZToKICAgICAgICAgICAgICAgICAgICBwcmludChqc29uLmR1bXBzKHsic3VjY2VzcyI6IFRydWUsICJjb2x1bW5zIjogW10sICJyb3dzIjogW10sICJhZmZlY3RlZCI6IGN1ci5yb3djb3VudCBpZiBjdXIucm93Y291bnQgPj0gMCBlbHNlIDB9KSkKICAgICAgICAgICAgICAgIHJldHVybgogICAgICAgICAgICBlbGlmIGFjdGlvbiA9PSAibGlzdF91c2VycyI6CiAgICAgICAgICAgICAgICBjdXIuZXhlY3V0ZSgiU0VMRUNUIFVzZXIsIEhvc3QgRlJPTSBteXNxbC51c2VyOyIpCiAgICAgICAgICAgICAgICByb3dzID0gY3VyLmZldGNoYWxsKCkKICAgICAgICAgICAgICAgIHVzZXJzID0gW3sidXNlcm5hbWUiOiByLmdldCgiVXNlciIsICIiKSwgImhvc3QiOiByLmdldCgiSG9zdCIsICIlIiksICJwcml2aWxlZ2VzIjogWyJBTEwgUFJJVklMRUdFUyJdfSBmb3IgciBpbiByb3dzIGlmIHIuZ2V0KCJVc2VyIildCiAgICAgICAgICAgICAgICBwcmludChqc29uLmR1bXBzKHsic3VjY2VzcyI6IFRydWUsICJ1c2VycyI6IHVzZXJzfSkpCiAgICAgICAgICAgICAgICByZXR1cm4KICAgIGV4Y2VwdCBFeGNlcHRpb24gYXMgZToKICAgICAgICBwcmludChqc29uLmR1bXBzKHsic3VjY2VzcyI6IEZhbHNlLCAiZXJyb3IiOiBzdHIoZSl9KSkKCmlmIF9fbmFtZV9fID09ICJfX21haW5fXyI6CiAgICBydW4oKQ=="

    private fun executeQueryRunner(payload: JSONObject): JSONObject? {
        return try {
            val pythonBin = listOf(
                File(context.filesDir, "runtimes/python/bin/python3"),
                File(context.filesDir, "runtimes/python/bin/python3.14"),
                File(context.filesDir, "runtimes/python/bin/python")
            ).firstOrNull { it.exists() && it.canExecute() } ?: listOf(
                File(context.filesDir, "runtimes/python/bin/python3"),
                File(context.filesDir, "runtimes/python/bin/python3.14"),
                File(context.filesDir, "runtimes/python/bin/python")
            ).firstOrNull { it.exists() } ?: return null

            val mariadbDir = File(context.filesDir, "runtimes/mariadb").apply { mkdirs() }
            val queryScript = File(mariadbDir, "db_query.py")
            val decoded = String(Base64.decode(scriptB64, Base64.DEFAULT), StandardCharsets.UTF_8)
            if (!queryScript.exists() || queryScript.readText() != decoded) {
                queryScript.writeText(decoded)
                queryScript.setExecutable(true, false)
            }
            payload.put("sock", File(context.filesDir, "runtimes/mariadb/mariadb.sock").absolutePath)
            val pb = ProcessBuilder(pythonBin.absolutePath, queryScript.absolutePath)
            val env = pb.environment()
            env["LD_LIBRARY_PATH"] = "${context.filesDir.absolutePath}/runtimes/python/lib:${context.filesDir.absolutePath}/runtimes/mariadb/lib"
            env["PYTHONPATH"] = "${context.filesDir.absolutePath}/runtimes/python/lib/python3.14/site-packages:${context.filesDir.absolutePath}/runtimes/python/lib/python3.12/site-packages"
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
