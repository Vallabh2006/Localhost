package com.localhost.runtime.manager

import android.content.Context
import android.os.Build
import com.localhost.core.common.FileUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MariaDBManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    val runtimeDir: File
        get() = File(context.filesDir, "runtimes/mariadb").apply { mkdirs() }

    val binDir: File
        get() = File(runtimeDir, "bin").apply { mkdirs() }

    val dataDir: File
        get() = File(runtimeDir, "data").apply { mkdirs() }

    val tmpDir: File
        get() = File(runtimeDir, "tmp").apply { mkdirs() }

    val shareDir: File
        get() = File(runtimeDir, "share/mariadb")

    val pluginDir: File
        get() = File(runtimeDir, "lib/mysql/plugin")

    val mariadbdFile: File
        get() = File(binDir, "mariadbd")

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>("MariaDB Service Idle")
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private var activeProcess: Process? = null

    init {
        _isServiceRunning.value = checkIsPortOpen()
    }

    fun isInstalled(): Boolean {
        val hasBinary = mariadbdFile.exists() && mariadbdFile.canExecute()
        val hasData = File(dataDir, "mysql").exists()
        return hasBinary && hasData
    }

    fun checkIsPortOpen(): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress("127.0.0.1", 3306), 350)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun installMariaDB(): Result<Unit> = withContext(Dispatchers.IO) {
        if (isInstalled()) {
            _statusMessage.value = "MariaDB already installed"
            return@withContext Result.success(Unit)
        }

        _isDownloading.value = true
        _downloadProgress.value = 0.05f
        _statusMessage.value = "Preparing MariaDB runtime download..."

        try {
            val arch = if (Build.SUPPORTED_ABIS.firstOrNull()?.contains("x86_64") == true) "x86_64" else "aarch64"
            val debs = listOf(
                "https://packages.termux.dev/apt/termux-main/pool/main/m/mariadb/mariadb_2%3A13.0.2_${arch}.deb",
                "https://packages.termux.dev/apt/termux-main/pool/main/libc/libcrypt/libcrypt_0.2-6_${arch}.deb"
            )

            runtimeDir.mkdirs()
            binDir.mkdirs()
            dataDir.mkdirs()
            tmpDir.mkdirs()

            var step = 0
            for (url in debs) {
                step++
                _statusMessage.value = "Downloading MariaDB package $step of ${debs.size}..."
                val tempDeb = File(context.cacheDir, "mariadb_pkg_$step.deb")
                val req = Request.Builder().url(url).build()
                okHttpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        throw Exception("HTTP ${resp.code} downloading $url")
                    }
                    val body = resp.body ?: throw Exception("Empty body for $url")
                    val total = body.contentLength()
                    var downloaded = 0L
                    body.byteStream().use { input ->
                        FileOutputStream(tempDeb).use { output ->
                            val buf = ByteArray(32768)
                            var r: Int
                            while (input.read(buf).also { r = it } != -1) {
                                output.write(buf, 0, r)
                                downloaded += r
                                if (total > 0) {
                                    val pct = (downloaded.toFloat() / total.toFloat()) * (0.4f / debs.size)
                                    _downloadProgress.value = 0.05f + ((step - 1) * 0.4f) + pct
                                }
                            }
                        }
                    }
                }

                _statusMessage.value = "Extracting MariaDB package $step..."
                FileUtils.extractDeb(tempDeb, runtimeDir)
                tempDeb.delete()
            }

            _statusMessage.value = "Configuring MariaDB files..."
            _downloadProgress.value = 0.85f

            val termuxUsr = File(runtimeDir, "data/data/com.termux/files/usr")
            if (termuxUsr.exists() && termuxUsr.isDirectory) {
                termuxUsr.walkTopDown().forEach { file ->
                    val rel = file.relativeTo(termuxUsr).path
                    if (rel.isNotEmpty()) {
                        val dest = File(runtimeDir, rel)
                        if (file.isDirectory) {
                            dest.mkdirs()
                        } else {
                            dest.parentFile?.mkdirs()
                            file.copyTo(dest, overwrite = true)
                        }
                    }
                }
                File(runtimeDir, "data").deleteRecursively()
                dataDir.mkdirs()
            }

            binDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    file.setExecutable(true, false)
                    file.setReadable(true, false)
                }
            }

            val cryptInPython = File(context.filesDir, "runtimes/python/lib/libcrypt.so")
            val cryptInMaria = File(runtimeDir, "lib/libcrypt.so")
            if (cryptInMaria.exists() && !cryptInPython.exists()) {
                cryptInPython.parentFile?.mkdirs()
                cryptInMaria.copyTo(cryptInPython, overwrite = true)
            }

            if (!File(dataDir, "mysql").exists()) {
                _statusMessage.value = "Initializing MariaDB system tables..."
                _downloadProgress.value = 0.92f
                bootstrapDataDirectory()
            }

            _downloadProgress.value = 1.0f
            _statusMessage.value = "MariaDB 13.0 installed and ready"
            _isDownloading.value = false
            Result.success(Unit)
        } catch (e: Throwable) {
            _isDownloading.value = false
            _statusMessage.value = "MariaDB install failed: ${e.message}"
            Result.failure(e)
        }
    }

    private fun bootstrapDataDirectory() {
        val sysTables = File(shareDir, "mariadb_system_tables.sql")
        val sysData = File(shareDir, "mariadb_system_tables_data.sql")
        val sqlBuilder = java.lang.StringBuilder()
        sqlBuilder.append("CREATE DATABASE IF NOT EXISTS mysql;\nUSE mysql;\n")
        if (sysTables.exists()) sqlBuilder.append(sysTables.readText()).append("\n")
        if (sysData.exists()) sqlBuilder.append(sysData.readText()).append("\n")

        val pb = ProcessBuilder(
            mariadbdFile.absolutePath,
            "--no-defaults",
            "--bootstrap",
            "--basedir=${runtimeDir.absolutePath}",
            "--datadir=${dataDir.absolutePath}",
            "--tmpdir=${tmpDir.absolutePath}",
            "--lc-messages-dir=${shareDir.absolutePath}",
            "--plugin-dir=${pluginDir.absolutePath}"
        )
        val env = pb.environment()
        env["LD_LIBRARY_PATH"] = "${runtimeDir.absolutePath}/lib:${context.filesDir.absolutePath}/runtimes/python/lib:${context.applicationInfo.nativeLibraryDir}"

        val proc = pb.start()
        proc.outputStream.bufferedWriter().use { writer ->
            writer.write(sqlBuilder.toString())
            writer.flush()
        }
        proc.waitFor(45, TimeUnit.SECONDS)
    }

    suspend fun startService(): Result<Unit> = withContext(Dispatchers.IO) {
        if (checkIsPortOpen()) {
            _isServiceRunning.value = true
            _statusMessage.value = "MariaDB is already running on 127.0.0.1:3306"
            return@withContext Result.success(Unit)
        }

        if (!isInstalled()) {
            val installRes = installMariaDB()
            if (installRes.isFailure) {
                return@withContext Result.failure(Exception("MariaDB is not installed: ${installRes.exceptionOrNull()?.message}"))
            }
        }

        try {
            binDir.walkTopDown().forEach { file ->
                if (file.isFile) file.setExecutable(true, false)
            }

            val pb = ProcessBuilder(
                mariadbdFile.absolutePath,
                "--no-defaults",
                "--basedir=${runtimeDir.absolutePath}",
                "--datadir=${dataDir.absolutePath}",
                "--tmpdir=${tmpDir.absolutePath}",
                "--lc-messages-dir=${shareDir.absolutePath}",
                "--plugin-dir=${pluginDir.absolutePath}",
                "--port=3306",
                "--bind-address=127.0.0.1",
                "--socket=${runtimeDir.absolutePath}/mariadb.sock"
            )
            val env = pb.environment()
            env["LD_LIBRARY_PATH"] = "${runtimeDir.absolutePath}/lib:${context.filesDir.absolutePath}/runtimes/python/lib:${context.applicationInfo.nativeLibraryDir}"

            val logFile = File(runtimeDir, "mariadb.log")
            try {
                pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile))
                pb.redirectError(ProcessBuilder.Redirect.appendTo(logFile))
            } catch (_: Throwable) {}

            activeProcess = pb.start()

            var attempts = 0
            while (attempts < 20) {
                kotlinx.coroutines.delay(250)
                if (checkIsPortOpen()) {
                    _isServiceRunning.value = true
                    _statusMessage.value = "MariaDB Server running on 127.0.0.1:3306"
                    return@withContext Result.success(Unit)
                }
                attempts++
            }

            if (checkIsPortOpen()) {
                _isServiceRunning.value = true
                _statusMessage.value = "MariaDB Server running on 127.0.0.1:3306"
                Result.success(Unit)
            } else {
                _isServiceRunning.value = false
                _statusMessage.value = "Failed to bind MariaDB on port 3306"
                Result.failure(Exception("MariaDB process started but port 3306 is not listening"))
            }
        } catch (e: Throwable) {
            _isServiceRunning.value = false
            _statusMessage.value = "Failed to start MariaDB: ${e.message}"
            Result.failure(e)
        }
    }

    suspend fun stopService(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            activeProcess?.destroy()
            activeProcess = null

            try {
                Runtime.getRuntime().exec(arrayOf("sh", "-c", "pkill -15 mariadbd || pkill -9 mariadbd")).waitFor(2, TimeUnit.SECONDS)
            } catch (_: Exception) {}

            var attempts = 0
            while (attempts < 15 && checkIsPortOpen()) {
                kotlinx.coroutines.delay(200)
                attempts++
            }

            _isServiceRunning.value = false
            _statusMessage.value = "MariaDB Server stopped"
            Result.success(Unit)
        } catch (e: Throwable) {
            _statusMessage.value = "Error stopping MariaDB: ${e.message}"
            Result.failure(e)
        }
    }

    suspend fun uninstall(): Boolean = withContext(Dispatchers.IO) {
        try {
            stopService()
            runtimeDir.deleteRecursively()
            _isServiceRunning.value = false
            _statusMessage.value = "MariaDB uninstalled"
            true
        } catch (_: Throwable) {
            false
        }
    }
}
