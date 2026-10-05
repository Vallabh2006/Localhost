package com.localhost.core.model

import kotlinx.serialization.Serializable

@Serializable
data class SupabaseConfig(
    val projectUrl: String = "",
    val anonKey: String = "",
    val serviceKey: String = "",
    val isConnected: Boolean = false
)
