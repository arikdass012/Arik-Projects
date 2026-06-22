package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun SystemStatusBar(
    isSimulating: Boolean,
    uptimePercent: Double,
    uptimeSeconds: Long,
    faultCount: Int,
    activeFaultLabel: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        shape = RoundedCornerShape(12.dp),
        border = CardBorder()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status led + Sim state
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (isSimulating) NeonGreen else TextLabelMuted)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "ONLINE SIMULATION",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextLabelMuted
                    )
                    Text(
                        text = if (isSimulating) "ACTIVE" else "STOPPED",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        fontWeight = FontWeight.Bold,
                        color = if (isSimulating) Color.White else TextLabelMuted
                    )
                }
            }

            // Uptime
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "SYSTEM SITE UPTIME",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextLabelMuted
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${String.format(Locale.US, "%.2f", uptimePercent)}%",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        fontWeight = FontWeight.Bold,
                        color = if (uptimePercent > 95) NeonGreen else NeonAmber
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${uptimeSeconds}s)",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = TextLabelMuted
                    )
                }
            }

            // Fault Count / Danger Info
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "PENDING FAULTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextLabelMuted
                )
                Text(
                    text = if (faultCount > 0) "$faultCount DISRUPTIONS" else "0 ACTIVE FAULTS",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    fontWeight = FontWeight.Bold,
                    color = if (faultCount > 0) NeonRed else NeonGreen
                )
            }
        }
    }
}

