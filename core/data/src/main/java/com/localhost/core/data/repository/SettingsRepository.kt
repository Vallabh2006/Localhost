package com.localhost.core.data.repository

import com.localhost.core.model.SavedTunnelToken
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.localhost.core.model.DashboardConfig
import com.localhost.core.model.TunnelConfig
import com.localhost.core.security.PasswordHasher
import com.localhost.core.security.SecretStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "localhost_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secretStore: SecretStore
) {
    private object Keys {
        val WAKE_LOCK_ENABLED = booleanPreferencesKey("wake_lock_enabled")
        val VPN_ENABLED = booleanPreferencesKey("vpn_enabled")
        val WIFI_LOCK_ENABLED = booleanPreferencesKey("wifi_lock_enabled")
        val WIFI_ONLY_MODE = booleanPreferencesKey("wifi_only_mode")
        val DASHBOARD_PORT = intPreferencesKey("dashboard_port")
        val DASHBOARD_ENABLED = booleanPreferencesKey("dashboard_enabled")
        val DASHBOARD_LAN_ONLY = booleanPreferencesKey("dashboard_lan_only")
        val DASHBOARD_USERNAME = stringPreferencesKey("dashboard_username")
        val TUNNEL_NAME = stringPreferencesKey("tunnel_name")
        val TUNNEL_IS_QUICK = booleanPreferencesKey("tunnel_is_quick")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val SAVED_TUNNEL_TOKENS = stringPreferencesKey("saved_tunnel_tokens")
    }

    val wakeLockEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.WAKE_LOCK_ENABLED] ?: true }
    val vpnEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.VPN_ENABLED] ?: false }
    val wifiLockEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.WIFI_LOCK_ENABLED] ?: true }
    val wifiOnlyMode: Flow<Boolean> = context.dataStore.data.map { it[Keys.WIFI_ONLY_MODE] ?: false }
    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDING_COMPLETED] ?: false }

    suspend fun setWakeLockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.WAKE_LOCK_ENABLED] = enabled }
    }

    suspend fun setVpnEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VPN_ENABLED] = enabled }
    }

    suspend fun setWifiLockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.WIFI_LOCK_ENABLED] = enabled }
    }

    suspend fun setWifiOnlyMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.WIFI_ONLY_MODE] = enabled }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    fun getDashboardConfig(): Flow<DashboardConfig> = context.dataStore.data.map { prefs ->
        val port = prefs[Keys.DASHBOARD_PORT] ?: 8080
        val enabled = prefs[Keys.DASHBOARD_ENABLED] ?: true
        val lanOnly = prefs[Keys.DASHBOARD_LAN_ONLY] ?: true
        val username = prefs[Keys.DASHBOARD_USERNAME] ?: "admin"
        val passwordHash = secretStore.getSecret("dashboard_password_hash") ?: PasswordHasher.hash("admin")
        DashboardConfig(
            port = port,
            isEnabled = enabled,
            requireAuth = true,
            username = username,
            passwordHash = passwordHash,
            lanOnly = lanOnly
        )
    }

    suspend fun saveDashboardConfig(config: DashboardConfig, newPasswordPlain: String? = null) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DASHBOARD_PORT] = config.port
            prefs[Keys.DASHBOARD_ENABLED] = config.isEnabled
            prefs[Keys.DASHBOARD_LAN_ONLY] = config.lanOnly
            prefs[Keys.DASHBOARD_USERNAME] = config.username
        }
        if (!newPasswordPlain.isNullOrBlank()) {
            secretStore.putSecret("dashboard_password_hash", PasswordHasher.hash(newPasswordPlain))
        }
    }

    fun getTunnelConfig(): Flow<TunnelConfig> = context.dataStore.data.map { prefs ->
        val tunnelName = prefs[Keys.TUNNEL_NAME] ?: "localhost-phone"
        val isQuick = prefs[Keys.TUNNEL_IS_QUICK] ?: true
        val token = secretStore.getSecret("cloudflare_tunnel_token") ?: ""
        val accountId = secretStore.getSecret("cloudflare_account_id") ?: ""
        val tunnelId = secretStore.getSecret("cloudflare_tunnel_id") ?: ""
        TunnelConfig(
            accountId = accountId,
            tunnelId = tunnelId,
            tunnelName = tunnelName,
            token = token,
            isQuickTunnel = isQuick
        )
    }

    suspend fun saveTunnelConfig(config: TunnelConfig) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TUNNEL_NAME] = config.tunnelName
            prefs[Keys.TUNNEL_IS_QUICK] = config.isQuickTunnel
        }
        if (config.token.isNotBlank()) {
            secretStore.putSecret("cloudflare_tunnel_token", config.token)
        }
        if (config.accountId.isNotBlank()) {
            secretStore.putSecret("cloudflare_account_id", config.accountId)
        }
        if (config.tunnelId.isNotBlank()) {
            secretStore.putSecret("cloudflare_tunnel_id", config.tunnelId)
        }
    }

    fun getSavedTunnelTokens(): Flow<List<SavedTunnelToken>> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.SAVED_TUNNEL_TOKENS] ?: "[]"
        try {
            Json.decodeFromString<List<SavedTunnelToken>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun saveTunnelToken(token: SavedTunnelToken) {
        context.dataStore.edit { prefs ->
            val raw = prefs[Keys.SAVED_TUNNEL_TOKENS] ?: "[]"
            val current = try {
                Json.decodeFromString<List<SavedTunnelToken>>(raw).toMutableList()
            } catch (_: Exception) {
                mutableListOf()
            }
            val existingIdx = current.indexOfFirst { it.id == token.id || it.token == token.token }
            if (existingIdx >= 0) {
                current[existingIdx] = token
            } else {
                current.add(0, token)
            }
            prefs[Keys.SAVED_TUNNEL_TOKENS] = Json.encodeToString<List<SavedTunnelToken>>(current)
        }
    }

    suspend fun deleteTunnelToken(tokenId: String) {
        context.dataStore.edit { prefs ->
            val raw = prefs[Keys.SAVED_TUNNEL_TOKENS] ?: "[]"
            val current = try {
                Json.decodeFromString<List<SavedTunnelToken>>(raw).filter { it.id != tokenId }
            } catch (_: Exception) {
                emptyList()
            }
            prefs[Keys.SAVED_TUNNEL_TOKENS] = Json.encodeToString<List<SavedTunnelToken>>(current)
        }
    }

}
