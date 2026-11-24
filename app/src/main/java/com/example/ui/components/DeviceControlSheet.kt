package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GppMaybe
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.NetworkDevice
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.LocalMawjaColors
import com.example.ui.theme.MonoLabelSmall
import com.example.ui.theme.MonoMetricMedium
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DeviceControlSheet(
    device: NetworkDevice,
    onDismiss: () -> Unit,
    onToggleBlock: () -> Unit,
    onRename: (String) -> Unit,
    onApplySpeedLimits: (Int, Int) -> Unit,
    onTrustDevice: () -> Unit
) {
    val s = LocalAppStrings.current
    val mawjaColors = LocalMawjaColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var editableName by remember(device.macAddress) { mutableStateOf(device.name) }
    var downLimit by remember(device.macAddress, device.downloadLimitMbps) {
        mutableFloatStateOf(device.downloadLimitMbps.toFloat())
    }
    var upLimit by remember(device.macAddress, device.uploadLimitMbps) {
        mutableFloatStateOf(device.uploadLimitMbps.toFloat())
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        color = if (device.isBlocked) mawjaColors.blockedRedContainer else MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (device.isBlocked) Icons.Rounded.Block else Icons.Rounded.Devices,
                            contentDescription = s.deviceSettingsTitle,
                            tint = if (device.isBlocked) mawjaColors.blockedRed else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Column {
                        Text(
                            text = device.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${s.categoryName(device.category)} • ${device.manufacturer}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Unknown device warning banner
            if (!device.isTrusted) {
                Surface(
                    color = mawjaColors.warningAmberContainer,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.GppMaybe,
                                contentDescription = s.unknownBadge,
                                tint = mawjaColors.warningAmber
                            )
                            Text(
                                text = s.unknownDeviceWarning,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Button(
                            onClick = onTrustDevice,
                            modifier = Modifier.testTag("trust_device_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(s.trustDeviceBtn)
                        }
                    }
                }
            }

            // Instant Internet Block / Allow card
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, mawjaColors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = s.internetAccessTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (device.isBlocked) s.internetAccessBlocked else s.internetAccessAllowed,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (device.isBlocked) mawjaColors.blockedRed else mawjaColors.healthyGreen
                        )
                    }
                    Switch(
                        checked = !device.isBlocked,
                        onCheckedChange = { onToggleBlock() },
                        modifier = Modifier.testTag("sheet_block_switch")
                    )
                }
            }

            // Live device consumption sparkline + specs
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, mawjaColors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = s.trafficActivityTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(
                                Locale.US,
                                "↓ %.1f  ↑ %.1f %s",
                                device.currentDownloadMbps,
                                device.currentUploadMbps,
                                s.mbpsUnit
                            ),
                            style = MonoMetricMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    DeviceSparkline(
                        currentMbps = device.currentDownloadMbps,
                        isBlocked = device.isBlocked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SpecColumn(label = s.ipAddressLabel, value = device.ipAddress)
                        SpecColumn(label = s.macAddressLabel, value = device.macAddress)
                    }
                }
            }

            // Per-device Speed Limiter
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, mawjaColors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = s.speedLimiterTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Quick presets
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(0, 5, 15, 30, 50).forEach { preset ->
                            val selected = downLimit.roundToInt() == preset
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    downLimit = preset.toFloat()
                                    upLimit = if (preset == 0) 0f else (preset / 3).coerceAtLeast(2).toFloat()
                                },
                                label = {
                                    Text(if (preset == 0) s.unlimitedLabel else "$preset ${s.mbpsUnit}")
                                }
                            )
                        }
                    }

                    // Download slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(s.downloadLimitLabel, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = if (downLimit.roundToInt() == 0) s.unlimitedLabel else "${downLimit.roundToInt()} ${s.mbpsUnit}",
                            style = MonoLabelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = downLimit,
                        onValueChange = { downLimit = it },
                        valueRange = 0f..100f,
                        steps = 19,
                        modifier = Modifier.testTag("download_limit_slider")
                    )

                    // Upload slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(s.uploadLimitLabel, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = if (upLimit.roundToInt() == 0) s.unlimitedLabel else "${upLimit.roundToInt()} ${s.mbpsUnit}",
                            style = MonoLabelSmall,
                            color = mawjaColors.uploadTeal
                        )
                    }
                    Slider(
                        value = upLimit,
                        onValueChange = { upLimit = it },
                        valueRange = 0f..50f,
                        steps = 9,
                        modifier = Modifier.testTag("upload_limit_slider")
                    )

                    Button(
                        onClick = {
                            onApplySpeedLimits(downLimit.roundToInt(), upLimit.roundToInt())
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("apply_speed_limits_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(s.applyLimitsBtn)
                    }
                }
            }

            // Rename device
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, mawjaColors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = editableName,
                        onValueChange = { editableName = it },
                        label = { Text(s.deviceNameLabel) },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("rename_device_input"),
                        shape = RoundedCornerShape(16.dp)
                    )
                    TextButton(
                        onClick = { onRename(editableName) },
                        modifier = Modifier.testTag("save_device_name_button")
                    ) {
                        Text(s.saveNameBtn)
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecColumn(label: String, value: String) {
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