@Composable
fun WaveformPanel(
    points: List<WavePoint>,
    modifier: Modifier = Modifier
) {
    var waveformMode by remember { mutableStateOf("CURRENT") } // "CURRENT" or "VOLTAGE"

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        shape = RoundedCornerShape(12.dp),
        border = CardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = NeonBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "3-PHASE WAVEFORMS",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Waveform mode toggles
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SlateSurfaceVariant)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (waveformMode == "CURRENT") SlateSurface else Color.Transparent)
                            .clickable { waveformMode = "CURRENT" }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Current (A)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (waveformMode == "CURRENT") Color.White else TextLabelMuted
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (waveformMode == "VOLTAGE") SlateSurface else Color.Transparent)
                            .clickable { waveformMode = "VOLTAGE" }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Voltage (V)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (waveformMode == "VOLTAGE") Color.White else TextLabelMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Oscilloscope screen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BaseOscilloscopeScreen)
                    .border(1.dp, GridDarkLine, RoundedCornerShape(8.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val centerY = h / 2f

                    // Draw grid lines
                    val cols = 8
                    val rows = 6
                    val colWidth = w / cols
                    val rowHeight = h / rows

                    for (i in 1 until cols) {
                        drawLine(
                            color = GridDarkLine,
                            start = Offset(i * colWidth, 0f),
                            end = Offset(i * colWidth, h),
                            strokeWidth = 1f
                        )
                    }
                    for (i in 1 until rows) {
                        drawLine(
                            color = GridDarkLine,
                            start = Offset(0f, i * rowHeight),
                            end = Offset(w, i * rowHeight),
                            strokeWidth = 1f
                        )
                    }

                    // Central horizontal baseline
                    drawLine(
                        color = Color(0xFF334155),
                        start = Offset(0f, centerY),
                        end = Offset(w, centerY),
                        strokeWidth = 1.5f
                    )

                    if (points.isNotEmpty()) {
                        val numPoints = points.size
                        val xStep = w / (numPoints - 1)

                        // Mapping scalers
                        val scale = if (waveformMode == "VOLTAGE") {
                            centerY / 240f // Peak ~170V plus margin
                        } else {
                            centerY / 50f  // Peak ~14A, fault peaks ~42A
                        }

                        val pathA = Path()
                        val pathB = Path()
                        val pathC = Path()

                        points.forEachIndexed { idx, pt ->
                            val x = idx * xStep
                            val yA = centerY - (if (waveformMode == "VOLTAGE") pt.vA else pt.iA).toFloat() * scale
                            val yB = centerY - (if (waveformMode == "VOLTAGE") pt.vB else pt.iB).toFloat() * scale
                            val yC = centerY - (if (waveformMode == "VOLTAGE") pt.vC else pt.iC).toFloat() * scale

                            if (idx == 0) {
                                pathA.moveTo(x, yA)
                                pathB.moveTo(x, yB)
                                pathC.moveTo(x, yC)
                            } else {
                                pathA.lineTo(x, yA)
                                pathB.lineTo(x, yB)
                                pathC.lineTo(x, yC)
                            }
                        }

                        // Drawing glow and paths
                        drawPath(
                            path = pathA,
                            color = NeonGreen,
                            style = Stroke(width = 2.5f * density, cap = StrokeCap.Round)
                        )
                        drawPath(
                            path = pathB,
                            color = NeonAmber,
                            style = Stroke(width = 2.5f * density, cap = StrokeCap.Round)
                        )
                        drawPath(
                            path = pathC,
                            color = NeonBlue,
                            style = Stroke(width = 2.5f * density, cap = StrokeCap.Round)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Legend indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                LegendItem(label = "Phase A (Ia/Va)", color = NeonGreen)
                LegendItem(label = "Phase B (Ib/Vb)", color = NeonAmber)
                LegendItem(label = "Phase C (Ic/Vc)", color = NeonBlue)
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}

@Composable
fun FftSpectrumPanel(
    fftData: FftData?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        shape = RoundedCornerShape(12.dp),
        border = CardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Equalizer,
                    contentDescription = null,
                    tint = NeonAmber,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "FFT SPECTRUM ANALYZER",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Baseline
                    drawLine(
                        color = GridDarkLine,
                        start = Offset(0f, h),
                        end = Offset(w, h),
                        strokeWidth = 2f
                    )

                    if (fftData != null && fftData.magnitudes.isNotEmpty()) {
                        val numBars = fftData.magnitudes.size
                        val barSpacing = 40f
                        val totalSpacing = barSpacing * (numBars + 1)
                        val barWidth = (w - totalSpacing) / numBars

                        val maxAvailableMag = 18.0 // standard max harmonic
                        val maxFundamentalExpected = 15.0

                        fftData.magnitudes.forEachIndexed { i, mag ->
                            val freq = fftData.frequencies[i]
                            val barX = barSpacing + i * (barWidth + barSpacing)

                            // Base normalized height
                            val expect = if (i == 0) maxFundamentalExpected else maxAvailableMag
                            val ratio = (mag / expect).coerceAtMost(1.0)
                            val barHeight = (h * ratio).coerceAtLeast(4.0).toFloat()

                            // Linear gradient fill
                            val brush = Brush.verticalGradient(
                                colors = listOf(
                                    if (i == 0) NeonGreen else NeonBlue,
                                    if (i == 0) NeonGreen.copy(alpha = 0.5f) else NeonBlue.copy(alpha = 0.4f)
                                ),
                                startY = h - barHeight,
                                endY = h
                            )

                            // Rounded top bar
                            drawRoundRect(
                                brush = brush,
                                topLeft = Offset(barX, h - barHeight),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                        }
                    }
                }
            }

            // Frequency Labels under the canvas
            if (fftData != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    fftData.frequencies.forEachIndexed { i, freq ->
                        val label = if (i == 0) "$freq Hz\n(Fund.)" else "$freq Hz\n(${getOrdinal(i * 2 + 1)})"
                        Text(
                            text = label,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            ),
                            color = TextLabelMuted
                        )
                    }
                }
            }
        }
    }
}

private fun getOrdinal(i: Int): String {
    return when (i) {
        3 -> "3rd"
        5 -> "5th"
        7 -> "7th"
        else -> "${i}th"
    }
}

