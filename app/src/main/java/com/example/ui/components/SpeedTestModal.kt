package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.SpeedTestLiveState
import com.example.data.model.SpeedTestPhase
import com.example.data.model.SpeedTestRecord
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.LocalMawjaColors
import com.example.ui.theme.MonoLabelSmall
import com.example.ui.theme.MonoMetricMedium
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpeedTestOverlay(
    liveState: SpeedTestLiveState,
    history: List<SpeedTestRecord>,
    onStartTest: () -> Unit,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    val s = LocalAppStrings.current
    val mawjaColors = LocalMawjaColors.current
    val isRunning = liveState.phase == SpeedTestPhase.PING ||
        liveState.phase == SpeedTestPhase.DOWNLOAD ||
        liveState.phase == SpeedTestPhase.UPLOAD

    val phaseLabel = when (liveState.phase) {
        SpeedTestPhase.IDLE -> s.startTestBtn
        SpeedTestPhase.PING -> s.testingPing
        SpeedTestPhase.DOWNLOAD -> s.testingDownload
        SpeedTestPhase.UPLOAD -> s.testingUpload
        SpeedTestPhase.COMPLETE -> s.testComplete
        SpeedTestPhase.ERROR -> liveState.errorMessage ?: s.statusOffline
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = s.speedTestTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("close_speed_test_button")
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = s.closeBtn)
                }
            }

            // Main Radial Gauge Card
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, mawjaColors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SpeedTestGauge(
                        mbps = liveState.liveGaugeMbps,
                        unitLabel = s.mbpsUnit,
                        phaseLabel = phaseLabel,
                        isUploadPhase = liveState.phase == SpeedTestPhase.UPLOAD,
                        modifier = Modifier.size(220.dp)
                    )

                    if (isRunning) {
                        LinearProgressIndicator(
                            progress = { liveState.progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(0.75f)
                        )
                    }

                    // 4 key KPI metrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SpeedMetricTile(
                            label = s.downloadLabel,
                            value = String.format(Locale.US, "%.1f", liveState.downloadMbps),
                            unit = s.mbpsUnit,
                            accent = mawjaColors.downloadBlue,
                            modifier = Modifier.weight(1f)
                        )
                        SpeedMetricTile(
                            label = s.uploadLabel,
                            value = String.format(Locale.US, "%.1f", liveState.uploadMbps),
                            unit = s.mbpsUnit,
                            accent = mawjaColors.uploadTeal,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SpeedMetricTile(
                            label = s.pingLabel,
                            value = "${liveState.pingMs}",
                            unit = s.msUnit,
                            accent = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f)
                        )
                        SpeedMetricTile(
                            label = s.jitterLabel,
                            value = "${liveState.jitterMs}",
                            unit = s.msUnit,
                            accent = mawjaColors.warningAmber,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Button(
                        onClick = onStartTest,
                        enabled = !isRunning,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("run_speed_test_button")
                    ) {
                        Icon(
                            imageVector = if (liveState.phase == SpeedTestPhase.COMPLETE) Icons.Rounded.Refresh else Icons.Rounded.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (liveState.phase == SpeedTestPhase.COMPLETE) s.testAgainBtn else s.startTestBtn,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            // Practical Quality Grade Card (shown when completed or latest history exists)
            val evaluatedDown = if (liveState.phase == SpeedTestPhase.COMPLETE) {
                liveState.downloadMbps
            } else {
                history.firstOrNull()?.downloadMbps ?: 0f
            }
            if (evaluatedDown > 0f) {
                val gradeLabel = when {
                    evaluatedDown >= 80f -> s.gradeExcellent
                    evaluatedDown >= 25f -> s.gradeVeryGood
                    else -> s.gradeModerate
                }
                Surface(
                    color = mawjaColors.healthyGreenContainer,
                    shape = RoundedCornerShape(22.dp),
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
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = mawjaColors.healthyGreen
                            )
                            Text(
                                text = "${s.qualityGradeTitle}: $gradeLabel",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (evaluatedDown >= 25f) {
                                AssistChip(onClick = {}, label = { Text(s.readyFor4k) })
                            }
                            if (evaluatedDown >= 15f) {
                                AssistChip(onClick = {}, label = { Text(s.readyForGaming) })
                            }
                            AssistChip(onClick = {}, label = { Text(s.readyForCalls) })
                        }
                    }
                }
            }

            // Recent History
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Rounded.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = s.speedHistoryTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (history.isEmpty()) {
                    Text(
                        text = s.noSpeedHistory,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val fmt = SimpleDateFormat("dd MMM • HH:mm", Locale.getDefault())
                    history.forEach { rec ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, mawjaColors.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = fmt.format(Date(rec.timestampMillis)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${s.pingLabel}: ${rec.pingMs} ${s.msUnit}",
                                        style = MonoLabelSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Rounded.ArrowDownward,
                                            contentDescription = null,
                                            tint = mawjaColors.downloadBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%.1f", rec.downloadMbps),
                                            style = MonoMetricMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Rounded.ArrowUpward,
                                            contentDescription = null,
                                            tint = mawjaColors.uploadTeal,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%.1f", rec.uploadMbps),
                                            style = MonoMetricMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
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
private fun SpeedMetricTile(
    label: String,
    value: String,
    unit: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = value,
                    style = MonoMetricMedium,
                    color = accent
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}
