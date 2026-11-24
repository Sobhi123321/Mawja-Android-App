package com.example.data.network

import com.example.data.model.AccessSchedule
import com.example.data.model.BandwidthSample
import com.example.data.model.DeviceCategory
import com.example.data.model.DeviceProfile
import com.example.data.model.NetworkDevice
import com.example.data.model.RouterGatewayStatus
import kotlin.math.sin
import kotlin.random.Random

object DemoNetworkEngine {

    private var tick = 0

    fun createDemoGatewayStatus(gatewayIp: String, isAllPaused: Boolean): RouterGatewayStatus =
        RouterGatewayStatus(
            ssid = "Mawja_Home_6E",
            gatewayIp = gatewayIp.ifBlank { "192.168.1.1" },
            localDeviceIp = "192.168.1.14",
            dnsServer = "1.1.1.1",
            routerModel = "Nest Wifi Pro 6E",
            isConnected = true,
            isWifiTransport = true,
            signalDbm = -42,
            linkSpeedMbps = 1200,
            uptimeHours = 184,
            gatewayReachable = true,
            isAllPaused = isAllPaused
        )

    fun seedDemoDevices(): List<NetworkDevice> = listOf(
        NetworkDevice(
            macAddress = "A4:83:E7:21:9C:01",
            name = "Pixel 9 Pro",
            manufacturer = "Google",
            ipAddress = "192.168.1.14",
            category = DeviceCategory.PHONE,
            profile = DeviceProfile.FAMILY,
            isOnline = true,
            isBlocked = false,
            isTrusted = true,
            isThisDevice = true,
            isDemoDevice = true,
            currentDownloadMbps = 14.2f,
            currentUploadMbps = 3.6f,
            todayUsageGb = 4.8f,
            signalStrengthDbm = -42
        ),
        NetworkDevice(
            macAddress = "F0:18:98:4B:11:02",
            name = "MacBook Pro M4",
            manufacturer = "Apple",
            ipAddress = "192.168.1.22",
            category = DeviceCategory.LAPTOP,
            profile = DeviceProfile.WORK,
            isOnline = true,
            isBlocked = false,
            isTrusted = true,
            isDemoDevice = true,
            currentDownloadMbps = 38.5f,
            currentUploadMbps = 11.4f,
            todayUsageGb = 12.6f,
            signalStrengthDbm = -47
        ),
        NetworkDevice(
            macAddress = "8C:79:F5:99:2A:03",
            name = "Samsung OLED 4K TV",
            manufacturer = "Samsung",
            ipAddress = "192.168.1.35",
            category = DeviceCategory.TV,
            profile = DeviceProfile.FAMILY,
            isOnline = true,
            isBlocked = false,
            isTrusted = true,
            isDemoDevice = true,
            currentDownloadMbps = 54.0f,
            currentUploadMbps = 1.8f,
            todayUsageGb = 26.4f,
            signalStrengthDbm = -51
        ),
        NetworkDevice(
            macAddress = "BC:D1:77:0F:88:04",
            name = "PlayStation 5",
            manufacturer = "Sony",
            ipAddress = "192.168.1.41",
            category = DeviceCategory.CONSOLE,
            profile = DeviceProfile.KIDS,
            isOnline = true,
            isBlocked = false,
            isTrusted = true,
            isDemoDevice = true,
            downloadLimitMbps = 40,
            uploadLimitMbps = 15,
            currentDownloadMbps = 28.4f,
            currentUploadMbps = 6.2f,
            todayUsageGb = 18.9f,
            signalStrengthDbm = -54
        ),
        NetworkDevice(
            macAddress = "3C:22:FB:6E:55:05",
            name = "iPad Air — Kids",
            manufacturer = "Apple",
            ipAddress = "192.168.1.48",
            category = DeviceCategory.TABLET,
            profile = DeviceProfile.KIDS,
            isOnline = true,
            isBlocked = false,
            isTrusted = true,
            isDemoDevice = true,
            downloadLimitMbps = 15,
            uploadLimitMbps = 5,
            currentDownloadMbps = 9.1f,
            currentUploadMbps = 1.1f,
            todayUsageGb = 5.3f,
            signalStrengthDbm = -57
        ),
        NetworkDevice(
            macAddress = "54:60:09:3A:7C:06",
            name = "Google Nest Hub",
            manufacturer = "Google",
            ipAddress = "192.168.1.60",
            category = DeviceCategory.IOT,
            profile = DeviceProfile.SMART_HOME,
            isOnline = true,
            isBlocked = false,
            isTrusted = true,
            isDemoDevice = true,
            currentDownloadMbps = 1.4f,
            currentUploadMbps = 0.6f,
            todayUsageGb = 0.9f,
            signalStrengthDbm = -45
        ),
        NetworkDevice(
            macAddress = "D8:3A:DD:71:FE:99",
            name = "Redmi Note — Unknown",
            manufacturer = "Xiaomi",
            ipAddress = "192.168.1.89",
            category = DeviceCategory.UNKNOWN,
            profile = DeviceProfile.UNKNOWN,
            isOnline = true,
            isBlocked = false,
            isTrusted = false,
            isDemoDevice = true,
            currentDownloadMbps = 22.7f,
            currentUploadMbps = 4.3f,
            todayUsageGb = 3.4f,
            signalStrengthDbm = -68
        )
    )

