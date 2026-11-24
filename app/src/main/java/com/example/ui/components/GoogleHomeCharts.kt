package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.data.model.BandwidthSample
import com.example.data.network.QrCodeMatrix
import com.example.ui.theme.LocalMawjaColors
import com.example.ui.theme.MonoMetricLarge
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Minimalist Google Nest Wifi Pro ceramic router pod with animated breathing LED & subtle signal ripples.
 */
@Composable
fun NestRouterIllustration(
    isOnline: Boolean,
    isPaused: Boolean,
    modifier: Modifier = Modifier
) {
    val mawjaColors = LocalMawjaColors.current
    val primary = MaterialTheme.colorScheme.primary
    val surfaceLowest = MaterialTheme.colorScheme.surfaceContainerLowest
    val surfaceHigh = MaterialTheme.colorScheme.surfaceContainerHigh
    val outline = MaterialTheme.colorScheme.outlineVariant

    val statusDotColor = when {
        isPaused -> mawjaColors.warningAmber
        isOnline -> mawjaColors.healthyGreen
        else -> mawjaColors.blockedRed
    }

    val infiniteTransition = rememberInfiniteTransition(label = "router_pulse")
    val ripplePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_phase"
    )
    val ledBreath by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "led_breath"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.52f

        // Concentric translucent Wi-Fi signal ripples behind the hardware pod
        if (isOnline && !isPaused) {
            for (i in 0..2) {
                val p = (ripplePhase + i * 0.33f) % 1f
                val radius = w * (0.22f + p * 0.28f)
                val alpha = (1f - p) * 0.22f
                drawCircle(
                    color = primary.copy(alpha = alpha),
                    radius = radius,
                    center = Offset(cx, cy),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Soft studio ambient ground shadow
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color.Black.copy(alpha = 0.16f), Color.Transparent),
                center = Offset(cx, cy + h * 0.27f),
                radius = w * 0.34f
            ),
            topLeft = Offset(cx - w * 0.3f, cy + h * 0.21f),
            size = Size(w * 0.6f, h * 0.14f)
        )

        // Ceramic rounded pebble router body (Nest Wifi Pro proportions)
        val bodyW = w * 0.54f
        val bodyH = h * 0.48f
        val bodyLeft = cx - bodyW / 2f
        val bodyTop = cy - bodyH / 2f

        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(surfaceLowest, surfaceHigh),
                startY = bodyTop,
                endY = bodyTop + bodyH
            ),
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyW, bodyH),
            cornerRadius = CornerRadius(bodyW * 0.42f, bodyW * 0.42f)
        )

        // Subtle rim highlight
        drawRoundRect(
            color = outline.copy(alpha = 0.7f),
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyW, bodyH),
            cornerRadius = CornerRadius(bodyW * 0.42f, bodyW * 0.42f),
            style = Stroke(width = 1.2.dp.toPx())
        )

        // Top gloss reflection crescent
        drawOval(
            brush = Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.32f), Color.Transparent),
                startY = bodyTop + bodyH * 0.08f,
                endY = bodyTop + bodyH * 0.38f
            ),
            topLeft = Offset(bodyLeft + bodyW * 0.16f, bodyTop + bodyH * 0.07f),
            size = Size(bodyW * 0.68f, bodyH * 0.24f)
        )

        // Breathing Nest-style status LED dot
        val ledCenter = Offset(cx, bodyTop + bodyH * 0.66f)
        drawCircle(
            color = statusDotColor.copy(alpha = 0.28f * ledBreath),
            radius = 7.dp.toPx(),
            center = ledCenter
        )
        drawCircle(
            color = statusDotColor.copy(alpha = ledBreath),
            radius = 3.2.dp.toPx(),
            center = ledCenter
        )
    }
}

/**
 * Smooth dual-curve Bezier Canvas chart for real-time Download & Upload bandwidth.
 */
@Composable
fun LiveBandwidthChart(
    samples: List<BandwidthSample>,
    modifier: Modifier = Modifier
) {
    val mawjaColors = LocalMawjaColors.current
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    val downColor = mawjaColors.downloadBlue
    val upColor = mawjaColors.uploadTeal

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val padTop = 12.dp.toPx()
        val padBottom = 12.dp.toPx()
        val chartH = (h - padTop - padBottom).coerceAtLeast(10f)

        // Subtle horizontal reference grid lines
        for (i in 0..3) {
            val y = padTop + chartH * (i / 3f)
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val displaySamples = if (samples.size >= 2) {
            samples
        } else {
            val single = samples.firstOrNull() ?: BandwidthSample(0f, 0f)
            listOf(single, single)
        }

        val maxVal = max(
            10f,
            displaySamples.maxOf { max(it.downloadMbps, it.uploadMbps) } * 1.22f
        )

        fun buildPoints(selector: (BandwidthSample) -> Float): List<Offset> {
            val stepX = w / (displaySamples.size - 1).coerceAtLeast(1)
            return displaySamples.mapIndexed { idx, s ->
                val ratio = (selector(s) / maxVal).coerceIn(0f, 1f)
                Offset(
                    x = idx * stepX,
                    y = padTop + chartH * (1f - ratio)
                )
            }
        }

        fun drawSmoothSeries(points: List<Offset>, strokeColor: Color, fillAlpha: Float) {
            if (points.size < 2) return
            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 0 until points.lastIndex) {
                    val p0 = points[i]
                    val p1 = points[i + 1]
                    val midX = (p0.x + p1.x) / 2f
                    cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                }
            }

            val fillPath = Path().apply {
                addPath(linePath)
                lineTo(points.last().x, padTop + chartH)
                lineTo(points.first().x, padTop + chartH)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(strokeColor.copy(alpha = fillAlpha), Color.Transparent),
                    startY = padTop,
                    endY = padTop + chartH
                )
            )

            drawPath(
                path = linePath,
                color = strokeColor,
                style = Stroke(
                    width = 2.6.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Live pulse indicator on latest sample
            val last = points.last()
            drawCircle(
                color = strokeColor.copy(alpha = 0.22f),
                radius = 7.dp.toPx(),
                center = last
            )
            drawCircle(
                color = strokeColor,
                radius = 3.5.dp.toPx(),
                center = last
            )
        }

        val upPoints = buildPoints { it.uploadMbps }
        val downPoints = buildPoints { it.downloadMbps }

        drawSmoothSeries(upPoints, upColor, fillAlpha = 0.14f)
        drawSmoothSeries(downPoints, downColor, fillAlpha = 0.22f)
    }
}

