package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AppLanguage(val code: String) {
    AR("ar"),
    EN("en")
}

enum class ThemeMode {
    AUTO,
    LIGHT,
    DARK
}

enum class ConnectionMode {
    LOCAL_LAN,
    DEMO,
    MIKROTIK
}

enum class DeviceCategory {
    PHONE,
    LAPTOP,
    TV,
    CONSOLE,
    TABLET,
    IOT,
    UNKNOWN
}

enum class DeviceProfile {
    FAMILY,
    KIDS,
    WORK,
    GUEST,
    SMART_HOME,
    UNKNOWN
}

enum class DeviceFilter {
    ALL,
    ONLINE,
    BLOCKED,
    THROTTLED,
    UNKNOWN
}

enum class BandwidthTimeframe {
    LIVE,
    TODAY,
    WEEK
}

enum class SpeedTestPhase {
    IDLE,
    PING,
    DOWNLOAD,
    UPLOAD,
    COMPLETE,
    ERROR
}

enum class MainTab {
    HOME,
    DEVICES,
    CONTROLS,
    SETTINGS
}

@Entity(tableName = "devices")
data class NetworkDevice(
    @PrimaryKey val macAddress: String,
    val name: String,
    val manufacturer: String,
    val ipAddress: String,
    val category: DeviceCategory = DeviceCategory.PHONE,
    val profile: DeviceProfile = DeviceProfile.FAMILY,
    val isOnline: Boolean = true,
    val isBlocked: Boolean = false,
    val isTrusted: Boolean = true,
    val isThisDevice: Boolean = false,
    val isDemoDevice: Boolean = false,
    val isRouterDevice: Boolean = false, // true = read from the MikroTik router itself
    val downloadLimitMbps: Int = 0, // 0 = Unlimited
    val uploadLimitMbps: Int = 0,   // 0 = Unlimited
    val currentDownloadMbps: Float = 0f,
    val currentUploadMbps: Float = 0f,
    val todayUsageGb: Float = 0f,
    val signalStrengthDbm: Int = -52,
    val lastSeenMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "schedules")
data class AccessSchedule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titleAr: String,
    val titleEn: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val daysMask: Int = 0b1111111, // Bit 0=Sat .. Bit 6=Fri
    val targetProfile: DeviceProfile = DeviceProfile.KIDS,
    val isEnabled: Boolean = true
)

@Entity(tableName = "speed_tests")
data class SpeedTestRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val downloadMbps: Float,
    val uploadMbps: Float,
    val pingMs: Int,
    val jitterMs: Int,
    val connectionMode: ConnectionMode,
    val timestampMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "security_alerts")
data class SecurityAlert(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceMac: String,
    val deviceName: String,
    val deviceIp: String,
    val isResolved: Boolean = false,
    val wasBlocked: Boolean = false,
    val isRead: Boolean = false,
    val timestampMillis: Long = System.currentTimeMillis()
)

data class BandwidthSample(
    val downloadMbps: Float,
    val uploadMbps: Float,
    val label: String = "",
    val timestampMillis: Long = System.currentTimeMillis()
)

data class RouterGatewayStatus(
    val ssid: String,
    val gatewayIp: String,
    val localDeviceIp: String,
    val dnsServer: String,
    val routerModel: String,
    val isConnected: Boolean,
    val isWifiTransport: Boolean,
    val signalDbm: Int,
    val linkSpeedMbps: Int,
    val uptimeHours: Int,
    val gatewayReachable: Boolean,
    val isAllPaused: Boolean = false,
    val pauseUntilMillis: Long? = null
)

data class GuestWifiConfig(
    val enabled: Boolean = true,
    val ssid: String = "Mawja_Guest_5G",
    val password: String = "Welcome2026",
    val speedLimitMbps: Int = 25,
    val durationHours: Int = 6,
    val connectedGuests: Int = 1
)

data class GatewayConfig(
    val gatewayIp: String = "192.168.1.1",
    val routerBrand: String = "Google Nest / Generic",
    val adminUsername: String = "admin",
    val adminPassword: String = "",
    val useHttps: Boolean = true
)

data class SpeedTestLiveState(
    val phase: SpeedTestPhase = SpeedTestPhase.IDLE,
    val progress: Float = 0f,
    val liveGaugeMbps: Float = 0f,
    val downloadMbps: Float = 0f,
    val uploadMbps: Float = 0f,
    val pingMs: Int = 0,
    val jitterMs: Int = 0,
    val errorMessage: String? = null
)
