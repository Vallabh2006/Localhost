package com.localhost.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class TunnelStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

@Serializable
data class TunnelConfig(
    val accountId: String = "",
    val tunnelId: String = "",
    val tunnelName: String = "localhost-phone",
    val token: String = "",
    val isQuickTunnel: Boolean = true,
    val status: TunnelStatus = TunnelStatus.DISCONNECTED,
    val activeUrl: String = "",
    val errorMessage: String? = null,
    val hostnameMappings: Map<String, String> = emptyMap()
)

@Serializable
data class SavedTunnelToken(
    val id: String,
    val name: String,
    val token: String,
    val createdAt: Long = System.currentTimeMillis(),
    val domain: String = ""
)

@Serializable
data class NginxProxyConfig(
    val isRunning: Boolean = false,
    val targetPort: Int = 8080,
    val httpsPort: Int = 8443,
    val localHttpsUrl: String = "",
    val lanHttpsUrl: String = "",
    val error: String? = null
)