/**
 * Compact per-device consumption sparkline used inside Device Control Sheet.
 */
@Composable
fun DeviceSparkline(
    currentMbps: Float,
    isBlocked: Boolean,
    modifier: Modifier = Modifier
) {
    val mawjaColors = LocalMawjaColors.current
    val strokeColor = if (isBlocked) mawjaColors.blockedRed else mawjaColors.downloadBlue
    val pointsData = remember(currentMbps, isBlocked) {
        if (isBlocked) {
            List(12) { 0.05f }
        } else {
            val base = currentMbps.coerceAtLeast(0.5f)
            listOf(0.55f, 0.72f, 0.64f, 0.90f, 0.81f, 1.05f, 0.78f, 0.95f, 0.88f, 1.08f, 0.96f, 1.0f)
                .map { it * base }
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val maxVal = (pointsData.maxOrNull() ?: 1f).coerceAtLeast(1f) * 1.15f
        val step = w / (pointsData.size - 1)
        val pts = pointsData.mapIndexed { idx, v ->
            Offset(idx * step, h - (v / maxVal) * (h * 0.82f) - h * 0.08f)
        }
        val path = Path().apply {
            moveTo(pts.first().x, pts.first().y)
            for (i in 0 until pts.lastIndex) {
                val p0 = pts[i]
                val p1 = pts[i + 1]
                val mx = (p0.x + p1.x) / 2f
                cubicTo(mx, p0.y, mx, p1.y, p1.x, p1.y)
            }
        }
        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

/**
 * Animated 240-degree Google Material radial Speed Test Gauge.
 */
@Composable
fun SpeedTestGauge(
    mbps: Float,
    maxScaleMbps: Float = 350f,
    unitLabel: String,
    phaseLabel: String,
    isUploadPhase: Boolean,
    modifier: Modifier = Modifier
) {
    val mawjaColors = LocalMawjaColors.current
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val activeColor = if (isUploadPhase) mawjaColors.uploadTeal else mawjaColors.downloadBlue

    val animatedFraction by animateFloatAsState(
        targetValue = (mbps / maxScaleMbps).coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 240f),
        label = "gauge_sweep"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 16.dp.toPx()
            val inset = strokePx * 1.2f
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            val startAngle = 150f
            val totalSweep = 240f

            // Background track
            drawArc(
                color = trackColor,
                startAngle = startAngle,
                sweepAngle = totalSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Active gradient arc
            val sweep = totalSweep * animatedFraction
            if (sweep > 0.5f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(activeColor.copy(alpha = 0.65f), activeColor, mawjaColors.uploadTeal)
                    ),
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )

                // Luminous indicator tip
                val endRad = Math.toRadians((startAngle + sweep).toDouble())
                val r = arcSize.width / 2f
                val tipX = center.x + (r * cos(endRad)).toFloat()
                val tipY = center.y + (r * sin(endRad)).toFloat()
                drawCircle(
                    color = activeColor.copy(alpha = 0.26f),
                    radius = 12.dp.toPx(),
                    center = Offset(tipX, tipY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 5.dp.toPx(),
                    center = Offset(tipX, tipY)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = String.format(Locale.US, "%.1f", mbps),
                style = MonoMetricLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = unitLabel,
                style = MaterialTheme.typography.labelLarge,
                color = activeColor
            )
            Text(
                text = phaseLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

/**
 * Crisp, scannable Wi-Fi QR Code rendered in pure Compose Canvas on a pure white quiet-zone card.
 */
@Composable
fun WifiQrCodeCanvas(
    ssid: String,
    password: String,
    modifier: Modifier = Modifier
) {
    val matrix = remember(ssid, password) {
        QrCodeMatrix.encodeWifi(ssid, password)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(168.dp)) {
            val count = matrix.size
            val cell = size.width / count.toFloat()
            val radius = cell * 0.22f

            for (y in 0 until count) {
                for (x in 0 until count) {
                    if (matrix[y][x]) {
                        drawRoundRect(
                            color = Color(0xFF121826),
                            topLeft = Offset(x * cell, y * cell),
                            size = Size(cell * 0.96f, cell * 0.96f),
                            cornerRadius = CornerRadius(radius, radius)
                        )
                    }
                }
            }
        }
    }
}
