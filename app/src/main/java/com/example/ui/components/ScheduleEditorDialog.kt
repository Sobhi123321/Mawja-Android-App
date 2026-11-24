package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.AccessSchedule
import com.example.data.model.DeviceProfile
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.MonoLabelSmall
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScheduleEditorDialog(
    onDismiss: () -> Unit,
    onSave: (AccessSchedule) -> Unit
) {
    val s = LocalAppStrings.current
    var title by remember { mutableStateOf("") }
    var startHour by remember { mutableIntStateOf(21) }
    var endHour by remember { mutableIntStateOf(7) }
    var selectedProfile by remember { mutableStateOf(DeviceProfile.KIDS) }
    var daysMask by remember { mutableIntStateOf(0b1111111) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = s.newScheduleTitle,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text(s.scheduleNameHint) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("schedule_name_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(s.startTimeLabel, style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = String.format(Locale.US, "%02d:00", startHour),
                        style = MonoLabelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = startHour.toFloat(),
                    onValueChange = { startHour = it.roundToInt() },
                    valueRange = 0f..23f,
                    steps = 22
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(s.endTimeLabel, style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = String.format(Locale.US, "%02d:00", endHour),
                        style = MonoLabelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = endHour.toFloat(),
                    onValueChange = { endHour = it.roundToInt() },
                    valueRange = 0f..23f,
                    steps = 22
                )

                Text(s.targetGroupLabel, style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(DeviceProfile.KIDS, DeviceProfile.FAMILY, DeviceProfile.GUEST, DeviceProfile.WORK).forEach { prof ->
                        FilterChip(
                            selected = selectedProfile == prof,
                            onClick = { selectedProfile = prof },
                            label = { Text(s.profileName(prof)) }
                        )
                    }
                }

                Text(s.daysOfWeekLabel, style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    s.daysShort.forEachIndexed { idx, dayLabel ->
                        val active = (daysMask and (1 shl idx)) != 0
                        FilterChip(
                            selected = active,
                            onClick = {
                                val next = daysMask xor (1 shl idx)
                                if (next != 0) daysMask = next
                            },
                            label = { Text(dayLabel) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanTitle = title.trim().ifEmpty { s.profileName(selectedProfile) }
                    onSave(
                        AccessSchedule(
                            titleAr = cleanTitle,
                            titleEn = cleanTitle,
                            startHour = startHour,
                            startMinute = 0,
                            endHour = endHour,
                            endMinute = 0,
                            daysMask = daysMask,
                            targetProfile = selectedProfile,
                            isEnabled = true
                        )
                    )
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.testTag("save_schedule_button")
            ) {
                Text(s.saveBtn)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(s.cancelBtn)
            }
        }
    )
}
