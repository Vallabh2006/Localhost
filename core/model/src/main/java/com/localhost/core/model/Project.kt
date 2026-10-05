package com.localhost.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Project(
    val id: String,
    val name: String,
    val runtime: RuntimeType,
    val port: Int,
    val workingDir: String,
    val startupCommand: String,
    val envVars: List<EnvironmentVar> = emptyList(),
    val restartPolicy: RestartPolicy = RestartPolicy.ON_CRASH,
    val maxRetries: Int = 5,
    val retryBackoffMs: Long = 2000L,
    val autoStartOnLaunch: Boolean = false,
    val autoStartOnBoot: Boolean = false,
    val publicExposureEnabled: Boolean = false,
    val publicHostname: String = "",
    val memoryLimitMb: Int = 256,
    val cpuLimitPercent: Int = 100,
    val maxLogSizeBytes: Long = 5 * 1024 * 1024L,
    val status: ProjectStatus = ProjectStatus.STOPPED,
    val pid: Long? = null,
    val startedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
