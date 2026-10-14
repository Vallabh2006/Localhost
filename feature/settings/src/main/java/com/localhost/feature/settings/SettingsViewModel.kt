package com.localhost.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localhost.core.data.repository.SettingsRepository
import com.localhost.core.model.RuntimePack
import com.localhost.core.model.RuntimeType
import com.localhost.runtime.manager.ComposerManager
import com.localhost.runtime.manager.MariaDBManager
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
    private val runtimeManager: RuntimeManager,
    private val composerManager: ComposerManager,
    private val mariaDBManager: MariaDBManager
) : ViewModel() {

    val wakeLock: StateFlow<Boolean> = settingsRepository.wakeLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val wifiLock: StateFlow<Boolean> = settingsRepository.wifiLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val wifiOnly: StateFlow<Boolean> = settingsRepository.wifiOnlyMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val vpnEnabled: StateFlow<Boolean> = settingsRepository.vpnEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val accentColorName: StateFlow<String> = settingsRepository.accentColorName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Indigo")

    val secondaryColorHex: StateFlow<String> = settingsRepository.secondaryColorHex
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "#181C28")

    val successColorHex: StateFlow<String> = settingsRepository.successColorHex
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "#10B981")

    val failColorHex: StateFlow<String> = settingsRepository.failColorHex
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "#F43F5E")

    val warnColorHex: StateFlow<String> = settingsRepository.warnColorHex
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "#F59E0B")

    val themeMode: StateFlow<String> = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DARK")

    val installedPacks: StateFlow<List<RuntimePack>> = runtimeManager.installedPacks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isComposerDownloading: StateFlow<Boolean> = composerManager.isDownloading
    val composerDownloadProgress: StateFlow<Float> = composerManager.downloadProgress

    fun isComposerInstalled(): Boolean = composerManager.isInstalled()

    val isMariaDBDownloading: StateFlow<Boolean> = mariaDBManager.isDownloading
    val mariaDBDownloadProgress: StateFlow<Float> = mariaDBManager.downloadProgress
    val mariaDBStatusMessage: StateFlow<String?> = mariaDBManager.statusMessage

    fun isMariaDBInstalled(): Boolean = mariaDBManager.isInstalled()

    fun installMariaDB() {
        viewModelScope.launch {
            val res = mariaDBManager.installMariaDB()
            if (res.isFailure) {
                _errorMessage.value = "Failed to install MariaDB: ${res.exceptionOrNull()?.message}"
            }
        }
    }

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

    fun setAccentColorName(name: String) {
        viewModelScope.launch { settingsRepository.setAccentColorName(name) }
    }

    fun setSecondaryColorHex(hex: String) {
        viewModelScope.launch { settingsRepository.setSecondaryColorHex(hex) }
    }

    fun setSuccessColorHex(hex: String) {
        viewModelScope.launch { settingsRepository.setSuccessColorHex(hex) }
    }

    fun setFailColorHex(hex: String) {
        viewModelScope.launch { settingsRepository.setFailColorHex(hex) }
    }

    fun setWarnColorHex(hex: String) {
        viewModelScope.launch { settingsRepository.setWarnColorHex(hex) }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
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

    fun uninstallComposer() {
        composerManager.composerFile.delete()
    }

    fun uninstallMariaDB() {
        viewModelScope.launch {
            mariaDBManager.uninstall()
        }
    }

    fun downloadComposer() {
        viewModelScope.launch {
            val res = composerManager.downloadComposer()
            if (res.isFailure) {
                _errorMessage.value = "Failed to download Composer: ${res.exceptionOrNull()?.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
