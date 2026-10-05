package com.localhost.core.model

import kotlinx.serialization.Serializable

@Serializable
data class ProjectSnapshot(
    val id: String,
    val projectId: String,
    val message: String,
    val timestamp: Long,
    val fileCount: Int,
    val totalSizeBytes: Long,
    val archivePath: String
)
