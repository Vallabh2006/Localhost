package com.localhost.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class LogStream {
    STDOUT,
    STDERR,
    SYSTEM
}

@Serializable
data class LogEntry(
    val id: Long = 0L,
    val projectId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val stream: LogStream,
    val message: String
)
