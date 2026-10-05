package com.localhost.runtime.process

import android.content.Context
import com.localhost.core.data.repository.LogRepository
import com.localhost.core.data.repository.MetricsRepository
import com.localhost.core.data.repository.ProjectRepository
import com.localhost.core.model.Project
import com.localhost.core.model.ProjectStatus
import com.localhost.core.model.RestartPolicy
import com.localhost.core.model.RuntimeType
import com.localhost.runtime.manager.RuntimeManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.ServerSocket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProcessSupervisor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val projectRepository: ProjectRepository,
    private val logRepository: LogRepository,
    private val metricsRepository: MetricsRepository,
    private val runtimeManager: RuntimeManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val processes = ConcurrentHashMap<String, ManagedProcess>()
    private val crashRetryCounts = ConcurrentHashMap<String, AtomicInteger>()

    private val _runningCount = MutableStateFlow(0)
    val runningCount = _runningCount.asStateFlow()

    init {
        startMetricsSampling()
    }

    fun isPortInUse(port: Int): Boolean {
        return try {
            val socket = ServerSocket(port)
            socket.reuseAddress = true
            socket.close()
            false
        } catch (_: Exception) {
            true
        }
    }

    suspend fun startProject(project: Project): Result<Unit> {
        val safePort = if (project.port == 8080 || project.port <= 0) {
            when (project.runtime) {
                RuntimeType.STATIC -> 8081
                RuntimeType.JAVA -> 8088
                RuntimeType.PHP -> 8000
                RuntimeType.PYTHON -> 5000
                RuntimeType.NODEJS -> 3000
            }
        } else {
            project.port
        }
        val actualProject = if (safePort != project.port) {
            val updated = project.copy(
                port = safePort,
                startupCommand = project.startupCommand.replace(":${project.port}", ":$safePort")
            )
            projectRepository.save(updated)
            updated
        } else {
            project
        }

        val existing = processes[actualProject.id]
        if (existing != null) {
            if (existing.status.value == ProjectStatus.RUNNING) {
                return Result.success(Unit)
            }
            existing.stop()
            processes.remove(actualProject.id)
            delay(300)
        }

        val otherProcess = processes.values.find { it.project.port == actualProject.port && it.status.value == ProjectStatus.RUNNING }
        if (otherProcess != null && otherProcess.project.id != actualProject.id) {
            return Result.failure(Exception("Port ${actualProject.port} is already in use by '${otherProcess.project.name}'"))
        }

        val execPath = if (actualProject.runtime == RuntimeType.STATIC || actualProject.runtime == RuntimeType.PHP) {
            runtimeManager.getExecutablePath(actualProject.runtime) ?: "builtin"
        } else {
            val path = runtimeManager.getExecutablePath(actualProject.runtime)
            if (path == null) {
                return Result.failure(Exception("${actualProject.runtime.displayName} runtime is not installed. Please download it in Settings."))
            }
            path
        }

        val env = runtimeManager.getEnvironmentForRuntime(actualProject.runtime)

        val managed = ManagedProcess(
            project = actualProject,
            executablePath = execPath,
            runtimeEnv = env,
            logRepository = logRepository,
            scope = scope,
            onCrash = { crashedProc ->
                handleCrash(crashedProc)
            }
        )

        processes[actualProject.id] = managed
        managed.start()

        scope.launch {
            managed.status.collect { st ->
                projectRepository.updateStatus(actualProject.id, st, managed.pid.value, if (st == ProjectStatus.RUNNING) System.currentTimeMillis() else null)
                updateRunningCount()
                if (st == ProjectStatus.RUNNING) {
                    delay(30_000)
                    if (managed.status.value == ProjectStatus.RUNNING) {
                        crashRetryCounts[actualProject.id]?.set(0)
                    }
                }
            }
        }

        return Result.success(Unit)
    }

    suspend fun stopProject(projectId: String) {
        val proc = processes[projectId]
        proc?.stop()
        processes.remove(projectId)
        crashRetryCounts.remove(projectId)
        projectRepository.updateStatus(projectId, ProjectStatus.STOPPED, null, null)
        updateRunningCount()
    }

    suspend fun restartProject(projectId: String): Result<Unit> {
        val project = projectRepository.getById(projectId) ?: return Result.failure(Exception("Project not found"))
        crashRetryCounts[projectId]?.set(0)
        stopProject(projectId)
        var count = 0
        while (isPortInUse(project.port) && count < 10) {
            delay(200)
            count++
        }
        delay(300)
        return startProject(project)
    }

    suspend fun stopAll() {
        processes.values.forEach { it.stop() }
        processes.clear()
        crashRetryCounts.clear()
        updateRunningCount()
    }

    private fun handleCrash(proc: ManagedProcess) {
        scope.launch {
            val project = projectRepository.getById(proc.project.id) ?: return@launch
            if (project.restartPolicy == RestartPolicy.NEVER) return@launch

            val retryCounter = crashRetryCounts.getOrPut(project.id) { AtomicInteger(0) }
            val count = retryCounter.incrementAndGet()

            if (count <= 3) {
                val backoff = (project.retryBackoffMs * (1 shl (count - 1))).coerceAtLeast(3000L).coerceAtMost(15000L)
                projectRepository.updateStatus(project.id, ProjectStatus.RESTARTING)
                delay(backoff)
                startProject(project)
            } else {
                projectRepository.updateStatus(project.id, ProjectStatus.CRASHED)
            }
        }
    }

    private fun updateRunningCount() {
        _runningCount.value = processes.values.count { it.status.value == ProjectStatus.RUNNING }
    }

    private fun startMetricsSampling() {
        scope.launch {
            while (isActive) {
                delay(5000)
                processes.forEach { (projectId, proc) ->
                    if (proc.status.value == ProjectStatus.RUNNING) {
                        val metrics = ProcessMetricsSampler.sampleProcess(projectId, proc.pid.value)
                        metricsRepository.recordSample(metrics)
                    }
                }
            }
        }
    }
}
