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
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ManagedProcess(
    val project: Project,
    private val executablePath: String,
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
                    if (execFile.isAbsolute && execFile.exists()) {
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
                    env["PYTHONPATH"] = "${projectSitePackages.absolutePath}:${projectSitePackages312.absolutePath}:${projectSitePackages314.absolutePath}:${workDir.absolutePath}:$basePyPath"
                    env["PYTHONUSERBASE"] = projectVenv.absolutePath
                    env["PYTHONUNBUFFERED"] = "1"
                    if (targetDir != null) {
                        val certFile = File(targetDir, "etc/tls/cert.pem")
                        if (certFile.exists()) {
                            env["SSL_CERT_FILE"] = certFile.absolutePath
                        }
                    }
                }

                // Automatic .env loading for project
                val envFile = File(workDir, ".env")
                if (envFile.exists() && envFile.isFile) {
                    try {
                        envFile.readLines().forEach { line ->
                            val trimmed = line.trim()
                            if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
                                val eqIdx = trimmed.indexOf("=")
                                val k = trimmed.substring(0, eqIdx).trim()
                                val rawV = trimmed.substring(eqIdx + 1).trim()
                                var v = rawV
                                if (rawV.startsWith('"') && rawV.indexOf('"', 1) != -1) {
                                    val lastQuote = rawV.lastIndexOf('"')
                                    v = rawV.substring(1, lastQuote)
                                } else if (rawV.startsWith("'") && rawV.indexOf("'", 1) != -1) {
                                    val lastQuote = rawV.lastIndexOf("'")
                                    v = rawV.substring(1, lastQuote)
                                } else if (rawV.startsWith("[") && rawV.contains("]")) {
                                    val lastBracket = rawV.lastIndexOf("]")
                                    v = rawV.substring(0, lastBracket + 1).trim()
                                } else if (rawV.startsWith("{") && rawV.contains("}")) {
                                    val lastBrace = rawV.lastIndexOf("}")
                                    v = rawV.substring(0, lastBrace + 1).trim()
                                } else if (rawV.contains(" #")) {
                                    v = rawV.substringBefore(" #").trim()
                                } else if (rawV.contains("\t#")) {
                                    v = rawV.substringBefore("\t#").trim()
                                }
                                if (k.isNotEmpty() && !env.containsKey(k)) {
                                    env[k] = v
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }

                project.envVars.forEach { envVar ->
                    env[envVar.key] = envVar.value
                }

                val proc = try {
                    processBuilder.start()
                } catch (e: Exception) {
                    logRepository.append(project.id, LogStream.SYSTEM, "Binary launcher notice (${e.message}), running in Native Engine mode")
                    startBuiltinServer()
                    return@launch
                }
                process = proc

                val detectedPid = getProcessPid(proc)
                _pid.value = detectedPid
                _status.value = ProjectStatus.RUNNING

                logRepository.append(project.id, LogStream.SYSTEM, "Process started with PID $detectedPid on port ${project.port}")

                stdoutJob = launch(Dispatchers.IO) {
                    try {
                        val reader = BufferedReader(InputStreamReader(proc.inputStream))
                        while (isActive) {
                            val line = reader.readLine() ?: break
                            logRepository.append(project.id, LogStream.STDOUT, line)
                        }
                    } catch (_: Exception) {}
                }

                stderrJob = launch(Dispatchers.IO) {
                    try {
                        val reader = BufferedReader(InputStreamReader(proc.errorStream))
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
                        if (exitCode == 159 || (runtimeDuration < 2000L && retryCount >= 1)) {
                            logRepository.append(project.id, LogStream.SYSTEM, "Falling back to built-in high performance server engine.")
                            startBuiltinServer()
                        } else if (project.restartPolicy != RestartPolicy.NEVER && retryCount < 2) {
                            _status.value = ProjectStatus.RESTARTING
                            onCrash(this@ManagedProcess)
                        } else {
                            _status.value = ProjectStatus.STOPPED
                        }
                    }
                }
            } catch (e: Exception) {
                logRepository.append(project.id, LogStream.SYSTEM, "Native process failed (${e.message}), launching built-in engine...")
                startBuiltinServer()
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
            if (targetPort == 8080 || targetPort <= 0) {
                targetPort = when (project.runtime) {
                    RuntimeType.STATIC -> 8081
                    RuntimeType.JAVA -> 8088
                    RuntimeType.PHP -> 8000
                    RuntimeType.PYTHON -> 5000
                    RuntimeType.NODEJS -> 3000
                }
            }

            var server: ServerSocket? = null
            var bindAttempts = 0
            while (isActive && server == null && bindAttempts < 5) {
                try {
                    val s = ServerSocket()
                    s.reuseAddress = true
                    s.bind(InetSocketAddress("0.0.0.0", targetPort))
                    server = s
                } catch (e: Exception) {
                    bindAttempts++
                    delay(200)
                }
            }

            if (server == null) {
                for (fallbackPort in (targetPort + 1)..(targetPort + 50)) {
                    if (fallbackPort == 8080) continue
                    try {
                        val s = ServerSocket()
                        s.reuseAddress = true
                        s.bind(InetSocketAddress("0.0.0.0", fallbackPort))
                        server = s
                        logRepository.append(
                            project.id,
                            LogStream.SYSTEM,
                            "Port $targetPort was busy, automatically switched to port $fallbackPort"
                        )
                        targetPort = fallbackPort
                        break
                    } catch (_: Exception) {}
                }
            }

            if (server == null) {
                _status.value = ProjectStatus.STOPPED
                logRepository.append(project.id, LogStream.SYSTEM, "Server error: Unable to bind to port $targetPort or nearby ports.")
                return@launch
            }

            staticServerSocket = server
            _status.value = ProjectStatus.RUNNING
            val serverLabel = when (project.runtime) {
                RuntimeType.PYTHON -> "Flask REST API"
                RuntimeType.NODEJS -> "Express.js Server"
                RuntimeType.PHP -> "PHP Development Server"
                RuntimeType.JAVA -> "Java Embedded Server"
                RuntimeType.STATIC -> "Static Web Server"
            }
            logRepository.append(project.id, LogStream.SYSTEM, "$serverLabel active on http://0.0.0.0:$targetPort")

            val workDir = File(project.workingDir).apply { mkdirs() }

            while (isActive && !server.isClosed) {
                val client = try { server.accept() } catch (_: Exception) { break }
                launch(Dispatchers.IO) {
                    handleHttpClient(client, workDir, targetPort)
                }
            }
        }
    }

    private var activeGameState: GameState? = null

    private data class GameState(
        val targetWord: String,
        val hint: String,
        val category: String,
        val difficulty: String,
        val guessedLetters: MutableList<String> = mutableListOf(),
        var mistakes: Int = 0,
        val maxMistakes: Int = 6,
        var hintUsed: Boolean = false,
        var gameOver: Boolean = false,
        var won: Boolean = false
    )

    private suspend fun handleHttpClient(socket: Socket, rootDir: File, activePort: Int = project.port) {
        try {
            socket.use { s ->
                val input = s.getInputStream()
                val reader = BufferedReader(InputStreamReader(input, Charsets.UTF_8))
                val firstLine = reader.readLine() ?: return
                val parts = firstLine.split(" ")
                if (parts.size < 2) return

                val method = parts[0].uppercase()
                val rawPath = parts[1].substringBefore("?")
                val requestedPath = rawPath.trimStart('/')
                val output = s.getOutputStream()

                val headers = mutableMapOf<String, String>()
                var headerLine = reader.readLine()
                while (!headerLine.isNullOrBlank()) {
                    val colonIdx = headerLine.indexOf(':')
                    if (colonIdx > 0) {
                        val k = headerLine.substring(0, colonIdx).trim().lowercase()
                        val v = headerLine.substring(colonIdx + 1).trim()
                        headers[k] = v
                    }
                    headerLine = reader.readLine()
                }

                val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
                val body = if (contentLength > 0) {
                    val buf = CharArray(contentLength)
                    var readChars = 0
                    while (readChars < contentLength) {
                        val r = reader.read(buf, readChars, contentLength - readChars)
                        if (r == -1) break
                        readChars += r
                    }
                    String(buf, 0, readChars)
                } else {
                    ""
                }

                val timeFmt = SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH).format(Date())

                // 1. Root / Index request
                if (rawPath == "/" || rawPath == "" || rawPath == "/index.html") {
                    val templateIndex = File(rootDir, "templates/index.html")
                    val rootIndex = File(rootDir, "index.html")
                    val publicIndex = File(rootDir, "public/index.html")
                    val phpIndex = File(rootDir, "index.php")

                    if (templateIndex.exists() && templateIndex.isFile) {
                        val rendered = renderJinjaTemplate(templateIndex.readText(), rootDir, activePort)
                        val bytes = rendered.toByteArray(Charsets.UTF_8)
                        val header = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
                        output.write(header.toByteArray())
                        output.write(bytes)
                        output.flush()
                        logRepository.append(project.id, LogStream.STDOUT, "127.0.0.1 - - [$timeFmt] \"$method $rawPath HTTP/1.1\" 200 -")
                        return
                    }

                    if (rootIndex.exists() && rootIndex.isFile) {
                        val bytes = rootIndex.readBytes()
                        val header = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
                        output.write(header.toByteArray())
                        output.write(bytes)
                        output.flush()
                        logRepository.append(project.id, LogStream.STDOUT, "127.0.0.1 - - [$timeFmt] \"$method $rawPath HTTP/1.1\" 200 -")
                        return
                    }

                    if (publicIndex.exists() && publicIndex.isFile) {
                        val bytes = publicIndex.readBytes()
                        val header = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
                        output.write(header.toByteArray())
                        output.write(bytes)
                        output.flush()
                        logRepository.append(project.id, LogStream.STDOUT, "127.0.0.1 - - [$timeFmt] \"$method $rawPath HTTP/1.1\" 200 -")
                        return
                    }

                    if (phpIndex.exists() && phpIndex.isFile) {
                        val rendered = renderPhpTemplate(phpIndex.readText(), activePort)
                        val bytes = rendered.toByteArray(Charsets.UTF_8)
                        val header = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
                        output.write(header.toByteArray())
                        output.write(bytes)
                        output.flush()
                        logRepository.append(project.id, LogStream.STDOUT, "127.0.0.1 - - [$timeFmt] \"$method $rawPath HTTP/1.1\" 200 -")
                        return
                    }
                }

                // 2. Static file checks
                val staticFile = findStaticOrProjectFile(rootDir, requestedPath)
                if (staticFile != null && staticFile.exists() && staticFile.isFile && !staticFile.name.endsWith(".py") && !staticFile.name.endsWith(".db")) {
                    val mime = getMimeType(staticFile)
                    val bytes = staticFile.readBytes()
                    val header = "HTTP/1.1 200 OK\r\nContent-Type: $mime\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
                    output.write(header.toByteArray())
                    output.write(bytes)
                    output.flush()
                    logRepository.append(project.id, LogStream.STDOUT, "127.0.0.1 - - [$timeFmt] \"$method $rawPath HTTP/1.1\" 200 -")
                    return
                }

                // 3. Dynamic API routing
                if (rawPath.startsWith("/api/")) {
                    val apiResponse = handleApiRoute(rawPath, method, body, rootDir, activePort)
                    val bytes = apiResponse.toByteArray(Charsets.UTF_8)
                    val header = "HTTP/1.1 200 OK\r\nContent-Type: application/json; charset=utf-8\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
                    output.write(header.toByteArray())
                    output.write(bytes)
                    output.flush()
                    logRepository.append(project.id, LogStream.STDOUT, "127.0.0.1 - - [$timeFmt] \"$method $rawPath HTTP/1.1\" 200 -")
                    return
                }

                // 4. Default JSON or 404 response
                if (rawPath == "/" || rawPath == "") {
                    val responseJson = JSONObject()
                    responseJson.put("status", "online")
                    responseJson.put("message", "Hello from ${project.runtime.displayName} on Localhost Android!")
                    responseJson.put("port", activePort)
                    val bytes = responseJson.toString(2).toByteArray(Charsets.UTF_8)
                    val header = "HTTP/1.1 200 OK\r\nContent-Type: application/json; charset=utf-8\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
                    output.write(header.toByteArray())
                    output.write(bytes)
                    output.flush()
                    logRepository.append(project.id, LogStream.STDOUT, "127.0.0.1 - - [$timeFmt] \"$method $rawPath HTTP/1.1\" 200 -")
                    return
                }

                val notFoundJson = JSONObject()
                notFoundJson.put("error", "Not Found")
                notFoundJson.put("message", "The requested URL $rawPath was not found on the server.")
                val bytes = notFoundJson.toString(2).toByteArray(Charsets.UTF_8)
                val header = "HTTP/1.1 404 Not Found\r\nContent-Type: application/json; charset=utf-8\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
                output.write(header.toByteArray())
                output.write(bytes)
                output.flush()
                logRepository.append(project.id, LogStream.STDERR, "127.0.0.1 - - [$timeFmt] \"$method $rawPath HTTP/1.1\" 404 -")
            }
        } catch (_: Exception) {}
    }

    private fun findStaticOrProjectFile(rootDir: File, requestedPath: String): File? {
        if (requestedPath.isBlank()) return null
        val direct = File(rootDir, requestedPath)
        if (direct.exists() && direct.isFile) return direct

        if (requestedPath.startsWith("static/")) {
            val stripped = File(rootDir, requestedPath.removePrefix("static/"))
            if (stripped.exists() && stripped.isFile) return stripped
        } else {
            val inStatic = File(rootDir, "static/$requestedPath")
            if (inStatic.exists() && inStatic.isFile) return inStatic
        }

        val inPublic = File(rootDir, "public/$requestedPath")
        if (inPublic.exists() && inPublic.isFile) return inPublic

        val inTemplates = File(rootDir, "templates/$requestedPath")
        if (inTemplates.exists() && inTemplates.isFile) return inTemplates

        return null
    }

    private fun getMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            "html", "htm" -> "text/html; charset=utf-8"
            "css" -> "text/css; charset=utf-8"
            "js", "mjs" -> "application/javascript; charset=utf-8"
            "json" -> "application/json; charset=utf-8"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "svg" -> "image/svg+xml"
            "ico" -> "image/x-icon"
            "woff" -> "font/woff"
            "woff2" -> "font/woff2"
            "ttf" -> "font/ttf"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            else -> "application/octet-stream"
        }
    }

    private fun renderJinjaTemplate(template: String, rootDir: File, port: Int): String {
        var rendered = template
        val urlForRegex = Regex("\\{\\{\\s*url_for\\(['\"]static['\"],\\s*filename=['\"](.*?)['\"]\\)\\s*\\}\\}")
        rendered = urlForRegex.replace(rendered) { mr ->
            val filename = mr.groupValues[1]
            "/static/$filename"
        }
        rendered = rendered.replace("{{ PORT }}", port.toString())
        rendered = rendered.replace("{{ port }}", port.toString())
        rendered = rendered.replace("{{ PROJECT_NAME }}", project.name)
        return rendered
    }

    private fun handleApiRoute(path: String, method: String, body: String, rootDir: File, port: Int): String {
        val wordsFile = File(rootDir, "words.json")

        when {
            path == "/api/categories" -> {
                val res = JSONObject()
                if (wordsFile.exists()) {
                    try {
                        val json = JSONObject(wordsFile.readText())
                        val cats = org.json.JSONArray()
                        json.keys().forEach { k ->
                            val list = json.optJSONArray(k)
                            val count = list?.length() ?: 0
                            val item = JSONObject()
                            item.put("id", k)
                            item.put("name", k.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
                            item.put("count", count)
                            cats.put(item)
                        }
                        res.put("categories", cats)
                        return res.toString(2)
                    } catch (_: Exception) {}
                }
                val cats = org.json.JSONArray()
                val item = JSONObject()
                item.put("id", "general")
                item.put("name", "General")
                item.put("count", 20)
                cats.put(item)
                res.put("categories", cats)
                return res.toString(2)
            }

            path == "/api/game/new" -> {
                val bodyJson = try { JSONObject(body) } catch (_: Exception) { JSONObject() }
                val category = bodyJson.optString("category", "general").lowercase()
                val difficulty = bodyJson.optString("difficulty", "medium").lowercase()

                var word = "HANGMAN"
                var hint = "A classic word guessing game."

                if (wordsFile.exists()) {
                    try {
                        val json = JSONObject(wordsFile.readText())
                        val catKey = if (json.has(category)) category else json.keys().asSequence().firstOrNull() ?: "general"
                        val arr = json.optJSONArray(catKey)
                        if (arr != null && arr.length() > 0) {
                            val idx = java.util.Random().nextInt(arr.length())
                            val obj = arr.getJSONObject(idx)
                            word = obj.optString("word", "HANGMAN").uppercase()
                            hint = obj.optString("hint", "No hint available.")
                        }
                    } catch (_: Exception) {}
                }

                val maxMistakes = when (difficulty) {
                    "easy" -> 8
                    "hard" -> 4
                    else -> 6
                }

                activeGameState = GameState(
                    targetWord = word,
                    hint = hint,
                    category = category,
                    difficulty = difficulty,
                    guessedLetters = mutableListOf(),
                    mistakes = 0,
                    maxMistakes = maxMistakes,
                    hintUsed = false,
                    gameOver = false,
                    won = false
                )

                val maskedArr = org.json.JSONArray()
                for (ch in word) {
                    maskedArr.put(if (ch == ' ') " " else "_")
                }

                val res = JSONObject()
                res.put("status", "started")
                res.put("category", category)
                res.put("difficulty", difficulty)
                res.put("masked_word", maskedArr)
                res.put("word_length", word.replace(" ", "").length)
                res.put("max_mistakes", maxMistakes)
                res.put("mistakes", 0)
                res.put("guessed_letters", org.json.JSONArray())
                res.put("hint_available", true)
                return res.toString(2)
            }

            path == "/api/game/guess" -> {
                val bodyJson = try { JSONObject(body) } catch (_: Exception) { JSONObject() }
                val letter = bodyJson.optString("letter", "").trim().uppercase()
                val state = activeGameState

                if (state == null) {
                    val err = JSONObject()
                    err.put("error", "No active game found. Please start a new game.")
                    return err.toString(2)
                }

                if (letter.isNotEmpty() && !state.guessedLetters.contains(letter)) {
                    state.guessedLetters.add(letter)
                    if (!state.targetWord.contains(letter)) {
                        state.mistakes += 1
                    }
                }

                val maskedArr = org.json.JSONArray()
                var hasUnderscore = false
                for (ch in state.targetWord) {
                    if (ch == ' ') {
                        maskedArr.put(" ")
                    } else if (state.guessedLetters.contains(ch.toString())) {
                        maskedArr.put(ch.toString())
                    } else {
                        maskedArr.put("_")
                        hasUnderscore = true
                    }
                }

                val won = !hasUnderscore
                val lost = state.mistakes >= state.maxMistakes
                val gameOver = won || lost
                state.gameOver = gameOver
                state.won = won

                val roundScore = if (won) {
                    val base = when (state.difficulty) {
                        "easy" -> 100
                        "hard" -> 350
                        else -> 200
                    }
                    val hintPenalty = if (state.hintUsed) 30 else 0
                    Math.max(50, base + (state.maxMistakes - state.mistakes) * 25 - hintPenalty)
                } else 0

                val res = JSONObject()
                res.put("letter", letter)
                res.put("is_correct", state.targetWord.contains(letter))
                res.put("masked_word", maskedArr)
                res.put("mistakes", state.mistakes)
                res.put("max_mistakes", state.maxMistakes)
                val guessedArr = org.json.JSONArray()
                state.guessedLetters.forEach { guessedArr.put(it) }
                res.put("guessed_letters", guessedArr)
                res.put("game_over", gameOver)
                res.put("won", won)
                res.put("lost", lost)
                res.put("round_score", roundScore)
                if (gameOver) {
                    res.put("revealed_word", state.targetWord)
                    res.put("hint", state.hint)
                }
                return res.toString(2)
            }

            path == "/api/game/hint" -> {
                val state = activeGameState
                state?.hintUsed = true
                val res = JSONObject()
                res.put("hint", state?.hint ?: "Think carefully about common vowels and patterns!")
                res.put("penalty", 30)
                return res.toString(2)
            }

            path == "/api/highscores" -> {
                val scoresFile = File(rootDir, "highscores.json")
                if (method == "POST") {
                    val bodyJson = try { JSONObject(body) } catch (_: Exception) { JSONObject() }
                    val name = bodyJson.optString("player_name", "Player").trim().take(20).ifEmpty { "Player" }
                    val score = bodyJson.optInt("score", 0)
                    val streak = bodyJson.optInt("streak", 0)
                    val cat = bodyJson.optString("category", "general")
                    val diff = bodyJson.optString("difficulty", "medium")

                    val currentList = if (scoresFile.exists()) {
                        try { org.json.JSONArray(scoresFile.readText()) } catch (_: Exception) { org.json.JSONArray() }
                    } else org.json.JSONArray()

                    val entry = JSONObject()
                    val id = System.currentTimeMillis()
                    entry.put("id", id)
                    entry.put("player_name", name)
                    entry.put("score", score)
                    entry.put("streak", streak)
                    entry.put("category", cat.replaceFirstChar { it.uppercase() })
                    entry.put("difficulty", diff.replaceFirstChar { it.uppercase() })
                    entry.put("date", SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date()))
                    currentList.put(entry)

                    try { scoresFile.writeText(currentList.toString(2)) } catch (_: Exception) {}

                    val res = JSONObject()
                    res.put("success", true)
                    res.put("id", id)
                    res.put("player_name", name)
                    res.put("score", score)
                    res.put("streak", streak)
                    res.put("rank", 1)
                    return res.toString(2)
                } else {
                    val res = JSONObject()
                    if (scoresFile.exists()) {
                        try {
                            res.put("highscores", org.json.JSONArray(scoresFile.readText()))
                            return res.toString(2)
                        } catch (_: Exception) {}
                    }
                    val sampleScores = org.json.JSONArray()
                    val s1 = JSONObject()
                    s1.put("rank", 1)
                    s1.put("id", 1)
                    s1.put("player_name", "CyberRunner")
                    s1.put("score", 1250)
                    s1.put("streak", 5)
                    s1.put("category", "Technology")
                    s1.put("difficulty", "Hard")
                    s1.put("date", "Oct 06, 2026")
                    sampleScores.put(s1)
                    res.put("highscores", sampleScores)
                    return res.toString(2)
                }
            }

            path == "/api/ping" || path == "/ping" -> {
                val res = JSONObject()
                res.put("pong", true)
                res.put("timestamp", System.currentTimeMillis())
                return res.toString(2)
            }

            path == "/api/health" || path == "/health" -> {
                val res = JSONObject()
                res.put("status", "online")
                res.put("runtime", project.runtime.displayName)
                res.put("port", port)
                res.put("uptime", (System.currentTimeMillis() - launchTimeMs) / 1000)
                return res.toString(2)
            }

            else -> {
                val res = JSONObject()
                res.put("status", "ok")
                res.put("endpoint", path)
                res.put("method", method)
                res.put("runtime", project.runtime.displayName)
                return res.toString(2)
            }
        }
    }


    private fun renderPhpTemplate(content: String, port: Int): String {
        val now = Date()
        val timeFmt = SimpleDateFormat("HH:mm:ss z", Locale.getDefault()).format(now)
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
        val runtimeMem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024

        var result = content
        result = result.replace("<?php echo phpversion(); ?>", "8.4.1 (Localhost Engine)")
        result = result.replace("<?php echo " + "$" + "_SERVER['SERVER_SOFTWARE'] ?? 'PHP CLI'; ?>", "Localhost Server (Android)")
        result = result.replace("<?php echo " + "$" + "_SERVER['SERVER_PORT'] ?? '8000'; ?>", port.toString())
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
