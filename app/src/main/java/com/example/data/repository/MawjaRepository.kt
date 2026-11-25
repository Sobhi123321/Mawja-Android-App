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
import com.example.data.network.MikroTikClient
import com.example.data.network.MikroTikException
import com.example.data.network.MikroTikInfo
import com.example.data.network.LocalNetworkEngine
import com.example.data.network.NotificationHelper
import com.example.data.network.SpeedTestResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

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
        deviceDao.observeDevices(
            isDemo = mode == ConnectionMode.DEMO,
            isRouter = mode == ConnectionMode.MIKROTIK
        )

    suspend fun ensureSeededData() {
        if (scheduleDao.count() == 0) {
            DemoNetworkEngine.seedDefaultSchedules().forEach { scheduleDao.upsert(it) }
        }
        val demoDevices = deviceDao.getDevicesSnapshot(isDemo = true, isRouter = false)
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
        val existing = deviceDao.getDevicesSnapshot(isDemo = false, isRouter = false)
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

    // ------------------------------------------------------------------ MikroTik

    private var mtClient: MikroTikClient? = null
    private var mtKey: String = ""
    private var wanPrev: Triple<Long, Long, Long>? = null // rx bytes, tx bytes, elapsed ms

    @Synchronized
    private fun mikrotik(cfg: GatewayConfig): MikroTikClient {
        val key = "${cfg.gatewayIp}|${cfg.adminUsername}|${cfg.adminPassword}|${cfg.useHttps}"
        val current = mtClient
        if (current != null && key == mtKey) return current
        wanPrev = null
        return MikroTikClient(cfg.gatewayIp, cfg.adminUsername, cfg.adminPassword, cfg.useHttps)
            .also { mtClient = it; mtKey = key }
    }

    /** Runs a router call off the main thread; only router errors become a failed Result. */
    private suspend fun <T> routerCall(block: () -> T): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.success(block())
        } catch (e: MikroTikException) {
            Result.failure(e)
        } catch (e: IllegalArgumentException) {
            Result.failure(MikroTikException(MikroTikException.Kind.UNREACHABLE, 0, e.message ?: "Invalid router settings"))
        }
    }

    suspend fun testMikroTik(cfg: GatewayConfig): Result<MikroTikInfo> =
        routerCall { mikrotik(cfg).fetchInfo() }

    class MikroTikSync(val info: MikroTikInfo, val newUnknown: List<NetworkDevice>)

    /**
     * Reads the real device list, block list and speed limits from the router and mirrors them in
     * the local database. The router is the single source of truth: nothing here is invented.
     */
    suspend fun syncMikroTik(cfg: GatewayConfig, thisDeviceIp: String): Result<MikroTikSync> {
        val fetched = routerCall {
            val c = mikrotik(cfg)
            val info = c.fetchInfo()
            Triple(info, c.fetchDevices(), Pair(c.fetchBlockedMacs(), c.fetchLimits()))
        }
        val (info, leases, rest) = fetched.getOrElse { return Result.failure(it) }
        val (blocked, limits) = rest

        val existing = deviceDao.getDevicesSnapshot(isDemo = false, isRouter = true).associateBy { it.macAddress }
        val firstSync = existing.isEmpty()
        val now = System.currentTimeMillis()
        val seen = HashSet<String>()
        val rows = ArrayList<NetworkDevice>()
        val newUnknown = ArrayList<NetworkDevice>()

        for (l in leases) {
            seen.add(l.mac)
            val prev = existing[l.mac]
            val lim = limits[l.mac]
            val device = NetworkDevice(
                macAddress = l.mac,
                name = prev?.name ?: l.hostName.ifBlank { "Device ${l.mac.takeLast(5)}" },
                manufacturer = prev?.manufacturer ?: "Network device",
                ipAddress = l.ip,
                category = prev?.category ?: com.example.data.model.DeviceCategory.UNKNOWN,
                profile = prev?.profile ?: com.example.data.model.DeviceProfile.UNKNOWN,
                isOnline = l.isOnline,
                isBlocked = l.mac in blocked,
                // The very first sync is the baseline: everything already on the network is trusted.
                isTrusted = prev?.isTrusted ?: firstSync,
                isThisDevice = l.ip == thisDeviceIp,
                isDemoDevice = false,
                isRouterDevice = true,
                downloadLimitMbps = lim?.downMbps ?: 0,
                uploadLimitMbps = lim?.upMbps ?: 0,
                currentDownloadMbps = 0f,
                currentUploadMbps = 0f,
                todayUsageGb = prev?.todayUsageGb ?: 0f,
                signalStrengthDbm = prev?.signalStrengthDbm ?: -52,
                lastSeenMillis = if (l.isOnline) now else (prev?.lastSeenMillis ?: now)
            )
            rows.add(device)
            if (prev == null && !firstSync) newUnknown.add(device)
        }
        // Known devices the router no longer lists are shown as offline.
        existing.values.filter { it.macAddress !in seen }.forEach {
            rows.add(it.copy(isOnline = false, currentDownloadMbps = 0f, currentUploadMbps = 0f))
        }
        deviceDao.upsertAll(rows)
        return Result.success(MikroTikSync(info, newUnknown))
    }

    /** Total internet throughput from the router's WAN counters, as (download, upload) in Mbps. */
    suspend fun sampleMikroTikThroughput(cfg: GatewayConfig): Result<Pair<Float, Float>> {
        val counters = routerCall { mikrotik(cfg).fetchWanCounters() }
            .getOrElse { wanPrev = null; return Result.failure(it) }
        val now = android.os.SystemClock.elapsedRealtime()
        val prev = wanPrev
        wanPrev = Triple(counters.first, counters.second, now)
        if (prev == null) return Result.success(Pair(0f, 0f))
        val dt = ((now - prev.third).coerceAtLeast(250L)) / 1000f
        val down = ((counters.first - prev.first).coerceAtLeast(0L) * 8f / dt) / 1_000_000f
        val up = ((counters.second - prev.second).coerceAtLeast(0L) * 8f / dt) / 1_000_000f
        return Result.success(Pair(down.coerceIn(0f, 10_000f), up.coerceIn(0f, 10_000f)))
    }

    /** Blocks/unblocks on the router first; the local record only changes after the router confirms. */
    suspend fun setBlockedOnRouter(cfg: GatewayConfig, device: NetworkDevice, blocked: Boolean): Result<Unit> {
        val r = routerCall { mikrotik(cfg).setBlocked(device.macAddress, device.ipAddress, blocked) }
        if (r.isSuccess) setDeviceBlocked(device.macAddress, blocked)
        return r
    }

    suspend fun setLimitsOnRouter(cfg: GatewayConfig, device: NetworkDevice, downMbps: Int, upMbps: Int): Result<Unit> {
        val r = routerCall {
            mikrotik(cfg).setLimits(device.macAddress, device.ipAddress, downMbps.coerceAtLeast(0), upMbps.coerceAtLeast(0))
        }
        if (r.isSuccess) setDeviceSpeedLimits(device.macAddress, downMbps, upMbps)
        return r
    }

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
