package com.localhost.feature.supabase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localhost.core.data.repository.SettingsRepository
import com.localhost.core.model.SupabaseConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

@HiltViewModel
class SupabaseViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val okHttpClient: OkHttpClient
) : ViewModel() {

    val config: StateFlow<SupabaseConfig> = settingsRepository.getSupabaseConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SupabaseConfig())

    private val _testResult = MutableStateFlow<String?>(null)
    val testResult = _testResult.asStateFlow()

    fun saveConfig(projectUrl: String, anonKey: String, serviceKey: String) {
        viewModelScope.launch {
            settingsRepository.saveSupabaseConfig(
                SupabaseConfig(
                    projectUrl = projectUrl.trim(),
                    anonKey = anonKey.trim(),
                    serviceKey = serviceKey.trim(),
                    isConnected = projectUrl.isNotBlank() && anonKey.isNotBlank()
                )
            )
        }
    }

    fun testConnection() {
        val current = config.value
        if (current.projectUrl.isBlank()) {
            _testResult.value = "Please enter a Supabase URL"
            return
        }

        viewModelScope.launch {
            _testResult.value = "Testing connection..."
            withContext(Dispatchers.IO) {
                try {
                    val url = "${current.projectUrl.trimEnd('/')}/rest/v1/"
                    val req = Request.Builder()
                        .url(url)
                        .addHeader("apikey", current.anonKey)
                        .build()
                    val res = okHttpClient.newCall(req).execute()
                    if (res.isSuccessful || res.code == 404 || res.code == 200) {
                        _testResult.value = "Connection successful (HTTP ${res.code})"
                    } else {
                        _testResult.value = "HTTP error ${res.code}: ${res.message}"
                    }
                } catch (e: Exception) {
                    _testResult.value = "Connection failed: ${e.message}"
                }
            }
        }
    }
}
