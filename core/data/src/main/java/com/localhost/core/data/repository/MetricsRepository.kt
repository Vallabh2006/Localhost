package com.localhost.core.data.repository

import com.localhost.core.data.database.MetricSampleEntity
import com.localhost.core.data.database.MetricsDao
import com.localhost.core.model.ResourceMetrics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetricsRepository @Inject constructor(
    private val metricsDao: MetricsDao
) {
    fun observeRecent(projectId: String, durationMs: Long = 3600000L): Flow<List<ResourceMetrics>> {
        val since = System.currentTimeMillis() - durationMs
        return metricsDao.observeRecentMetrics(projectId, since).map { list ->
            list.map {
                ResourceMetrics(
                    id = it.id,
                    projectId = it.projectId,
                    timestamp = it.timestamp,
                    cpuPercent = it.cpuPercent,
                    memoryRssBytes = it.memoryRssBytes,
                    diskUsageBytes = it.diskUsageBytes,
                    networkRxBytes = it.networkRxBytes,
                    networkTxBytes = it.networkTxBytes
                )
            }
        }
    }

    suspend fun recordSample(sample: ResourceMetrics) {
        metricsDao.insert(
            MetricSampleEntity(
                projectId = sample.projectId,
                timestamp = sample.timestamp,
                cpuPercent = sample.cpuPercent,
                memoryRssBytes = sample.memoryRssBytes,
                diskUsageBytes = sample.diskUsageBytes,
                networkRxBytes = sample.networkRxBytes,
                networkTxBytes = sample.networkTxBytes
            )
        )
    }

    suspend fun cleanup(olderThanMs: Long = 86400000L) {
        val threshold = System.currentTimeMillis() - olderThanMs
        metricsDao.deleteOldMetrics(threshold)
    }
}
