package com.localhost.core.model

import kotlinx.serialization.Serializable

@Serializable
data class ResourceMetrics(
    val id: Long = 0L,
    val projectId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val cpuPercent: Double = 0.0,
    val memoryRssBytes: Long = 0L,
    val diskUsageBytes: Long = 0L,
    val networkRxBytes: Long = 0L,
    val networkTxBytes: Long = 0L
)

@Serializable
data class SystemMetrics(
    val timestamp: Long = System.currentTimeMillis(),
    val totalCpuPercent: Double = 0.0,
    val availableMemoryBytes: Long = 0L,
    val totalMemoryBytes: Long = 0L,
    val freeStorageBytes: Long = 0L,
    val totalStorageBytes: Long = 0L,
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    val runningProjectsCount: Int = 0
)
