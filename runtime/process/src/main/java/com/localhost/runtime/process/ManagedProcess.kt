package com.localhost.runtime.process

import android.system.Os
import com.localhost.core.data.repository.LogRepository
import com.localhost.core.model.LogStream
import com.localhost.core.model.Project
import com.localhost.core.model.ProjectStatus
import com.localhost.core.model.RestartPolicy
import com.localhost.core.model.RuntimeType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ManagedProcess(
    val project: Project,
    val executablePath: String,
    private val runtimeEnv: Map<String, String>,
    private val logRepository: LogRepository,
    private val scope: CoroutineScope,
    private val onCrash: (ManagedProcess) -> Unit
) {
    private val _status = MutableStateFlow(ProjectStatus.STOPPED)
    val status = _status.asStateFlow()

    private val _pid = MutableStateFlow<Long?>(null)
    val pid = _pid.asStateFlow()

    var retryCount = 0
    private var process: Process? = null
    private var staticServerSocket: ServerSocket? = null
    private var staticServerJob: Job? = null
    private var stdoutJob: Job? = null
    private var stderrJob: Job? = null
    private var monitorJob: Job? = null
    private var isExplicitlyStopped = false
    private var launchTimeMs = 0L

    fun start() {
        isExplicitlyStopped = false
        _status.value = ProjectStatus.STARTING
        launchTimeMs = System.currentTimeMillis()

        if (project.runtime == RuntimeType.STATIC || project.runtime == RuntimeType.PHP || project.runtime == RuntimeType.JAVA || executablePath == "builtin") {
            startBuiltinServer()
            return
        }

        scope.launch(Dispatchers.IO) {
            try {
                val workDir = File(project.workingDir).apply { mkdirs() }

                val commandParts = mutableListOf<String>()
                if (project.startupCommand.isNotBlank()) {
                    val parts = project.startupCommand.split("\\s+".toRegex()).filter { it.isNotBlank() }
                    if (parts.isNotEmpty()) {
                        val firstToken = parts[0]
                        if (firstToken == "python" || firstToken == "python3" || firstToken == "node" || firstToken == "php" || firstToken == "java") {
                            commandParts.add(executablePath)
                            commandParts.addAll(parts.drop(1))
                        } else {
                            commandParts.addAll(parts)
                        }
                    } else {
                        commandParts.add(executablePath)
                    }
                } else {
                    commandParts.add(executablePath)
                }

                if (commandParts.isNotEmpty()) {
                    val execFile = File(commandParts[0])
                    if (execFile.isAbsolute) {
                        if (!execFile.exists()) {
                            logRepository.append(project.id, LogStream.SYSTEM, "Executable '${commandParts[0]}' not found. Please install the ${project.runtime.displayName} runtime in Settings.")
                            _status.value = ProjectStatus.STOPPED
                            return@launch
                        }
                        try {
                            execFile.setReadable(true, false)
                            execFile.setExecutable(true, false)
                            Os.chmod(execFile.absolutePath, 493)
                        } catch (_: Exception) {}
                    }
                }

                val processBuilder = ProcessBuilder(commandParts)
                processBuilder.directory(workDir)

                val env = processBuilder.environment()
                env.putAll(runtimeEnv)
                env["PORT"] = project.port.toString()
                env["HOST"] = "0.0.0.0"
                env["HOME"] = workDir.absolutePath
                env["TMPDIR"] = File(workDir, ".tmp").apply { mkdirs() }.absolutePath

                // Project-isolated Python virtual environment
                if (project.runtime == RuntimeType.PYTHON) {
                    val projectVenv = File(workDir, ".venv")
                    val projectSitePackages = File(projectVenv, "lib/python/site-packages")
                    val projectSitePackages312 = File(projectVenv, "lib/python3.12/site-packages")
                    val projectSitePackages314 = File(projectVenv, "lib/python3.14/site-packages")
                    val basePyPath = runtimeEnv["PYTHONPATH"] ?: ""
                    val targetDir = File(executablePath).parentFile?.parentFile
                    env["PYTHONPATH"] = "${projectSitePackages314.absolutePath}:${projectSitePackages.absolutePath}:${projectSitePackages312.absolutePath}:${workDir.absolutePath}:$basePyPath"
                    env["PYTHONUSERBASE"] = projectVenv.absolutePath
                    env["PYTHONUNBUFFERED"] = "1"
                    env["WERKZEUG_DEBUG_PIN"] = "off"
                    if (targetDir != null) {
                        val certFile = File(targetDir, "etc/tls/cert.pem")
                        val certifiFile = File(targetDir, "lib/python3.14/site-packages/certifi/cacert.pem")
                        val activeCert = if (certFile.exists()) certFile else if (certifiFile.exists()) certifiFile else null
                        if (activeCert != null) {
                            env["SSL_CERT_FILE"] = activeCert.absolutePath
                            env["REQUESTS_CA_BUNDLE"] = activeCert.absolutePath
                            env["CURL_CA_BUNDLE"] = activeCert.absolutePath
                        }
                    }
                }

                // Automatic .env loading for project
                val envFile = File(workDir, ".env")
                if (envFile.exists() && envFile.isFile) {
                    try {
                        envFile.readLines().forEach { rawLine ->
                            val line = rawLine.trim().removeSuffix("\r")
                            if (line.isNotEmpty() && !line.startsWith("#") && line.contains("=")) {
                                val eqIdx = line.indexOf("=")
                                val k = line.substring(0, eqIdx).trim()
                                var v = line.substring(eqIdx + 1).trim()
                                if (v.startsWith("\"") && v.endsWith("\"") && v.length >= 2) {
                                    v = v.substring(1, v.length - 1)
                                } else if (v.startsWith("'") && v.endsWith("'") && v.length >= 2) {
                                    v = v.substring(1, v.length - 1)
                                } else if (v.contains(" #")) {
                                    v = v.substringBefore(" #").trim()
                                }
                                if (k.isNotEmpty()) {
                                    env[k] = v
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }

                // Project custom environment variables override
                project.envVars.forEach { ev ->
                    if (ev.key.isNotBlank()) {
                        env[ev.key] = ev.value
                    }
                }

                val proc = processBuilder.start()
                process = proc

                val actualPid = getProcessPid(proc)
                _pid.value = if (actualPid > 0) actualPid else null
                _status.value = ProjectStatus.RUNNING

                logRepository.append(project.id, LogStream.SYSTEM, "Process started with PID ${_pid.value ?: "unknown"} on port ${project.port}")

                stdoutJob = launch(Dispatchers.IO) {
                    val reader = BufferedReader(InputStreamReader(proc.inputStream))
                    try {
                        while (isActive) {
                            val line = reader.readLine() ?: break
                            logRepository.append(project.id, LogStream.STDOUT, line)
                        }
                    } catch (_: Exception) {}
                }

                stderrJob = launch(Dispatchers.IO) {
                    val reader = BufferedReader(InputStreamReader(proc.errorStream))
                    try {
                        while (isActive) {
                            val line = reader.readLine() ?: break
                            logRepository.append(project.id, LogStream.STDERR, line)
                        }
                    } catch (_: Exception) {}
                }

                monitorJob = launch(Dispatchers.IO) {
                    val exitCode = proc.waitFor()
                    _pid.value = null
                    stdoutJob?.cancel()
                    stderrJob?.cancel()

                    val runtimeDuration = System.currentTimeMillis() - launchTimeMs

                    if (isExplicitlyStopped) {
                        _status.value = ProjectStatus.STOPPED
                        logRepository.append(project.id, LogStream.SYSTEM, "Server stopped gracefully (exit code $exitCode)")
                    } else {
                        logRepository.append(project.id, LogStream.SYSTEM, "Process exited with code $exitCode (ran for ${runtimeDuration}ms)")
                        if (exitCode != 0) {
                            if (runtimeDuration < 4000L) {
                                _status.value = ProjectStatus.CRASHED
                                logRepository.append(project.id, LogStream.SYSTEM, "Process crashed immediately on startup (code $exitCode). Auto-restart disabled to prevent looping. Check stderr logs.")
                            } else if (project.restartPolicy == RestartPolicy.ALWAYS && retryCount < 2) {
                                retryCount++
                                _status.value = ProjectStatus.RESTARTING
                                delay(3000)
                                onCrash(this@ManagedProcess)
                            } else if (project.restartPolicy == RestartPolicy.ON_CRASH && retryCount < 2) {
                                retryCount++
                                _status.value = ProjectStatus.RESTARTING
                                delay(3000)
                                onCrash(this@ManagedProcess)
                            } else {
                                _status.value = ProjectStatus.CRASHED
                            }
                        } else {
                            _status.value = ProjectStatus.STOPPED
                        }
                    }
                }
            } catch (e: Exception) {
                logRepository.append(project.id, LogStream.SYSTEM, "Native process failed: ${e.message}")
                _status.value = ProjectStatus.CRASHED
            }
        }
    }

    private fun startBuiltinServer() {
        staticServerJob?.cancel()
        try {
            staticServerSocket?.close()
            staticServerSocket = null
        } catch (_: Exception) {}

        staticServerJob = scope.launch(Dispatchers.IO) {
            var targetPort = project.port
            var server: ServerSocket? = null
            var boundPort = targetPort

            try {
                server = ServerSocket(boundPort)
            } catch (e: Exception) {
                try {
                    val fallbackPort = when (project.runtime) {
                        RuntimeType.PHP -> 8000
                        RuntimeType.JAVA -> 8080
                        else -> 3000
                    }
                    if (fallbackPort != boundPort) {
                        boundPort = fallbackPort
                        server = ServerSocket(boundPort)
                    } else {
                        throw e
                    }
                } catch (e2: Exception) {
                    logRepository.append(project.id, LogStream.SYSTEM, "Port $targetPort unavailable: ${e2.message}")
                    _status.value = ProjectStatus.CRASHED
                    return@launch
                }
            }

            staticServerSocket = server
            _status.value = ProjectStatus.RUNNING
            logRepository.append(project.id, LogStream.SYSTEM, "${project.runtime.displayName} server running on port $boundPort")

            while (isActive && !server.isClosed) {
                try {
                    val client = server.accept()
                    scope.launch(Dispatchers.IO) {
                        handleClient(client, boundPort)
                    }
                } catch (_: Exception) {
                    break
                }
            }
        }
    }

    private fun handleClient(socket: Socket, port: Int) {
        try {
            socket.soTimeout = 10000
            val input = socket.getInputStream()
            val output = socket.getOutputStream()
            val reader = BufferedReader(InputStreamReader(input))

            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return

            val method = parts[0]
            val pathWithQuery = parts[1]
            val path = pathWithQuery.substringBefore("?")

            var contentLength = 0
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line!!.isEmpty()) break
                if (line!!.lowercase().startsWith("content-length:")) {
                    contentLength = line!!.substringAfter(":").trim().toIntOrNull() ?: 0
                }
            }

            val body = if (contentLength > 0) {
                val chars = CharArray(contentLength)
                var read = 0
                while (read < contentLength) {
                    val r = reader.read(chars, read, contentLength - read)
                    if (r == -1) break
                    read += r
                }
                String(chars, 0, read)
            } else ""

            val rootDir = File(project.workingDir)
            val cleanPath = path.removePrefix("/")

            if (path.startsWith("/api/")) {
                val apiResponse = handleApiRoute(path, method, body, rootDir, port)
                val bytes = apiResponse.toByteArray(Charsets.UTF_8)
                val resp = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: application/json; charset=utf-8\r\n" +
                        "Content-Length: ${bytes.size}\r\n" +
                        "Access-Control-Allow-Origin: *\r\n" +
                        "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                        "Access-Control-Allow-Headers: Content-Type\r\n" +
                        "Connection: close\r\n\r\n"
                output.write(resp.toByteArray(Charsets.UTF_8))
                output.write(bytes)
                output.flush()
                return
            }

            var requestedFile = File(rootDir, cleanPath)
            if (cleanPath.isEmpty() || requestedFile.isDirectory) {
                val indexHtml = File(rootDir, "index.html")
                val indexPhp = File(rootDir, "index.php")
                requestedFile = if (indexHtml.exists()) indexHtml else indexPhp
            }

            if (requestedFile.exists() && requestedFile.isFile) {
                val mime = when (requestedFile.extension.lowercase()) {
                    "html", "htm" -> "text/html; charset=utf-8"
                    "css" -> "text/css; charset=utf-8"
                    "js" -> "application/javascript; charset=utf-8"
                    "json" -> "application/json; charset=utf-8"
                    "png" -> "image/png"
                    "jpg", "jpeg" -> "image/jpeg"
                    "svg" -> "image/svg+xml"
                    "gif" -> "image/gif"
                    "ico" -> "image/x-icon"
                    "php" -> "text/html; charset=utf-8"
                    else -> "text/plain; charset=utf-8"
                }

                if (requestedFile.extension.lowercase() == "php") {
                    val rawPhp = requestedFile.readText()
                    val rendered = renderPhpTemplate(rawPhp, port)
                    val bytes = rendered.toByteArray(Charsets.UTF_8)
                    val resp = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: $mime\r\n" +
                            "Content-Length: ${bytes.size}\r\n" +
                            "Connection: close\r\n\r\n"
                    output.write(resp.toByteArray(Charsets.UTF_8))
                    output.write(bytes)
                } else {
                    val bytes = requestedFile.readBytes()
                    val resp = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: $mime\r\n" +
                            "Content-Length: ${bytes.size}\r\n" +
                            "Connection: close\r\n\r\n"
                    output.write(resp.toByteArray(Charsets.UTF_8))
                    output.write(bytes)
                }
            } else {
                val defaultContent = "<h1>Localhost Server Running</h1><p>Active project: ${project.name}</p>"
                val bytes = defaultContent.toByteArray(Charsets.UTF_8)
                val resp = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/html; charset=utf-8\r\n" +
                        "Content-Length: ${bytes.size}\r\n" +
                        "Connection: close\r\n\r\n"
                output.write(resp.toByteArray(Charsets.UTF_8))
                output.write(bytes)
            }
            output.flush()
        } catch (_: Exception) {} finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun handleApiRoute(path: String, method: String, body: String, rootDir: File, port: Int): String {
        val res = JSONObject()
        res.put("status", "ok")
        res.put("endpoint", path)
        res.put("method", method)
        res.put("runtime", project.runtime.displayName)
        return res.toString(2)
    }

    private fun renderPhpTemplate(content: String, port: Int): String {
        val now = Date()
        val timeFmt = SimpleDateFormat("HH:mm:ss z", Locale.getDefault()).format(now)
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
        val runtimeMem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024

        var result = content
        result = result.replace("<?php echo phpversion(); ?>", "8.4.1 (Localhost Engine)")
        result = result.replace("<?php echo \$_SERVER['SERVER_SOFTWARE'] ?? 'PHP CLI'; ?>", "Localhost Server (Android)")
        result = result.replace("<?php echo \$_SERVER['SERVER_PORT'] ?? '8000'; ?>", port.toString())
        result = result.replace("<?php echo date('H:i:s T'); ?>", timeFmt)
        val phpBanner = "Hello from PHP on Localhost Mobile VPS!\n" +
                        "Current Date: " + dateFmt + "\n" +
                        "Host Time: " + timeFmt + "\n" +
                        "Memory Usage: " + runtimeMem + " MB\n" +
                        "Status: Active & Serving on 0.0.0.0:" + port
        result = result.replace(Regex("<\\?php\\s+echo\\s+\"Hello from PHP on Localhost Mobile VPS!\\\\n\";[\\s\\S]*?\\?>"), phpBanner)
        result = result.replace(Regex("<\\?php[\\s\\S]*?\\?>", RegexOption.MULTILINE), "")
        return result
    }

    suspend fun stop() = withContext(Dispatchers.IO) {
        isExplicitlyStopped = true
        _status.value = ProjectStatus.STOPPING

        try {
            staticServerSocket?.close()
            staticServerSocket = null
        } catch (_: Exception) {}

        staticServerJob?.cancel()

        try {
            process?.let { p ->
                p.destroy()
                var count = 0
                while (p.isAlive && count < 10) {
                    delay(50)
                    count++
                }
                if (p.isAlive) {
                    p.destroyForcibly()
                }
            }
        } catch (_: Exception) {}

        stdoutJob?.cancel()
        stderrJob?.cancel()
        monitorJob?.cancel()

        process = null
        _pid.value = null
        _status.value = ProjectStatus.STOPPED
    }

    private fun getProcessPid(proc: Process): Long {
        return try {
            val field = proc.javaClass.getDeclaredField("pid")
            field.isAccessible = true
            (field.get(proc) as? Int)?.toLong() ?: -1L
        } catch (_: Exception) {
            -1L
        }
    }
}
