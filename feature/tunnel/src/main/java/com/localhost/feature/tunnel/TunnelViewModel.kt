package com.localhost.feature.tunnel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localhost.core.data.repository.SettingsRepository
import com.localhost.core.model.NginxProxyConfig
import com.localhost.core.model.SavedTunnelToken
import com.localhost.core.model.TunnelConfig
import com.localhost.core.model.TunnelStatus
import com.localhost.tunnel.CloudflareTunnelManager
import com.localhost.tunnel.NginxManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TunnelViewModel @Inject constructor(
    private val tunnelManager: CloudflareTunnelManager,
    private val nginxManager: NginxManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val tokenTunnelStatus: StateFlow<TunnelStatus> = tunnelManager.tokenTunnelStatus
    val tokenErrorMessage: StateFlow<String?> = tunnelManager.tokenErrorMessage

    val forwardingStatus: StateFlow<TunnelStatus> = tunnelManager.forwardingStatus
    val forwardingActiveUrl: StateFlow<String> = tunnelManager.forwardingActiveUrl
    val forwardingErrorMessage: StateFlow<String?> = tunnelManager.forwardingErrorMessage

    val tunnelStatus: StateFlow<TunnelStatus> = tunnelManager.tokenTunnelStatus
    val activeUrl: StateFlow<String> = tunnelManager.tokenActiveUrl
    val errorMessage: StateFlow<String?> = tunnelManager.tokenErrorMessage

    val nginxProxyConfig: StateFlow<NginxProxyConfig> = nginxManager.proxyConfig

    val savedTokens: StateFlow<List<SavedTunnelToken>> = settingsRepository.getSavedTunnelTokens()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tunnelConfig: StateFlow<TunnelConfig> = settingsRepository.getTunnelConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TunnelConfig())

    fun startQuickTunnel(targetPort: Int = 8080) {
        tunnelManager.startQuickTunnel(targetPort)
    }

    fun stopForwarding() {
        tunnelManager.stopPortForwarding()
    }

    fun startNamedTunnel(token: String, domain: String? = null) {
        tunnelManager.startNamedTunnel(token, domain = domain)
    }

    fun setNamedTunnelDomain(domain: String) {
        tunnelManager.setNamedTunnelDomain(domain)
    }

    fun stopNamedTunnel() {
        tunnelManager.stopNamedTunnel()
    }

    fun stopTunnel() {
        tunnelManager.stopTunnel()
    }

    fun startNginx(targetPort: Int = 8080, httpsPort: Int = 8443) {
        viewModelScope.launch {
            nginxManager.startProxy(targetPort, httpsPort)
        }
    }

    fun stopNginx() {
        nginxManager.stopProxy()
    }

    fun saveToken(name: String, token: String) {
        if (token.isBlank()) return
        val finalName = if (name.isBlank()) "Tunnel-${token.take(6)}" else name.trim()
        val entry = SavedTunnelToken(
            id = UUID.randomUUID().toString(),
            name = finalName,
            token = token.trim()
        )
        viewModelScope.launch {
            settingsRepository.saveTunnelToken(entry)
            settingsRepository.saveTunnelConfig(tunnelConfig.value.copy(token = token.trim()))
        }
    }

    fun deleteToken(id: String) {
        viewModelScope.launch {
            settingsRepository.deleteTunnelToken(id)
        }
    }

    fun saveConfig(config: TunnelConfig) {
        viewModelScope.launch {
            settingsRepository.saveTunnelConfig(config)
        }
    }
}
