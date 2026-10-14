package com.localhost.core.model

import kotlinx.serialization.Serializable

@Serializable
data class RequestRecord(
    val id: Long = 0L,
    val projectId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val method: String,
    val path: String,
    val statusCode: Int,
    val responseBytes: Long = 0L,
    val durationMs: Long = 0L,
    val errorMessage: String? = null
)
