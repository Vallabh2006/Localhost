package com.localhost.tunnel

import android.content.Context
import com.localhost.core.common.NetworkUtils
import com.localhost.core.model.NginxProxyConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.math.BigInteger
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLServerSocketFactory

@Singleton
class NginxManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var proxyJob: Job? = null
    private var sslServerSocket: SSLServerSocket? = null
    private var nativeProcess: Process? = null

    private val _proxyConfig = MutableStateFlow(NginxProxyConfig())
    val proxyConfig = _proxyConfig.asStateFlow()

    private val nginxDir: File
        get() = File(context.filesDir, "nginx").apply { mkdirs() }

    private val certFile: File
        get() = File(nginxDir, "server.crt")

    private val keyFile: File
        get() = File(nginxDir, "server.key")

    private val confFile: File
        get() = File(nginxDir, "nginx.conf")

    fun generateNginxConfig(targetPort: Int, httpsPort: Int): String {
        return """
            # Localhost Nginx Configuration
            worker_processes 1;
            error_log ${File(nginxDir, "error.log").absolutePath} warn;
            pid ${File(nginxDir, "nginx.pid").absolutePath};

            events {
                worker_connections 1024;
            }

            http {
                include /etc/nginx/mime.types;
                default_type application/octet-stream;
                sendfile on;
                keepalive_timeout 65;

                server {
                    listen $httpsPort ssl;
                    server_name localhost;

                    ssl_certificate ${certFile.absolutePath};
                    ssl_certificate_key ${keyFile.absolutePath};
                    ssl_protocols TLSv1.2 TLSv1.3;
                    ssl_ciphers HIGH:!aNULL:!MD5;

                    location / {
                        proxy_pass http://127.0.0.1:$targetPort;
                        proxy_set_header Host ${'$'}host;
                        proxy_set_header X-Real-IP ${'$'}remote_addr;
                        proxy_set_header X-Forwarded-For ${'$'}proxy_add_x_forwarded_for;
                        proxy_set_header X-Forwarded-Proto https;
                        proxy_http_version 1.1;
                        proxy_set_header Upgrade ${'$'}http_upgrade;
                        proxy_set_header Connection "upgrade";
                    }
                }
            }
        """.trimIndent()
    }

    suspend fun startProxy(targetPort: Int = 8080, httpsPort: Int = 8443): Result<Unit> = withContext(Dispatchers.IO) {
        stopProxy()

        try {
            val confContent = generateNginxConfig(targetPort, httpsPort)
            confFile.writeText(confContent)

            val sslContext = SelfSignedCertHelper.getOrCreateSSLContext()
            val serverFactory = sslContext.serverSocketFactory
            val serverSocket = serverFactory.createServerSocket(httpsPort) as SSLServerSocket
            serverSocket.needClientAuth = false
            sslServerSocket = serverSocket

            val lanIp = NetworkUtils.getLocalIpAddress() ?: "127.0.0.1"
            val localHttps = "https://localhost:$httpsPort"
            val lanHttps = "https://$lanIp:$httpsPort"

            _proxyConfig.update {
                it.copy(
                    isRunning = true,
                    targetPort = targetPort,
                    httpsPort = httpsPort,
                    localHttpsUrl = localHttps,
                    lanHttpsUrl = lanHttps,
                    error = null
                )
            }

            proxyJob = scope.launch(Dispatchers.IO) {
                while (isActive && !serverSocket.isClosed) {
                    try {
                        val clientSocket = serverSocket.accept()
                        launch(Dispatchers.IO) {
                            handleConnection(clientSocket, targetPort)
                        }
                    } catch (e: Exception) {
                        if (!serverSocket.isClosed) {
                        }
                        break
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            _proxyConfig.update {
                it.copy(isRunning = false, error = e.message ?: "Failed to start HTTPS proxy")
            }
            Result.failure(e)
        }
    }

    private fun handleConnection(clientSocket: Socket, targetPort: Int) {
        try {
            val targetSocket = Socket("127.0.0.1", targetPort)
            scope.launch(Dispatchers.IO) {
                pipeStreams(clientSocket.getInputStream(), targetSocket.getOutputStream())
            }
            scope.launch(Dispatchers.IO) {
                pipeStreams(targetSocket.getInputStream(), clientSocket.getOutputStream())
            }
        } catch (_: Exception) {
            try { clientSocket.close() } catch (_: Exception) {}
        }
    }

    private fun pipeStreams(input: InputStream, output: OutputStream) {
        try {
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                output.flush()
            }
        } catch (_: Exception) {
        } finally {
            try { input.close() } catch (_: Exception) {}
            try { output.close() } catch (_: Exception) {}
        }
    }

    fun stopProxy() {
        try {
            proxyJob?.cancel()
            proxyJob = null
            sslServerSocket?.close()
            sslServerSocket = null
            nativeProcess?.destroy()
            nativeProcess = null
        } catch (_: Exception) {}
        _proxyConfig.update {
            it.copy(isRunning = false)
        }
    }

    private fun createSelfSignedSSLContext(): SSLContext {
        val keyPairGen = KeyPairGenerator.getInstance("RSA")
        keyPairGen.initialize(2048, SecureRandom())
        val keyPair = keyPairGen.generateKeyPair()

        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null, null)

        val pwd = "localhost_cert_pwd".toCharArray()
        val cert = generateCertificateCompat(keyPair)
        keyStore.setKeyEntry("localhost", keyPair.private, pwd, arrayOf(cert))

        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(keyStore, pwd)

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(kmf.keyManagers, null, SecureRandom())
        return sslContext
    }

    private fun generateCertificateCompat(keyPair: java.security.KeyPair): X509Certificate {
        return SelfSignedCertHelper.createCert(keyPair)
    }
}