    fun seedDefaultSchedules(): List<AccessSchedule> = listOf(
        AccessSchedule(
            titleAr = "وقت النوم للأطفال",
            titleEn = "Kids Bedtime",
            startHour = 22,
            startMinute = 0,
            endHour = 6,
            endMinute = 30,
            daysMask = 0b1111111,
            targetProfile = DeviceProfile.KIDS,
            isEnabled = true
        ),
        AccessSchedule(
            titleAr = "وقت المذاكرة والتركيز",
            titleEn = "Study Focus Window",
            startHour = 16,
            startMinute = 0,
            endHour = 18,
            endMinute = 0,
            daysMask = 0b0111110,
            targetProfile = DeviceProfile.KIDS,
            isEnabled = true
        )
    )

    fun createRandomIntruderDevice(): NetworkDevice {
        val lastOctet = Random.nextInt(110, 230)
        val hex1 = Random.nextInt(16, 255).toString(16).uppercase()
        val hex2 = Random.nextInt(16, 255).toString(16).uppercase()
        val names = listOf("Galaxy A55 (New)", "Poco X6 Pro", "Unknown Laptop", "Android-9F2A")
        return NetworkDevice(
            macAddress = "9E:44:2C:A1:$hex1:$hex2",
            name = names.random(),
            manufacturer = "Unverified Vendor",
            ipAddress = "192.168.1.$lastOctet",
            category = DeviceCategory.UNKNOWN,
            profile = DeviceProfile.UNKNOWN,
            isOnline = true,
            isBlocked = false,
            isTrusted = false,
            isDemoDevice = true,
            currentDownloadMbps = Random.nextDouble(8.0, 28.0).toFloat(),
            currentUploadMbps = Random.nextDouble(1.5, 6.0).toFloat(),
            todayUsageGb = 0.2f,
            signalStrengthDbm = Random.nextInt(-74, -58),
            lastSeenMillis = System.currentTimeMillis()
        )
    }

    /**
     * Generates smooth, realistic per-device and aggregate bandwidth reacting directly to blocked & capped states.
     */
    fun stepDemoTraffic(
        devices: List<NetworkDevice>,
        isAllPaused: Boolean
    ): Pair<List<NetworkDevice>, BandwidthSample> {
        tick++
        if (isAllPaused) {
            val zeroed = devices.map {
                it.copy(currentDownloadMbps = 0f, currentUploadMbps = 0f)
            }
            return Pair(zeroed, BandwidthSample(0f, 0f))
        }

        var totalDown = 0f
        var totalUp = 0f

        val updated = devices.mapIndexed { idx, device ->
            if (device.isBlocked || !device.isOnline) {
                device.copy(currentDownloadMbps = 0f, currentUploadMbps = 0f)
            } else {
                val baseDown = when (device.category) {
                    DeviceCategory.TV -> 46f
                    DeviceCategory.LAPTOP -> 32f
                    DeviceCategory.CONSOLE -> 25f
                    DeviceCategory.PHONE -> 14f
                    DeviceCategory.TABLET -> 10f
                    DeviceCategory.IOT -> 1.8f
                    DeviceCategory.UNKNOWN -> 21f
                }
                val baseUp = baseDown * 0.24f
                val wave = (sin((tick + idx * 1.7) * 0.45) * 0.28 + sin((tick + idx) * 0.17) * 0.14).toFloat()
                val jitter = (Random.nextFloat() - 0.5f) * 0.14f

                var d = (baseDown * (1f + wave + jitter)).coerceAtLeast(0.2f)
                var u = (baseUp * (1f + wave * 0.8f + jitter)).coerceAtLeast(0.1f)

                if (device.downloadLimitMbps > 0) d = d.coerceAtMost(device.downloadLimitMbps.toFloat())
                if (device.uploadLimitMbps > 0) u = u.coerceAtMost(device.uploadLimitMbps.toFloat())

                totalDown += d
                totalUp += u
                val addedGb = ((d + u) / 8f) / 1024f // 1 second slice in GB
                device.copy(
                    currentDownloadMbps = d,
                    currentUploadMbps = u,
                    todayUsageGb = device.todayUsageGb + addedGb
                )
            }
        }

        return Pair(updated, BandwidthSample(totalDown, totalUp))
    }

    fun buildHistoricalSamples(isToday: Boolean): List<BandwidthSample> {
        return if (isToday) {
            listOf("00", "04", "08", "12", "16", "20", "23").mapIndexed { idx, label ->
                val down = listOf(18f, 12f, 45f, 78f, 112f, 168f, 134f)[idx]
                val up = listOf(5f, 3f, 14f, 22f, 31f, 44f, 29f)[idx]
                BandwidthSample(down, up, label = "$label:00")
            }
        } else {
            listOf("S", "S", "M", "T", "W", "T", "F").mapIndexed { idx, label ->
                val down = listOf(96f, 118f, 84f, 92f, 108f, 142f, 156f)[idx]
                val up = listOf(24f, 31f, 22f, 25f, 28f, 39f, 41f)[idx]
                BandwidthSample(down, up, label = label)
            }
        }
    }
}
