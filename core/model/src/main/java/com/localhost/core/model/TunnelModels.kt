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
