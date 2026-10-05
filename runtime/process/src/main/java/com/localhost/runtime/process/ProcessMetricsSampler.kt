package com.localhost.runtime.process

import android.content.Context
import android.os.Debug
import android.os.Environment
import android.os.StatFs
import com.localhost.core.model.ResourceMetrics
import java.io.File
import java.io.RandomAccessFile

object ProcessMetricsSampler {

    fun sampleProcess(projectId: String, pid: Long?): ResourceMetrics {
        if (pid == null || pid <= 0) {
            return ResourceMetrics(projectId = projectId)
        }
        var rssBytes = 0L
        var cpuPercent = 0.0

        try {
            val statusFile = File("/proc/$pid/status")
            if (statusFile.exists()) {
                statusFile.forEachLine { line ->
                    if (line.startsWith("VmRSS:")) {
                        val kbStr = line.substringAfter("VmRSS:").trim().substringBefore(" ").trim()
                        rssBytes = (kbStr.toLongOrNull() ?: 0L) * 1024L
                    }
                }
            }
        } catch (_: Exception) {}

        try {
            val statFile = File("/proc/$pid/stat")
            if (statFile.exists()) {
                val parts = statFile.readText().split(" ")
                if (parts.size > 15) {
                    val utime = parts[13].toLongOrNull() ?: 0L
                    val stime = parts[14].toLongOrNull() ?: 0L
                    cpuPercent = ((utime + stime) % 100).toDouble()
                }
            }
        } catch (_: Exception) {}

        return ResourceMetrics(
            projectId = projectId,
            timestamp = System.currentTimeMillis(),
            cpuPercent = cpuPercent,
            memoryRssBytes = rssBytes
        )
    }

    fun getStorageMetrics(context: Context): Pair<Long, Long> {
        return try {
            val stat = StatFs(context.filesDir.absolutePath)
            val total = stat.totalBytes
            val free = stat.availableBytes
            Pair(free, total)
        } catch (_: Exception) {
            Pair(0L, 0L)
        }
    }
}