@Composable
fun GaugePanel(
    packet: SignalPacket?,
    modifier: Modifier = Modifier
) {
    val curA = packet?.phases?.A?.current ?: 0.0
    val curB = packet?.phases?.B?.current ?: 0.0
    val curC = packet?.phases?.C?.current ?: 0.0
    val maxCurrent = listOf(curA, curB, curC).maxOrNull() ?: 10.0

    val volA = packet?.phases?.A?.voltage ?: 120.0
    val volB = packet?.phases?.B?.voltage ?: 120.0
    val volC = packet?.phases?.C?.voltage ?: 120.0
    val avgVoltage = listOf(volA, volB, volC).average()

    val powerFactor = packet?.powerFactor ?: 0.95

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        shape = RoundedCornerShape(12.dp),
        border = CardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CompassCalibration,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SYSTEM HEALTH TELEMETRY",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Voltage Gauge
                CircularGauge(
                    title = "VOLTAGE (Avg)",
                    value = String.format(Locale.US, "%.1f", avgVoltage),
                    unit = "V",
                    percentage = (avgVoltage / 160.0).coerceIn(0.0, 1.0).toFloat(),
                    arcColor = when {
                        avgVoltage < 90.0 || avgVoltage > 140.0 -> NeonRed
                        avgVoltage < 105.0 || avgVoltage > 128.0 -> NeonAmber
                        else -> NeonGreen
                    }
                )

                // Current Gauge
                CircularGauge(
                    title = "MAX CURRENT",
                    value = String.format(Locale.US, "%.1f", maxCurrent),
                    unit = "A",
                    percentage = (maxCurrent / 45.0).coerceIn(0.0, 1.0).toFloat(),
                    arcColor = when {
                        maxCurrent > 25.0 -> NeonRed
                        maxCurrent > 14.5 -> NeonAmber
                        else -> NeonGreen
                    }
                )

                // Power Factor Gauge
                CircularGauge(
                    title = "POWER FACTOR",
                    value = String.format(Locale.US, "%.2f", powerFactor),
                    unit = "cosφ",
                    percentage = powerFactor.toFloat(),
                    arcColor = when {
                        powerFactor < 0.75 -> NeonRed
                        powerFactor < 0.90 -> NeonAmber
                        else -> NeonGreen
                    }
                )
            }
        }
    }
}

@Composable
fun CircularGauge(
    title: String,
    value: String,
    unit: String,
    percentage: Float,
    arcColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.size(90.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val sizeVal = size.width
                val strokeWidth = 18f

                // Draw background circle tracker track
                drawArc(
                    color = GridDarkLine,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Draw filled status ring
                drawArc(
                    color = arcColor,
                    startAngle = 135f,
                    sweepAngle = 270f * percentage,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextLabelMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = TextLabelMuted
        )
    }
}

@Composable
fun FaultFeedPanel(
    events: List<FaultEvent>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        shape = RoundedCornerShape(12.dp),
        border = CardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = NeonRed,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "GRID FAULT ALERT FEED",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (events.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "NO FAULTS DETECTED",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextLabelMuted
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(events, key = { it.id }) { event ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + expandVertically()
                        ) {
                            FaultFeedItem(event = event)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FaultFeedItem(event: FaultEvent) {
    val dateStr = remember(event.timestamp) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(event.timestamp)
    }

    val isCritical = event.confidenceScore > 85.0

    val backgroundItemColor = if (isCritical) {
        DeepAlertRedBackground // #2D1616
    } else {
        SlateSurfaceVariant
    }

    val borderColor = if (isCritical) {
        AlertRedBorder // #7F1D1D
    } else {
        GridDarkLine
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundItemColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isCritical) Icons.Default.NotificationsActive else Icons.Default.PriorityHigh,
            contentDescription = null,
            tint = if (isCritical) NeonRed else NeonAmber,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = event.faultType.name.replace("_", " "),
                    fontWeight = FontWeight.Bold,
                    color = if (isCritical) NeonRed else Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = TextLabelMuted
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Phase: ${event.affectedPhase} | Confidence: ${event.confidenceScore}%",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = event.recommendedAction,
                style = MaterialTheme.typography.bodySmall,
                color = TextLabelMuted
            )
        }
    }
}

