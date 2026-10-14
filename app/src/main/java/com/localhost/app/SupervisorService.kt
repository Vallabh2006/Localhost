package com.localhost.app

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.localhost.core.data.repository.SettingsRepository
import com.localhost.core.model.TunnelStatus
import com.localhost.runtime.process.ProcessSupervisor
import com.localhost.tunnel.CloudflareTunnelManager
import com.localhost.tunnel.NginxManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SupervisorService : Service() {

    @Inject
    lateinit var processSupervisor: ProcessSupervisor

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var cloudflareTunnelManager: CloudflareTunnelManager

    @Inject
    lateinit var nginxManager: NginxManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var isForegroundActive = false

    override fun onCreate() {
        super.onCreate()
        try {
            Runtime.getRuntime().exec(arrayOf("/system/bin/device_config", "put", "activity_manager", "max_phantom_processes", "2147483647"))
            Runtime.getRuntime().exec(arrayOf("/system/bin/settings", "put", "global", "settings_enable_monitor_phantom_procs", "false"))
        } catch (_: Exception) {}
        try {
            val notification = createNotification()
            val fgType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, fgType)
            isForegroundActive = true
        } catch (_: Exception) {}
        observeLocks()
        observeActiveState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            if (!isForegroundActive) {
                val notification = createNotification()
                val fgType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                }
                ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, fgType)
                isForegroundActive = true
            }
        } catch (_: Exception) {}
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        releaseLocks()
        super.onDestroy()
    }

    private fun observeActiveState() {
        scope.launch {
            combine(
                processSupervisor.runningCount,
                cloudflareTunnelManager.tunnelStatus,
                nginxManager.proxyConfig
            ) { count, tunnelStatus, proxyConfig ->
                val activeItems = mutableListOf<String>()
                if (count > 0) {
                    activeItems.add(if (count == 1) "1 Web Server running" else "$count Web Servers running")
                }
                if (tunnelStatus == TunnelStatus.CONNECTED) {
                    activeItems.add("Cloudflare Tunnel active")
                }
                if (proxyConfig.isRunning) {
                    activeItems.add("Port Forwarding (8443) active")
                }
                activeItems
            }.collect { activeItems ->
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (activeItems.isNotEmpty()) {
                    acquireWakeLock()
                    acquireWifiLock()
                    val notification = createNotification(activeItems)
                    val fgType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    } else {
                        0
                    }
                    try {
                        ServiceCompat.startForeground(this@SupervisorService, NOTIFICATION_ID, notification, fgType)
                        isForegroundActive = true
                    } catch (_: Exception) {}
                    manager.notify(NOTIFICATION_ID, notification)
                } else {
                    releaseLocks()
                    if (isForegroundActive) {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                stopForeground(STOP_FOREGROUND_REMOVE)
                            } else {
                                @Suppress("DEPRECATION")
                                stopForeground(true)
                            }
                            manager.cancel(NOTIFICATION_ID)
                            isForegroundActive = false
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    private fun observeLocks() {
        scope.launch {
            val wlEnabled = settingsRepository.wakeLockEnabled.first()
            if (wlEnabled) acquireWakeLock()

            val wifiEnabled = settingsRepository.wifiLockEnabled.first()
            if (wifiEnabled) acquireWifiLock()
        }
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock?.isHeld != true) {
                val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "localhost:SupervisorWakeLock").apply {
                    setReferenceCounted(false)
                    acquire(24 * 60 * 60 * 1000L)
                }
            }
        } catch (_: Exception) {}
    }

    private fun acquireWifiLock() {
        try {
            val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "localhost:SupervisorWifiLock").apply {
                setReferenceCounted(false)
                acquire()
            }
        } catch (_: Exception) {}
    }

    private fun releaseLocks() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
            if (wifiLock?.isHeld == true) wifiLock?.release()
        } catch (_: Exception) {}
    }

    private fun createNotification(activeItems: List<String> = emptyList()): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = when {
            activeItems.size == 1 -> activeItems[0]
            activeItems.isNotEmpty() -> "Localhost Services Active"
            else -> "Localhost"
        }

        val text = if (activeItems.isNotEmpty()) {
            activeItems.joinToString(" • ")
        } else {
            "No active services"
        }

        return NotificationCompat.Builder(this, LocalhostApplication.CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
    }
}
