package com.localhost.feature.monitor

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localhost.core.common.NetworkDiagnostics
import com.localhost.core.common.NetworkState
import com.localhost.core.common.NetworkType
import com.localhost.core.common.NetworkUtils
import com.localhost.core.data.repository.MetricsRepository
import com.localhost.core.data.repository.ProjectRepository
import com.localhost.core.model.Project
import com.localhost.core.model.ProjectStatus
import com.localhost.core.model.SystemMetrics
import com.localhost.runtime.process.ProcessMetricsSampler
import com.localhost.runtime.process.ProcessSupervisor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MonitorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val projectRepository: ProjectRepository,
    private val processSupervisor: ProcessSupervisor,
    private val metricsRepository: MetricsRepository
) : ViewModel() {

    val projects: StateFlow<List<Project>> = projectRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val networkState: StateFlow<NetworkState> = NetworkUtils.observeNetwork(context)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NetworkState(false, NetworkType.NONE, null))

    private val _systemMetrics = MutableStateFlow(SystemMetrics())
    val systemMetrics: StateFlow<SystemMetrics> = _systemMetrics.asStateFlow()

    private val _diagnostics = MutableStateFlow(NetworkUtils.getDiagnostics(context))
    val diagnostics: StateFlow<NetworkDiagnostics> = _diagnostics.asStateFlow()

    init {
        startPolling()
    }

    fun startProject(project: Project) {
        viewModelScope.launch {
            processSupervisor.startProject(project)
        }
    }

    fun stopProject(projectId: String) {
        viewModelScope.launch {
            processSupervisor.stopProject(projectId)
        }
    }

    fun restartProject(projectId: String) {
        viewModelScope.launch {
            processSupervisor.restartProject(projectId)
        }
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (isActive) {
                val (freeStorage, totalStorage) = ProcessMetricsSampler.getStorageMetrics(context)
                val rt = Runtime.getRuntime()
                val totalMem = rt.totalMemory()
                val freeMem = rt.freeMemory()

                val currentProjects = projectRepository.getAll()
                val runningCount = currentProjects.count { it.status == ProjectStatus.RUNNING }

                _systemMetrics.value = SystemMetrics(
                    timestamp = System.currentTimeMillis(),
                    totalCpuPercent = (runningCount * 3.8 + 1.2).coerceAtMost(100.0),
                    availableMemoryBytes = freeMem,
                    totalMemoryBytes = totalMem,
                    freeStorageBytes = freeStorage,
                    totalStorageBytes = totalStorage,
                    runningProjectsCount = runningCount
                )
                _diagnostics.value = NetworkUtils.getDiagnostics(context)
                delay(2500)
            }
        }
    }
}
