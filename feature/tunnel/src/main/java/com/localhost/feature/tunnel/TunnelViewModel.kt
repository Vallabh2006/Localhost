package com.localhost.feature.tunnel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localhost.core.data.repository.SettingsRepository
import com.localhost.core.model.TunnelConfig
import com.localhost.core.model.TunnelStatus
import com.localhost.tunnel.CloudflareTunnelManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TunnelViewModel @Inject constructor(
    private val tunnelManager: CloudflareTunnelManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val tunnelStatus: StateFlow<TunnelStatus> = tunnelManager.tunnelStatus
    val activeUrl: StateFlow<String> = tunnelManager.activeUrl
    val errorMessage: StateFlow<String?> = tunnelManager.errorMessage

    val tunnelConfig: StateFlow<TunnelConfig> = settingsRepository.getTunnelConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TunnelConfig())

    fun startQuickTunnel(targetPort: Int? = null) {
        if (targetPort != null && targetPort > 0) {
            tunnelManager.startQuickTunnel(targetPort)
        } else {
            viewModelScope.launch {
                val config = settingsRepository.getDashboardConfig().first()
                tunnelManager.startQuickTunnel(config.port)
            }
        }
    }

    fun startNamedTunnel(token: String, tunnelId: String? = null) {
        tunnelManager.startNamedTunnel(token, tunnelId)
    }

    fun stopTunnel() {
        tunnelManager.stopTunnel()
    }

    fun saveConfig(config: TunnelConfig) {
        viewModelScope.launch {
            settingsRepository.saveTunnelConfig(config)
        }
    }
}
