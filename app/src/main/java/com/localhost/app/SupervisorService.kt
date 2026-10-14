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
            val notification = createNotification(
                processSupervisor.runningCount.value,
                cloudflareTunnelManager.tunnelStatus.value == TunnelStatus.CONNECTED,
                nginxManager.proxyConfig.value.isRunning
            )
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
                val notification = createNotification(
                    processSupervisor.runningCount.value,
                    cloudflareTunnelManager.tunnelStatus.value == TunnelStatus.CONNECTED,
                    nginxManager.proxyConfig.value.isRunning
                )
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
                Triple(count, tunnelStatus == TunnelStatus.CONNECTED, proxyConfig.isRunning)
            }.collect { (count, isCfRunning, isNginxRunning) ->
                val isTunnelRunning = isCfRunning
                val isAnyActive = count > 0 || isTunnelRunning

                if (isAnyActive) {
                    acquireWakeLock()
                    acquireWifiLock()
                }

                val notification = createNotification(count, isCfRunning, isNginxRunning)
                try {
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.notify(NOTIFICATION_ID, notification)
                } catch (_: Exception) {}
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

    private fun createNotification(runningCount: Int, isCfRunning: Boolean, isNginxRunning: Boolean = false): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = when {
            runningCount > 0 && (isCfRunning) -> "Localhost Active"
            runningCount > 0 -> "Localhost Server Active"
            isCfRunning -> "Cloudflare Tunnel Active"
                        else -> "Localhost Platform"
        }

        val text = when {
            runningCount > 0 && (isCfRunning) -> "$runningCount server(s) running • Tunnel active"
            runningCount > 0 -> "$runningCount server(s) running"
            isCfRunning -> "Tunnel active and routing traffic"
                        else -> "Localhost service standby"
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
