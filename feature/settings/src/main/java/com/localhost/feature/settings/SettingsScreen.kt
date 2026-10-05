package com.localhost.feature.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.localhost.core.designsystem.theme.StatusYellow
import com.localhost.core.designsystem.theme.TextMuted
import com.localhost.core.designsystem.theme.TextOnAccent
import com.localhost.core.designsystem.theme.TextPrimary
import com.localhost.core.designsystem.theme.TextSecondary
import com.localhost.core.model.DashboardConfig
import com.localhost.core.model.DownloadStatus
import com.localhost.core.model.RuntimePack
import com.localhost.core.model.RuntimeType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val wakeLockActive by viewModel.wakeLock.collectAsState()
    val dashboardConfig by viewModel.dashboardConfig.collectAsState()
    val installedPacks by viewModel.installedPacks.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val snackbarHostState = remember { SnackbarHostState() }

    var isBatteryOptimized by remember { mutableStateOf(true) }
    var newPasswordInput by remember { mutableStateOf("") }
    var showPasswordDialog by remember { mutableStateOf(false) }
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
                    Text("Settings & Admin", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextPrimary)
                },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Help Guide", tint = PrimaryAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
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
                    AdminSection(
                        onUpdatePassword = { showPasswordDialog = true }
                    )

                    SystemSection(
                        context = context,
                        wakeLockActive = wakeLockActive,
                        isBatteryOptimized = isBatteryOptimized,
                        onToggleWakeLock = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setWakeLock(it)
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

                    SecurityHardeningSection()
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
                AdminSection(
                    onUpdatePassword = { showPasswordDialog = true }
                )

                SystemSection(
                    context = context,
                    wakeLockActive = wakeLockActive,
                    isBatteryOptimized = isBatteryOptimized,
                    onToggleWakeLock = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.setWakeLock(it)
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

                SecurityHardeningSection()

                RuntimesSection(
                    installedPacks = installedPacks,
                    viewModel = viewModel
                )

                AboutSection()

                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            containerColor = DarkSurfaceElevated,
            title = { Text("Update Dashboard Password", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Set a secure access password for web dashboard and remote admin endpoints.", color = TextSecondary, fontSize = 13.sp)
                    OutlinedTextField(
                        value = newPasswordInput,
                        onValueChange = { newPasswordInput = it },
                        placeholder = { Text("New password (min 6 chars)", color = TextMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = PrimaryAccent,
                            unfocusedBorderColor = DarkBorderSubtle,
                            cursorColor = PrimaryAccent,
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPasswordInput.length >= 6) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.saveDashboardConfig(dashboardConfig, newPasswordInput)
                            newPasswordInput = ""
                            showPasswordDialog = false
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = PrimaryAccent)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            containerColor = DarkSurfaceElevated,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = PrimaryAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Localhost Documentation", fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Terminal & Shell Basics", fontWeight = FontWeight.Bold, color = PrimaryAccent, fontSize = 14.sp)
                    HelpCmdItem("help", "Display built-in commands list")
                    HelpCmdItem("ls / pwd / cd", "Navigate filesystem relative to /app root")
                    HelpCmdItem("python3 / node", "Execute scripts directly via installed runtimes")
                    HelpCmdItem("pip install <pkg>", "Install Python libraries into user site-packages")
                    HelpCmdItem("npm install <pkg>", "Install Node.js packages into project directory")
                    HelpCmdItem("curl <url>", "Make HTTP requests directly from phone shell")

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Security & Networking", fontWeight = FontWeight.Bold, color = PrimaryAccent, fontSize = 14.sp)
                    Text("- Cloudflare Tunnels: Create public URLs for your local port without opening router ports.", color = TextSecondary, fontSize = 12.sp)
                    Text("- Rate Limiter: Blocks abusive request bursts automatically (120 req/min per IP).", color = TextSecondary, fontSize = 12.sp)
                    Text("- Host Header Verification: Blocks DNS rebinding attacks.", color = TextSecondary, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Background Survival", fontWeight = FontWeight.Bold, color = PrimaryAccent, fontSize = 14.sp)
                    Text("- Enable CPU Wake Lock and Battery Optimization Exemption so Android does not kill long-running web servers when the screen turns off.", color = TextSecondary, fontSize = 12.sp)
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
private fun AdminSection(
    onUpdatePassword: () -> Unit
) {
    Text("Dashboard Administration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(modifier = Modifier.weight(1f).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VpnKey, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Admin Password", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
                OutlinedButton(
                    onClick = onUpdatePassword,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, DarkBorderSubtle)
                ) {
                    Text("Change", color = PrimaryAccent, fontSize = 12.sp)
                }
            }

            Text(
                "Protects the web dashboard (http://127.0.0.1:8080/dashboard) against unauthorized requests on your local network.",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun SystemSection(
    context: Context,
    wakeLockActive: Boolean,
    isBatteryOptimized: Boolean,
    onToggleWakeLock: (Boolean) -> Unit,
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
private fun SecurityHardeningSection() {
    Text("Security & Hardening", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rate Limiter & Brute-Force Shield", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
                Text("Active (120 req/m)", color = StatusGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("OWASP Security Headers", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
                Text("Enforced", color = StatusGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryAccent, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Password Hash Protection", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
                Text("PBKDF2/SHA-256", color = PrimaryAccent, fontSize = 12.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.End)
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
