package com.localhost.feature.monitor

import android.content.Context
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
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
                val (availRam, totalRam) = ProcessMetricsSampler.getDeviceMemory(context)
                val rt = Runtime.getRuntime()
                val heapUsed = (rt.totalMemory() - rt.freeMemory()).coerceAtLeast(0L)
                val heapTotal = rt.totalMemory()

                val currentProjects = projectRepository.getAll()
                val runningList = currentProjects.filter { it.status == ProjectStatus.RUNNING }
                val runningCount = runningList.size
                val activePorts = runningList.map { it.port }

                val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                val batteryLevel = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.coerceIn(0, 100) ?: 100
                val isCharging = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) bm?.isCharging == true else false

                val totalRx = TrafficStats.getTotalRxBytes().coerceAtLeast(0L)
                val totalTx = TrafficStats.getTotalTxBytes().coerceAtLeast(0L)
                val uptime = SystemClock.elapsedRealtime() / 1000L
                val cpuUsage = ProcessMetricsSampler.getSystemCpuUsage()
                val cpuCores = Runtime.getRuntime().availableProcessors()

                _systemMetrics.value = SystemMetrics(
                    timestamp = System.currentTimeMillis(),
                    totalCpuPercent = cpuUsage,
                    cpuCores = cpuCores,
                    availableMemoryBytes = availRam,
                    totalMemoryBytes = totalRam,
                    appHeapUsedBytes = heapUsed,
                    appHeapTotalBytes = heapTotal,
                    freeStorageBytes = freeStorage,
                    totalStorageBytes = totalStorage,
                    batteryPercent = batteryLevel,
                    isCharging = isCharging,
                    runningProjectsCount = runningCount,
                    activePorts = activePorts,
                    networkRxBytes = totalRx,
                    networkTxBytes = totalTx,
                    uptimeSeconds = uptime
                )
                _diagnostics.value = NetworkUtils.getDiagnostics(context)
                delay(2000)
            }
        }
    }
}