@Composable
fun ControlPanel(
    isSimulating: Boolean,
    speed: Double,
    injection: FaultType,
    injectedPhase: String,
    onToggleSim: (Boolean) -> Unit,
    onSetSpeed: (Double) -> Unit,
    onSetInjection: (FaultType) -> Unit,
    onInjectNow: () -> Unit,
    onExportHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedFaultDropdown by remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        shape = RoundedCornerShape(12.dp),
        border = CardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SettingsInputComponent,
                    contentDescription = null,
                    tint = NeonBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SIMULATION CONTROL ENGINE",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Start/Stop Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Simulator Engine Status",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )

                Button(
                    onClick = { onToggleSim(!isSimulating) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSimulating) Color(0xFFDC2626) else NeonGreen,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("toggle_simulator_button")
                ) {
                    Icon(
                        imageVector = if (isSimulating) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = if (isSimulating) "STOP GENERATION" else "START GENERATION")
                }
            }

            Divider(color = GridDarkLine, modifier = Modifier.padding(vertical = 12.dp))

            // Speed Slider
            Text(
                text = "Simulation Speed Multiplier: ${String.format(Locale.US, "%.1f", speed)}x",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
            Slider(
                value = speed.toFloat(),
                onValueChange = { onSetSpeed(it.toDouble()) },
                valueRange = 0.5f..3.0f,
                steps = 4,
                colors = SliderDefaults.colors(
                    thumbColor = NeonGreen,
                    activeTrackColor = NeonGreen,
                    inactiveTrackColor = GridDarkLine
                )
            )

            Divider(color = GridDarkLine, modifier = Modifier.padding(vertical = 12.dp))

            // Injection Dropdown & Inject Button
            Text(
                text = "Target Fault Type Configuration",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedFaultDropdown = true }
                        .border(1.dp, GridDarkLine, RoundedCornerShape(8.dp))
                        .background(SlateSurfaceVariant, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (injection == FaultType.NORMAL) "Random Fault Injector (Cycles)" else injection.name.replace("_", " "),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }

                DropdownMenu(
                    expanded = expandedFaultDropdown,
                    onDismissRequest = { expandedFaultDropdown = false },
                    modifier = Modifier
                        .background(SlateSurface)
                        .border(1.dp, GridDarkLine)
                ) {
                    DropdownMenuItem(
                        text = { Text("Random Fault Injector", color = Color.White) },
                        onClick = {
                            onSetInjection(FaultType.NORMAL)
                            expandedFaultDropdown = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Short Circuit (Current Spike)", color = Color.White) },
                        onClick = {
                            onSetInjection(FaultType.SHORT_CIRCUIT)
                            expandedFaultDropdown = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Overload (Thermal Gradual)", color = Color.White) },
                        onClick = {
                            onSetInjection(FaultType.OVERLOAD)
                            expandedFaultDropdown = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Phase Imbalance (Deviation)", color = Color.White) },
                        onClick = {
                            onSetInjection(FaultType.PHASE_IMBALANCE)
                            expandedFaultDropdown = false
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Trigger Button
                Button(
                    onClick = onInjectNow,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonAmber, contentColor = Color.Black),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("inject_fault_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "INJECT FAULT", fontWeight = FontWeight.Bold)
                }

                // Export Button
                Button(
                    onClick = onExportHistory,
                    colors = ButtonDefaults.buttonColors(containerColor = SlateSurfaceVariant, contentColor = Color.White),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, GridDarkLine, RoundedCornerShape(100.dp))
                        .testTag("export_csv_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "EXPORT CSV")
                }
            }
        }
    }
}

@Composable
fun CardBorder(): BorderStroke {
    return BorderStroke(1.dp, GridDarkLine)
}
