package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AppLanguage
import com.example.data.model.ConnectionMode
import com.example.data.model.MainTab
import com.example.data.model.ThemeMode
import com.example.ui.components.AlertsCenterOverlay
import com.example.ui.components.DeviceControlSheet
import com.example.ui.components.ScheduleEditorDialog
import com.example.ui.components.SpeedTestOverlay
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.stringsFor
import com.example.ui.screens.ControlsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DevicesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.LocalMawjaColors
import com.example.ui.theme.MawjaTheme
import com.example.ui.viewmodel.RouterViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: RouterViewModel = viewModel()
            MawjaAppRoot(viewModel = viewModel)
        }
    }
}

private data class NavItem(
    val tab: MainTab,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MawjaAppRoot(viewModel: RouterViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = state.preferences

    val useDarkTheme = when (prefs.themeMode) {
        ThemeMode.AUTO -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val appStrings = remember(prefs.language) { stringsFor(prefs.language) }
    val layoutDirection = if (prefs.language == AppLanguage.AR) LayoutDirection.Rtl else LayoutDirection.Ltr

    MawjaTheme(darkTheme = useDarkTheme) {
        CompositionLocalProvider(
            LocalAppStrings provides appStrings,
            LocalLayoutDirection provides layoutDirection
        ) {
            val s = LocalAppStrings.current
            val mawjaColors = LocalMawjaColors.current
            val snackbarHostState = remember { SnackbarHostState() }

            // Handle Undo Snackbars cleanly
            LaunchedEffect(state.snackbarMessage?.id) {
                val msg = state.snackbarMessage ?: return@LaunchedEffect
                val res = snackbarHostState.showSnackbar(
                    message = msg.text,
                    actionLabel = if (msg.undoDeviceMac != null) s.undoAction else null,
                    duration = SnackbarDuration.Short
                )
                if (res == SnackbarResult.ActionPerformed && msg.undoDeviceMac != null) {
                    viewModel.undoLastBlockAction()
                } else {
                    viewModel.clearSnackbar()
                }
            }

            // Back button on secondary tabs returns cleanly to Home
            BackHandler(enabled = state.selectedTab != MainTab.HOME && !state.showSpeedTestModal && !state.showAlertsSheet) {
                viewModel.selectTab(MainTab.HOME)
            }

            val navItems = listOf(
                NavItem(MainTab.HOME, s.tabHome, Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
                NavItem(MainTab.DEVICES, s.tabDevices, Icons.Rounded.Devices, Icons.Outlined.Devices, "nav_devices"),
                NavItem(MainTab.CONTROLS, s.tabControls, Icons.Rounded.Tune, Icons.Outlined.Tune, "nav_controls"),
                NavItem(MainTab.SETTINGS, s.tabSettings, Icons.Rounded.Settings, Icons.Outlined.Settings, "nav_settings")
            )

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWideScreen = maxWidth >= 600.dp

                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                titleContentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Wifi,
                                            contentDescription = s.appName,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(7.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = s.appName,
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                    }
                                }
                            },
                            actions = {
                                // Interactive Mode Pill (Local LAN <-> Demo)
                                val isDemo = prefs.connectionMode == ConnectionMode.DEMO
                                Surface(
                                    color = if (isDemo) MaterialTheme.colorScheme.tertiaryContainer else mawjaColors.healthyGreenContainer,
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable { viewModel.toggleConnectionMode() }
                                        .testTag("top_mode_toggle_pill")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isDemo) Icons.Rounded.Science else Icons.Rounded.Wifi,
                                            contentDescription = null,
                                            tint = if (isDemo) MaterialTheme.colorScheme.tertiary else mawjaColors.healthyGreen,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (isDemo) s.modeDemoShort else s.modeLocalShort,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isDemo) MaterialTheme.colorScheme.tertiary else mawjaColors.healthyGreen
                                        )
                                    }
                                }

                                // Instant AR / EN Language Toggle
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, mawjaColors.cardBorder),
                                    modifier = Modifier
                                        .padding(start = 6.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { viewModel.toggleLanguage() }
                                        .testTag("top_language_toggle")
                                ) {
                                    Text(
                                        text = s.langToggleLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                    )
                                }

                                // Alerts Bell with Unread Badge
                                IconButton(
                                    onClick = { viewModel.openAlertsSheet(true) },
                                    modifier = Modifier.testTag("top_alerts_button")
                                ) {
                                    BadgedBox(
                                        badge = {
                                            if (state.unreadAlertsCount > 0) {
                                                Badge(containerColor = mawjaColors.blockedRed) {
                                                    Text("${state.unreadAlertsCount}")
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Notifications,
                                            contentDescription = s.alertsTitle
                                        )
                                    }
                                }
                            }
                        )
                    },
                    bottomBar = {
                        if (!isWideScreen) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                            ) {
                                navItems.forEach { item ->
                                    val selected = state.selectedTab == item.tab
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { viewModel.selectTab(item.tab) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.label
                                            )
                                        },
                                        label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.testTag(item.testTag)
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (isWideScreen) {
                            NavigationRail(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                navItems.forEach { item ->
                                    val selected = state.selectedTab == item.tab
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { viewModel.selectTab(item.tab) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.label
                                            )
                                        },
                                        label = { Text(item.label) },
                                        modifier = Modifier.testTag(item.testTag)
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .widthIn(max = 760.dp)
                                    .fillMaxWidth()
                            ) {
                                AnimatedContent(
                                    targetState = state.selectedTab,
                                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                                    label = "main_tab_transition"
                                ) { tab ->
                                    when (tab) {
                                        MainTab.HOME -> DashboardScreen(
                                            state = state,
                                            onOpenSpeedTest = { viewModel.openSpeedTestModal(true) },
                                            onTogglePauseAll = { viewModel.togglePauseAll() },
                                            onOpenGuestWifi = { viewModel.selectTab(MainTab.CONTROLS) },
                                            onSelectTimeframe = { viewModel.setTimeframe(it) },
                                            onNavigateDevices = { filter ->
                                                viewModel.setDeviceFilter(filter)
                                                viewModel.selectTab(MainTab.DEVICES)
                                            },
                                            onNavigateControls = { viewModel.selectTab(MainTab.CONTROLS) },
                                            onOpenAlerts = { viewModel.openAlertsSheet(true) },
                                            onSwitchToDemoMode = { viewModel.setConnectionMode(ConnectionMode.DEMO) }
                                        )

                                        MainTab.DEVICES -> DevicesScreen(
                                            state = state,
                                            onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                            onSelectFilter = { viewModel.setDeviceFilter(it) },
                                            onScanNetwork = { viewModel.scanLocalNetwork() },
                                            onSelectDevice = { viewModel.openDeviceDetail(it.macAddress) },
                                            onToggleBlock = { dev ->
                                                viewModel.toggleDeviceBlocked(
                                                    device = dev,
                                                    blockedToast = s.deviceBlockedToast,
                                                    unblockedToast = s.deviceUnblockedToast
                                                )
                                            }
                                        )

                                        MainTab.CONTROLS -> ControlsScreen(
                                            state = state,
                                            onPauseMinutes = { viewModel.pauseForMinutes(it) },
                                            onResumeNow = { viewModel.resumeAllNow() },
                                            onAddScheduleClick = { viewModel.openScheduleDialog(true) },
                                            onToggleSchedule = { id, enabled -> viewModel.toggleSchedule(id, enabled) },
                                            onDeleteSchedule = { viewModel.deleteSchedule(it) },
                                            onUpdateGuestWifi = { viewModel.updateGuestWifi(it) },
                                            onShowMessage = { viewModel.showSimpleMessage(it) }
                                        )

                                        MainTab.SETTINGS -> SettingsScreen(
                                            state = state,
                                            onSelectLanguage = { viewModel.setLanguage(it) },
                                            onSelectTheme = { viewModel.setThemeMode(it) },
                                            onSelectConnectionMode = { viewModel.setConnectionMode(it) },
                                            onSaveGatewayConfig = { viewModel.updateGatewayConfig(it) },
                                            onTestGateway = { viewModel.testGatewayConnection() },
                                            onToggleIntruderAlerts = { viewModel.setIntruderAlertsEnabled(it) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Device Control Bottom Sheet
                state.selectedDevice?.let { device ->
                    DeviceControlSheet(
                        device = device,
                        onDismiss = { viewModel.openDeviceDetail(null) },
                        onToggleBlock = {
                            viewModel.toggleDeviceBlocked(
                                device = device,
                                blockedToast = s.deviceBlockedToast,
                                unblockedToast = s.deviceUnblockedToast
                            )
                        },
                        onRename = { newName -> viewModel.renameDevice(device.macAddress, newName) },
                        onApplySpeedLimits = { down, up ->
                            viewModel.setDeviceSpeedLimits(device.macAddress, down, up)
                            viewModel.openDeviceDetail(null)
                        },
                        onTrustDevice = { viewModel.trustDevice(device.macAddress) }
                    )
                }

                // Add Schedule Dialog
                if (state.showScheduleDialog) {
                    ScheduleEditorDialog(
                        onDismiss = { viewModel.openScheduleDialog(false) },
                        onSave = { viewModel.addSchedule(it) }
                    )
                }

                // Full-screen Speed Test Overlay
                if (state.showSpeedTestModal) {
                    SpeedTestOverlay(
                        liveState = state.speedTestLive,
                        history = state.speedTestHistory,
                        onStartTest = { viewModel.startSpeedTest() },
                        onClose = { viewModel.openSpeedTestModal(false) }
                    )
                }

                // Full-screen Alerts Center Overlay
                if (state.showAlertsSheet) {
                    AlertsCenterOverlay(
                        alerts = state.alerts,
                        hasNotificationPermission = viewModel.hasNotificationPermission(),
                        onResolveAlert = { alert, block -> viewModel.resolveAlert(alert, block) },
                        onSimulateIntruder = { viewModel.simulateIntruderAlert() },
                        onClose = { viewModel.openAlertsSheet(false) }
                    )
                }
            }
        }
    }
}
