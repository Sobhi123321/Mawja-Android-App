package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.GppMaybe
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BandwidthTimeframe
import com.example.data.model.ConnectionMode
import com.example.data.model.DeviceFilter
import com.example.data.model.MainTab
import com.example.ui.components.LiveBandwidthChart
import com.example.ui.components.NestRouterIllustration
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.LocalMawjaColors
import com.example.ui.theme.MonoLabelSmall
import com.example.ui.theme.MonoMetricMedium
import com.example.ui.viewmodel.RouterUiState
import java.util.Locale

@Composable
fun DashboardScreen(
    state: RouterUiState,
    onOpenSpeedTest: () -> Unit,
    onTogglePauseAll: () -> Unit,
    onOpenGuestWifi: () -> Unit,
    onSelectTimeframe: (BandwidthTimeframe) -> Unit,
    onNavigateDevices: (DeviceFilter) -> Unit,
    onNavigateControls: () -> Unit,
    onOpenAlerts: () -> Unit,
    onSwitchToDemoMode: () -> Unit
) {
    val s = LocalAppStrings.current
    val mawjaColors = LocalMawjaColors.current
    val gw = state.gatewayStatus

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active unresolved intruder alert banner
        item(key = "intruder_banner") {
            val unresolvedCount = state.alerts.count { !it.isResolved }
            AnimatedVisibility(visible = unresolvedCount > 0) {
                Surface(
                    color = mawjaColors.blockedRedContainer,
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, mawjaColors.blockedRed.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenAlerts() }
                        .testTag("intruder_alert_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = mawjaColors.blockedRed,
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.GppMaybe,
                                contentDescription = s.alertsBannerTitle,
                                tint = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = s.alertsBannerTitle,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = s.alertsBannerDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Google Home Style Router Hero Card
        item(key = "router_hero") {
            Surface(
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, mawjaColors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(mawjaColors.heroStart, mawjaColors.heroEnd)
                            )
                        )
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Live Status Pill
                            val statusColor = when {
                                gw.isAllPaused -> mawjaColors.warningAmber
                                gw.isConnected -> mawjaColors.healthyGreen
                                else -> mawjaColors.blockedRed
                            }
                            val statusBg = when {
                                gw.isAllPaused -> mawjaColors.warningAmberContainer
                                gw.isConnected -> mawjaColors.healthyGreenContainer
                                else -> mawjaColors.blockedRedContainer
                            }
                            val statusText = when {
                                gw.isAllPaused -> s.statusPausedAll
                                gw.isConnected -> s.statusOnlineHealthy
                                else -> s.statusOffline
                            }

                            Surface(
                                color = statusBg,
                                shape = RoundedCornerShape(50)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(statusColor)
                                    )
                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = statusColor
                                    )
                                }
                            }

                            Text(
                                text = gw.ssid,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = gw.routerModel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                MiniMetaChip(label = s.gatewayLabel, value = gw.gatewayIp)
                                MiniMetaChip(label = s.uptimeLabel, value = "${gw.uptimeHours}${s.hoursShort}")
                            }
                        }

                        NestRouterIllustration(
                            isOnline = gw.isConnected,
                            isPaused = gw.isAllPaused,
                            modifier = Modifier.size(116.dp)
                        )
                    }

                    // Hero Quick Actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HeroActionPill(
                            icon = Icons.Rounded.Speed,
                            label = s.actionSpeedTest,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            onClick = onOpenSpeedTest,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("hero_speed_test_button")
                        )
                        HeroActionPill(
                            icon = Icons.Rounded.QrCode2,
                            label = s.actionGuestWifi,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            onClick = onOpenGuestWifi,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("hero_guest_wifi_button")
                        )
                        HeroActionPill(
                            icon = if (gw.isAllPaused) Icons.Rounded.PlayCircle else Icons.Rounded.PauseCircle,
                            label = if (gw.isAllPaused) s.actionResumeAll else s.actionPauseAll,
                            containerColor = if (gw.isAllPaused) mawjaColors.warningAmberContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
                            contentColor = if (gw.isAllPaused) mawjaColors.warningAmber else MaterialTheme.colorScheme.onSurface,
                            onClick = onTogglePauseAll,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("hero_pause_all_button")
                        )
                    }
                }
            }
        }

        // Subtle Local LAN <-> Demo mode discovery card when in LOCAL_LAN mode
        if (state.preferences.connectionMode != ConnectionMode.DEMO) {
            item(key = "local_mode_tip") {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, mawjaColors.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Science,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = s.statusLanMonitoring,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${s.gatewayLabel}: ${gw.gatewayIp} • ${s.linkSpeedLabel}: ${gw.linkSpeedMbps} ${s.mbpsUnit}",
                                style = MonoLabelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (state.preferences.connectionMode == ConnectionMode.MIKROTIK && state.routerError != null) {
                                Text(
                                    text = state.routerError,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = mawjaColors.blockedRed
                                )
                            }
                        }
                        FilledTonalButton(
                            onClick = onSwitchToDemoMode,
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("switch_to_demo_cta")
                        ) {
                            Text(
                                text = s.modeDemoShort,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }

        // Live Bandwidth Telemetry Card
        item(key = "live_telemetry") {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(1.dp, mawjaColors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = s.liveUsageTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Timeframe selector pills (history is only simulated, so it exists in demo mode only)
                    if (state.preferences.connectionMode == ConnectionMode.DEMO) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            BandwidthTimeframe.LIVE to s.timeframeLive,
                            BandwidthTimeframe.TODAY to s.timeframeToday,
                            BandwidthTimeframe.WEEK to s.timeframeWeek
                        ).forEach { (tf, label) ->
                            FilterChip(
                                selected = state.timeframe == tf,
                                onClick = { onSelectTimeframe(tf) },
                                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                modifier = Modifier.testTag("timeframe_${tf.name.lowercase()}")
                            )
                        }
                    }

                    // Download & Upload KPI Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TelemetryKpiBox(
                            icon = Icons.Rounded.ArrowDownward,
                            label = s.downloadLabel,
                            value = String.format(Locale.US, "%.1f", state.currentDownloadMbps),
                            unit = s.mbpsUnit,
                            accent = mawjaColors.downloadBlue,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryKpiBox(
                            icon = Icons.Rounded.ArrowUpward,
                            label = s.uploadLabel,
                            value = String.format(Locale.US, "%.1f", state.currentUploadMbps),
                            unit = s.mbpsUnit,
                            accent = mawjaColors.uploadTeal,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Live Bezier Dual-Line Chart
                    LiveBandwidthChart(
                        samples = state.activeSamplesForChart,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(152.dp)
                    )

                    // Footer Peak & Total Today
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = s.peakLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format(Locale.US, "%.1f %s", state.peakDownloadMbps, s.mbpsUnit),
                                style = MonoLabelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = s.todayTotalLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format(Locale.US, "%.1f %s", state.totalTodayGb, s.gbUnit),
                                style = MonoLabelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // 2x2 Quick Status Bento Grid
        item(key = "bento_grid") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BentoStatCard(
                        icon = Icons.Rounded.Devices,
                        title = s.activeDevicesLabel,
                        value = "${state.activeDeviceCount}",
                        subtitle = s.devicesCountUnit,
                        iconTint = MaterialTheme.colorScheme.primary,
                        iconBg = MaterialTheme.colorScheme.primaryContainer,
                        onClick = { onNavigateDevices(DeviceFilter.ONLINE) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bento_active_devices")
                    )
                    BentoStatCard(
                        icon = Icons.Rounded.Block,
                        title = s.blockedDevicesLabel,
                        value = "${state.blockedDeviceCount}",
                        subtitle = s.devicesCountUnit,
                        iconTint = mawjaColors.blockedRed,
                        iconBg = mawjaColors.blockedRedContainer,
                        onClick = { onNavigateDevices(DeviceFilter.BLOCKED) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bento_blocked_devices")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val guestOn = state.preferences.guestWifi.enabled
                    BentoStatCard(
                        icon = Icons.Rounded.Wifi,
                        title = s.guestNetworkLabel,
                        value = if (guestOn) s.activeState else s.offState,
                        subtitle = state.preferences.guestWifi.ssid,
                        iconTint = if (guestOn) mawjaColors.uploadTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                        iconBg = if (guestOn) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        onClick = onNavigateControls,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bento_guest_wifi")
                    )
                    BentoStatCard(
                        icon = Icons.Rounded.Schedule,
                        title = s.activeSchedulesLabel,
                        value = "${state.activeScheduleCount}",
                        subtitle = s.rulesCountUnit,
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        iconBg = MaterialTheme.colorScheme.tertiaryContainer,
                        onClick = onNavigateControls,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bento_schedules")
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniMetaChip(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MonoLabelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun HeroActionPill(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TelemetryKpiBox(
    icon: ImageVector,
    label: String,
    value: String,
    unit: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = value,
                    style = MonoMetricMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun BentoStatCard(
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String,
    iconTint: Color,
    iconBg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mawjaColors = LocalMawjaColors.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, mawjaColors.cardBorder),
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = iconBg,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.padding(9.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }
        }
    }
}
