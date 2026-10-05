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
import kotlinx.coroutines.isActive
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

    private val _tunnelStatus = MutableStateFlow(TunnelStatus.DISCONNECTED)
    val tunnelStatus = _tunnelStatus.asStateFlow()

    private val _activeUrl = MutableStateFlow("")
    val activeUrl = _activeUrl.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val cloudflaredFile: File
        get() {
            val binDir = File(context.filesDir, "bin").apply { mkdirs() }
            return File(binDir, "cloudflared")
        }

    fun isBinaryInstalled(): Boolean {
        return cloudflaredFile.exists() && cloudflaredFile.canExecute()
    }

    suspend fun ensureBinary(): Result<Unit> {
        if (isBinaryInstalled()) return Result.success(Unit)
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

    fun startQuickTunnel(targetPort: Int = 8080) {
        if (_tunnelStatus.value == TunnelStatus.CONNECTED || _tunnelStatus.value == TunnelStatus.CONNECTING) return
        _tunnelStatus.value = TunnelStatus.CONNECTING
        _errorMessage.value = null

        scope.launch {
            val ensureRes = ensureBinary()
            if (ensureRes.isFailure) {
                _tunnelStatus.value = TunnelStatus.ERROR
                _errorMessage.value = "cloudflared binary download failed: ${ensureRes.exceptionOrNull()?.message}"
                return@launch
            }

            try {
                val command = listOf(
                    cloudflaredFile.absolutePath,
                    "tunnel",
                    "--url",
                    "http://127.0.0.1:$targetPort",
                    "--no-autoupdate"
                )

                val pb = ProcessBuilder(command)
                pb.redirectErrorStream(true)
                val proc = pb.start()
                tunnelProcess = proc

                logReaderJob = launch {
                    BufferedReader(InputStreamReader(proc.inputStream)).useLines { lines ->
                        lines.forEach { line ->
                            val urlMatch = Regex("https://[a-zA-Z0-9-]+\\.trycloudflare\\.com").find(line)
                            if (urlMatch != null) {
                                _activeUrl.value = urlMatch.value
                                _tunnelStatus.value = TunnelStatus.CONNECTED
                            }
                        }
                    }
                }

                val exitCode = proc.waitFor()
                if (_tunnelStatus.value == TunnelStatus.CONNECTED || _tunnelStatus.value == TunnelStatus.CONNECTING) {
                    _tunnelStatus.value = TunnelStatus.DISCONNECTED
                    _activeUrl.value = ""
                }
            } catch (e: Exception) {
                _tunnelStatus.value = TunnelStatus.ERROR
                _errorMessage.value = e.message
            }
        }
    }

    fun startNamedTunnel(token: String) {
        if (_tunnelStatus.value == TunnelStatus.CONNECTED || _tunnelStatus.value == TunnelStatus.CONNECTING) return
        _tunnelStatus.value = TunnelStatus.CONNECTING
        _errorMessage.value = null

        scope.launch {
            val ensureRes = ensureBinary()
            if (ensureRes.isFailure) {
                _tunnelStatus.value = TunnelStatus.ERROR
                _errorMessage.value = "cloudflared binary download failed"
                return@launch
            }

            try {
                val command = listOf(
                    cloudflaredFile.absolutePath,
                    "tunnel",
                    "run",
                    "--token",
                    token
                )

                val pb = ProcessBuilder(command)
                pb.redirectErrorStream(true)
                val proc = pb.start()
                tunnelProcess = proc

                logReaderJob = launch {
                    BufferedReader(InputStreamReader(proc.inputStream)).useLines { lines ->
                        lines.forEach { line ->
                            if (line.contains("Registered tunnel connection", ignoreCase = true) ||
                                line.contains("Connection", ignoreCase = true) && line.contains("registered", ignoreCase = true) ||
                                line.contains("Connected to", ignoreCase = true)
                            ) {
                                _tunnelStatus.value = TunnelStatus.CONNECTED
                            }
                        }
                    }
                }

                launch {
                    val exitCode = proc.waitFor()
                    _tunnelStatus.value = TunnelStatus.DISCONNECTED
                }
            } catch (e: Exception) {
                _tunnelStatus.value = TunnelStatus.ERROR
                _errorMessage.value = e.message
            }
        }
    }

    fun stopTunnel() {
        scope.launch {
            logReaderJob?.cancel()
            tunnelProcess?.destroy()
            delay(1000)
            if (tunnelProcess?.isAlive == true) {
                tunnelProcess?.destroyForcibly()
            }
            tunnelProcess = null
            _tunnelStatus.value = TunnelStatus.DISCONNECTED
            _activeUrl.value = ""
        }
    }
}
