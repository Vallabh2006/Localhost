package com.localhost.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localhost.core.data.repository.SettingsRepository
import com.localhost.core.model.RuntimePack
import com.localhost.core.model.RuntimeType
import com.localhost.runtime.manager.RuntimeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val runtimeManager: RuntimeManager
) : ViewModel() {

    val wakeLock: StateFlow<Boolean> = settingsRepository.wakeLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val wifiLock: StateFlow<Boolean> = settingsRepository.wifiLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val wifiOnly: StateFlow<Boolean> = settingsRepository.wifiOnlyMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val vpnEnabled: StateFlow<Boolean> = settingsRepository.vpnEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val installedPacks: StateFlow<List<RuntimePack>> = runtimeManager.installedPacks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun setWakeLock(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setWakeLockEnabled(enabled) }
    }

    fun setWifiLock(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setWifiLockEnabled(enabled) }
    }

    fun setWifiOnly(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setWifiOnlyMode(enabled) }
    }

    fun setVpnEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setVpnEnabled(enabled) }
    }

    fun installRuntime(pack: RuntimePack) {
        _errorMessage.value = null
        runtimeManager.installRuntime(pack)
    }

    fun pauseDownload(packId: String) {
        runtimeManager.pauseDownload(packId)
    }

    fun resumeDownload(pack: RuntimePack) {
        runtimeManager.resumeDownload(pack)
    }

    fun cancelDownload(packId: String) {
        runtimeManager.cancelDownload(packId)
    }

    fun uninstallRuntime(runtime: RuntimeType) {
        viewModelScope.launch {
            runtimeManager.uninstallRuntime(runtime)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
