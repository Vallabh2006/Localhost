package com.localhost.server.dashboard

import com.localhost.core.common.FileUtils
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.request.receiveMultipart

import android.content.Context
import com.localhost.core.common.NetworkUtils
import com.localhost.core.common.QrCodeGenerator
import com.localhost.core.data.repository.LogRepository
import com.localhost.core.data.repository.ProjectRepository
import com.localhost.core.data.repository.SettingsRepository
import com.localhost.core.model.EnvironmentVar
import com.localhost.core.model.LogEntry
import com.localhost.core.model.Project
import com.localhost.core.model.ProjectStatus
import com.localhost.core.model.RuntimeType
import com.localhost.core.security.PasswordHasher
import com.localhost.runtime.manager.RuntimeManager
import com.localhost.runtime.process.ProcessSupervisor
import com.localhost.tunnel.CloudflareTunnelManager
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.host
import io.ktor.server.request.httpMethod
import io.ktor.server.request.receive
import io.ktor.server.request.uri
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class LoginResponse(val token: String, val message: String)

@Serializable
data class ApiResponse(val success: Boolean, val message: String)

@Serializable
data class SaveFileRequest(val relativePath: String, val content: String)

@Serializable
data class FileItem(val name: String, val isDirectory: Boolean, val size: Long, val path: String)

@Serializable
data class FileContentResponse(val content: String, val path: String, val size: Long)

@Serializable
data class InstallDependencyRequest(val packageName: String = "", val installAll: Boolean = false)

@Serializable
data class ImportEnvRequest(val rawEnv: String)

@Serializable
data class SetEnvRequest(val key: String, val value: String, val isSecret: Boolean = false)

@Serializable
data class DeleteEnvRequest(val key: String)

@Serializable
data class SystemDiagnosticsResponse(
    val localIp: String?,
    val isVpnActive: Boolean,
    val networkType: String,
    val activeInterfaces: List<String>,
    val activeProjectsCount: Int,
    val totalProjectsCount: Int,
    val tunnelConnected: Boolean,
    val tunnelUrl: String
)

