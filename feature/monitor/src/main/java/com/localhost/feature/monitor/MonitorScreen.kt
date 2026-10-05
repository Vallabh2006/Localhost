package com.localhost.feature.monitor

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localhost.core.common.NetworkDiagnostics
import com.localhost.core.designsystem.component.StatusChip
import com.localhost.core.designsystem.theme.DarkBackground
import com.localhost.core.designsystem.theme.DarkBorder
import com.localhost.core.designsystem.theme.DarkBorderSubtle
import com.localhost.core.designsystem.theme.DarkSurface
import com.localhost.core.designsystem.theme.DarkSurfaceElevated
import com.localhost.core.designsystem.theme.PrimaryAccent
import com.localhost.core.designsystem.theme.StatusGreen
import com.localhost.core.designsystem.theme.StatusOrange
import com.localhost.core.designsystem.theme.StatusYellow
import com.localhost.core.designsystem.theme.TextMuted
import com.localhost.core.designsystem.theme.TextPrimary
import com.localhost.core.designsystem.theme.TextSecondary
import com.localhost.core.model.Project
import com.localhost.core.model.ProjectStatus
import com.localhost.core.model.SystemMetrics
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorScreen(
    viewModel: MonitorViewModel,
    onProjectClick: (String) -> Unit = {}
) {
    val stats by viewModel.systemMetrics.collectAsState()
    val diagnostics by viewModel.diagnostics.collectAsState()
    val projects by viewModel.projects.collectAsState()

    val haptics = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text("Server Monitor", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextPrimary)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        }
    ) { padding ->
        if (isLandscape) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SystemMetricsCard(stats)
                }

                item {
                    NetworkDiagnosticsCard(diagnostics)
                }

                item(span = { GridItemSpan(2) }) {
                    Text(
                        text = "Process Control Center (${projects.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (projects.isEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("No hosted servers created yet.", color = TextMuted, fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    items(projects, key = { it.id }) { project ->
                        ProjectMonitorCard(
                            project = project,
                            onClick = { onProjectClick(project.id) },
                            onStart = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.startProject(project)
                            },
                            onStop = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.stopProject(project.id)
                            },
                            onRestart = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.restartProject(project.id)
                            }
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SystemMetricsCard(stats)
                }

                item {
                    NetworkDiagnosticsCard(diagnostics)
                }

                item {
                    Text(
                        text = "Process Control Center (${projects.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (projects.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("No hosted servers created yet.", color = TextMuted, fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    items(projects, key = { it.id }) { project ->
                        ProjectMonitorCard(
                            project = project,
                            onClick = { onProjectClick(project.id) },
                            onStart = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.startProject(project)
                            },
                            onStop = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.stopProject(project.id)
                            },
                            onRestart = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.restartProject(project.id)
                            }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }
}

@Composable
private fun SystemMetricsCard(stats: SystemMetrics) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("System Telemetry", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("CPU Load", color = TextSecondary, fontSize = 12.sp)
                    Text(String.format(Locale.US, "%.1f%%", stats.totalCpuPercent), color = PrimaryAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                LinearProgressIndicator(
                    progress = { (stats.totalCpuPercent.toFloat() / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = if (stats.totalCpuPercent > 80.0) StatusOrange else PrimaryAccent,
                    trackColor = DarkSurfaceElevated
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val usedMem = (stats.totalMemoryBytes - stats.availableMemoryBytes).coerceAtLeast(0L)
                val memPct = if (stats.totalMemoryBytes > 0) (usedMem.toFloat() / stats.totalMemoryBytes.toFloat()) * 100f else 0f
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("App Memory", color = TextSecondary, fontSize = 12.sp)
                    Text("${formatBytes(usedMem)} / ${formatBytes(stats.totalMemoryBytes)} (${String.format(Locale.US, "%.0f%%", memPct)})", color = StatusGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                LinearProgressIndicator(
                    progress = { (memPct / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = StatusGreen,
                    trackColor = DarkSurfaceElevated
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val usedDisk = (stats.totalStorageBytes - stats.freeStorageBytes).coerceAtLeast(0L)
                val diskPct = if (stats.totalStorageBytes > 0) (usedDisk.toFloat() / stats.totalStorageBytes.toFloat()) * 100f else 0f
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Storage Available", color = TextSecondary, fontSize = 12.sp)
                    Text("${formatBytes(stats.freeStorageBytes)} free", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                }
                LinearProgressIndicator(
                    progress = { (diskPct / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = PrimaryAccent,
                    trackColor = DarkSurfaceElevated
                )
            }
        }
    }
}

@Composable
private fun NetworkDiagnosticsCard(diagnostics: NetworkDiagnostics) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Network Diagnostics", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                }

                if (diagnostics.isVpnActive) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(PrimaryAccent.copy(alpha = 0.15f))
                            .border(1.dp, PrimaryAccent.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("VPN Active", color = PrimaryAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkBackground)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Direct Network", color = TextMuted, fontSize = 11.sp)
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Host IP", color = TextMuted, fontSize = 13.sp)
                Text(diagnostics.localIp ?: "127.0.0.1", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = PrimaryAccent, fontSize = 13.sp)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Transport Type", color = TextMuted, fontSize = 13.sp)
                Text(diagnostics.type.name, fontWeight = FontWeight.Medium, color = TextPrimary, fontSize = 13.sp)
            }

            if (diagnostics.activeInterfaces.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Interfaces", color = TextMuted, fontSize = 13.sp)
                    Text(diagnostics.activeInterfaces.take(3).joinToString(separator = ", "), fontFamily = FontFamily.Monospace, color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ProjectMonitorCard(
    project: Project,
    onClick: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit
) {
    val isRunning = project.status == ProjectStatus.RUNNING
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isRunning) PrimaryAccent.copy(alpha = 0.4f) else DarkBorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(project.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(":${project.port}", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = PrimaryAccent, fontWeight = FontWeight.Bold)
                    Text("-", color = TextMuted)
                    Text(project.runtime.displayName, fontSize = 12.sp, color = TextSecondary)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                StatusChip(status = project.status)

                Spacer(modifier = Modifier.width(4.dp))

                if (isRunning) {
                    IconButton(
                        onClick = onRestart,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Restart", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = onStop,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop", tint = StatusYellow, modifier = Modifier.size(20.dp))
                    }
                } else {
                    IconButton(
                        onClick = onStart,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Start", tint = StatusGreen, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
    val pre = "KMGTPE"[exp - 1]
    return String.format(Locale.US, "%.1f %sB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
}
