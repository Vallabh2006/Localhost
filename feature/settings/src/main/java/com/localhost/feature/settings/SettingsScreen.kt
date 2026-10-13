package com.localhost.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localhost.core.designsystem.theme.DarkBackground
import com.localhost.core.designsystem.theme.DarkBorder
import com.localhost.core.designsystem.theme.DarkBorderSubtle
import com.localhost.core.designsystem.theme.DarkSurface
import com.localhost.core.designsystem.theme.DarkSurfaceElevated
import com.localhost.core.designsystem.theme.PrimaryAccent
import com.localhost.core.designsystem.theme.PrimaryAccentContainer
import com.localhost.core.designsystem.theme.StatusGreen
import com.localhost.core.designsystem.theme.StatusOrange
import com.localhost.core.designsystem.theme.StatusRed
import com.localhost.core.designsystem.theme.TextMuted
import com.localhost.core.designsystem.theme.TextOnAccent
import com.localhost.core.designsystem.theme.TextPrimary
import com.localhost.core.designsystem.theme.TextSecondary
import com.localhost.core.model.DownloadStatus
import com.localhost.core.model.RuntimePack
import com.localhost.core.model.RuntimeType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val wakeLockActive by viewModel.wakeLock.collectAsState()
    val vpnActive by viewModel.vpnEnabled.collectAsState()
    val installedPacks by viewModel.installedPacks.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val snackbarHostState = remember { SnackbarHostState() }

    var isBatteryOptimized by remember { mutableStateOf(true) }
    var showHelpDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        isBatteryOptimized = if (pm != null) !pm.isIgnoringBatteryOptimizations(context.packageName) else false
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text("Settings", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Help Guide", tint = PrimaryAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface),
                modifier = Modifier.height(48.dp)
            )
        }
    ) { padding ->
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SystemSection(
                        context = context,
                        wakeLockActive = wakeLockActive,
                        vpnActive = vpnActive,
                        isBatteryOptimized = isBatteryOptimized,
                        onToggleWakeLock = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setWakeLock(it)
                        },
                        onToggleVpn = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setVpnEnabled(it)
                        },
                        onRequestBattery = {
                            try {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                context.startActivity(intent)
                            }
                        }
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RuntimesSection(
                        installedPacks = installedPacks,
                        viewModel = viewModel
                    )

                    AppearanceSection()

                AboutSection()
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SystemSection(
                    context = context,
                    wakeLockActive = wakeLockActive,
                    vpnActive = vpnActive,
                    isBatteryOptimized = isBatteryOptimized,
                    onToggleWakeLock = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.setWakeLock(it)
                    },
                    onToggleVpn = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.setVpnEnabled(it)
                    },
                    onRequestBattery = {
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            context.startActivity(intent)
                        }
                    }
                )

                RuntimesSection(
                    installedPacks = installedPacks,
                    viewModel = viewModel
                )

                AppearanceSection()

                AboutSection()

                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            containerColor = DarkSurfaceElevated,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = PrimaryAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Localhost Documentation & Help", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Bash & Terminal Commands", fontWeight = FontWeight.Bold, color = PrimaryAccent, fontSize = 14.sp)
                    HelpCmdItem("ls [-la / -lh / -ls a]", "List files with details, hidden files, or human-readable sizes")
                    HelpCmdItem("cd <path> / cd - / cd ~", "Navigate directories relative to app root")
                    HelpCmdItem("cat [-n] <file>", "Output file content with optional line numbers")
                    HelpCmdItem("head [-n] / tail [-n]", "Output first or last lines of a file")
                    HelpCmdItem("mkdir [-p] / touch <file>", "Create folders recursively or touch files")
                    HelpCmdItem("rm [-rf] / cp [-r] / mv", "Remove, copy, or move files and directories")
                    HelpCmdItem("grep [-i / -r / -n] <kw>", "Search text patterns in files")
                    HelpCmdItem("find [-name <pat>]", "Find files by name or pattern")
                    HelpCmdItem("curl [-o <file>] <url>", "Fetch HTTP content or download files")
                    HelpCmdItem("chmod [+x / 755] <file>", "Modify file read/write/execute permissions")
                    HelpCmdItem("start / stop / restart <id>", "Manage background project daemons")
                    HelpCmdItem("composer <cmd>", "Run PHP Composer package manager commands")
                    HelpCmdItem("python3 / node / php / java", "Execute runtime scripts directly")

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Local Nginx & HTTPS Tunnels", fontWeight = FontWeight.Bold, color = PrimaryAccent, fontSize = 14.sp)
                    Text("- Local Nginx HTTPS Proxy: Forwards local project ports to local port 8443 over SSL/TLS with auto-generated certificates.", color = TextSecondary, fontSize = 12.sp)
                    Text("- Cloudflare Named Tunnels: Use saved named tunnel tokens to temporarily expose your site to the public with HTTPS until stopped.", color = TextSecondary, fontSize = 12.sp)
                    Text("- Saved Tokens: Save and switch between your production and staging tunnel tokens with a single tap.", color = TextSecondary, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Languages & Package Management", fontWeight = FontWeight.Bold, color = PrimaryAccent, fontSize = 14.sp)
                    Text("- PHP + Composer: Download composer.phar directly in app to manage PHP dependencies (composer require / install).", color = TextSecondary, fontSize = 12.sp)
                    Text("- Python: Isolated .venv environments with pip package installer.", color = TextSecondary, fontSize = 12.sp)
                    Text("- Java: OpenJDK 17 execution of standard JAR files and Java source.", color = TextSecondary, fontSize = 12.sp)
                    Text("- Node.js: Full NPM package management inside project directories.", color = TextSecondary, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Projects & File Explorer", fontWeight = FontWeight.Bold, color = PrimaryAccent, fontSize = 14.sp)
                    Text("- GitHub Import: Paste any GitHub repo URL (e.g. github.com/user/repo) to download and auto-configure a project.", color = TextSecondary, fontSize = 12.sp)
                    Text("- File Context Actions: Long-press any file row to Rename, View File Details, Download, Cut, Copy, or Delete.", color = TextSecondary, fontSize = 12.sp)
                    Text("- Address Bar: Tap any breadcrumb chip to jump to parent folders, or edit path to see recent project folders.", color = TextSecondary, fontSize = 12.sp)
                    Text("- Code Editor: Syntax highlighting for Python, JS, PHP, Java, HTML, CSS + auto-complete suggestions for identifiers.", color = TextSecondary, fontSize = 12.sp)
                    Text("- Markdown Preview: Toggle Preview mode in .md files to read rendered GitHub-style documentation.", color = TextSecondary, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Background Execution", fontWeight = FontWeight.Bold, color = PrimaryAccent, fontSize = 14.sp)
                    Text("- Battery Optimization: Request exemption so Android system does not kill servers when screen is locked.", color = TextSecondary, fontSize = 12.sp)
                    Text("- CPU Wake Lock: Keeps the device CPU running for persistent background web servers.", color = TextSecondary, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showHelpDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Got it", color = TextOnAccent, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun SystemSection(
    context: Context,
    wakeLockActive: Boolean,
    vpnActive: Boolean,
    isBatteryOptimized: Boolean,
    onToggleWakeLock: (Boolean) -> Unit,
    onToggleVpn: (Boolean) -> Unit,
    onRequestBattery: () -> Unit
) {
    Text("System & Performance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = if (wakeLockActive) StatusGreen else TextMuted, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("CPU WakeLock", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("Prevents phone CPU from sleeping while servers run", color = TextSecondary, fontSize = 11.sp)
                    }
                }
                Switch(
                    checked = wakeLockActive,
                    onCheckedChange = onToggleWakeLock,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TextOnAccent,
                        checkedTrackColor = PrimaryAccent,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurfaceElevated
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VpnKey, contentDescription = null, tint = if (vpnActive) StatusGreen else TextMuted, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("VPN Mode", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("Allow networking and tunnel routing over active VPN", color = TextSecondary, fontSize = 11.sp)
                    }
                }
                Switch(
                    checked = vpnActive,
                    onCheckedChange = onToggleVpn,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TextOnAccent,
                        checkedTrackColor = PrimaryAccent,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurfaceElevated
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.BatteryAlert,
                        contentDescription = null,
                        tint = if (!isBatteryOptimized) StatusGreen else StatusOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Battery Exemption", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text(
                            if (!isBatteryOptimized) "Exempted (Background execution active)" else "Restricted (Android may pause servers)",
                            color = if (!isBatteryOptimized) StatusGreen else StatusOrange,
                            fontSize = 11.sp
                        )
                    }
                }

                if (isBatteryOptimized) {
                    Button(
                        onClick = onRequestBattery,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccentContainer),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Allow", color = PrimaryAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Icon(Icons.Default.Check, contentDescription = "Active", tint = StatusGreen, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun RuntimesSection(
    installedPacks: List<RuntimePack>,
    viewModel: SettingsViewModel
) {
    Text("Runtime Environments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        installedPacks.forEach { pack ->
            RuntimePackItem(
                pack = pack,
                onInstall = { viewModel.installRuntime(pack) },
                onPause = { viewModel.pauseDownload(pack.id) },
                onResume = { viewModel.resumeDownload(pack) },
                onCancel = { viewModel.cancelDownload(pack.id) },
                onUninstall = { viewModel.uninstallRuntime(pack.runtime) }
            )
        }
    }
}

