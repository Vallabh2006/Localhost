package com.localhost.core.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.localhost.core.model.EnvironmentVar
import com.localhost.core.model.LogStream
import com.localhost.core.model.Project
import com.localhost.core.model.ProjectStatus
import com.localhost.core.model.RestartPolicy
import com.localhost.core.model.RuntimeType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromRuntimeType(value: RuntimeType): String = value.name

    @TypeConverter
    fun toRuntimeType(value: String): RuntimeType = RuntimeType.valueOf(value)

    @TypeConverter
    fun fromProjectStatus(value: ProjectStatus): String = value.name

    @TypeConverter
    fun toProjectStatus(value: String): ProjectStatus = ProjectStatus.valueOf(value)

    @TypeConverter
    fun fromRestartPolicy(value: RestartPolicy): String = value.name

    @TypeConverter
    fun toRestartPolicy(value: String): RestartPolicy = RestartPolicy.valueOf(value)

    @TypeConverter
    fun fromLogStream(value: LogStream): String = value.name

    @TypeConverter
    fun toLogStream(value: String): LogStream = LogStream.valueOf(value)

    @TypeConverter
    fun fromEnvVars(value: List<EnvironmentVar>): String = json.encodeToString(value)

    @TypeConverter
    fun toEnvVars(value: String): List<EnvironmentVar> = try {
        json.decodeFromString(value)
    } catch (_: Exception) {
        emptyList()
    }
}

@Entity(tableName = "projects")
@TypeConverters(Converters::class)
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val runtime: RuntimeType,
    val port: Int,
    val workingDir: String,
    val startupCommand: String,
    val envVars: List<EnvironmentVar>,
    val restartPolicy: RestartPolicy,
    val maxRetries: Int,
    val retryBackoffMs: Long,
    val autoStartOnLaunch: Boolean,
    val autoStartOnBoot: Boolean,
    val publicExposureEnabled: Boolean,
    val publicHostname: String,
    val memoryLimitMb: Int,
    val cpuLimitPercent: Int,
    val maxLogSizeBytes: Long,
    val status: ProjectStatus,
    val pid: Long?,
    val startedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long
)

fun ProjectEntity.toDomain(): Project = Project(
    id = id,
    name = name,
    runtime = runtime,
    port = port,
    workingDir = workingDir,
    startupCommand = startupCommand,
    envVars = envVars,
    restartPolicy = restartPolicy,
    maxRetries = maxRetries,
    retryBackoffMs = retryBackoffMs,
    autoStartOnLaunch = autoStartOnLaunch,
    autoStartOnBoot = autoStartOnBoot,
    publicExposureEnabled = publicExposureEnabled,
    publicHostname = publicHostname,
    memoryLimitMb = memoryLimitMb,
    cpuLimitPercent = cpuLimitPercent,
    maxLogSizeBytes = maxLogSizeBytes,
    status = status,
    pid = pid,
    startedAt = startedAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Project.toEntity(): ProjectEntity = ProjectEntity(
    id = id,
    name = name,
    runtime = runtime,
    port = port,
    workingDir = workingDir,
    startupCommand = startupCommand,
    envVars = envVars,
    restartPolicy = restartPolicy,
    maxRetries = maxRetries,
    retryBackoffMs = retryBackoffMs,
    autoStartOnLaunch = autoStartOnLaunch,
    autoStartOnBoot = autoStartOnBoot,
    publicExposureEnabled = publicExposureEnabled,
    publicHostname = publicHostname,
    memoryLimitMb = memoryLimitMb,
    cpuLimitPercent = cpuLimitPercent,
    maxLogSizeBytes = maxLogSizeBytes,
    status = status,
    pid = pid,
    startedAt = startedAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)

@Entity(tableName = "logs")
@TypeConverters(Converters::class)
data class LogEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val projectId: String,
    val timestamp: Long,
    val stream: LogStream,
    val message: String
)

@Entity(tableName = "metrics")
data class MetricSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val projectId: String,
    val timestamp: Long,
    val cpuPercent: Double,
    val memoryRssBytes: Long,
    val diskUsageBytes: Long,
    val networkRxBytes: Long,
    val networkTxBytes: Long
)
