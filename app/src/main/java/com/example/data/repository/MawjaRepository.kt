package com.example.data.repository

import android.content.Context
import com.example.data.local.MawjaDatabase
import com.example.data.local.PreferencesRepository
import com.example.data.local.UserPreferences
import com.example.data.model.AccessSchedule
import com.example.data.model.AppLanguage
import com.example.data.model.ConnectionMode
import com.example.data.model.GatewayConfig
import com.example.data.model.GuestWifiConfig
import com.example.data.model.NetworkDevice
import com.example.data.model.RouterGatewayStatus
import com.example.data.model.SecurityAlert
import com.example.data.model.SpeedTestRecord
import com.example.data.model.ThemeMode
import com.example.data.network.DemoNetworkEngine
import com.example.data.network.LocalNetworkEngine
import com.example.data.network.NotificationHelper
import com.example.data.network.SpeedTestResult
import kotlinx.coroutines.flow.Flow

class MawjaRepository(context: Context) {

    private val db = MawjaDatabase.getInstance(context)
    private val deviceDao = db.deviceDao()
    private val scheduleDao = db.scheduleDao()
    private val speedTestDao = db.speedTestDao()
    private val alertDao = db.securityAlertDao()

    val preferences = PreferencesRepository(context)
    val localNetwork = LocalNetworkEngine(context)
    val notifications = NotificationHelper(context)

    val preferencesFlow: Flow<UserPreferences> = preferences.preferencesFlow
    val schedulesFlow: Flow<List<AccessSchedule>> = scheduleDao.observeAll()
    val speedTestsFlow: Flow<List<SpeedTestRecord>> = speedTestDao.observeRecent()
    val alertsFlow: Flow<List<SecurityAlert>> = alertDao.observeAlerts()
    val unreadAlertsCountFlow: Flow<Int> = alertDao.observeUnreadCount()

    fun observeDevices(mode: ConnectionMode): Flow<List<NetworkDevice>> =
        deviceDao.observeDevices(isDemo = mode == ConnectionMode.DEMO)

    suspend fun ensureSeededData() {
        if (scheduleDao.count() == 0) {
            DemoNetworkEngine.seedDefaultSchedules().forEach { scheduleDao.upsert(it) }
        }
        val demoDevices = deviceDao.getDevicesSnapshot(isDemo = true)
        if (demoDevices.isEmpty()) {
            val seeded = DemoNetworkEngine.seedDemoDevices()
            deviceDao.upsertAll(seeded)
            val unknownDevice = seeded.firstOrNull { !it.isTrusted }
            if (unknownDevice != null) {
                alertDao.insert(
                    SecurityAlert(
                        deviceMac = unknownDevice.macAddress,
                        deviceName = unknownDevice.name,
                        deviceIp = unknownDevice.ipAddress
                    )
                )
            }
        }
    }

    suspend fun scanAndStoreLocalDevices(status: RouterGatewayStatus): List<NetworkDevice> {
        val existing = deviceDao.getDevicesSnapshot(isDemo = false)
        val existingMacs = existing.map { it.macAddress }.toSet()
        val found = localNetwork.scanLocalSubnet(status, existing)
        if (found.isNotEmpty()) {
            deviceDao.upsertAll(found)
        }
        // Return newly discovered untrusted peers
        return found.filter { !it.isTrusted && it.macAddress !in existingMacs }
    }

    suspend fun upsertDevice(device: NetworkDevice) = deviceDao.upsert(device)

    suspend fun setDeviceBlocked(mac: String, blocked: Boolean) {
        deviceDao.setBlocked(mac, blocked)
        if (blocked) {
            alertDao.resolveByMac(mac, blocked = true)
        }
    }

    suspend fun setDeviceTrusted(mac: String, trusted: Boolean) {
        deviceDao.setTrusted(mac, trusted)
        if (trusted) {
            alertDao.resolveByMac(mac, blocked = false)
        }
    }

    suspend fun renameDevice(mac: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isNotEmpty()) {
            deviceDao.rename(mac, trimmed)
        }
    }

    suspend fun setDeviceSpeedLimits(mac: String, downMbps: Int, upMbps: Int) {
        deviceDao.setSpeedLimits(mac, downMbps.coerceAtLeast(0), upMbps.coerceAtLeast(0))
    }

    suspend fun updateLiveDeviceMetrics(devices: List<NetworkDevice>) {
        devices.forEach { d ->
            deviceDao.updateLiveMetrics(
                mac = d.macAddress,
                downMbps = d.currentDownloadMbps,
                upMbps = d.currentUploadMbps,
                todayGb = d.todayUsageGb
            )
        }
    }

    suspend fun upsertSchedule(schedule: AccessSchedule) = scheduleDao.upsert(schedule)

    suspend fun toggleSchedule(id: Long, enabled: Boolean) = scheduleDao.setEnabled(id, enabled)

    suspend fun deleteSchedule(id: Long) = scheduleDao.deleteById(id)

    suspend fun insertSpeedTest(record: SpeedTestRecord) = speedTestDao.insert(record)

    suspend fun insertAlert(alert: SecurityAlert) = alertDao.insert(alert)

    suspend fun resolveAlert(alertId: Long, mac: String, blockDevice: Boolean) {
        alertDao.resolveAlert(alertId, blocked = blockDevice)
        if (blockDevice) {
            deviceDao.setBlocked(mac, true)
        } else {
            deviceDao.setTrusted(mac, true)
            deviceDao.setBlocked(mac, false)
        }
    }

    suspend fun markAllAlertsRead() = alertDao.markAllRead()

    suspend fun setLanguage(language: AppLanguage) = preferences.setLanguage(language)
    suspend fun setThemeMode(mode: ThemeMode) = preferences.setThemeMode(mode)
    suspend fun setConnectionMode(mode: ConnectionMode) = preferences.setConnectionMode(mode)
    suspend fun setIntruderAlertsEnabled(enabled: Boolean) = preferences.setIntruderAlertsEnabled(enabled)
    suspend fun updateGuestWifi(config: GuestWifiConfig) = preferences.updateGuestWifi(config)
    suspend fun updateGatewayConfig(config: GatewayConfig) = preferences.updateGatewayConfig(config)

    suspend fun testGateway(ip: String): Boolean = localNetwork.testGatewayReachability(ip)

    suspend fun runRealSpeedTest(
        onPingProgress: (Int, Int, Float) -> Unit,
        onDownloadProgress: (Float, Float) -> Unit,
        onUploadProgress: (Float, Float) -> Unit
    ): Result<SpeedTestResult> = localNetwork.runRealSpeedTest(
        onPingProgress = onPingProgress,
        onDownloadProgress = onDownloadProgress,
        onUploadProgress = onUploadProgress
    )
}