@Composable
private fun AboutSection() {
    Text("About Localhost", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Version", color = TextMuted, fontSize = 13.sp)
                Text("2.5.0-native", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Root Path", color = TextMuted, fontSize = 13.sp)
                Text("/app (com.localhost.app)", color = PrimaryAccent, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Architecture", color = TextMuted, fontSize = 13.sp)
                Text(System.getProperty("os.arch") ?: "arm64-v8a", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun HelpCmdItem(cmd: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = DarkSurface,
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, DarkBorderSubtle)
        ) {
            Text(
                cmd,
                color = PrimaryAccent,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(desc, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun RuntimePackItem(
    pack: RuntimePack,
    onInstall: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onUninstall: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(pack.runtime.displayName, fontWeight = FontWeight.Bold, color = TextPrimary)
                    val sizeLabel = if (pack.sizeBytes > 0) "${pack.sizeBytes / (1024 * 1024)}MB" else "Built-in"
                    Text("v${pack.version} - $sizeLabel", color = TextMuted, fontSize = 12.sp)
                }

                when {
                    pack.isInstalled || pack.downloadStatus == DownloadStatus.READY -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = "Installed", tint = StatusGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.padding(2.dp))
                            Text("Ready", color = StatusGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            if (pack.runtime != RuntimeType.STATIC) {
                                Spacer(modifier = Modifier.padding(4.dp))
                                IconButton(onClick = onUninstall, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Uninstall", tint = StatusRed, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    pack.downloadStatus == DownloadStatus.DOWNLOADING -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${(pack.downloadProgress * 100).toInt()}%", color = PrimaryAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(onClick = onPause, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Pause, contentDescription = "Pause", tint = PrimaryAccent, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = onCancel, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = StatusRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    pack.downloadStatus == DownloadStatus.PAUSED -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Paused", color = StatusOrange, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(onClick = onResume, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = StatusOrange, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = onCancel, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = StatusRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    pack.downloadStatus == DownloadStatus.EXTRACTING -> {
                        Text("Extracting...", color = PrimaryAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    pack.downloadStatus == DownloadStatus.FAILED -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Failed", color = StatusRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(onClick = onInstall, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = PrimaryAccent, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    else -> {
                        Button(
                            onClick = onInstall,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = TextOnAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Install", color = TextOnAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (pack.downloadStatus == DownloadStatus.DOWNLOADING || pack.downloadStatus == DownloadStatus.PAUSED || pack.downloadStatus == DownloadStatus.EXTRACTING) {
                Spacer(modifier = Modifier.height(8.dp))
                if (pack.downloadStatus == DownloadStatus.EXTRACTING) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = PrimaryAccent
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { pack.downloadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = if (pack.downloadStatus == DownloadStatus.PAUSED) StatusOrange else PrimaryAccent
                    )
                }

                if (!pack.statusMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(pack.statusMessage ?: "", color = TextMuted, fontSize = 11.sp)
                }
            }
        }
    }
}


@Composable
private fun AppearanceSection() {
    var selectedAccent by remember { mutableStateOf("Emerald") }
    var isCompactMode by remember { mutableStateOf(false) }

    val accents = listOf(
        Pair("Emerald", Color(0xFF10B981)),
        Pair("Cyan", Color(0xFF06B6D4)),
        Pair("Indigo", Color(0xFF6366F1)),
        Pair("Amber", Color(0xFFF59E0B)),
        Pair("Rose", Color(0xFFF43F5E))
    )

    Text("Appearance & UI Accents", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Accent Color Palette", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                accents.forEach { (name, color) ->
                    val isSelected = selectedAccent == name
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) androidx.compose.ui.graphics.Color.White else DarkBorder,
                                shape = CircleShape
                            )
                            .clickable { selectedAccent = name },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Compact UI Mode", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                    Text("Tighter padding and minimal card spacing across views", fontSize = 11.sp, color = TextSecondary)
                }
                Switch(
                    checked = isCompactMode,
                    onCheckedChange = { isCompactMode = it }
                )
            }
        }
    }
}
