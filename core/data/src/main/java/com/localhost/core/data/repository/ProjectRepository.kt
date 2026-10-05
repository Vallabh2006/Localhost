package com.localhost.core.data.repository

import com.localhost.core.data.database.ProjectDao
import com.localhost.core.data.database.toDomain
import com.localhost.core.data.database.toEntity
import com.localhost.core.model.Project
import com.localhost.core.model.ProjectStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepository @Inject constructor(
    private val projectDao: ProjectDao
) {
    fun observeAll(): Flow<List<Project>> {
        return projectDao.observeAll().map { list -> list.map { it.toDomain() } }
    }

    suspend fun getAll(): List<Project> {
        return projectDao.getAll().map { it.toDomain() }
    }

    suspend fun getById(id: String): Project? {
        return projectDao.getById(id)?.toDomain()
    }

    fun observeById(id: String): Flow<Project?> {
        return projectDao.observeById(id).map { it?.toDomain() }
    }

    suspend fun save(project: Project) {
        val updated = project.copy(updatedAt = System.currentTimeMillis())
        projectDao.insert(updated.toEntity())
    }

    suspend fun updateStatus(id: String, status: ProjectStatus, pid: Long? = null, startedAt: Long? = null) {
        projectDao.updateStatus(id, status, pid, startedAt, System.currentTimeMillis())
    }

    suspend fun delete(id: String) {
        projectDao.deleteById(id)
    }
}
