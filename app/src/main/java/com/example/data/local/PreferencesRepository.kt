package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppLanguage
import com.example.data.model.ConnectionMode
import com.example.data.model.GatewayConfig
import com.example.data.model.GuestWifiConfig
import com.example.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.mawjaDataStore: DataStore<Preferences> by preferencesDataStore(name = "mawja_prefs")

data class UserPreferences(
    val language: AppLanguage = AppLanguage.AR,
    val themeMode: ThemeMode = ThemeMode.AUTO,
    val connectionMode: ConnectionMode = ConnectionMode.LOCAL_LAN,
    val intruderAlertsEnabled: Boolean = true,
    val guestWifi: GuestWifiConfig = GuestWifiConfig(),
    val gatewayConfig: GatewayConfig = GatewayConfig()
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CONNECTION_MODE = stringPreferencesKey("connection_mode")
        val INTRUDER_ALERTS = booleanPreferencesKey("intruder_alerts")

        val GUEST_ENABLED = booleanPreferencesKey("guest_enabled")
        val GUEST_SSID = stringPreferencesKey("guest_ssid")
        val GUEST_PASSWORD = stringPreferencesKey("guest_password")
        val GUEST_SPEED_CAP = intPreferencesKey("guest_speed_cap")
        val GUEST_DURATION_HOURS = intPreferencesKey("guest_duration_hours")

        val GATEWAY_IP = stringPreferencesKey("gateway_ip")
        val ROUTER_BRAND = stringPreferencesKey("router_brand")
        val ADMIN_USER = stringPreferencesKey("admin_user")
        val ADMIN_PASS = stringPreferencesKey("admin_pass")
    }

    val preferencesFlow: Flow<UserPreferences> = context.mawjaDataStore.data.map { prefs ->
        UserPreferences(
            language = prefs[Keys.LANGUAGE]?.let { code ->
                AppLanguage.entries.firstOrNull { it.code == code }
            } ?: AppLanguage.AR,
            themeMode = prefs[Keys.THEME_MODE]?.let { name ->
                runCatching { ThemeMode.valueOf(name) }.getOrNull()
            } ?: ThemeMode.AUTO,
            connectionMode = prefs[Keys.CONNECTION_MODE]?.let { name ->
                runCatching { ConnectionMode.valueOf(name) }.getOrNull()
            } ?: ConnectionMode.LOCAL_LAN,
            intruderAlertsEnabled = prefs[Keys.INTRUDER_ALERTS] ?: true,
            guestWifi = GuestWifiConfig(
                enabled = prefs[Keys.GUEST_ENABLED] ?: true,
                ssid = prefs[Keys.GUEST_SSID] ?: "Mawja_Guest_5G",
                password = prefs[Keys.GUEST_PASSWORD] ?: "Welcome2026",
                speedLimitMbps = prefs[Keys.GUEST_SPEED_CAP] ?: 25,
                durationHours = prefs[Keys.GUEST_DURATION_HOURS] ?: 6,
                connectedGuests = if (prefs[Keys.GUEST_ENABLED] != false) 1 else 0
            ),
            gatewayConfig = GatewayConfig(
                gatewayIp = prefs[Keys.GATEWAY_IP] ?: "192.168.1.1",
                routerBrand = prefs[Keys.ROUTER_BRAND] ?: "Google Nest / Generic",
                adminUsername = prefs[Keys.ADMIN_USER] ?: "admin",
                adminPassword = prefs[Keys.ADMIN_PASS] ?: ""
            )
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.mawjaDataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.mawjaDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setConnectionMode(mode: ConnectionMode) {
        context.mawjaDataStore.edit { it[Keys.CONNECTION_MODE] = mode.name }
    }

    suspend fun setIntruderAlertsEnabled(enabled: Boolean) {
        context.mawjaDataStore.edit { it[Keys.INTRUDER_ALERTS] = enabled }
    }

    suspend fun updateGuestWifi(config: GuestWifiConfig) {
        context.mawjaDataStore.edit {
            it[Keys.GUEST_ENABLED] = config.enabled
            it[Keys.GUEST_SSID] = config.ssid
            it[Keys.GUEST_PASSWORD] = config.password
            it[Keys.GUEST_SPEED_CAP] = config.speedLimitMbps
            it[Keys.GUEST_DURATION_HOURS] = config.durationHours
        }
    }

    suspend fun updateGatewayConfig(config: GatewayConfig) {
        context.mawjaDataStore.edit {
            it[Keys.GATEWAY_IP] = config.gatewayIp.trim()
            it[Keys.ROUTER_BRAND] = config.routerBrand.trim()
            it[Keys.ADMIN_USER] = config.adminUsername.trim()
            it[Keys.ADMIN_PASS] = config.adminPassword
        }
    }
}
