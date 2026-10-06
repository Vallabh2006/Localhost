package com.localhost.core.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.localhost.core.model.DashboardConfig
import com.localhost.core.model.SupabaseConfig
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
        val SUPABASE_URL = stringPreferencesKey("supabase_url")
        val TUNNEL_NAME = stringPreferencesKey("tunnel_name")
        val TUNNEL_IS_QUICK = booleanPreferencesKey("tunnel_is_quick")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
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

    fun getSupabaseConfig(): Flow<SupabaseConfig> = context.dataStore.data.map { prefs ->
        val url = prefs[Keys.SUPABASE_URL] ?: ""
        val anonKey = secretStore.getSecret("supabase_anon_key") ?: ""
        val serviceKey = secretStore.getSecret("supabase_service_key") ?: ""
        SupabaseConfig(
            projectUrl = url,
            anonKey = anonKey,
            serviceKey = serviceKey,
            isConnected = url.isNotBlank() && anonKey.isNotBlank()
        )
    }

    suspend fun saveSupabaseConfig(config: SupabaseConfig) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SUPABASE_URL] = config.projectUrl
        }
        if (config.anonKey.isNotBlank()) {
            secretStore.putSecret("supabase_anon_key", config.anonKey)
        }
        if (config.serviceKey.isNotBlank()) {
            secretStore.putSecret("supabase_service_key", config.serviceKey)
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
}
