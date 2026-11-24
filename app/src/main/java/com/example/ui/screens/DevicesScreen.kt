package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DevicesOther
import androidx.compose.material.icons.rounded.GppMaybe
import androidx.compose.material.icons.rounded.LaptopMac
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.TabletMac
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DeviceCategory
import com.example.data.model.DeviceFilter
import com.example.data.model.NetworkDevice
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.LocalMawjaColors
import com.example.ui.theme.MonoLabelSmall
import com.example.ui.viewmodel.RouterUiState
import java.util.Locale

@Composable
fun DevicesScreen(
    state: RouterUiState,
    onSearchQueryChange: (String) -> Unit,
    onSelectFilter: (DeviceFilter) -> Unit,
    onScanNetwork: () -> Unit,
    onSelectDevice: (NetworkDevice) -> Unit,
    onToggleBlock: (NetworkDevice) -> Unit
) {
    val s = LocalAppStrings.current
    val mawjaColors = LocalMawjaColors.current
    val filtered = state.filteredDevices
    val maxDeviceMbps = (state.devices.maxOfOrNull { it.currentDownloadMbps } ?: 25f).coerceAtLeast(10f)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header + Scan button
        item(key = "devices_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = s.devicesHeaderTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${state.activeDeviceCount} ${s.devicesCountUnit} • ${state.blockedDeviceCount} ${s.blockedBadge}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(
                    onClick = onScanNetwork,
                    enabled = !state.isScanningLan,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("scan_network_button")
                ) {
                    if (state.isScanningLan) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(s.scanningNetwork)
                    } else {
                        Icon(Icons.Rounded.Radar, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(s.scanNetworkBtn)
                    }
                }
            }
        }

        // Search Bar
        item(key = "search_bar") {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text(s.searchDevicesHint) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = s.closeBtn)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_devices_input")
            )
        }

        // Filter Chips
        item(key = "filter_chips") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    DeviceFilter.ALL to s.filterAll,
                    DeviceFilter.ONLINE to s.filterOnline,
                    DeviceFilter.BLOCKED to s.filterBlocked,
                    DeviceFilter.THROTTLED to s.filterThrottled,
                    DeviceFilter.UNKNOWN to s.filterUnknown
                ).forEach { (filter, label) ->
                    FilterChip(
                        selected = state.deviceFilter == filter,
                        onClick = { onSelectFilter(filter) },
                        label = { Text(label) },
                        modifier = Modifier.testTag("device_filter_${filter.name.lowercase()}")
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item(key = "empty_devices") {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, mawjaColors.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DevicesOther,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = s.noDevicesFound,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = s.noDevicesTip,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filtered, key = { it.macAddress }) { device ->
                DeviceRowCard(
                    device = device,
                    maxReferenceMbps = maxDeviceMbps,
                    onClick = { onSelectDevice(device) },
                    onToggleBlock = { onToggleBlock(device) }
                )
            }
        }
    }
}

@Composable
private fun DeviceRowCard(
    device: NetworkDevice,
    maxReferenceMbps: Float,
    onClick: () -> Unit,
    onToggleBlock: () -> Unit
) {
    val s = LocalAppStrings.current
    val mawjaColors = LocalMawjaColors.current

    val iconBg = when {
        device.isBlocked -> mawjaColors.blockedRedContainer
        !device.isTrusted -> mawjaColors.warningAmberContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val iconTint = when {
        device.isBlocked -> mawjaColors.blockedRed
        !device.isTrusted -> mawjaColors.warningAmber
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (device.isBlocked) {
                mawjaColors.blockedRed.copy(alpha = 0.35f)
            } else if (!device.isTrusted) {
                mawjaColors.warningAmber.copy(alpha = 0.45f)
            } else {
                mawjaColors.cardBorder
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .testTag("device_card_${device.macAddress}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = iconBg,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector = categoryIcon(device.category),
                        contentDescription = s.categoryName(device.category),
                        tint = iconTint,
                        modifier = Modifier.padding(11.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = device.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (device.isThisDevice) {
                            StatusMiniPill(
                                text = s.thisDeviceBadge,
                                bg = MaterialTheme.colorScheme.primaryContainer,
                                fg = MaterialTheme.colorScheme.primary
                            )
                        } else if (!device.isTrusted) {
                            StatusMiniPill(
                                text = s.unknownBadge,
                                bg = mawjaColors.warningAmberContainer,
                                fg = mawjaColors.warningAmber
                            )
                        }
                    }

                    Text(
                        text = "${device.ipAddress} • ${device.macAddress}",
                        style = MonoLabelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // One-tap Block / Unblock pill button
                FilledTonalButton(
                    onClick = onToggleBlock,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = if (device.isBlocked) {
                        ButtonDefaults.filledTonalButtonColors(
                            containerColor = mawjaColors.healthyGreenContainer,
                            contentColor = mawjaColors.healthyGreen
                        )
                    } else {
                        ButtonDefaults.filledTonalButtonColors(
                            containerColor = mawjaColors.blockedRedContainer,
                            contentColor = mawjaColors.blockedRed
                        )
                    },
                    modifier = Modifier.testTag("toggle_block_${device.macAddress}")
                ) {
                    Icon(
                        imageVector = if (device.isBlocked) Icons.Rounded.CheckCircle else Icons.Rounded.Block,
                        contentDescription = if (device.isBlocked) s.unblockBtn else s.blockBtn,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (device.isBlocked) s.unblockBtn else s.blockBtn,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            // Live consumption bar & tags
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (device.isBlocked) {
                                s.blockedBadge
                            } else {
                                String.format(Locale.US, "↓ %.1f %s", device.currentDownloadMbps, s.mbpsUnit)
                            },
                            style = MonoLabelSmall,
                            color = if (device.isBlocked) mawjaColors.blockedRed else mawjaColors.downloadBlue
                        )
                        if (device.downloadLimitMbps > 0 && !device.isBlocked) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Speed,
                                    contentDescription = null,
                                    tint = mawjaColors.warningAmber,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "${s.speedCapPrefix}: ${device.downloadLimitMbps} ${s.mbpsUnit}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = mawjaColors.warningAmber
                                )
                            }
                        }
                    }

                    Text(
                        text = String.format(Locale.US, "%.1f %s", device.todayUsageGb, s.gbUnit),
                        style = MonoLabelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                LinearProgressIndicator(
                    progress = {
                        if (device.isBlocked) 0f
                        else (device.currentDownloadMbps / maxReferenceMbps).coerceIn(0.03f, 1f)
                    },
                    color = if (device.isBlocked) mawjaColors.blockedRed else mawjaColors.downloadBlue,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                )
            }
        }
    }
}

@Composable
private fun StatusMiniPill(
    text: String,
    bg: androidx.compose.ui.graphics.Color,
    fg: androidx.compose.ui.graphics.Color
) {
    Surface(color = bg, shape = RoundedCornerShape(8.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

private fun categoryIcon(category: DeviceCategory): ImageVector = when (category) {
    DeviceCategory.PHONE -> Icons.Rounded.PhoneAndroid
    DeviceCategory.LAPTOP -> Icons.Rounded.LaptopMac
    DeviceCategory.TV -> Icons.Rounded.Tv
    DeviceCategory.CONSOLE -> Icons.Rounded.SportsEsports
    DeviceCategory.TABLET -> Icons.Rounded.TabletMac
    DeviceCategory.IOT -> Icons.Rounded.Router
    DeviceCategory.UNKNOWN -> Icons.Rounded.GppMaybe
}
