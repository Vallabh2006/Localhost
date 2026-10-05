package com.localhost.core.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.localhost.core.model.ProjectStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects")
    suspend fun getAll(): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getById(id: String): ProjectEntity?

    @Query("SELECT * FROM projects WHERE id = :id")
    fun observeById(id: String): Flow<ProjectEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: ProjectEntity)

    @Update
    suspend fun update(project: ProjectEntity)

    @Query("UPDATE projects SET status = :status, pid = :pid, startedAt = :startedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: ProjectStatus, pid: Long?, startedAt: Long?, updatedAt: Long)

    @Delete
    suspend fun delete(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface LogDao {
    @Query("SELECT * FROM logs WHERE projectId = :projectId ORDER BY id DESC LIMIT :limit")
    fun observeLogs(projectId: String, limit: Int = 500): Flow<List<LogEntryEntity>>

    @Query("SELECT * FROM logs WHERE projectId = :projectId ORDER BY id ASC")
    suspend fun getLogsForProject(projectId: String): List<LogEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: LogEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<LogEntryEntity>)

    @Query("DELETE FROM logs WHERE projectId = :projectId")
    suspend fun clearLogsForProject(projectId: String)

    @Query("DELETE FROM logs WHERE id IN (SELECT id FROM logs WHERE projectId = :projectId ORDER BY id ASC LIMIT (SELECT MAX(0, COUNT(*) - :keepCount) FROM logs WHERE projectId = :projectId))")
    suspend fun trimLogs(projectId: String, keepCount: Int = 1000)
}

@Dao
interface MetricsDao {
    @Query("SELECT * FROM metrics WHERE projectId = :projectId AND timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    fun observeRecentMetrics(projectId: String, sinceTimestamp: Long): Flow<List<MetricSampleEntity>>

    @Query("SELECT * FROM metrics WHERE projectId = :projectId AND timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    suspend fun getMetricsSince(projectId: String, sinceTimestamp: Long): List<MetricSampleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sample: MetricSampleEntity)

    @Query("DELETE FROM metrics WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOldMetrics(beforeTimestamp: Long)
}