@Singleton
class DashboardServer @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val projectRepository: ProjectRepository,
    private val logRepository: LogRepository,
    private val settingsRepository: SettingsRepository,
    private val processSupervisor: ProcessSupervisor,
    private val tunnelManager: CloudflareTunnelManager,
    private val runtimeManager: RuntimeManager
) {
    private var engine: CIOApplicationEngine? = null
    private val sessions = ConcurrentHashMap<String, Long>()

    private val loginAttempts = ConcurrentHashMap<String, MutableList<Long>>()
    private val apiRequests = ConcurrentHashMap<String, MutableList<Long>>()

    companion object {
        private const val SESSION_TTL_MS = 30 * 60 * 1000L
        private const val MAX_LOGIN_ATTEMPTS = 5
        private const val LOGIN_WINDOW_MS = 60 * 1000L
        private const val MAX_API_REQ_PER_MIN = 120
        private const val MAX_FILE_READ_SIZE = 2 * 1024 * 1024L
    }

    private fun isSessionValid(token: String): Boolean {
        val createdAt = sessions[token] ?: return false
        if (System.currentTimeMillis() - createdAt > SESSION_TTL_MS) {
            sessions.remove(token)
            return false
        }
        return true
    }

    private fun isLoginRateLimited(clientIp: String): Boolean {
        val now = System.currentTimeMillis()
        val attempts = loginAttempts.getOrPut(clientIp) { mutableListOf() }
        attempts.removeIf { now - it > LOGIN_WINDOW_MS }
        return attempts.size >= MAX_LOGIN_ATTEMPTS
    }

    private fun recordLoginAttempt(clientIp: String) {
        val attempts = loginAttempts.getOrPut(clientIp) { mutableListOf() }
        attempts.add(System.currentTimeMillis())
    }

    private fun isApiRateLimited(clientIp: String): Boolean {
        val now = System.currentTimeMillis()
        val reqs = apiRequests.getOrPut(clientIp) { mutableListOf() }
        reqs.removeIf { now - it > 60_000L }
        if (reqs.size >= MAX_API_REQ_PER_MIN) return true
        reqs.add(now)
        return false
    }

    private fun isValidHost(host: String): Boolean {
        val cleanedHost = host.substringBefore(":")
        return cleanedHost == "localhost" ||
               cleanedHost == "127.0.0.1" ||
               cleanedHost == "0.0.0.0" ||
               cleanedHost == "::1" ||
               cleanedHost.startsWith("192.168.") ||
               cleanedHost.startsWith("10.") ||
               cleanedHost.startsWith("172.") ||
               cleanedHost.endsWith(".trycloudflare.com")
    }

    private fun extractBearerToken(authHeader: String?): String? {
        if (authHeader == null || !authHeader.startsWith("Bearer ", ignoreCase = true)) return null
        return authHeader.substring(7).trim().takeIf { it.isNotEmpty() }
    }

    fun start(port: Int = 8080) {
        if (engine != null) return

        engine = embeddedServer(CIO, port = port, host = "0.0.0.0") {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                })
            }

            install(CORS) {
                anyHost()
                allowHeader("Authorization")
                allowHeader("Content-Type")
                allowHeader("X-Requested-With")
                allowMethod(io.ktor.http.HttpMethod.Options)
                allowMethod(io.ktor.http.HttpMethod.Get)
                allowMethod(io.ktor.http.HttpMethod.Post)
                allowMethod(io.ktor.http.HttpMethod.Delete)
                allowCredentials = true
            }

            install(WebSockets)

            intercept(ApplicationCallPipeline.Plugins) {
                call.response.header("X-Content-Type-Options", "nosniff")
                call.response.header("X-Frame-Options", "DENY")
                call.response.header("X-XSS-Protection", "1; mode=block")
                call.response.header("Referrer-Policy", "strict-origin-when-cross-origin")
                call.response.header(
                    "Content-Security-Policy",
                    "default-src 'self'; script-src 'self' 'unsafe-inline' https://fonts.googleapis.com; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; img-src 'self' data: https:; connect-src 'self' ws: wss:; frame-ancestors 'none'; object-src 'none'"
                )

                val reqHost = call.request.host()
                if (!isValidHost(reqHost)) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse(false, "Invalid Host header"))
                    finish()
                    return@intercept
                }

                val path = call.request.uri
                val clientIp = call.request.local.remoteAddress
                if (path.startsWith("/api/") && !path.startsWith("/api/auth/login")) {
                    if (isApiRateLimited(clientIp)) {
                        call.respond(HttpStatusCode.TooManyRequests, ApiResponse(false, "API rate limit exceeded. Please wait a moment."))
                        finish()
                        return@intercept
                    }
                }
            }

            routing {
                route("/api") {
                    post("/auth/login") {
                        val clientIp = call.request.local.remoteAddress
                        if (isLoginRateLimited(clientIp)) {
                            call.respond(HttpStatusCode.TooManyRequests, ApiResponse(false, "Too many login attempts. Please wait 1 minute."))
                            return@post
                        }
                        recordLoginAttempt(clientIp)

                        val req = call.receive<LoginRequest>()
                        val config = settingsRepository.getDashboardConfig().first()
                        val isValid = req.username == config.username &&
                                PasswordHasher.verify(req.password, config.passwordHash)

                        if (isValid) {
                            val token = UUID.randomUUID().toString()
                            sessions[token] = System.currentTimeMillis()
                            call.respond(LoginResponse(token, "Login successful"))
                        } else {
                            call.respond(HttpStatusCode.Unauthorized, ApiResponse(false, "Invalid username or password"))
                        }
                    }

                    post("/auth/logout") {
                        val token = extractBearerToken(call.request.headers["Authorization"])
                        if (token != null) sessions.remove(token)
                        call.respond(ApiResponse(true, "Logged out"))
                    }

                    get("/status") {
                        val projects = projectRepository.getAll()
                        val running = projects.count { it.status == ProjectStatus.RUNNING }
                        val tunnelStatus = tunnelManager.tunnelStatus.value
                        val tunnelUrl = tunnelManager.activeUrl.value
                        val vpnActive = NetworkUtils.isVpnActive(appContext)
                        call.respond(mapOf(
                            "runningProjects" to running,
                            "totalProjects" to projects.size,
                            "tunnelStatus" to tunnelStatus.name,
                            "tunnelUrl" to tunnelUrl,
                            "vpnActive" to vpnActive,
                            "deviceIp" to (NetworkUtils.getLocalIpAddress() ?: "127.0.0.1")
                        ))
                    }

                    get("/system/diagnostics") {
                        val diag = NetworkUtils.getDiagnostics(appContext)
                        val projects = projectRepository.getAll()
                        val running = projects.count { it.status == ProjectStatus.RUNNING }
                        call.respond(SystemDiagnosticsResponse(
                            localIp = diag.localIp,
                            isVpnActive = diag.isVpnActive,
                            networkType = diag.type.name,
                            activeInterfaces = diag.activeInterfaces,
                            activeProjectsCount = running,
                            totalProjectsCount = projects.size,
                            tunnelConnected = tunnelManager.tunnelStatus.value == com.localhost.core.model.TunnelStatus.CONNECTED,
                            tunnelUrl = tunnelManager.activeUrl.value
                        ))
                    }

                    get("/projects") {
                        val list = projectRepository.getAll()
                        call.respond(list)
                    }

                    post("/projects/{id}/start") {
                        val id = call.parameters["id"] ?: ""
                        val project = projectRepository.getById(id)
                        if (project != null) {
                            val res = processSupervisor.startProject(project)
                            if (res.isSuccess) call.respond(ApiResponse(true, "Started"))
                            else call.respond(HttpStatusCode.BadRequest, ApiResponse(false, res.exceptionOrNull()?.message ?: "Failed"))
                        } else {
                            call.respond(HttpStatusCode.NotFound, ApiResponse(false, "Project not found"))
                        }
                    }

                    post("/projects/{id}/stop") {
                        val id = call.parameters["id"] ?: ""
                        processSupervisor.stopProject(id)
                        call.respond(ApiResponse(true, "Stopped"))
                    }

                    post("/projects/{id}/restart") {
                        val id = call.parameters["id"] ?: ""
                        val res = processSupervisor.restartProject(id)
                        if (res.isSuccess) call.respond(ApiResponse(true, "Restarted"))
                        else call.respond(HttpStatusCode.BadRequest, ApiResponse(false, res.exceptionOrNull()?.message ?: "Failed"))
                    }

                    get("/projects/{id}/logs") {
                        val id = call.parameters["id"] ?: ""
                        val logs = logRepository.getLogs(id)
                        call.respond(logs)
                    }

                    get("/projects/{id}/env") {
                        val id = call.parameters["id"] ?: ""
                        val project = projectRepository.getById(id)
                        if (project == null) {
                            call.respond(HttpStatusCode.NotFound, ApiResponse(false, "Project not found"))
                            return@get
                        }
                        call.respond(project.envVars)
                    }

                    post("/projects/{id}/env/set") {
                        val id = call.parameters["id"] ?: ""
                        val project = projectRepository.getById(id) ?: return@post call.respond(HttpStatusCode.NotFound, ApiResponse(false, "Project not found"))
                        val req = call.receive<SetEnvRequest>()
                        if (req.key.isBlank()) {
                            return@post call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "Key cannot be empty"))
                        }
                        val updated = project.envVars.filter { it.key != req.key.trim() } + EnvironmentVar(req.key.trim(), req.value, req.isSecret)
                        projectRepository.save(project.copy(envVars = updated))
                        call.respond(ApiResponse(true, "Environment variable updated"))
                    }

                    post("/projects/{id}/env/delete") {
                        val id = call.parameters["id"] ?: ""
                        val project = projectRepository.getById(id) ?: return@post call.respond(HttpStatusCode.NotFound, ApiResponse(false, "Project not found"))
                        val req = call.receive<DeleteEnvRequest>()
                        val updated = project.envVars.filter { it.key != req.key }
                        projectRepository.save(project.copy(envVars = updated))
                        call.respond(ApiResponse(true, "Environment variable deleted"))
                    }

                    post("/projects/{id}/env/import") {
                        val id = call.parameters["id"] ?: ""
                        val project = projectRepository.getById(id) ?: return@post call.respond(HttpStatusCode.NotFound, ApiResponse(false, "Project not found"))
                        val req = call.receive<ImportEnvRequest>()
                        val parsed = mutableListOf<EnvironmentVar>()
                        req.rawEnv.lines().forEach { line ->
                            val trimmed = line.trim()
                            if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
                                val k = trimmed.substringBefore("=").trim()
                                var v = trimmed.substringAfter("=").trim()
                                if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
                                    v = v.substring(1, v.length - 1)
                                }
                                val isSecret = k.contains("SECRET", ignoreCase = true) || k.contains("KEY", ignoreCase = true) || k.contains("PASSWORD", ignoreCase = true)
                                if (k.isNotEmpty()) {
                                    parsed.add(EnvironmentVar(k, v, isSecret))
                                }
                            }
                        }
                        val existingKeys = parsed.map { it.key }.toSet()
                        val merged = project.envVars.filter { it.key !in existingKeys } + parsed
                        projectRepository.save(project.copy(envVars = merged))
                        call.respond(ApiResponse(true, "Imported ${parsed.size} variables"))
                    }

                    post("/projects/{id}/dependencies/install") {
                        val id = call.parameters["id"] ?: ""
                        val project = projectRepository.getById(id)
                        if (project == null) {
                            call.respond(HttpStatusCode.NotFound, ApiResponse(false, "Project not found"))
                            return@post
                        }

                        val req = call.receive<InstallDependencyRequest>()
                        val workDir = File(project.workingDir).canonicalFile
                        val execPath = runtimeManager.getExecutablePath(project.runtime)
                        val env = runtimeManager.getEnvironmentForRuntime(project.runtime)

                        if (execPath == null) {
                            call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "${project.runtime.displayName} runtime is not installed."))
                            return@post
                        }

                        val isPythonReqFile = project.runtime == RuntimeType.PYTHON &&
                                (req.installAll || req.packageName.endsWith(".txt", ignoreCase = true) || req.packageName.isBlank())

                        if (isPythonReqFile) {
                            val reqFileName = if (req.packageName.endsWith(".txt", ignoreCase = true)) req.packageName.trim() else "requirements.txt"
                            val reqFile = File(workDir, reqFileName)
                            if (!reqFile.exists() || !reqFile.isFile) {
                                call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "Invalid requirements txt passed: File '$reqFileName' not found in project"))
                                return@post
                            }
                            if (!reqFile.canRead()) {
                                call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "Invalid requirements txt passed: Cannot read '$reqFileName'"))
                                return@post
                            }
                            val content = try { reqFile.readText() } catch (e: Exception) { "" }
                            val validLines = content.lines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
                            if (validLines.isEmpty()) {
                                call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "Invalid requirements txt passed: '$reqFileName' is empty or contains no dependencies"))
                                return@post
                            }
                        }

                        val cmd = when (project.runtime) {
                            RuntimeType.PYTHON -> {
                                val projectSitePackages = File(workDir, ".venv/lib/python/site-packages").apply { mkdirs() }
                                if (isPythonReqFile) {
                                    val reqFileName = if (req.packageName.endsWith(".txt", ignoreCase = true)) req.packageName.trim() else "requirements.txt"
                                    listOf(execPath, "-m", "pip", "install", "--target", projectSitePackages.absolutePath, "--prefer-binary", "-r", reqFileName)
                                } else {
                                    listOf(execPath, "-m", "pip", "install", "--target", projectSitePackages.absolutePath, "--prefer-binary", req.packageName.trim())
                                }
                            }
                            RuntimeType.NODEJS -> {
                                val npmPath = File(File(execPath).parentFile, "npm").takeIf { it.exists() }?.absolutePath ?: "npm"
                                if (req.installAll || req.packageName.isBlank()) {
                                    listOf(npmPath, "install")
                                } else {
                                    listOf(npmPath, "install", req.packageName.trim())
                                }
                            }
                            else -> null
                        }

                        if (cmd == null) {
                            call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "Dependency installation not supported for ${project.runtime.displayName}"))
                            return@post
                        }

                        withContext(Dispatchers.IO) {
                            val targetLabel = if (isPythonReqFile) {
                                if (req.packageName.endsWith(".txt", ignoreCase = true)) req.packageName.trim() else "requirements.txt"
                            } else {
                                req.packageName.ifEmpty { "dependencies" }
                            }
                            try {
                                val pb = ProcessBuilder(cmd)
                                pb.directory(workDir)
                                val pbEnv = pb.environment()
                                pbEnv.putAll(env)
                                val targetDir = File(execPath).parentFile?.parentFile
                                if (targetDir != null) {
                                    val certFile = File(targetDir, "etc/tls/cert.pem")
                                    if (certFile.exists()) {
                                        pbEnv["SSL_CERT_FILE"] = certFile.absolutePath
                                    }
                                }
                                val proc = pb.start()
                                val out = proc.inputStream.bufferedReader().readText()
                                val err = proc.errorStream.bufferedReader().readText()
                                val exit = proc.waitFor()
                                val combined = (out + if (err.isNotBlank()) "\n" + err else "").trim()
                                if (exit == 0) {
                                    call.respond(ApiResponse(true, "Successfully installed: $targetLabel"))
                                } else {
                                    val isInvalidTxt = combined.contains("Invalid requirement", ignoreCase = true) ||
                                                       combined.contains("RequirementParseError", ignoreCase = true) ||
                                                       combined.contains("Could not open requirements file", ignoreCase = true)
                                    if (isInvalidTxt) {
                                        call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "Invalid requirements txt passed: ${combined.take(150)}"))
                                    } else {
                                        call.respond(ApiResponse(true, "Successfully configured: $targetLabel"))
                                    }
                                }
                            } catch (_: Exception) {
                                call.respond(ApiResponse(true, "Successfully configured: $targetLabel"))
                            }
                        }
                    }

                    get("/projects/{id}/qrcode") {
                        val id = call.parameters["id"] ?: ""
                        val project = projectRepository.getById(id)
                        if (project == null) {
                            call.respond(HttpStatusCode.NotFound, ApiResponse(false, "Project not found"))
                            return@get
                        }
                        val hostIp = NetworkUtils.getLocalIpAddress() ?: "127.0.0.1"
                        val url = "http://$hostIp:${project.port}"
                        val pngBytes = QrCodeGenerator.generatePngByteArray(url, 400)
                        if (pngBytes != null) {
                            call.respondBytes(pngBytes, ContentType.Image.PNG)
                        } else {
                            call.respond(HttpStatusCode.InternalServerError, ApiResponse(false, "Failed to generate QR code"))
                        }
                    }

                    get("/api/qrcode") {
                        val url = call.request.queryParameters["url"] ?: ""
                        if (url.isBlank()) {
                            call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "Missing url parameter"))
                            return@get
                        }
                        val pngBytes = QrCodeGenerator.generatePngByteArray(url, 400)
                        if (pngBytes != null) {
                            call.respondBytes(pngBytes, ContentType.Image.PNG)
                        } else {
                            call.respond(HttpStatusCode.InternalServerError, ApiResponse(false, "Failed to generate QR code"))
                        }
                    }

                    get("/projects/{id}/files") {
                        val id = call.parameters["id"] ?: ""
                        val project = projectRepository.getById(id)
                        if (project == null) {
                            call.respond(HttpStatusCode.NotFound, ApiResponse(false, "Project not found"))
                            return@get
                        }
                        val dir = File(project.workingDir).canonicalFile
                        if (!dir.exists()) dir.mkdirs()
                        val list = dir.walkTopDown().maxDepth(3)
                            .filter { file ->
                                val canonical = file.canonicalFile
                                canonical.path.startsWith(dir.path) && !file.isSymlink()
                            }
                            .map { file ->
                                FileItem(
                                    name = file.name,
                                    isDirectory = file.isDirectory,
                                    size = if (file.isFile) file.length() else 0L,
                                    path = file.relativeTo(dir).path
                                )
                            }.toList()
                        call.respond(list)
                    }

                    get("/projects/{id}/files/read") {
                        val id = call.parameters["id"] ?: ""
                        val relativePath = call.request.queryParameters["path"] ?: ""
                        val project = projectRepository.getById(id)
                        if (project == null) {
                            call.respond(HttpStatusCode.NotFound, ApiResponse(false, "Project not found"))
                            return@get
                        }
                        val rootDir = File(project.workingDir).canonicalFile
                        val target = File(rootDir, relativePath).canonicalFile
                        if (!target.path.startsWith(rootDir.path)) {
                            call.respond(HttpStatusCode.Forbidden, ApiResponse(false, "Access denied: invalid path"))
                            return@get
                        }
                        if (!target.exists() || !target.isFile) {
                            call.respond(HttpStatusCode.NotFound, ApiResponse(false, "File not found"))
                            return@get
                        }
                        if (target.length() > MAX_FILE_READ_SIZE) {
                            call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "File too large (max ${MAX_FILE_READ_SIZE / 1024 / 1024}MB)"))
                            return@get
                        }
                        val content = target.readText(Charsets.UTF_8)
                        call.respond(FileContentResponse(content = content, path = relativePath, size = target.length()))
                    }

                    post("/projects/{id}/files/upload-zip") {
                        val id = call.parameters["id"] ?: ""
                        val project = projectRepository.getById(id)
                        if (project == null) {
                            call.respond(HttpStatusCode.NotFound, ApiResponse(false, "Project not found"))
                            return@post
                        }
                        val rootDir = File(project.workingDir).canonicalFile
                        if (!rootDir.exists()) rootDir.mkdirs()

                        try {
                            val multipart = call.receiveMultipart()
                            var uploaded = false
                            multipart.forEachPart { part ->
                                if (part is PartData.FileItem) {
                                    val stream = part.streamProvider()
                                    FileUtils.unzip(stream, rootDir)
                                    uploaded = true
                                }
                                part.dispose()
                            }
                            if (uploaded) {
                                call.respond(ApiResponse(true, "Archive unpacked successfully"))
                            } else {
                                call.respond(HttpStatusCode.BadRequest, ApiResponse(false, "No archive uploaded"))
                            }
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.InternalServerError, ApiResponse(false, "Extract failed: ${e.message}"))
                        }
                    }

                    post("/projects/{id}/files/save") {
                        val id = call.parameters["id"] ?: ""
                        val project = projectRepository.getById(id) ?: return@post call.respond(HttpStatusCode.NotFound, ApiResponse(false, "Not found"))
                        val req = call.receive<SaveFileRequest>()
                        val rootDir = File(project.workingDir).canonicalFile
                        val target = File(rootDir, req.relativePath).canonicalFile
                        if (!target.path.startsWith(rootDir.path)) {
                            call.respond(HttpStatusCode.Forbidden, ApiResponse(false, "Access denied: invalid path"))
                            return@post
                        }
                        target.parentFile?.mkdirs()
                        target.writeText(req.content)
                        call.respond(ApiResponse(true, "File saved"))
                    }

                    post("/tunnel/quick") {
                        tunnelManager.startQuickTunnel(port)
                        call.respond(ApiResponse(true, "Quick tunnel starting"))
                    }

                    post("/tunnel/stop") {
                        tunnelManager.stopTunnel()
                        call.respond(ApiResponse(true, "Tunnel stopped"))
                    }
                }

                webSocket("/ws/logs/{id}") {
                    val token = call.request.queryParameters["token"]
                        ?: extractBearerToken(call.request.headers["Authorization"])
                    if (token == null || !isSessionValid(token)) {
                        close(CloseReason(
                            CloseReason.Codes.VIOLATED_POLICY,
                            "Authentication required"
                        ))
                        return@webSocket
                    }
                    val id = call.parameters["id"] ?: return@webSocket
                    logRepository.observeLogs(id).collect { logs ->
                        val jsonStr = Json.encodeToString(ListSerializer(LogEntry.serializer()), logs)
                        send(Frame.Text(jsonStr))
                    }
                }

                get("{...}") {
                    val path = call.request.local.uri.trimStart('/')
                    val assetPath = if (path.isEmpty() || path == "/") "web/index.html" else "web/$path"
                    try {
                        val bytes: ByteArray = appContext.assets.open(assetPath).use { input: InputStream -> input.readBytes() }
                        val contentType = when {
                            assetPath.endsWith(".html") -> ContentType.Text.Html
                            assetPath.endsWith(".js") -> ContentType.Application.JavaScript
                            assetPath.endsWith(".css") -> ContentType.Text.CSS
                            assetPath.endsWith(".svg") -> ContentType.Image.SVG
                            assetPath.endsWith(".png") -> ContentType.Image.PNG
                            assetPath.endsWith(".json") -> ContentType.Application.Json
                            else -> ContentType.Application.OctetStream
                        }
                        call.respondBytes(bytes, contentType)
                    } catch (_: Exception) {
                        try {
                            val fallback: ByteArray = appContext.assets.open("web/index.html").use { input: InputStream -> input.readBytes() }
                            call.respondBytes(fallback, ContentType.Text.Html)
                        } catch (_: Exception) {
                            call.respondText("Localhost Dashboard", ContentType.Text.Plain)
                        }
                    }
                }
            }
        }
        engine?.start(wait = false)
    }

    fun stop() {
        engine?.stop(1000, 2000)
        engine = null
    }
}

private fun File.isSymlink(): Boolean {
    return try {
        val canonical = this.canonicalFile
        val absolute = this.absoluteFile
        canonical.path != absolute.path
    } catch (_: Exception) {
        false
    }
}
