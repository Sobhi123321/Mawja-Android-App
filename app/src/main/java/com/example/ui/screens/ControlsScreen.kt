package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.AccessSchedule
import com.example.data.model.AppLanguage
import com.example.data.model.GuestWifiConfig
import com.example.ui.components.WifiQrCodeCanvas
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.LocalMawjaColors
import com.example.ui.theme.MonoLabelSmall
import com.example.ui.viewmodel.RouterUiState
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ControlsScreen(
    state: RouterUiState,
    onPauseMinutes: (Int) -> Unit,
    onResumeNow: () -> Unit,
    onAddScheduleClick: () -> Unit,
    onToggleSchedule: (Long, Boolean) -> Unit,
    onDeleteSchedule: (Long) -> Unit,
    onUpdateGuestWifi: (GuestWifiConfig) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val s = LocalAppStrings.current
    val mawjaColors = LocalMawjaColors.current
    val context = LocalContext.current
    val guest = state.preferences.guestWifi

    var editingGuest by remember { mutableStateOf(false) }
    var draftSsid by remember(guest.ssid) { mutableStateOf(guest.ssid) }
    var draftPass by remember(guest.password) { mutableStateOf(guest.password) }
    var draftLimit by remember(guest.speedLimitMbps) { mutableIntStateOf(guest.speedLimitMbps) }
    var draftHours by remember(guest.durationHours) { mutableIntStateOf(guest.durationHours) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "controls_title") {
            Text(
                text = s.controlsHeaderTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Quick Pause Strip
        item(key = "quick_pause") {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, mawjaColors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PauseCircle,
                            contentDescription = null,
                            tint = if (state.gatewayStatus.isAllPaused) mawjaColors.warningAmber else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = s.quickPauseTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (state.gatewayStatus.isAllPaused) {
                        Button(
                            onClick = onResumeNow,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("resume_network_now_button")
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(s.resumeNow)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { onPauseMinutes(30) },
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("pause_30m_button")
                            ) {
                                Text(s.pause30m, style = MaterialTheme.typography.labelMedium)
                            }
                            FilledTonalButton(
                                onClick = { onPauseMinutes(60) },
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("pause_1h_button")
                            ) {
                                Text(s.pause1h, style = MaterialTheme.typography.labelMedium)
                            }
                            FilledTonalButton(
                                onClick = { onPauseMinutes(480) },
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("pause_morning_button")
                            ) {
                                Text(s.pauseUntilMorning, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }

        // Parental Schedules Header
        item(key = "schedules_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = s.parentalSchedulesTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                FilledTonalButton(
                    onClick = onAddScheduleClick,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("add_schedule_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(s.addScheduleBtn)
                }
            }
        }

        items(state.schedules, key = { it.id }) { schedule ->
            ScheduleCard(
                schedule = schedule,
                language = s.language,
                onToggle = { enabled -> onToggleSchedule(schedule.id, enabled) },
                onDelete = { onDeleteSchedule(schedule.id) }
            )
        }

        // Guest Wi-Fi & Scannable QR Code Card
        item(key = "guest_wifi_card") {
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Wifi,
                                    contentDescription = s.guestWifiTitle,
                                    tint = mawjaColors.uploadTeal,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = s.guestWifiTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = s.guestWifiSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = guest.enabled,
                            onCheckedChange = { onUpdateGuestWifi(guest.copy(enabled = it)) },
                            modifier = Modifier.testTag("guest_wifi_switch")
                        )
                    }

                    AnimatedVisibility(visible = guest.enabled) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Genuine Scannable QR Code
                            WifiQrCodeCanvas(
                                ssid = guest.ssid,
                                password = guest.password,
                                modifier = Modifier.testTag("guest_wifi_qr_code")
                            )

                            Text(
                                text = s.scanQrHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // SSID & Password metadata strip
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(s.ssidLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(guest.ssid, style = MonoLabelSmall, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(s.passwordLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(guest.password, style = MonoLabelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(s.guestSpeedLimitLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${guest.speedLimitMbps} ${s.mbpsUnit} • ${guest.durationHours}${s.hoursShort}", style = MonoLabelSmall, color = mawjaColors.uploadTeal)
                                    }
                                }
                            }

                            // Copy / Share / Edit buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        cm?.setPrimaryClip(ClipData.newPlainText("Guest Wi-Fi", guest.password))
                                        onShowMessage(s.passwordCopiedToast)
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("copy_guest_password_button")
                                ) {
                                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(s.copyPasswordBtn, style = MaterialTheme.typography.labelMedium)
                                }
                                Button(
                                    onClick = {
                                        val shareText = "Wi-Fi: ${guest.ssid}\nPassword: ${guest.password}"
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                        }
                                        context.startActivity(Intent.createChooser(intent, s.shareWifiBtn))
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("share_guest_wifi_button")
                                ) {
                                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(s.shareWifiBtn, style = MaterialTheme.typography.labelMedium)
                                }
                            }

                            FilledTonalButton(
                                onClick = { editingGuest = !editingGuest },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(s.editGuestBtn)
                            }

                            AnimatedVisibility(visible = editingGuest) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = draftSsid,
                                        onValueChange = { draftSsid = it },
                                        label = { Text(s.ssidLabel) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = draftPass,
                                        onValueChange = { draftPass = it },
                                        label = { Text(s.passwordLabel) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf(10, 25, 50).forEach { cap ->
                                            FilterChip(
                                                selected = draftLimit == cap,
                                                onClick = { draftLimit = cap },
                                                label = { Text("$cap ${s.mbpsUnit}") }
                                            )
                                        }
                                        listOf(2, 6, 24).forEach { hrs ->
                                            FilterChip(
                                                selected = draftHours == hrs,
                                                onClick = { draftHours = hrs },
                                                label = { Text("$hrs${s.hoursShort}") }
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            onUpdateGuestWifi(
                                                guest.copy(
                                                    ssid = draftSsid.trim().ifEmpty { guest.ssid },
                                                    password = draftPass.trim().ifEmpty { guest.password },
                                                    speedLimitMbps = draftLimit,
                                                    durationHours = draftHours
                                                )
                                            )
                                            editingGuest = false
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(s.saveBtn)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleCard(
    schedule: AccessSchedule,
    language: AppLanguage,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val s = LocalAppStrings.current
    val mawjaColors = LocalMawjaColors.current
    val title = if (language == AppLanguage.AR) schedule.titleAr else schedule.titleEn
    val timeRange = String.format(
        Locale.US,
        "%02d:%02d — %02d:%02d",
        schedule.startHour,
        schedule.startMinute,
        schedule.endHour,
        schedule.endMinute
    )

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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = if (schedule.isEnabled) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = title,
                    tint = if (schedule.isEnabled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$timeRange • ${s.profileName(schedule.targetProfile)}",
                    style = MonoLabelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_schedule_${schedule.id}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = s.deleteBtn,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = schedule.isEnabled,
                onCheckedChange = onToggle,
                modifier = Modifier.testTag("toggle_schedule_${schedule.id}")
            )
        }
    }
}
