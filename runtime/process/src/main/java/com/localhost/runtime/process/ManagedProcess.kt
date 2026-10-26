package com.localhost.runtime.process

import android.system.Os
import android.util.Log
import com.localhost.core.data.repository.LogRepository
import com.localhost.core.model.LogStream
import com.localhost.core.model.Project
import com.localhost.core.model.ProjectStatus
import com.localhost.core.model.RestartPolicy
import com.localhost.core.model.RuntimeType
import com.localhost.core.model.RequestRecord
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
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class PhpExecutionResult(
    val statusCode: Int = 200,
    val statusText: String = "OK",
    val headers: List<Pair<String, String>> = emptyList(),
    val body: ByteArray = ByteArray(0)
)

class ManagedProcess(
    val project: Project,
    val executablePath: String,
    private val runtimeEnv: Map<String, String>,
    private val logRepository: LogRepository,
    private val scope: CoroutineScope,
    private val onRequestRecord: ((RequestRecord) -> Unit)? = null,
    private val isVpnBlocked: ((String) -> Boolean)? = null,
    private val getVpnBlockedMessage: (() -> String)? = null,
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
    @Volatile
    private var isExplicitlyStopped = false
    private var launchTimeMs = 0L
    private var vpnProxyServerSocket: ServerSocket? = null
    private var vpnProxyJob: Job? = null
    private var internalPort: Int = 0

    fun start() {
        isExplicitlyStopped = false
        _status.value = ProjectStatus.STARTING
        launchTimeMs = System.currentTimeMillis()

        if (project.runtime == RuntimeType.STATIC || project.runtime == RuntimeType.PHP || executablePath == "builtin") {
            startBuiltinServer()
            return
        }

        scope.launch(Dispatchers.IO) {
            try {
                val workDir = File(project.workingDir).apply { mkdirs() }

                val preferredPort = if (project.port <= 55535) project.port + 10000 else project.port - 10000
                internalPort = try {
                    ServerSocket(preferredPort).use { preferredPort }
                } catch (_: Exception) {
                    ServerSocket(0).use { it.localPort }
                }
                startVpnProxy(publicPort = project.port, backendPort = internalPort)

                val commandParts = mutableListOf<String>()
                if (project.runtime == RuntimeType.PHP && (project.startupCommand.isBlank() || project.startupCommand == "builtin")) {
                    val docRoot = if (File(workDir, "public").isDirectory && (File(workDir, "public/index.php").exists() || File(workDir, "public/index.html").exists())) {
                        File(workDir, "public").absolutePath
                    } else {
                        workDir.absolutePath
                    }
                    val tmpDir = File(workDir, ".tmp").apply { mkdirs() }
                    val phpArgs = mutableListOf(
                        executablePath,
                        "-d", "max_execution_time=0",
                        "-d", "memory_limit=512M",
                        "-d", "sys_temp_dir=${tmpDir.absolutePath}",
                        "-d", "upload_tmp_dir=${tmpDir.absolutePath}",
                        "-d", "session.save_path=${tmpDir.absolutePath}",
                        "-d", "display_errors=1",
                        "-S", "0.0.0.0:${project.port}",
                        "-t", docRoot
                    )
                    val router = listOf("router.php", "server.php").map { File(workDir, it) }.firstOrNull { it.exists() }
                    if (router != null) {
                        phpArgs.add(router.absolutePath)
                    }
                    commandParts.addAll(phpArgs)
                } else if (project.startupCommand.isNotBlank()) {
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

                if (project.runtime == RuntimeType.PYTHON && commandParts.size >= 2 && (commandParts[0] == "python" || commandParts[0] == "python3" || commandParts[0] == executablePath) && commandParts[1] == "app.py" && !File(workDir, "app.py").exists()) {
                    val pyCandidates = listOf("main.py", "server.py", "run.py", "manage.py", "index.py", "wsgi.py")
                    val foundPy = pyCandidates.firstOrNull { File(workDir, it).exists() }
                    if (foundPy != null) {
                        commandParts[1] = foundPy
                    }
                }

                if (project.runtime == RuntimeType.NODEJS && commandParts.size >= 2 && (commandParts[0] == "node" || commandParts[0] == executablePath) && commandParts[1] == "server.js" && !File(workDir, "server.js").exists()) {
                    var nodeEntry: String? = null
                    val pkgJson = File(workDir, "package.json")
                    if (pkgJson.exists()) {
                        try {
                            val json = JSONObject(pkgJson.readText())
                            val mainFile = json.optString("main", "")
                            if (mainFile.isNotBlank() && File(workDir, mainFile).exists()) {
                                nodeEntry = mainFile
                            }
                        } catch (_: Exception) {}
                    }
                    if (nodeEntry == null) {
                        val jsCandidates = listOf("index.js", "app.js", "main.js", "src/index.js", "src/app.js", "src/server.js")
                        nodeEntry = jsCandidates.firstOrNull { File(workDir, it).exists() }
                    }
                    if (nodeEntry != null) {
                        commandParts[1] = nodeEntry
                    }
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

                val finalCommand = mutableListOf<String>()
                if (project.runtime == RuntimeType.PYTHON && commandParts.isNotEmpty() && (commandParts[0] == "python" || commandParts[0] == "python3" || commandParts[0] == executablePath)) {
                    val projectVenv = listOf(
                        File(workDir, ".venv"),
                        File(workDir, "myenv"),
                        File(workDir, "venv")
                    ).firstOrNull { File(it, "bin/python").exists() }
                    val venvPy = projectVenv?.let { File(it, "bin/python") }
                    val pyExec = if (venvPy != null && venvPy.exists()) venvPy.absolutePath else executablePath
                    finalCommand.add(pyExec)
                    finalCommand.addAll(commandParts.drop(1).map { it.trim() }.filter { it.isNotBlank() })
                } else {
                    finalCommand.addAll(commandParts.map { it.trim() }.filter { it.isNotBlank() })
                }

                val mappedCommand = finalCommand.map { part ->
                    when {
                        part == project.port.toString() -> internalPort.toString()
                        part.contains(":${project.port}") -> part.replace(":${project.port}", ":$internalPort")
                        part.contains("=${project.port}") -> part.replace("=${project.port}", "=$internalPort")
                        else -> part
                    }
                }
                val processBuilder = ProcessBuilder(mappedCommand)
                processBuilder.directory(workDir)

                val tmpDir = File(workDir, ".tmp").apply { mkdirs() }
                val env = processBuilder.environment()
                env.putAll(runtimeEnv)
                env["PORT"] = internalPort.toString()
                env["INTERNAL_PORT"] = internalPort.toString()
                env["PUBLIC_PORT"] = project.port.toString()
                env["HOST"] = "0.0.0.0"
                env["HOME"] = workDir.absolutePath
                env["TMPDIR"] = tmpDir.absolutePath
                env["TEMP"] = tmpDir.absolutePath
                env["TMP"] = tmpDir.absolutePath

                val dotEnvFile = File(workDir, ".env")
                if (dotEnvFile.exists()) {
                    try {
                        dotEnvFile.readLines().forEach { rawLine ->
                            val line = rawLine.trim()
                            if (line.isNotEmpty() && !line.startsWith("#") && line.contains("=")) {
                                val key = line.substringBefore("=").trim()
                                val value = line.substringAfter("=").trim().removeSurrounding("\"").removeSurrounding("'")
                                if (key.isNotEmpty()) {
                                    env[key] = value
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }

                if (project.runtime == RuntimeType.PYTHON) {
                    val projectVenv = listOf(
                        File(workDir, ".venv"),
                        File(workDir, "myenv"),
                        File(workDir, "venv")
                    ).firstOrNull { File(it, "bin/python").exists() } ?: File(workDir, ".venv")
                    val venvBin = File(projectVenv, "bin")
                    val curPath = env["PATH"] ?: (System.getenv("PATH") ?: "/system/bin")
                    env["PATH"] = "${venvBin.absolutePath}:$curPath"
                    env["VIRTUAL_ENV"] = projectVenv.absolutePath
                    val projectSitePackages = File(projectVenv, "lib/python/site-packages")
                    val projectSitePackages312 = File(projectVenv, "lib/python3.12/site-packages")
                    val projectSitePackages314 = File(projectVenv, "lib/python3.14/site-packages")
                    val basePyPath = runtimeEnv["PYTHONPATH"] ?: ""
                    val targetDir = File(executablePath).parentFile?.parentFile
                    env["PYTHONPATH"] = "${projectSitePackages314.absolutePath}:${projectSitePackages.absolutePath}:${projectSitePackages312.absolutePath}:${workDir.absolutePath}:$basePyPath"
                    env["PYTHONUSERBASE"] = projectVenv.absolutePath
                    env["PYTHONUNBUFFERED"] = "1"
                    env["WERKZEUG_DEBUG_PIN"] = "off"
                    val curLd = env["LD_LIBRARY_PATH"] ?: ""
                    val targetParent = targetDir?.parentFile
                    val mariadbLib = targetParent?.let { File(it, "mariadb/lib") }
                    if (mariadbLib != null && mariadbLib.exists()) {
                        env["LD_LIBRARY_PATH"] = "${mariadbLib.absolutePath}:$curLd"
                    }
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

                project.envVars.forEach { ev ->
                    if (ev.key.isNotBlank()) {
                        env[ev.key] = ev.value
                    }
                }
                env["PORT"] = internalPort.toString()

                val proc = processBuilder.start()
                process = proc

                val actualPid = getProcessPid(proc)
                _pid.value = if (actualPid > 0) actualPid else null
                _status.value = ProjectStatus.RUNNING

                logRepository.append(project.id, LogStream.SYSTEM, "Process started with PID ${_pid.value ?: "unknown"} on port ${project.port}")

                val httpRegex = Regex("""(?:"|')?(GET|POST|PUT|DELETE|PATCH|HEAD|OPTIONS)\s+([^ "'\?]+)(?:\?[^ "']*)?(?:\s+HTTP/[0-9.]+)?["']?\s+(\d{3})""", RegexOption.IGNORE_CASE)
                val phpRegex = Regex("""\[(\d{3})\]:\s+(GET|POST|PUT|DELETE|PATCH|HEAD|OPTIONS)\s+([^ \n]+)""", RegexOption.IGNORE_CASE)
                val uvicornRegex = Regex("""(GET|POST|PUT|DELETE|PATCH|HEAD|OPTIONS)\s+([^ "'\?]+).*?(\d{3})""", RegexOption.IGNORE_CASE)

                fun parseLine(line: String) {
                    val httpMatch = httpRegex.find(line)
                    if (httpMatch != null) {
                        val m = httpMatch.groupValues[1].uppercase()
                        val p = httpMatch.groupValues[2]
                        val code = httpMatch.groupValues[3].toIntOrNull() ?: 200
                        onRequestRecord?.invoke(RequestRecord(projectId = project.id, timestamp = System.currentTimeMillis(), method = m, path = p, statusCode = code, responseBytes = 0L, durationMs = 5L, errorMessage = if (code >= 400) "HTTP $code" else null))
                        return
                    }
                    val phpMatch = phpRegex.find(line)
                    if (phpMatch != null) {
                        val code = phpMatch.groupValues[1].toIntOrNull() ?: 200
                        val m = phpMatch.groupValues[2].uppercase()
                        val p = phpMatch.groupValues[3]
                        onRequestRecord?.invoke(RequestRecord(projectId = project.id, timestamp = System.currentTimeMillis(), method = m, path = p, statusCode = code, responseBytes = 0L, durationMs = 5L, errorMessage = if (code >= 400) "HTTP $code" else null))
                        return
                    }
                    val uvicornMatch = uvicornRegex.find(line)
                    if (uvicornMatch != null) {
                        val m = uvicornMatch.groupValues[1].uppercase()
                        val p = uvicornMatch.groupValues[2]
                        val code = uvicornMatch.groupValues[3].toIntOrNull() ?: 200
                        onRequestRecord?.invoke(RequestRecord(projectId = project.id, timestamp = System.currentTimeMillis(), method = m, path = p, statusCode = code, responseBytes = 0L, durationMs = 5L, errorMessage = if (code >= 400) "HTTP $code" else null))
                        return
                    }
                    if (line.contains("error", ignoreCase = true) || line.contains("exception", ignoreCase = true) || line.contains("traceback", ignoreCase = true)) {
                        onRequestRecord?.invoke(RequestRecord(projectId = project.id, timestamp = System.currentTimeMillis(), method = "ERR", path = "/server", statusCode = 500, responseBytes = 0L, durationMs = 0L, errorMessage = line.take(160)))
                    }
                }

                stdoutJob = launch(Dispatchers.IO) {
                    val reader = BufferedReader(InputStreamReader(proc.inputStream))
                    try {
                        while (isActive) {
                            val line = reader.readLine() ?: break
                            logRepository.append(project.id, LogStream.STDOUT, line)
                            parseLine(line)
                        }
                    } catch (_: Exception) {}
                }

                stderrJob = launch(Dispatchers.IO) {
                    val reader = BufferedReader(InputStreamReader(proc.errorStream))
                    try {
                        while (isActive) {
                            val line = reader.readLine() ?: break
                            logRepository.append(project.id, LogStream.STDERR, line)
                            parseLine(line)
                        }
                    } catch (_: Exception) {}
                }

                monitorJob = launch(Dispatchers.IO) {
                    val exitCode = proc.waitFor()
                    _pid.value = null
                    stdoutJob?.cancel()
                    stderrJob?.cancel()

                    if (isExplicitlyStopped) {
                        try {
                            vpnProxyServerSocket?.close()
                            vpnProxyServerSocket = null
                        } catch (_: Exception) {}
                        vpnProxyJob?.cancel()
                        _status.value = ProjectStatus.STOPPED
                        logRepository.append(project.id, LogStream.SYSTEM, "Process stopped explicitly")
                        return@launch
                    }

                    val elapsed = System.currentTimeMillis() - launchTimeMs
                    logRepository.append(project.id, LogStream.SYSTEM, "Process exited with code $exitCode after ${elapsed}ms")

                    if (elapsed > 10_000L) {
                        retryCount = 0
                    }

                    val shouldRestart = when (project.restartPolicy) {
                        RestartPolicy.ALWAYS -> true
                        RestartPolicy.ON_CRASH -> exitCode != 0
                        RestartPolicy.NEVER -> false
                    }

                    if (shouldRestart && retryCount < 5) {
                        retryCount++
                        val backoff = (1000L * (1 shl (retryCount - 1))).coerceAtMost(30_000L)
                        _status.value = ProjectStatus.STARTING
                        logRepository.append(project.id, LogStream.SYSTEM, "Restarting in ${backoff}ms (attempt $retryCount/5)")
                        delay(backoff)
                        if (!isExplicitlyStopped) {
                            start()
                        }
                    } else {
                        try {
                            vpnProxyServerSocket?.close()
                            vpnProxyServerSocket = null
                        } catch (_: Exception) {}
                        vpnProxyJob?.cancel()
                        if (exitCode != 0) {
                            _status.value = ProjectStatus.CRASHED
                            onCrash(this@ManagedProcess)
                        } else {
                            _status.value = ProjectStatus.STOPPED
                        }
                    }
                }
            } catch (e: Exception) {
                logRepository.append(project.id, LogStream.SYSTEM, "Failed to launch process: ${e.message}")
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
                server = ServerSocket().apply {
                    reuseAddress = true
                    bind(java.net.InetSocketAddress(boundPort))
                }
            } catch (e: Exception) {
                try {
                    val fallbackPort = when (project.runtime) {
                        RuntimeType.PHP -> 8000
                        RuntimeType.JAVA -> 8080
                        else -> 3000
                    }
                    if (fallbackPort != boundPort) {
                        boundPort = fallbackPort
                        server = ServerSocket().apply {
                            reuseAddress = true
                            bind(java.net.InetSocketAddress(boundPort))
                        }
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

            var cfConnectingIp: String? = null
            var xForwardedFor: String? = null
            var xRealIp: String? = null
            var contentLength = 0
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line!!.isEmpty()) break
                val lower = line!!.lowercase()
                if (lower.startsWith("cf-connecting-ip:")) {
                    cfConnectingIp = line!!.substringAfter(":").trim()
                } else if (lower.startsWith("x-forwarded-for:")) {
                    xForwardedFor = line!!.substringAfter(":").trim().split(",").firstOrNull()?.trim()
                } else if (lower.startsWith("x-real-ip:")) {
                    xRealIp = line!!.substringAfter(":").trim()
                } else if (lower.startsWith("content-length:")) {
                    contentLength = line!!.substringAfter(":").trim().toIntOrNull() ?: 0
                }
            }

            val clientIp = cfConnectingIp ?: xForwardedFor ?: xRealIp ?: socket.inetAddress?.hostAddress ?: "127.0.0.1"

            if (isVpnBlocked?.invoke(clientIp) == true) {
                Log.d("VpnProxy", "[$clientIp] 403 Forbidden (builtin) - VPN Detected on project ${project.name}")
                val errorCode = "ERR_VPN_403_" + String.format(Locale.US, "%06X", (System.currentTimeMillis() % 0xFFFFFF))
                val customMsg = getVpnBlockedMessage?.invoke() ?: "VPN Detected, Please turn it off. If you think it is a mistake, contact the Developer."

                scope.launch(Dispatchers.IO) {
                    try {
                        logRepository.append(project.id, LogStream.STDERR, "[$clientIp] 403 Forbidden - VPN Detected ($errorCode). Access blocked.")
                    } catch (_: Exception) {}
                }

                val html = renderVpnBlockedPage(
                    projectName = project.name,
                    port = port,
                    clientIp = clientIp,
                    errorCode = errorCode,
                    errorMessage = customMsg
                )
                val blockedBytes = html.toByteArray(Charsets.UTF_8)
                val resp = "HTTP/1.1 403 Forbidden\r\n" +
                        "Content-Type: text/html; charset=utf-8\r\n" +
                        "Content-Length: ${blockedBytes.size}\r\n" +
                        "Access-Control-Allow-Origin: *\r\n" +
                        "Connection: close\r\n\r\n"
                output.write(resp.toByteArray(Charsets.UTF_8))
                output.write(blockedBytes)
                output.flush()
                onRequestRecord?.invoke(RequestRecord(projectId = project.id, timestamp = System.currentTimeMillis(), method = method, path = path, statusCode = 403, responseBytes = blockedBytes.size.toLong(), durationMs = 1L, errorMessage = "$errorCode [$clientIp] VPN Blocked"))
                return
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

            if (method.equals("OPTIONS", ignoreCase = true)) {
                val resp = "HTTP/1.1 204 No Content\r\n" +
                        "Access-Control-Allow-Origin: *\r\n" +
                        "Access-Control-Allow-Methods: GET, POST, PUT, DELETE, PATCH, OPTIONS\r\n" +
                        "Access-Control-Allow-Headers: *\r\n" +
                        "Access-Control-Max-Age: 86400\r\n" +
                        "Connection: close\r\n\r\n"
                output.write(resp.toByteArray(Charsets.UTF_8))
                output.flush()
                onRequestRecord?.invoke(RequestRecord(projectId = project.id, timestamp = System.currentTimeMillis(), method = "OPTIONS", path = path, statusCode = 204, responseBytes = 0L, durationMs = 1L))
                return
            }

            val rootDir = File(project.workingDir).canonicalFile
            val cleanPath = path.removePrefix("/")

            if (path.startsWith("/api/")) {
                val apiResponse = handleApiRoute(path, method, body, rootDir, port)
                val bytes = apiResponse.toByteArray(Charsets.UTF_8)
                val resp = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: application/json; charset=utf-8\r\n" +
                        "Content-Length: ${bytes.size}\r\n" +
                        "Access-Control-Allow-Origin: *\r\n" +
                        "Access-Control-Allow-Methods: GET, POST, PUT, DELETE, PATCH, OPTIONS\r\n" +
                        "Access-Control-Allow-Headers: *\r\n" +
                        "Connection: close\r\n\r\n"
                output.write(resp.toByteArray(Charsets.UTF_8))
                output.write(bytes)
                output.flush()
                onRequestRecord?.invoke(RequestRecord(projectId = project.id, timestamp = System.currentTimeMillis(), method = method, path = path, statusCode = 200, responseBytes = bytes.size.toLong(), durationMs = 2L))
                return
            }

            val requestedFileRaw = if (cleanPath.isEmpty()) rootDir else File(rootDir, cleanPath)
            val canonicalReq = requestedFileRaw.canonicalFile
            if (!canonicalReq.path.startsWith(rootDir.path)) {
                val errBytes = "403 Forbidden".toByteArray(Charsets.UTF_8)
                val resp = "HTTP/1.1 403 Forbidden\r\n" +
                        "Content-Type: text/plain; charset=utf-8\r\n" +
                        "Content-Length: ${errBytes.size}\r\n" +
                        "Access-Control-Allow-Origin: *\r\n" +
                        "Connection: close\r\n\r\n"
                output.write(resp.toByteArray(Charsets.UTF_8))
                output.write(errBytes)
                output.flush()
                onRequestRecord?.invoke(RequestRecord(projectId = project.id, timestamp = System.currentTimeMillis(), method = method, path = path, statusCode = 403, responseBytes = errBytes.size.toLong(), durationMs = 1L, errorMessage = "Forbidden"))
                return
            }

            var requestedFile = canonicalReq
            if (requestedFile.isDirectory) {
                val indexPhp = File(requestedFile, "index.php")
                val indexHtml = File(requestedFile, "index.html")
                val indexHtm = File(requestedFile, "index.htm")
                requestedFile = when {
                    project.runtime == RuntimeType.PHP && indexPhp.exists() -> indexPhp
                    indexHtml.exists() -> indexHtml
                    indexPhp.exists() -> indexPhp
                    indexHtm.exists() -> indexHtm
                    else -> requestedFile
                }
            }

            if (requestedFile.exists() && requestedFile.isFile) {
                if (requestedFile.extension.lowercase() == "php") {
                    val query = if (pathWithQuery.contains("?")) pathWithQuery.substringAfter("?") else ""
                    val result = executePhpScript(requestedFile, method, query, body, port)
                    val headerBuilder = StringBuilder()
                    headerBuilder.append("HTTP/1.1 ${result.statusCode} ${result.statusText}\r\n")
                    headerBuilder.append("Access-Control-Allow-Origin: *\r\n")
                    headerBuilder.append("Access-Control-Allow-Methods: GET, POST, PUT, DELETE, PATCH, OPTIONS\r\n")
                    headerBuilder.append("Access-Control-Allow-Headers: *\r\n")
                    headerBuilder.append("Content-Length: ${result.body.size}\r\n")
                    headerBuilder.append("Connection: close\r\n")
                    var hasContentType = false
                    result.headers.forEach { (k, v) ->
                        if (k.equals("Content-Type", ignoreCase = true)) hasContentType = true
                        if (!k.equals("Transfer-Encoding", ignoreCase = true) && !k.equals("Content-Length", ignoreCase = true) && !k.equals("Connection", ignoreCase = true)) {
                            headerBuilder.append("$k: $v\r\n")
                        }
                    }
                    if (!hasContentType) {
                        headerBuilder.append("Content-Type: text/html; charset=utf-8\r\n")
                    }
                    headerBuilder.append("\r\n")
                    output.write(headerBuilder.toString().toByteArray(Charsets.UTF_8))
                    output.write(result.body)
                    output.flush()
                    onRequestRecord?.invoke(RequestRecord(projectId = project.id, timestamp = System.currentTimeMillis(), method = method, path = path, statusCode = result.statusCode, responseBytes = result.body.size.toLong(), durationMs = 5L, errorMessage = if (result.statusCode >= 400) "HTTP ${result.statusCode}" else null))
                } else {
                    val mime = when (requestedFile.extension.lowercase()) {
                        "html", "htm" -> "text/html; charset=utf-8"
                        "css" -> "text/css; charset=utf-8"
                        "js", "mjs" -> "application/javascript; charset=utf-8"
                        "json" -> "application/json; charset=utf-8"
                        "png" -> "image/png"
                        "jpg", "jpeg" -> "image/jpeg"
                        "webp" -> "image/webp"
                        "svg" -> "image/svg+xml"
                        "gif" -> "image/gif"
                        "ico" -> "image/x-icon"
                        "wasm" -> "application/wasm"
                        "mp3" -> "audio/mpeg"
                        "wav" -> "audio/wav"
                        "ogg" -> "audio/ogg"
                        "mp4" -> "video/mp4"
                        "webm" -> "video/webm"
                        "woff" -> "font/woff"
                        "woff2" -> "font/woff2"
                        "ttf" -> "font/ttf"
                        "xml" -> "application/xml"
                        "pdf" -> "application/pdf"
                        "txt" -> "text/plain; charset=utf-8"
                        else -> "application/octet-stream"
                    }
                    val bytes = requestedFile.readBytes()
                    val resp = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: $mime\r\n" +
                            "Content-Length: ${bytes.size}\r\n" +
                            "Access-Control-Allow-Origin: *\r\n" +
                            "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                            "Access-Control-Allow-Headers: *\r\n" +
                            "Connection: close\r\n\r\n"
                    output.write(resp.toByteArray(Charsets.UTF_8))
                    output.write(bytes)
                    output.flush()
                    onRequestRecord?.invoke(RequestRecord(projectId = project.id, timestamp = System.currentTimeMillis(), method = method, path = path, statusCode = 200, responseBytes = bytes.size.toLong(), durationMs = 2L))
                }
            } else {
                val notFoundContent = "<h1>404 Not Found</h1><p>The requested file '$cleanPath' was not found on this server.</p>"
                val bytes = notFoundContent.toByteArray(Charsets.UTF_8)
                val resp = "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Type: text/html; charset=utf-8\r\n" +
                        "Content-Length: ${bytes.size}\r\n" +
                        "Access-Control-Allow-Origin: *\r\n" +
                        "Connection: close\r\n\r\n"
                output.write(resp.toByteArray(Charsets.UTF_8))
                output.write(bytes)
                output.flush()
                onRequestRecord?.invoke(RequestRecord(projectId = project.id, timestamp = System.currentTimeMillis(), method = method, path = path, statusCode = 404, responseBytes = bytes.size.toLong(), durationMs = 1L, errorMessage = "HTTP 404"))
            }
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

    private fun executePhpScript(
        scriptFile: File,
        method: String,
        queryString: String,
        body: String,
        port: Int
    ): PhpExecutionResult {
        val phpBinary = listOf(
            File(executablePath),
            File(File(project.workingDir).parentFile?.parentFile ?: File(""), "runtimes/php/php"),
            File("/data/data/com.localhost.app/files/runtimes/php/php"),
            File("/data/user/0/com.localhost.app/files/runtimes/php/php")
        ).firstOrNull { it.exists() && it.canExecute() }
        if (phpBinary != null) {
            try {
                val tmpDir = File(project.workingDir, ".tmp").apply { mkdirs() }
                val runnerFile = File(tmpDir, "php_runner_${Thread.currentThread().id}.php")
                val escapedQuery = queryString.replace("\\", "\\\\").replace("'", "\\'")
                val escapedBody = body.replace("\\", "\\\\").replace("'", "\\'")
                val scriptPath = scriptFile.absolutePath.replace("\\", "/")
                val workDirPath = (scriptFile.parentFile?.absolutePath ?: project.workingDir).replace("\\", "/")

                val code = "<?php\n" +
                        "ini_set('display_errors', '0');\n" +
                        "ob_start();\n" +
                        "parse_str('$escapedQuery', \$_GET);\n" +
                        "\$_rawB = '$escapedBody';\n" +
                        "if (strpos(\$_rawB, '------') !== false || strpos(\$_rawB, 'Content-Disposition') !== false) {\n" +
                        "    preg_match_all('/name=\"([^\"]+)\"(?:\\r?\\n){2}(.*?)(?:\\r?\\n-|$)/s', \$_rawB, \$_m);\n" +
                        "    if (!empty(\$_m[1])) {\n" +
                        "        for (\$_i = 0; \$_i < count(\$_m[1]); \$_i++) {\n" +
                        "            \$_POST[\$_m[1][\$_i]] = trim(\$_m[2][\$_i]);\n" +
                        "        }\n" +
                        "    }\n" +
                        "} else {\n" +
                        "    parse_str(\$_rawB, \$_POST);\n" +
                        "}\n" +
                        "\$_REQUEST = array_merge(\$_GET, \$_POST);\n" +
                        "\$_SERVER['REQUEST_METHOD'] = '$method';\n" +
                        "\$_SERVER['SERVER_PORT'] = '$port';\n" +
                        "\$_SERVER['SERVER_SOFTWARE'] = 'PHP/8.4 (Localhost Engine)';\n" +
                        "\$_SERVER['QUERY_STRING'] = '$escapedQuery';\n" +
                        "\$_SERVER['SCRIPT_FILENAME'] = '$scriptPath';\n" +
                        "chdir('$workDirPath');\n" +
                        "try {\n" +
                        "    include '$scriptPath';\n" +
                        "} catch (\\Throwable \$e) {\n" +
                        "    http_response_code(500);\n" +
                        "    echo \$e->getMessage();\n" +
                        "}\n" +
                        "\$out = ob_get_clean();\n" +
                        "\$code = http_response_code() ?: 200;\n" +
                        "\$hdrs = headers_list();\n" +
                        "echo \"---LOCALHOST_PHP_STATUS---\\n\" . \$code . \"\\n\";\n" +
                        "echo \"---LOCALHOST_PHP_HEADERS---\\n\";\n" +
                        "foreach (\$hdrs as \$h) { echo \$h . \"\\n\"; }\n" +
                        "echo \"---LOCALHOST_PHP_BODY---\\n\";\n" +
                        "echo \$out;\n"

                runnerFile.writeText(code, Charsets.UTF_8)

                val pb = ProcessBuilder(phpBinary.absolutePath, "-d", "display_errors=0", "-d", "memory_limit=256M", runnerFile.absolutePath)
                pb.directory(File(project.workingDir))
                val env = pb.environment()
                env.putAll(runtimeEnv)
                val proc = pb.start()
                val out = proc.inputStream.readBytes()
                proc.waitFor()
                try { runnerFile.delete() } catch (_: Exception) {}
                if (out.isNotEmpty()) {
                    val text = String(out, Charsets.UTF_8)
                    val statusMarker = "---LOCALHOST_PHP_STATUS---\n"
                    val headersMarker = "---LOCALHOST_PHP_HEADERS---\n"
                    val bodyMarker = "---LOCALHOST_PHP_BODY---\n"
                    if (text.contains(statusMarker) && text.contains(bodyMarker)) {
                        val statusPart = text.substringAfter(statusMarker).substringBefore(headersMarker).trim()
                        val headersPart = text.substringAfter(headersMarker).substringBefore(bodyMarker)
                        val bodyPart = text.substringAfter(bodyMarker)
                        val code = statusPart.toIntOrNull() ?: 200
                        val statusText = when (code) {
                            200 -> "OK"
                            201 -> "Created"
                            204 -> "No Content"
                            400 -> "Bad Request"
                            401 -> "Unauthorized"
                            403 -> "Forbidden"
                            404 -> "Not Found"
                            500 -> "Internal Server Error"
                            else -> "OK"
                        }
                        val parsedHeaders = mutableListOf<Pair<String, String>>()
                        headersPart.lines().forEach { hLine ->
                            val trimmed = hLine.trim()
                            if (trimmed.contains(":")) {
                                val k = trimmed.substringBefore(":").trim()
                                val v = trimmed.substringAfter(":").trim()
                                if (k.isNotEmpty()) {
                                    parsedHeaders.add(k to v)
                                }
                            }
                        }
                        return PhpExecutionResult(
                            statusCode = code,
                            statusText = statusText,
                            headers = parsedHeaders,
                            body = bodyPart.toByteArray(Charsets.UTF_8)
                        )
                    }
                    return PhpExecutionResult(
                        statusCode = 200,
                        statusText = "OK",
                        headers = listOf("Content-Type" to "text/html; charset=utf-8"),
                        body = out
                    )
                }
            } catch (_: Exception) {}
        }
        val rawPhp = scriptFile.readText()
        val rendered = renderPhpTemplate(rawPhp, port)
        return PhpExecutionResult(
            statusCode = 200,
            statusText = "OK",
            headers = listOf("Content-Type" to "text/html; charset=utf-8"),
            body = rendered.toByteArray(Charsets.UTF_8)
        )
    }

    private fun renderPhpTemplate(content: String, port: Int): String {
        val now = Date()
        val timeFmt = SimpleDateFormat("HH:mm:ss z", Locale.getDefault()).format(now)
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
        val runtimeMem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024

        var result = content
        result = result.replace("<?php echo phpversion(); ?>", "8.4.1 (Localhost Engine)")
        result = result.replace("<?php echo \${'$'}_SERVER['SERVER_SOFTWARE'] ?? 'PHP CLI'; ?>", "Localhost Server (Android)")
        result = result.replace("<?php echo \${'$'}_SERVER['SERVER_PORT'] ?? '8000'; ?>", port.toString())
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

    private fun startVpnProxy(publicPort: Int, backendPort: Int) {
        vpnProxyJob?.cancel()
        try {
            vpnProxyServerSocket?.close()
            vpnProxyServerSocket = null
        } catch (_: Exception) {}

        vpnProxyJob = scope.launch(Dispatchers.IO) {
            val server: ServerSocket
            try {
                server = ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(publicPort))
                }
                vpnProxyServerSocket = server
                Log.d("VpnProxy", "VPN proxy listening on public port $publicPort -> backend port $backendPort for ${project.name}")
            } catch (e: Exception) {
                Log.e("VpnProxy", "Failed to bind VPN proxy to port $publicPort for ${project.name}: ${e.message}")
                logRepository.append(project.id, LogStream.SYSTEM, "Port $publicPort unavailable: ${e.message}")
                _status.value = ProjectStatus.CRASHED
                return@launch
            }

            while (isActive && !server.isClosed) {
                try {
                    val client = server.accept()
                    launch(Dispatchers.IO) {
                        handleProxyClient(client, publicPort, backendPort)
                    }
                } catch (_: Exception) {
                    break
                }
            }
        }
    }

    private suspend fun CoroutineScope.handleProxyClient(client: Socket, publicPort: Int, backendPort: Int) {
        val headerBuffer = java.io.ByteArrayOutputStream()
        val inStream = client.getInputStream()
        val maxHeaderSize = 16384
        client.soTimeout = 3000
        try {
            while (headerBuffer.size() < maxHeaderSize) {
                val b = inStream.read()
                if (b == -1) break
                headerBuffer.write(b)
                val bytes = headerBuffer.toByteArray()
                val len = bytes.size
                if (len >= 4 && bytes[len - 4] == 13.toByte() && bytes[len - 3] == 10.toByte() && bytes[len - 2] == 13.toByte() && bytes[len - 1] == 10.toByte()) {
                    break
                }
            }
        } catch (_: Exception) {}

        if (headerBuffer.size() == 0) {
            try { client.close() } catch (_: Exception) {}
            return
        }

        val headerBytes = headerBuffer.toByteArray()
        val headerText = String(headerBytes, Charsets.ISO_8859_1)
        val lines = headerText.lines()
        val requestLine = lines.firstOrNull() ?: ""
        var method = "GET"
        var path = "/"
        val parts = requestLine.split(" ")
        if (parts.size >= 2) {
            method = parts[0]
            path = parts[1].substringBefore("?")
        }

        var cfConnectingIp: String? = null
        var xForwardedFor: String? = null
        var xRealIp: String? = null

        for (line in lines.drop(1)) {
            val colonIdx = line.indexOf(':')
            if (colonIdx > 0) {
                val name = line.substring(0, colonIdx).trim().lowercase()
                val value = line.substring(colonIdx + 1).trim()
                when (name) {
                    "cf-connecting-ip" -> cfConnectingIp = value
                    "x-forwarded-for" -> xForwardedFor = value.split(",").firstOrNull()?.trim()
                    "x-real-ip" -> xRealIp = value
                }
            }
        }

        val visitorIp = cfConnectingIp ?: xForwardedFor ?: xRealIp ?: client.inetAddress?.hostAddress ?: "127.0.0.1"
        val blocked = isVpnBlocked?.invoke(visitorIp) == true
        Log.d("VpnProxy", "Incoming connection from $visitorIp (socket: ${client.inetAddress?.hostAddress}) on port $publicPort for ${project.name}. isVpnBlocked=$blocked")

        if (blocked) {
            val errorCode = "ERR_VPN_403_" + String.format(Locale.US, "%06X", (System.currentTimeMillis() % 0xFFFFFF))
            val customMsg = getVpnBlockedMessage?.invoke() ?: "VPN Detected, Please turn it off. If you think it is a mistake, contact the Developer."

            Log.d("VpnProxy", "[$visitorIp] 403 Forbidden - VPN Detected ($errorCode) on project ${project.name}")

            scope.launch(Dispatchers.IO) {
                try {
                    logRepository.append(project.id, LogStream.STDERR, "[$visitorIp] 403 Forbidden - VPN Detected ($errorCode). Access blocked.")
                } catch (_: Exception) {}
            }

            val html = renderVpnBlockedPage(
                projectName = project.name,
                port = publicPort,
                clientIp = visitorIp,
                errorCode = errorCode,
                errorMessage = customMsg
            )
            val blockedBytes = html.toByteArray(Charsets.UTF_8)
            val resp = "HTTP/1.1 403 Forbidden\r\n" +
                    "Content-Type: text/html; charset=utf-8\r\n" +
                    "Content-Length: ${blockedBytes.size}\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "Connection: close\r\n\r\n"
            try {
                val output = client.getOutputStream()
                output.write(resp.toByteArray(Charsets.UTF_8))
                output.write(blockedBytes)
                output.flush()
            } catch (_: Exception) {}
            onRequestRecord?.invoke(RequestRecord(projectId = project.id, timestamp = System.currentTimeMillis(), method = method, path = path, statusCode = 403, responseBytes = blockedBytes.size.toLong(), durationMs = 1L, errorMessage = "$errorCode [$visitorIp] VPN Blocked"))
            try { client.close() } catch (_: Exception) {}
            return
        }

        var backend: Socket? = null
        for (attempt in 1..25) {
            try {
                backend = Socket("127.0.0.1", backendPort)
                break
            } catch (_: Exception) {
                if (!isActive || isExplicitlyStopped) break
                delay(200)
            }
        }

        if (backend == null) {
            Log.e("VpnProxy", "Could not connect to backend at 127.0.0.1:$backendPort for ${project.name}")
            try {
                val resp = "HTTP/1.1 502 Bad Gateway\r\nContent-Type: text/plain\r\nConnection: close\r\n\r\nService Unavailable\r\n"
                client.getOutputStream().write(resp.toByteArray(Charsets.UTF_8))
                client.getOutputStream().flush()
            } catch (_: Exception) {}
            try { client.close() } catch (_: Exception) {}
            return
        }

        try {
            client.soTimeout = 0
            client.tcpNoDelay = true
            backend.tcpNoDelay = true

            // Send buffered header bytes to backend first!
            backend.getOutputStream().write(headerBytes)
            backend.getOutputStream().flush()

            val job1 = launch(Dispatchers.IO) {
                try {
                    relay(client.getInputStream(), backend.getOutputStream())
                } catch (_: Exception) {
                    try { backend.close() } catch (_: Exception) {}
                } finally {
                    try { backend.shutdownOutput() } catch (_: Exception) {}
                }
            }

            val job2 = launch(Dispatchers.IO) {
                try {
                    relay(backend.getInputStream(), client.getOutputStream())
                } catch (_: Exception) {
                    try { client.close() } catch (_: Exception) {}
                } finally {
                    try { client.shutdownOutput() } catch (_: Exception) {}
                    try { client.close() } catch (_: Exception) {}
                }
            }

            job1.join()
            job2.join()
        } catch (_: Exception) {
        } finally {
            try { client.close() } catch (_: Exception) {}
            try { backend.close() } catch (_: Exception) {}
        }
    }

    private fun relay(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(16384)
        while (true) {
            val bytesRead = input.read(buffer)
            if (bytesRead == -1) break
            output.write(buffer, 0, bytesRead)
            output.flush()
        }
    }

    suspend fun stop() = withContext(Dispatchers.IO) {
        isExplicitlyStopped = true
        _status.value = ProjectStatus.STOPPING
        monitorJob?.cancel()

        try {
            vpnProxyServerSocket?.close()
            vpnProxyServerSocket = null
        } catch (_: Exception) {}
        vpnProxyJob?.cancel()
        Log.d("VpnProxy", "Stopped VPN proxy for ${project.name}")

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

    private fun renderVpnBlockedPage(
        projectName: String,
        port: Int,
        clientIp: String,
        errorCode: String,
        errorMessage: String
    ): String {
        return """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>VPN Detected • Localhost</title>
    <style>
        :root {
            --bg: #0c0e14;
            --surface: #141720;
            --surface-elevated: #1b1f2c;
            --border: #252a3a;
            --error: #ef4444;
            --error-glow: rgba(239, 68, 68, 0.15);
            --text: #f3f4f8;
            --text-secondary: #9aa3b8;
            --text-muted: #646e85;
            --primary: #5a75e0;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
            background-color: var(--bg);
            color: var(--text);
            display: flex;
            align-items: center;
            justify-content: center;
            min-height: 100vh;
            padding: 1.25rem;
        }
        .container {
            width: 100%;
            max-width: 520px;
            background: var(--surface);
            border: 1px solid var(--border);
            border-radius: 16px;
            padding: 2rem;
            box-shadow: 0 16px 40px rgba(0, 0, 0, 0.5);
        }
        @media (max-width: 480px) {
            .container { padding: 1.5rem 1.25rem; }
        }
        .header {
            display: flex;
            align-items: center;
            gap: 1rem;
            margin-bottom: 1.5rem;
        }
        .icon-shield {
            width: 48px;
            height: 48px;
            border-radius: 12px;
            background: var(--error-glow);
            border: 1px solid rgba(239, 68, 68, 0.35);
            display: flex;
            align-items: center;
            justify-content: center;
            color: var(--error);
            flex-shrink: 0;
        }
        .header-text h1 {
            font-size: 1.35rem;
            font-weight: 700;
            line-height: 1.3;
        }
        .badge {
            display: inline-block;
            background: rgba(239, 68, 68, 0.15);
            color: var(--error);
            border: 1px solid rgba(239, 68, 68, 0.3);
            padding: 0.2rem 0.6rem;
            border-radius: 9999px;
            font-size: 0.72rem;
            font-weight: 700;
            letter-spacing: 0.05em;
            text-transform: uppercase;
            margin-top: 0.3rem;
        }
        .message-box {
            background: var(--surface-elevated);
            border: 1px solid var(--border);
            border-left: 3px solid var(--error);
            border-radius: 10px;
            padding: 1rem 1.1rem;
            margin-bottom: 1.5rem;
            color: var(--text);
            font-size: 0.95rem;
            line-height: 1.5;
            word-break: break-word;
        }
        .info-grid {
            display: grid;
            grid-template-columns: repeat(2, 1fr);
            gap: 0.75rem;
            margin-bottom: 1.5rem;
        }
        @media (max-width: 440px) {
            .info-grid { grid-template-columns: 1fr; }
        }
        .info-item {
            background: var(--surface-elevated);
            border: 1px solid var(--border);
            border-radius: 10px;
            padding: 0.8rem 1rem;
        }
        .info-label {
            display: block;
            font-size: 0.72rem;
            font-weight: 600;
            text-transform: uppercase;
            color: var(--text-muted);
            letter-spacing: 0.05em;
            margin-bottom: 0.25rem;
        }
        .info-val {
            font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
            font-size: 0.88rem;
            font-weight: 600;
            color: var(--text);
            word-break: break-all;
        }
        .btn-retry {
            display: block;
            width: 100%;
            padding: 0.85rem;
            background: var(--primary);
            color: #ffffff;
            font-weight: 600;
            font-size: 0.92rem;
            border: none;
            border-radius: 10px;
            cursor: pointer;
            text-align: center;
            transition: opacity 0.2s;
        }
        .btn-retry:hover { opacity: 0.9; }
        .footer {
            margin-top: 1.25rem;
            text-align: center;
            font-size: 0.75rem;
            color: var(--text-muted);
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <div class="icon-shield">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                    <line x1="12" y1="8" x2="12" y2="12"/>
                    <line x1="12" y1="16" x2="12.01" y2="16"/>
                </svg>
            </div>
            <div class="header-text">
                <h1>VPN Detected</h1>
                <span class="badge">403 Forbidden</span>
            </div>
        </div>

        <div class="message-box">
            $errorMessage
        </div>

        <div class="info-grid">
            <div class="info-item">
                <span class="info-label">Error Code</span>
                <span class="info-val" style="color: var(--error);">$errorCode</span>
            </div>
            <div class="info-item">
                <span class="info-label">Client IP</span>
                <span class="info-val">$clientIp</span>
            </div>
            <div class="info-item">
                <span class="info-label">Server</span>
                <span class="info-val">$projectName</span>
            </div>
            <div class="info-item">
                <span class="info-label">Port</span>
                <span class="info-val">:$port</span>
            </div>
        </div>

        <button class="btn-retry" onclick="window.location.reload()">Retry Connection</button>

        <div class="footer">
            Localhost Mobile VPS • Android Protected Host
        </div>
    </div>
</body>
</html>"""
    }

}
