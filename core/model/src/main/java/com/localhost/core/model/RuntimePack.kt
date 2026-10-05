package com.localhost.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class DownloadStatus {
    IDLE,
    DOWNLOADING,
    PAUSED,
    EXTRACTING,
    READY,
    FAILED
}

@Serializable
data class RuntimePack(
    val id: String,
    val runtime: RuntimeType,
    val version: String,
    val abi: String,
    val sha256: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val executableRelativePath: String,
    val isInstalled: Boolean = false,
    val installPath: String? = null,
    val downloadStatus: DownloadStatus = DownloadStatus.IDLE,
    val downloadProgress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val statusMessage: String? = null
)
