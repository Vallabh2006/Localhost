package com.localhost.core.data.repository

import android.content.Context
import com.localhost.core.common.FileUtils
import com.localhost.core.model.Project
import com.localhost.core.model.ProjectSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SnapshotRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val snapshotsFlowMap = mutableMapOf<String, MutableStateFlow<List<ProjectSnapshot>>>()

    private fun getBaseDir(projectId: String): File {
        return File(context.filesDir, "snapshots/$projectId").apply { mkdirs() }
    }

    private fun getIndexFile(projectId: String): File {
        return File(getBaseDir(projectId), "index.json")
    }

    private fun getFlow(projectId: String): MutableStateFlow<List<ProjectSnapshot>> {
        return snapshotsFlowMap.getOrPut(projectId) {
            MutableStateFlow(loadList(projectId))
        }
    }

    private fun loadList(projectId: String): List<ProjectSnapshot> {
        val index = getIndexFile(projectId)
        if (!index.exists()) return emptyList()
        return try {
            json.decodeFromString<List<ProjectSnapshot>>(index.readText())
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveList(projectId: String, list: List<ProjectSnapshot>) {
        val index = getIndexFile(projectId)
        try {
            index.writeText(json.encodeToString(list))
        } catch (_: Exception) {}
        getFlow(projectId).value = list
    }

    fun observeSnapshots(projectId: String): Flow<List<ProjectSnapshot>> {
        val flow = getFlow(projectId)
        flow.value = loadList(projectId)
        return flow.asStateFlow()
    }

    suspend fun getSnapshots(projectId: String): List<ProjectSnapshot> = withContext(Dispatchers.IO) {
        loadList(projectId)
    }

    suspend fun createSnapshot(project: Project, message: String): Result<ProjectSnapshot> = withContext(Dispatchers.IO) {
        try {
            val projectDir = File(project.workingDir)
            if (!projectDir.exists()) return@withContext Result.failure(Exception("Project directory does not exist"))

            val snapshotId = UUID.randomUUID().toString().take(8)
            val archiveFile = File(getBaseDir(project.id), "commit_$snapshotId.zip")

            FileOutputStream(archiveFile).use { fos ->
                FileUtils.zipDirectory(projectDir, fos)
            }

            val files = projectDir.walkTopDown().filter { it.isFile }.toList()
            val totalSize = files.sumOf { it.length() }

            val snapshot = ProjectSnapshot(
                id = snapshotId,
                projectId = project.id,
                message = message.ifBlank { "Commit $snapshotId" },
                timestamp = System.currentTimeMillis(),
                fileCount = files.size,
                totalSizeBytes = totalSize,
                archivePath = archiveFile.absolutePath
            )

            val current = loadList(project.id)
            val updated = listOf(snapshot) + current
            saveList(project.id, updated)

            Result.success(snapshot)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rollbackSnapshot(project: Project, snapshot: ProjectSnapshot): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val archive = File(snapshot.archivePath)
            if (!archive.exists()) return@withContext Result.failure(Exception("Snapshot archive file not found"))

            val projectDir = File(project.workingDir)
            if (projectDir.exists()) {
                projectDir.listFiles()?.forEach { file ->
                    if (file.name != ".localhost_vcs") {
                        if (file.isDirectory) file.deleteRecursively() else file.delete()
                    }
                }
            } else {
                projectDir.mkdirs()
            }

            FileInputStream(archive).use { fis ->
                FileUtils.unzip(fis, projectDir)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSnapshot(snapshot: ProjectSnapshot): Boolean = withContext(Dispatchers.IO) {
        try {
            val archive = File(snapshot.archivePath)
            if (archive.exists()) archive.delete()
            val current = loadList(snapshot.projectId)
            val updated = current.filter { it.id != snapshot.id }
            saveList(snapshot.projectId, updated)
            true
        } catch (_: Exception) {
            false
        }
    }
}
