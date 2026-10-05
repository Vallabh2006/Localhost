package com.localhost.core.data.repository

import com.localhost.core.data.database.LogDao
import com.localhost.core.data.database.LogEntryEntity
import com.localhost.core.model.LogEntry
import com.localhost.core.model.LogStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogRepository @Inject constructor(
    private val logDao: LogDao
) {
    fun observeLogs(projectId: String, limit: Int = 500): Flow<List<LogEntry>> {
        return logDao.observeLogs(projectId, limit).map { list ->
            list.map {
                LogEntry(
                    id = it.id,
                    projectId = it.projectId,
                    timestamp = it.timestamp,
                    stream = it.stream,
                    message = it.message
                )
            }.reversed()
        }
    }

    suspend fun getLogs(projectId: String): List<LogEntry> {
        return logDao.getLogsForProject(projectId).map {
            LogEntry(
                id = it.id,
                projectId = it.projectId,
                timestamp = it.timestamp,
                stream = it.stream,
                message = it.message
            )
        }
    }

    suspend fun append(projectId: String, stream: LogStream, message: String) {
        val entry = LogEntryEntity(
            projectId = projectId,
            timestamp = System.currentTimeMillis(),
            stream = stream,
            message = message
        )
        logDao.insert(entry)
        logDao.trimLogs(projectId, 1000)
    }

    suspend fun clear(projectId: String) {
        logDao.clearLogsForProject(projectId)
    }
}
