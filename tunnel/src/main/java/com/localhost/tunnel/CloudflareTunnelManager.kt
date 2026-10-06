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
    private var tunnelProcess: Process? = null
    private var logReaderJob: Job? = null
    private var timeoutJob: Job? = null
    private var isUserRequestedRunning = false
    private var reconnectAttempts = 0

    private val _tunnelStatus = MutableStateFlow(TunnelStatus.DISCONNECTED)
    val tunnelStatus = _tunnelStatus.asStateFlow()

    private val _activeUrl = MutableStateFlow("")
    val activeUrl = _activeUrl.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val cloudflaredFile: File
        get() = File(context.filesDir, "cloudflared")

    private fun getSetsidPath(): String? {
        return when {
            File("/system/bin/setsid").exists() -> "/system/bin/setsid"
            File("/bin/setsid").exists() -> "/bin/setsid"
            else -> null
        }
    }

    private fun buildDetachedCommand(vararg args: String): List<String> {
        val setsid = getSetsidPath()
        return if (setsid != null) {
            listOf(setsid) + args.toList()
        } else {
            args.toList()
        }
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
        val url = "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-$arch"

        return try {
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return Result.failure(Exception("Failed to download cloudflared: HTTP ${response.code}"))
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
        if (_tunnelStatus.value == TunnelStatus.CONNECTED || _tunnelStatus.value == TunnelStatus.CONNECTING) return
        _tunnelStatus.value = TunnelStatus.CONNECTING
        _errorMessage.value = null
        isUserRequestedRunning = true
        reconnectAttempts = 0

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

            val ensureRes = ensureBinary()
            if (ensureRes.isFailure) {
                _tunnelStatus.value = TunnelStatus.ERROR
                _errorMessage.value = "cloudflared binary download failed: ${ensureRes.exceptionOrNull()?.message}"
                return@launch
            }

            try {
                val command = buildDetachedCommand(
                    cloudflaredFile.absolutePath,
                    "tunnel",
                    "--url",
                    "http://127.0.0.1:$port",
                    "--no-autoupdate"
                )

                val pb = ProcessBuilder(command)
                pb.redirectErrorStream(true)
                val proc = pb.start()
                tunnelProcess = proc

                timeoutJob?.cancel()
                timeoutJob = launch {
                    delay(45_000L)
                    if (_tunnelStatus.value == TunnelStatus.CONNECTING) {
                        _tunnelStatus.value = TunnelStatus.ERROR
                        _errorMessage.value = "Tunnel connection timed out"
                        try { proc.destroy() } catch (_: Throwable) {}
                    }
                }

                logReaderJob?.cancel()
                logReaderJob = launch {
                    try {
                        BufferedReader(InputStreamReader(proc.inputStream)).useLines { lines ->
                            lines.forEach { line ->
                                val urlMatch = Regex("""https://[a-zA-Z0-9-]+\.trycloudflare\.com""").find(line)
                                if (urlMatch != null) {
                                    timeoutJob?.cancel()
                                    _activeUrl.value = urlMatch.value
                                    _tunnelStatus.value = TunnelStatus.CONNECTED
                                }
                            }
                        }
                    } catch (_: Throwable) {}
                }

                val exitCode = try { proc.waitFor() } catch (_: Throwable) { -1 }
                timeoutJob?.cancel()
                if (isUserRequestedRunning && reconnectAttempts < 3 && _tunnelStatus.value == TunnelStatus.CONNECTED) {
                    reconnectAttempts++
                    delay(2000L * reconnectAttempts)
                    startQuickTunnel(port)
                } else if (_tunnelStatus.value == TunnelStatus.CONNECTED || _tunnelStatus.value == TunnelStatus.CONNECTING) {
                    _tunnelStatus.value = TunnelStatus.DISCONNECTED
                    _activeUrl.value = ""
                }
            } catch (e: Exception) {
                timeoutJob?.cancel()
                _tunnelStatus.value = TunnelStatus.ERROR
                _errorMessage.value = e.message
            }
        }
    }

    fun startNamedTunnel(token: String, tunnelId: String? = null) {
        if (_tunnelStatus.value == TunnelStatus.CONNECTED || _tunnelStatus.value == TunnelStatus.CONNECTING) return
        _tunnelStatus.value = TunnelStatus.CONNECTING
        _errorMessage.value = null
        isUserRequestedRunning = true
        reconnectAttempts = 0

        scope.launch {
            val ensureRes = ensureBinary()
            if (ensureRes.isFailure) {
                _tunnelStatus.value = TunnelStatus.ERROR
                _errorMessage.value = "cloudflared binary download failed"
                return@launch
            }

            try {
                val args = mutableListOf(
                    cloudflaredFile.absolutePath,
                    "tunnel",
                    "run"
                )
                if (token.isNotBlank()) {
                    args.add("--token")
                    args.add(token)
                }

                val command = buildDetachedCommand(*args.toTypedArray())
                val pb = ProcessBuilder(command)
                pb.redirectErrorStream(true)
                val proc = pb.start()
                tunnelProcess = proc

                timeoutJob?.cancel()
                timeoutJob = launch {
                    delay(45_000L)
                    if (_tunnelStatus.value == TunnelStatus.CONNECTING) {
                        _tunnelStatus.value = TunnelStatus.ERROR
                        _errorMessage.value = "Named tunnel connection timed out"
                        try { proc.destroy() } catch (_: Throwable) {}
                    }
                }

                logReaderJob?.cancel()
                logReaderJob = launch {
                    try {
                        BufferedReader(InputStreamReader(proc.inputStream)).useLines { lines ->
                            lines.forEach { line ->
                                if (line.contains("Registered tunnel connection", ignoreCase = true) ||
                                    (line.contains("Connection", ignoreCase = true) && line.contains("registered", ignoreCase = true)) ||
                                    line.contains("Connected to", ignoreCase = true) ||
                                    line.contains("Connection is ready", ignoreCase = true)
                                ) {
                                    timeoutJob?.cancel()
                                    _tunnelStatus.value = TunnelStatus.CONNECTED
                                }
                            }
                        }
                    } catch (_: Throwable) {}
                }

                launch {
                    val exitCode = try { proc.waitFor() } catch (_: Throwable) { -1 }
                    timeoutJob?.cancel()
                    if (isUserRequestedRunning && reconnectAttempts < 3 && _tunnelStatus.value == TunnelStatus.CONNECTED) {
                        reconnectAttempts++
                        delay(2000L * reconnectAttempts)
                        startNamedTunnel(token, tunnelId)
                    } else {
                        _tunnelStatus.value = TunnelStatus.DISCONNECTED
                    }
                }
            } catch (e: Exception) {
                timeoutJob?.cancel()
                _tunnelStatus.value = TunnelStatus.ERROR
                _errorMessage.value = e.message
            }
        }
    }

    fun stopTunnel() {
        isUserRequestedRunning = false
        reconnectAttempts = 0
        scope.launch {
            try {
                timeoutJob?.cancel()
                logReaderJob?.cancel()
                tunnelProcess?.let { proc ->
                    try {
                        proc.destroy()
                    } catch (_: Throwable) {}
                    delay(300)
                    try {
                        if (proc.isAlive) {
                            proc.destroyForcibly()
                        }
                    } catch (_: Throwable) {}
                }
            } catch (_: Throwable) {} finally {
                tunnelProcess = null
                _tunnelStatus.value = TunnelStatus.DISCONNECTED
                _activeUrl.value = ""
            }
        }
    }
}
