package com.localhost.core.model

import kotlinx.serialization.Serializable

@Serializable
data class DashboardConfig(
    val port: Int = 8080,
    val isEnabled: Boolean = true,
    val requireAuth: Boolean = true,
    val username: String = "admin",
    val passwordHash: String = "",
    val sessionTimeoutMinutes: Int = 120,
    val lanOnly: Boolean = true
)
