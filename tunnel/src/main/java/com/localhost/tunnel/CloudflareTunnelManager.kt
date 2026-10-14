package com.localhost.tunnel

import android.content.Context
import android.os.Build
import com.localhost.core.data.repository.SettingsRepository
import com.localhost.core.model.TunnelConfig
import com.localhost.core.model.TunnelStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudflareTunnelManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val okHttpClient: OkHttpClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var quickProcess: Process? = null
    private var namedProcess: Process? = null
    private var quickLogJob: Job? = null
    private var namedLogJob: Job? = null
    private var quickTimeoutJob: Job? = null
    private var namedTimeoutJob: Job? = null
    private var isQuickUserRunning = false
    private var isNamedUserRunning = false
    private var lastQuickPort: Int = 8080
    private var lastNamedToken: String = ""

    private val _tokenTunnelStatus = MutableStateFlow(TunnelStatus.DISCONNECTED)
    val tokenTunnelStatus = _tokenTunnelStatus.asStateFlow()

    private val _tokenActiveUrl = MutableStateFlow("")
    val tokenActiveUrl = _tokenActiveUrl.asStateFlow()

    private val _tokenErrorMessage = MutableStateFlow<String?>(null)
    val tokenErrorMessage = _tokenErrorMessage.asStateFlow()

    private val _forwardingStatus = MutableStateFlow(TunnelStatus.DISCONNECTED)
    val forwardingStatus = _forwardingStatus.asStateFlow()

    private val _forwardingActiveUrl = MutableStateFlow("")
    val forwardingActiveUrl = _forwardingActiveUrl.asStateFlow()

    private val _forwardingErrorMessage = MutableStateFlow<String?>(null)
    val forwardingErrorMessage = _forwardingErrorMessage.asStateFlow()

    val tunnelStatus = tokenTunnelStatus
    val activeUrl = tokenActiveUrl
    val errorMessage = tokenErrorMessage

    private val cloudflaredFile: File
        get() = File(context.filesDir, "cloudflared")

    private fun getOrCreateCertBundle(): File? {
        val certFile = File(context.filesDir, "runtimes/python/etc/tls/cert.pem")
        if (certFile.exists()) return certFile
        val certifiFile = File(context.filesDir, "runtimes/python/lib/python3.14/site-packages/certifi/cacert.pem")
        if (certifiFile.exists()) return certifiFile
        val bundleFile = File(context.filesDir, "ca-bundle.crt")
        if (bundleFile.exists() && bundleFile.length() > 1024L) return bundleFile
        val systemCacerts = File("/system/etc/security/cacerts")
        if (systemCacerts.exists() && systemCacerts.isDirectory) {
            try {
                bundleFile.bufferedWriter().use { writer ->
                    systemCacerts.listFiles()?.filter { it.name.endsWith(".0") }?.forEach { f ->
                        writer.write(f.readText())
                        writer.write("\n")
                    }
                }
                if (bundleFile.exists() && bundleFile.length() > 1024L) return bundleFile
            } catch (_: Throwable) {}
        }
        return null
    }

    private fun applyTunnelEnvironment(pb: ProcessBuilder) {
        val env = pb.environment()
        val cert = getOrCreateCertBundle()
        if (cert != null) {
            env["SSL_CERT_FILE"] = cert.absolutePath
            env["REQUESTS_CA_BUNDLE"] = cert.absolutePath
            env["CURL_CA_BUNDLE"] = cert.absolutePath
        }
        env["HOME"] = context.filesDir.absolutePath
        env["TMPDIR"] = context.cacheDir.absolutePath
    }

    suspend fun isBinaryAvailable(): Boolean {
        return cloudflaredFile.exists() && cloudflaredFile.canExecute()
    }

    suspend fun ensureBinary(): Result<Unit> {
        if (isBinaryAvailable()) {
            return Result.success(Unit)
        }

        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
        val arch = when {
            abi.contains("arm64") -> "arm64"
            abi.contains("armeabi") -> "arm"
            abi.contains("x86_64") -> "amd64"
            else -> "arm64"
        }
        val url = "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-" + arch

        return try {
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return Result.failure(Exception("Failed to download cloudflared: HTTP " + response.code))
            }

            val body = response.body ?: return Result.failure(Exception("Empty body"))
            FileOutputStream(cloudflaredFile).use { output ->
                body.byteStream().copyTo(output)
            }
            cloudflaredFile.setExecutable(true, false)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun startQuickTunnel(targetPort: Int? = null) {
        isQuickUserRunning = true
        scope.launch {
            val port = if (targetPort != null && targetPort > 0) {
                targetPort
            } else {
                try {
                    settingsRepository.getDashboardConfig().first().port
                } catch (_: Exception) {
                    8080
                }
            }
            lastQuickPort = port
            launchQuickTunnel(port)
        }
    }

    private fun launchQuickTunnel(port: Int) {
        if (!isQuickUserRunning) return
        _forwardingStatus.value = TunnelStatus.CONNECTING
        _forwardingErrorMessage.value = null

        scope.launch {
            val ensureRes = ensureBinary()
            if (ensureRes.isFailure) {
                _forwardingStatus.value = TunnelStatus.ERROR
                _forwardingErrorMessage.value = "cloudflared binary download failed: " + (ensureRes.exceptionOrNull()?.message ?: "")
                return@launch
            }

            try {
                try {
                    quickProcess?.destroy()
                    quickProcess = null
                } catch (_: Throwable) {}

                val command = listOf(
                    cloudflaredFile.absolutePath,
                    "tunnel",
                    "--url",
                    "http://127.0.0.1:" + port,
                    "--protocol",
                    "http2",
                    "--no-autoupdate"
                )

                val pb = ProcessBuilder(command)
                pb.redirectErrorStream(true)
                applyTunnelEnvironment(pb)
                val proc = pb.start()
                quickProcess = proc

                quickTimeoutJob?.cancel()
                quickTimeoutJob = launch {
                    delay(45_000L)
                    if (_forwardingStatus.value == TunnelStatus.CONNECTING) {
                        _forwardingStatus.value = TunnelStatus.ERROR
                        _forwardingErrorMessage.value = "Port forwarding connection timed out"
                        try { proc.destroy() } catch (_: Throwable) {}
                    }
                }

                var lastErrorOutput = ""
                quickLogJob?.cancel()
                quickLogJob = launch(Dispatchers.IO) {
                    try {
                        BufferedReader(InputStreamReader(proc.inputStream)).useLines { lines ->
                            lines.forEach { line ->
                                if (line.contains("error", ignoreCase = true) || line.contains("failed", ignoreCase = true) || line.contains("fatal", ignoreCase = true)) {
                                    lastErrorOutput = line.take(180)
                                }
                                val urlMatch = Regex("""https://[a-zA-Z0-9-]+\.trycloudflare\.com""").find(line)
                                if (urlMatch != null && !urlMatch.value.contains("api.trycloudflare.com")) {
                                    quickTimeoutJob?.cancel()
                                    _forwardingActiveUrl.value = urlMatch.value
                                    _forwardingStatus.value = TunnelStatus.CONNECTED
                                }
                            }
                        }
                    } catch (_: Throwable) {}
                }

                launch(Dispatchers.IO) {
                    val exitCode = try { proc.waitFor() } catch (_: Throwable) { -1 }
                    quickTimeoutJob?.cancel()
                    if (isQuickUserRunning) {
                        if (_forwardingStatus.value == TunnelStatus.CONNECTED) {
                            delay(2000L)
                            if (isQuickUserRunning) {
                                launchQuickTunnel(port)
                            }
                        } else {
                            _forwardingStatus.value = TunnelStatus.ERROR
                            _forwardingErrorMessage.value = if (lastErrorOutput.isNotBlank()) lastErrorOutput else "Forwarding exited unexpectedly (code " + exitCode + "). Ensure the project server is online."
                            _forwardingActiveUrl.value = ""
                        }
                    } else {
                        _forwardingStatus.value = TunnelStatus.DISCONNECTED
                        _forwardingActiveUrl.value = ""
                    }
                }
            } catch (e: Exception) {
                quickTimeoutJob?.cancel()
                _forwardingStatus.value = TunnelStatus.ERROR
                _forwardingErrorMessage.value = e.message
            }
        }
    }

    fun startNamedTunnel(token: String, tunnelId: String? = null) {
        lastNamedToken = token
        isNamedUserRunning = true
        launchNamedTunnel(token)
    }

    private fun launchNamedTunnel(token: String) {
        if (!isNamedUserRunning) return
        _tokenTunnelStatus.value = TunnelStatus.CONNECTING
        _tokenErrorMessage.value = null

        scope.launch {
            val ensureRes = ensureBinary()
            if (ensureRes.isFailure) {
                _tokenTunnelStatus.value = TunnelStatus.ERROR
                _tokenErrorMessage.value = "cloudflared binary download failed"
                return@launch
            }

            try {
                try {
                    namedProcess?.destroy()
                    namedProcess = null
                } catch (_: Throwable) {}

                val args = mutableListOf(
                    cloudflaredFile.absolutePath,
                    "tunnel",
                    "--protocol",
                    "http2",
                    "--no-autoupdate",
                    "run"
                )
                if (token.isNotBlank()) {
                    args.add("--token")
                    args.add(token)
                }

                val pb = ProcessBuilder(args)
                pb.redirectErrorStream(true)
                applyTunnelEnvironment(pb)
                val proc = pb.start()
                namedProcess = proc

                namedTimeoutJob?.cancel()
                namedTimeoutJob = launch {
                    delay(45_000L)
                    if (_tokenTunnelStatus.value == TunnelStatus.CONNECTING) {
                        _tokenTunnelStatus.value = TunnelStatus.ERROR
                        _tokenErrorMessage.value = "Cloudflare Token tunnel connection timed out"
                        try { proc.destroy() } catch (_: Throwable) {}
                    }
                }

                namedLogJob?.cancel()
                namedLogJob = launch(Dispatchers.IO) {
                    try {
                        BufferedReader(InputStreamReader(proc.inputStream)).useLines { lines ->
                            lines.forEach { line ->
                                if (line.contains("Registered tunnel connection", ignoreCase = true) ||
                                    (line.contains("Connection", ignoreCase = true) && line.contains("registered", ignoreCase = true)) ||
                                    line.contains("Connected to", ignoreCase = true) ||
                                    line.contains("Connection is ready", ignoreCase = true)
                                ) {
                                    namedTimeoutJob?.cancel()
                                    _tokenTunnelStatus.value = TunnelStatus.CONNECTED
                                }
                            }
                        }
                    } catch (_: Throwable) {}
                }

                launch(Dispatchers.IO) {
                    val exitCode = try { proc.waitFor() } catch (_: Throwable) { -1 }
                    namedTimeoutJob?.cancel()
                    if (isNamedUserRunning) {
                        if (_tokenTunnelStatus.value == TunnelStatus.CONNECTED) {
                            delay(2000L)
                            if (isNamedUserRunning) {
                                launchNamedTunnel(token)
                            }
                        } else {
                            _tokenTunnelStatus.value = TunnelStatus.ERROR
                            _tokenErrorMessage.value = "Cloudflare Token tunnel failed to connect (code " + exitCode + "). Check your token."
                        }
                    } else {
                        _tokenTunnelStatus.value = TunnelStatus.DISCONNECTED
                    }
                }
            } catch (e: Exception) {
                namedTimeoutJob?.cancel()
                _tokenTunnelStatus.value = TunnelStatus.ERROR
                _tokenErrorMessage.value = e.message
            }
        }
    }

    fun stopPortForwarding() {
        isQuickUserRunning = false
        scope.launch {
            try {
                quickTimeoutJob?.cancel()
                quickLogJob?.cancel()
                quickProcess?.let { proc ->
                    try { proc.destroy() } catch (_: Throwable) {}
                    delay(200)
                    try { if (proc.isAlive) proc.destroyForcibly() } catch (_: Throwable) {}
                }
            } catch (_: Throwable) {} finally {
                quickProcess = null
                _forwardingStatus.value = TunnelStatus.DISCONNECTED
                _forwardingActiveUrl.value = ""
            }
        }
    }

    fun stopNamedTunnel() {
        isNamedUserRunning = false
        scope.launch {
            try {
                namedTimeoutJob?.cancel()
                namedLogJob?.cancel()
                namedProcess?.let { proc ->
                    try { proc.destroy() } catch (_: Throwable) {}
                    delay(200)
                    try { if (proc.isAlive) proc.destroyForcibly() } catch (_: Throwable) {}
                }
            } catch (_: Throwable) {} finally {
                namedProcess = null
                _tokenTunnelStatus.value = TunnelStatus.DISCONNECTED
            }
        }
    }

    fun stopTunnel() {
        stopPortForwarding()
        stopNamedTunnel()
    }
}
