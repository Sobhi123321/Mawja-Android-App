package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserPreferences
import com.example.data.model.AccessSchedule
import com.example.data.model.AppLanguage
import com.example.data.model.BandwidthSample
import com.example.data.model.BandwidthTimeframe
import com.example.data.model.ConnectionMode
import com.example.data.model.DeviceFilter
import com.example.data.model.GatewayConfig
import com.example.data.model.GuestWifiConfig
import com.example.data.model.MainTab
import com.example.data.model.NetworkDevice
import com.example.data.model.RouterGatewayStatus
import com.example.data.model.SecurityAlert
import com.example.data.model.SpeedTestLiveState
import com.example.data.model.SpeedTestPhase
import com.example.data.model.SpeedTestRecord
import com.example.data.model.ThemeMode
import com.example.data.network.DemoNetworkEngine
import com.example.data.network.MikroTikException
import com.example.data.network.MikroTikInfo
import com.example.ui.i18n.stringsFor
import com.example.data.repository.MawjaRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

data class SnackbarUndoMessage(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val undoDeviceMac: String? = null,
    val undoPreviousBlocked: Boolean = false
)

data class RouterUiState(
    val selectedTab: MainTab = MainTab.HOME,
    val preferences: UserPreferences = UserPreferences(),
    val gatewayStatus: RouterGatewayStatus = DemoNetworkEngine.createDemoGatewayStatus("192.168.1.1", false),
    val devices: List<NetworkDevice> = emptyList(),
    val deviceFilter: DeviceFilter = DeviceFilter.ALL,
    val searchQuery: String = "",
    val selectedDeviceMac: String? = null,
    val isScanningLan: Boolean = false,
    val timeframe: BandwidthTimeframe = BandwidthTimeframe.LIVE,
    val liveSamples: List<BandwidthSample> = emptyList(),
    val peakDownloadMbps: Float = 0f,
    val schedules: List<AccessSchedule> = emptyList(),
    val speedTestLive: SpeedTestLiveState = SpeedTestLiveState(),
    val speedTestHistory: List<SpeedTestRecord> = emptyList(),
    val alerts: List<SecurityAlert> = emptyList(),
    val unreadAlertsCount: Int = 0,
    val showSpeedTestModal: Boolean = false,
    val showAlertsSheet: Boolean = false,
    val showScheduleDialog: Boolean = false,
    val gatewayTestResult: Boolean? = null,
    val isTestingGateway: Boolean = false,
    val gatewayTestDetail: String? = null,
    val routerError: String? = null,
    val snackbarMessage: SnackbarUndoMessage? = null
) {
    val selectedDevice: NetworkDevice?
        get() = selectedDeviceMac?.let { mac -> devices.firstOrNull { it.macAddress == mac } }

    val filteredDevices: List<NetworkDevice>
        get() {
            val q = searchQuery.trim().lowercase()
            return devices.filter { d ->
                val matchesFilter = when (deviceFilter) {
                    DeviceFilter.ALL -> true
                    DeviceFilter.ONLINE -> d.isOnline && !d.isBlocked
                    DeviceFilter.BLOCKED -> d.isBlocked
                    DeviceFilter.THROTTLED -> d.downloadLimitMbps > 0 || d.uploadLimitMbps > 0
                    DeviceFilter.UNKNOWN -> !d.isTrusted
                }
                val matchesQuery = q.isEmpty() ||
                    d.name.lowercase().contains(q) ||
                    d.ipAddress.contains(q) ||
                    d.macAddress.lowercase().contains(q) ||
                    d.manufacturer.lowercase().contains(q)
                matchesFilter && matchesQuery
            }
        }

    val currentDownloadMbps: Float
        get() = liveSamples.lastOrNull()?.downloadMbps ?: 0f

    val currentUploadMbps: Float
        get() = liveSamples.lastOrNull()?.uploadMbps ?: 0f

    val totalTodayGb: Float
        get() = devices.sumOf { it.todayUsageGb.toDouble() }.toFloat()

    val activeDeviceCount: Int
        get() = devices.count { it.isOnline && !it.isBlocked }

    val blockedDeviceCount: Int
        get() = devices.count { it.isBlocked }

    val activeScheduleCount: Int
        get() = schedules.count { it.isEnabled }

    val activeSamplesForChart: List<BandwidthSample>
        get() = if (preferences.connectionMode != ConnectionMode.DEMO) liveSamples else when (timeframe) {
            BandwidthTimeframe.LIVE -> liveSamples
            BandwidthTimeframe.TODAY -> DemoNetworkEngine.buildHistoricalSamples(isToday = true)
            BandwidthTimeframe.WEEK -> DemoNetworkEngine.buildHistoricalSamples(isToday = false)
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class RouterViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MawjaRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(RouterUiState())
    val uiState: StateFlow<RouterUiState> = _uiState.asStateFlow()

    private val isAllPausedFlow = MutableStateFlow(false)
    private val pauseUntilFlow = MutableStateFlow<Long?>(null)
    private var speedTestJob: Job? = null
    private var lastLiveSamples = ArrayDeque<BandwidthSample>(24)

    // MikroTik link state (only meaningful in ConnectionMode.MIKROTIK)
    private var mikrotikInfo: MikroTikInfo? = null
    private var mikrotikLinkOk: Boolean = true
    private var mikrotikTick: Int = 0
    private var mikrotikSyncing: Boolean = false

    private val modeFlow: StateFlow<ConnectionMode> = repository.preferencesFlow
        .map { it.connectionMode }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ConnectionMode.LOCAL_LAN)

    init {
        viewModelScope.launch {
            repository.ensureSeededData()
        }

        // Observe preferences
        viewModelScope.launch {
            repository.preferencesFlow.collect { prefs ->
                _uiState.update { it.copy(preferences = prefs) }
                refreshGatewayStatus()
            }
        }

        // Observe devices reactive to current connection mode
        viewModelScope.launch {
            modeFlow.flatMapLatest { mode ->
                repository.observeDevices(mode)
            }.collect { list ->
                _uiState.update { it.copy(devices = list) }
            }
        }

        // Observe schedules
        viewModelScope.launch {
            repository.schedulesFlow.collect { list ->
                _uiState.update { it.copy(schedules = list) }
            }
        }

        // Observe speed tests
        viewModelScope.launch {
            repository.speedTestsFlow.collect { list ->
                _uiState.update { it.copy(speedTestHistory = list) }
            }
        }

        // Observe security alerts
        viewModelScope.launch {
            repository.alertsFlow.collect { list ->
                _uiState.update { it.copy(alerts = list) }
            }
        }

        viewModelScope.launch {
            repository.unreadAlertsCountFlow.collect { count ->
                _uiState.update { it.copy(unreadAlertsCount = count) }
            }
        }

        // Trigger initial real subnet scan when in LOCAL_LAN mode
        viewModelScope.launch {
            modeFlow.collect { mode ->
                lastLiveSamples.clear()
                mikrotikInfo = null
                mikrotikLinkOk = true
                mikrotikTick = 0
                isAllPausedFlow.value = false
                pauseUntilFlow.value = null
                _uiState.update {
                    it.copy(
                        liveSamples = emptyList(),
                        peakDownloadMbps = 0f,
                        gatewayTestResult = null,
                        gatewayTestDetail = null,
                        routerError = null
                    )
                }
                refreshGatewayStatus()
                if (mode != ConnectionMode.DEMO) {
                    scanLocalNetwork()
                }
            }
        }

        // Start 1-second live telemetry loop
        startTelemetryLoop()
    }

    private fun startTelemetryLoop() {
        viewModelScope.launch {
            while (isActive) {
                // Auto-resume if timed pause expired
                val pauseUntil = pauseUntilFlow.value
                if (pauseUntil != null && System.currentTimeMillis() >= pauseUntil) {
                    isAllPausedFlow.value = false
                    pauseUntilFlow.value = null
                }

                val state = _uiState.value
                val mode = state.preferences.connectionMode
                val isPaused = isAllPausedFlow.value

                val newSample = if (mode == ConnectionMode.DEMO) {
                    val (updatedDevices, sample) = DemoNetworkEngine.stepDemoTraffic(
                        devices = state.devices,
                        isAllPaused = isPaused
                    )
                    if (updatedDevices.isNotEmpty()) {
                        repository.updateLiveDeviceMetrics(updatedDevices)
                    }
                    sample
                } else if (mode == ConnectionMode.MIKROTIK) {
                    val result = repository.sampleMikroTikThroughput(state.preferences.gatewayConfig)
                    result.onSuccess {
                        mikrotikLinkOk = true
                    }.onFailure { e ->
                        mikrotikLinkOk = false
                        _uiState.update { it.copy(routerError = describeRouterError(e)) }
                    }
                    // Refresh the device list from the router every ~8 seconds, without blocking this loop.
                    mikrotikTick++
                    if (mikrotikTick % 8 == 1 && !mikrotikSyncing) {
                        launch { syncMikroTikNow() }
                    }
                    val (d, u) = result.getOrDefault(Pair(0f, 0f))
                    BandwidthSample(d, u)
                } else {
                    val (rawDown, rawUp) = repository.localNetwork.sampleRealThroughputMbps()
                    val effectiveDown = if (isPaused) 0f else rawDown
                    val effectiveUp = if (isPaused) 0f else rawUp
                    // Update "This device" live metrics in Room
                    val selfDevice = state.devices.firstOrNull { it.isThisDevice }
                    if (selfDevice != null) {
                        val addedGb = ((effectiveDown + effectiveUp) / 8f) / 1024f
                        repository.updateLiveDeviceMetrics(
                            listOf(
                                selfDevice.copy(
                                    currentDownloadMbps = effectiveDown,
                                    currentUploadMbps = effectiveUp,
                                    todayUsageGb = selfDevice.todayUsageGb + addedGb
                                )
                            )
                        )
                    }
                    BandwidthSample(effectiveDown, effectiveUp)
                }

                if (lastLiveSamples.size >= 24) {
                    lastLiveSamples.removeFirst()
                }
                lastLiveSamples.addLast(newSample)
                val samplesSnapshot = lastLiveSamples.toList()
                val newPeak = maxOf(state.peakDownloadMbps, newSample.downloadMbps)

                val gwStatus = buildCurrentGatewayStatus(
                    mode = mode,
                    gatewayConfig = state.preferences.gatewayConfig,
                    isPaused = isPaused,
                    pauseUntil = pauseUntilFlow.value
                )

                _uiState.update {
                    it.copy(
                        gatewayStatus = gwStatus,
                        liveSamples = samplesSnapshot,
                        peakDownloadMbps = newPeak
                    )
                }

                delay(1000L)
            }
        }
    }

    private fun buildCurrentGatewayStatus(
        mode: ConnectionMode,
        gatewayConfig: GatewayConfig,
        isPaused: Boolean,
        pauseUntil: Long?
    ): RouterGatewayStatus {
        return when (mode) {
            ConnectionMode.DEMO ->
                DemoNetworkEngine.createDemoGatewayStatus(gatewayConfig.gatewayIp, isPaused)
                    .copy(pauseUntilMillis = pauseUntil)
            ConnectionMode.MIKROTIK -> {
                val info = mikrotikInfo
                repository.localNetwork.readRealGatewayStatus(
                    configuredGateway = gatewayConfig.gatewayIp,
                    routerModel = "MikroTik",
                    isPaused = false
                ).copy(
                    gatewayIp = gatewayConfig.gatewayIp.trim()
                        .removePrefix("http://").removePrefix("https://").substringBefore("/"),
                    routerModel = if (info != null) "MikroTik ${info.boardName} (RouterOS ${info.version})" else "MikroTik",
                    gatewayReachable = mikrotikLinkOk,
                    uptimeHours = info?.uptimeHours ?: 1,
                    pauseUntilMillis = null
                )
            }
            ConnectionMode.LOCAL_LAN ->
                repository.localNetwork.readRealGatewayStatus(
                    configuredGateway = gatewayConfig.gatewayIp,
                    routerModel = gatewayConfig.routerBrand,
                    isPaused = isPaused
                ).copy(pauseUntilMillis = pauseUntil)
        }
    }

    private fun refreshGatewayStatus() {
        val prefs = _uiState.value.preferences
        val gw = buildCurrentGatewayStatus(
            mode = prefs.connectionMode,
            gatewayConfig = prefs.gatewayConfig,
            isPaused = isAllPausedFlow.value,
            pauseUntil = pauseUntilFlow.value
        )
        _uiState.update { it.copy(gatewayStatus = gw) }
    }

    fun selectTab(tab: MainTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun toggleLanguage() {
        val current = _uiState.value.preferences.language
        val next = if (current == AppLanguage.AR) AppLanguage.EN else AppLanguage.AR
        viewModelScope.launch { repository.setLanguage(next) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { repository.setLanguage(language) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setConnectionMode(mode: ConnectionMode) {
        viewModelScope.launch { repository.setConnectionMode(mode) }
    }

    fun toggleConnectionMode() {
        val next = if (_uiState.value.preferences.connectionMode == ConnectionMode.DEMO) {
            ConnectionMode.MIKROTIK
        } else {
            ConnectionMode.DEMO
        }
        setConnectionMode(next)
    }

    fun setTimeframe(timeframe: BandwidthTimeframe) {
        _uiState.update { it.copy(timeframe = timeframe) }
    }

    fun setDeviceFilter(filter: DeviceFilter) {
        _uiState.update { it.copy(deviceFilter = filter) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun openDeviceDetail(mac: String?) {
        _uiState.update { it.copy(selectedDeviceMac = mac) }
    }

    fun toggleDeviceBlocked(device: NetworkDevice, blockedToast: String, unblockedToast: String) {
        val nextBlocked = !device.isBlocked
        val prefs = _uiState.value.preferences
        if (prefs.connectionMode == ConnectionMode.MIKROTIK) {
            val s = stringsFor(prefs.language)
            if (nextBlocked && device.isThisDevice) {
                showSimpleMessage(s.mtSelfBlock)
                return
            }
            viewModelScope.launch {
                repository.setBlockedOnRouter(prefs.gatewayConfig, device, nextBlocked)
                    .onSuccess {
                        _uiState.update {
                            it.copy(
                                snackbarMessage = SnackbarUndoMessage(
                                    text = if (nextBlocked) "$blockedToast: ${device.name}" else "$unblockedToast: ${device.name}",
                                    undoDeviceMac = device.macAddress,
                                    undoPreviousBlocked = device.isBlocked
                                )
                            )
                        }
                    }
                    .onFailure { showSimpleMessage("${s.mtActionFailed}: ${describeRouterError(it)}") }
            }
            return
        }
        viewModelScope.launch {
            repository.setDeviceBlocked(device.macAddress, nextBlocked)
            _uiState.update {
                it.copy(
                    snackbarMessage = SnackbarUndoMessage(
                        text = if (nextBlocked) "$blockedToast: ${device.name}" else "$unblockedToast: ${device.name}",
                        undoDeviceMac = device.macAddress,
                        undoPreviousBlocked = device.isBlocked
                    )
                )
            }
        }
    }

    fun undoLastBlockAction() {
        val msg = _uiState.value.snackbarMessage ?: return
        val mac = msg.undoDeviceMac ?: return
        val prefs = _uiState.value.preferences
        viewModelScope.launch {
            if (prefs.connectionMode == ConnectionMode.MIKROTIK) {
                val dev = _uiState.value.devices.firstOrNull { it.macAddress == mac }
                if (dev != null) {
                    val r = repository.setBlockedOnRouter(prefs.gatewayConfig, dev, msg.undoPreviousBlocked)
                    if (r.isFailure) {
                        val s = stringsFor(prefs.language)
                        showSimpleMessage("${s.mtActionFailed}: ${describeRouterError(r.exceptionOrNull())}")
                        return@launch
                    }
                }
            } else {
                repository.setDeviceBlocked(mac, msg.undoPreviousBlocked)
            }
            _uiState.update { it.copy(snackbarMessage = null) }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun showSimpleMessage(message: String) {
        _uiState.update { it.copy(snackbarMessage = SnackbarUndoMessage(text = message)) }
    }

    fun renameDevice(mac: String, newName: String) {
        viewModelScope.launch { repository.renameDevice(mac, newName) }
    }

    fun setDeviceSpeedLimits(mac: String, downMbps: Int, upMbps: Int) {
        val prefs = _uiState.value.preferences
        if (prefs.connectionMode == ConnectionMode.MIKROTIK) {
            val dev = _uiState.value.devices.firstOrNull { it.macAddress == mac } ?: return
            val s = stringsFor(prefs.language)
            viewModelScope.launch {
                repository.setLimitsOnRouter(prefs.gatewayConfig, dev, downMbps, upMbps)
                    .onFailure { showSimpleMessage("${s.mtActionFailed}: ${describeRouterError(it)}") }
            }
            return
        }
        viewModelScope.launch { repository.setDeviceSpeedLimits(mac, downMbps, upMbps) }
    }

    fun trustDevice(mac: String) {
        viewModelScope.launch { repository.setDeviceTrusted(mac, true) }
    }

    fun scanLocalNetwork() {
        if (_uiState.value.isScanningLan) return
        viewModelScope.launch {
            _uiState.update { it.copy(isScanningLan = true) }
            val mode = _uiState.value.preferences.connectionMode
            if (mode == ConnectionMode.LOCAL_LAN) {
                refreshGatewayStatus()
                val newUnknowns = repository.scanAndStoreLocalDevices(_uiState.value.gatewayStatus)
                raiseUnknownDeviceAlerts(newUnknowns)
            } else if (mode == ConnectionMode.MIKROTIK) {
                syncMikroTikNow()
            } else {
                delay(700L)
            }
            _uiState.update { it.copy(isScanningLan = false) }
        }
    }

    private suspend fun raiseUnknownDeviceAlerts(devices: List<NetworkDevice>) {
        if (devices.isEmpty() || !_uiState.value.preferences.intruderAlertsEnabled) return
        val lang = _uiState.value.preferences.language
        devices.forEach { dev ->
            repository.insertAlert(
                SecurityAlert(deviceMac = dev.macAddress, deviceName = dev.name, deviceIp = dev.ipAddress)
            )
            repository.notifications.postUnknownDeviceAlert(dev.name, dev.ipAddress, lang)
        }
    }

    private suspend fun syncMikroTikNow() {
        if (mikrotikSyncing) return
        mikrotikSyncing = true
        try {
            val cfg = _uiState.value.preferences.gatewayConfig
            val thisIp = repository.localNetwork
                .readRealGatewayStatus(cfg.gatewayIp, "", false).localDeviceIp
            repository.syncMikroTik(cfg, thisIp)
                .onSuccess { sync ->
                    mikrotikInfo = sync.info
                    mikrotikLinkOk = true
                    _uiState.update { it.copy(routerError = null) }
                    raiseUnknownDeviceAlerts(sync.newUnknown)
                }
                .onFailure { e ->
                    mikrotikLinkOk = false
                    _uiState.update { it.copy(routerError = describeRouterError(e)) }
                }
        } finally {
            mikrotikSyncing = false
        }
    }

    private fun describeRouterError(e: Throwable?): String {
        val s = stringsFor(_uiState.value.preferences.language)
        val kind = (e as? MikroTikException)?.kind
        return when (kind) {
            MikroTikException.Kind.AUTH -> s.mtErrAuth
            MikroTikException.Kind.UNREACHABLE -> s.mtErrUnreachable
            MikroTikException.Kind.TLS -> s.mtErrTls
            MikroTikException.Kind.SERVER -> "${s.mtErrServer}: ${e?.message}"
            null -> e?.message ?: s.mtErrUnreachable
        }
    }

    fun togglePauseAll() {
        if (_uiState.value.preferences.connectionMode == ConnectionMode.MIKROTIK) {
            showSimpleMessage(stringsFor(_uiState.value.preferences.language).mtUnsupportedHere)
            return
        }
        val next = !isAllPausedFlow.value
        isAllPausedFlow.value = next
        if (!next) pauseUntilFlow.value = null
        refreshGatewayStatus()
    }

    fun pauseForMinutes(minutes: Int) {
        if (_uiState.value.preferences.connectionMode == ConnectionMode.MIKROTIK) {
            showSimpleMessage(stringsFor(_uiState.value.preferences.language).mtUnsupportedHere)
            return
        }
        isAllPausedFlow.value = true
        pauseUntilFlow.value = System.currentTimeMillis() + minutes * 60_000L
        refreshGatewayStatus()
    }

    fun resumeAllNow() {
        isAllPausedFlow.value = false
        pauseUntilFlow.value = null
        refreshGatewayStatus()
    }

    // Schedules
    fun openScheduleDialog(open: Boolean) {
        _uiState.update { it.copy(showScheduleDialog = open) }
    }

    fun addSchedule(schedule: AccessSchedule) {
        viewModelScope.launch {
            repository.upsertSchedule(schedule)
            _uiState.update { it.copy(showScheduleDialog = false) }
        }
    }

    fun toggleSchedule(id: Long, enabled: Boolean) {
        viewModelScope.launch { repository.toggleSchedule(id, enabled) }
    }

    fun deleteSchedule(id: Long) {
        viewModelScope.launch { repository.deleteSchedule(id) }
    }

    // Guest Wi-Fi
    fun updateGuestWifi(config: GuestWifiConfig) {
        viewModelScope.launch { repository.updateGuestWifi(config) }
    }

    // Gateway Settings
    /** Saves the settings, then tests them using exactly the values that were just saved. */
    fun saveAndTestGateway(config: GatewayConfig) {
        viewModelScope.launch {
            repository.updateGatewayConfig(config)
            _uiState.update { it.copy(isTestingGateway = true, gatewayTestResult = null, gatewayTestDetail = null) }
            val prefs = _uiState.value.preferences
            val mode = prefs.connectionMode
            val s = stringsFor(prefs.language)
            var ok: Boolean
            var detail: String? = null
            when (mode) {
                ConnectionMode.DEMO -> {
                    delay(650L)
                    ok = true
                }
                ConnectionMode.MIKROTIK -> {
                    val r = repository.testMikroTik(config)
                    ok = r.isSuccess
                    r.onSuccess { info ->
                        mikrotikInfo = info
                        mikrotikLinkOk = true
                        detail = "${s.mtTestSuccess}: ${info.boardName} • RouterOS ${info.version}"
                    }.onFailure { detail = describeRouterError(it) }
                }
                ConnectionMode.LOCAL_LAN -> ok = repository.testGateway(config.gatewayIp)
            }
            refreshGatewayStatus()
            _uiState.update { it.copy(isTestingGateway = false, gatewayTestResult = ok, gatewayTestDetail = detail) }
            if (ok && mode == ConnectionMode.MIKROTIK) {
                _uiState.update { it.copy(routerError = null) }
                syncMikroTikNow()
            }
        }
    }

    fun setIntruderAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setIntruderAlertsEnabled(enabled) }
    }

    // Security Alerts
    fun openAlertsSheet(open: Boolean) {
        _uiState.update { it.copy(showAlertsSheet = open) }
        if (open) {
            viewModelScope.launch { repository.markAllAlertsRead() }
        }
    }

    fun resolveAlert(alert: SecurityAlert, blockDevice: Boolean) {
        viewModelScope.launch {
            val prefs = _uiState.value.preferences
            if (prefs.connectionMode == ConnectionMode.MIKROTIK) {
                val s = stringsFor(prefs.language)
                val dev = _uiState.value.devices.firstOrNull { it.macAddress == alert.deviceMac }
                if (dev != null && blockDevice && dev.isThisDevice) {
                    showSimpleMessage(s.mtSelfBlock)
                    return@launch
                }
                if (dev != null && dev.isBlocked != blockDevice) {
                    val r = repository.setBlockedOnRouter(prefs.gatewayConfig, dev, blockDevice)
                    if (r.isFailure) {
                        showSimpleMessage("${s.mtActionFailed}: ${describeRouterError(r.exceptionOrNull())}")
                        return@launch
                    }
                }
            }
            repository.resolveAlert(alert.id, alert.deviceMac, blockDevice)
        }
    }

    fun simulateIntruderAlert() {
        if (_uiState.value.preferences.connectionMode == ConnectionMode.MIKROTIK) {
            showSimpleMessage(stringsFor(_uiState.value.preferences.language).mtUnsupportedHere)
            return
        }
        viewModelScope.launch {
            val intruder = DemoNetworkEngine.createRandomIntruderDevice().copy(
                isDemoDevice = _uiState.value.preferences.connectionMode == ConnectionMode.DEMO
            )
            repository.upsertDevice(intruder)
            repository.insertAlert(
                SecurityAlert(
                    deviceMac = intruder.macAddress,
                    deviceName = intruder.name,
                    deviceIp = intruder.ipAddress
                )
            )
            if (_uiState.value.preferences.intruderAlertsEnabled) {
                repository.notifications.postUnknownDeviceAlert(
                    deviceName = intruder.name,
                    ipAddress = intruder.ipAddress,
                    language = _uiState.value.preferences.language
                )
            }
        }
    }

    fun hasNotificationPermission(): Boolean = repository.notifications.hasNotificationPermission()

    // Speed Test
    fun openSpeedTestModal(open: Boolean) {
        if (!open) {
            speedTestJob?.cancel()
            _uiState.update { it.copy(showSpeedTestModal = false, speedTestLive = SpeedTestLiveState()) }
        } else {
            _uiState.update { it.copy(showSpeedTestModal = true) }
        }
    }

    fun startSpeedTest() {
        speedTestJob?.cancel()
        val mode = _uiState.value.preferences.connectionMode
        speedTestJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    speedTestLive = SpeedTestLiveState(
                        phase = SpeedTestPhase.PING,
                        progress = 0.05f
                    )
                )
            }

            if (mode != ConnectionMode.DEMO) {
                val res = repository.runRealSpeedTest(
                    onPingProgress = { ping, jitter, p ->
                        _uiState.update {
                            it.copy(
                                speedTestLive = it.speedTestLive.copy(
                                    phase = SpeedTestPhase.PING,
                                    pingMs = ping,
                                    jitterMs = jitter,
                                    progress = p
                                )
                            )
                        }
                    },
                    onDownloadProgress = { mbps, p ->
                        _uiState.update {
                            it.copy(
                                speedTestLive = it.speedTestLive.copy(
                                    phase = SpeedTestPhase.DOWNLOAD,
                                    liveGaugeMbps = mbps,
                                    downloadMbps = mbps,
                                    progress = p
                                )
                            )
                        }
                    },
                    onUploadProgress = { mbps, p ->
                        _uiState.update {
                            it.copy(
                                speedTestLive = it.speedTestLive.copy(
                                    phase = SpeedTestPhase.UPLOAD,
                                    liveGaugeMbps = mbps,
                                    uploadMbps = mbps,
                                    progress = p
                                )
                            )
                        }
                    }
                )
                res.onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            speedTestLive = SpeedTestLiveState(
                                phase = SpeedTestPhase.COMPLETE,
                                progress = 1f,
                                liveGaugeMbps = result.downloadMbps,
                                downloadMbps = result.downloadMbps,
                                uploadMbps = result.uploadMbps,
                                pingMs = result.pingMs,
                                jitterMs = result.jitterMs
                            )
                        )
                    }
                    repository.insertSpeedTest(
                        SpeedTestRecord(
                            downloadMbps = result.downloadMbps,
                            uploadMbps = result.uploadMbps,
                            pingMs = result.pingMs,
                            jitterMs = result.jitterMs,
                            connectionMode = mode
                        )
                    )
                }.onFailure { err ->
                    _uiState.update {
                        it.copy(
                            speedTestLive = it.speedTestLive.copy(
                                phase = SpeedTestPhase.ERROR,
                                errorMessage = err.localizedMessage ?: "Network unavailable"
                            )
                        )
                    }
                }
            } else {
                // Smooth realistic multi-stage Demo Speed Test
                val targetPing = Random.nextInt(9, 17)
                val targetJitter = Random.nextInt(1, 4)
                val targetDown = Random.nextDouble(210.0, 315.0).toFloat()
                val targetUp = Random.nextDouble(65.0, 118.0).toFloat()

                for (i in 1..6) {
                    delay(110L)
                    _uiState.update {
                        it.copy(
                            speedTestLive = it.speedTestLive.copy(
                                phase = SpeedTestPhase.PING,
                                pingMs = targetPing + Random.nextInt(-2, 3),
                                jitterMs = targetJitter,
                                progress = i / 6f * 0.2f
                            )
                        )
                    }
                }
                for (i in 1..18) {
                    delay(95L)
                    val ramp = (i / 18f)
                    val current = (targetDown * (0.25f + 0.75f * ramp) + Random.nextFloat() * 14f - 7f)
                        .coerceAtLeast(12f)
                    _uiState.update {
                        it.copy(
                            speedTestLive = it.speedTestLive.copy(
                                phase = SpeedTestPhase.DOWNLOAD,
                                liveGaugeMbps = current,
                                downloadMbps = current,
                                pingMs = targetPing,
                                jitterMs = targetJitter,
                                progress = 0.2f + ramp * 0.45f
                            )
                        )
                    }
                }
                for (i in 1..14) {
                    delay(95L)
                    val ramp = (i / 14f)
                    val current = (targetUp * (0.3f + 0.7f * ramp) + Random.nextFloat() * 8f - 4f)
                        .coerceAtLeast(6f)
                    _uiState.update {
                        it.copy(
                            speedTestLive = it.speedTestLive.copy(
                                phase = SpeedTestPhase.UPLOAD,
                                liveGaugeMbps = current,
                                downloadMbps = targetDown,
                                uploadMbps = current,
                                progress = 0.65f + ramp * 0.35f
                            )
                        )
                    }
                }
                _uiState.update {
                    it.copy(
                        speedTestLive = SpeedTestLiveState(
                            phase = SpeedTestPhase.COMPLETE,
                            progress = 1f,
                            liveGaugeMbps = targetDown,
                            downloadMbps = targetDown,
                            uploadMbps = targetUp,
                            pingMs = targetPing,
                            jitterMs = targetJitter
                        )
                    )
                }
                repository.insertSpeedTest(
                    SpeedTestRecord(
                        downloadMbps = targetDown,
                        uploadMbps = targetUp,
                        pingMs = targetPing,
                        jitterMs = targetJitter,
                        connectionMode = mode
                    )
                )
            }
        }
    }
}
