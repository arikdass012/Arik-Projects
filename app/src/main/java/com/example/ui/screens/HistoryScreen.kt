package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FaultEvent
import com.example.model.FaultType
import com.example.ui.components.CardBorder
import com.example.ui.theme.*
import com.example.viewmodel.GridGuardViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: GridGuardViewModel,
    isWideScreen: Boolean,
    modifier: Modifier = Modifier
) {
    val faultEvents by viewModel.faultEvents.collectAsState()
    val healthHistory by viewModel.systemHealthHistory.collectAsState()

    // 1. Calculate statistics
    val totalFaultsCount = faultEvents.size
    val uptimePercent = viewModel.getUptimePercentage()

    // Most common fault type calculation
    val faultGroups = faultEvents.groupBy { it.faultType }
    val baseFaultGroups = faultGroups.filterKeys { it != FaultType.NORMAL }
    val mostCommonType = baseFaultGroups.maxByOrNull { it.value.size }?.key?.name?.replace("_", " ") ?: "NONE"

    // Average confidence calculation
    val averageConfidence = if (faultEvents.isNotEmpty()) {
        faultEvents.map { it.confidenceScore }.average()
    } else {
        100.0
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header Label
        Text(
            text = "FAULT ANALYTICS & LOG HISTORIAN",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        // Summary Stats Grid
        SummaryStatsRow(
            totalFaults = totalFaultsCount,
            mostCommon = mostCommonType,
            avgConfidence = averageConfidence,
            uptime = uptimePercent,
            isWide = isWideScreen
        )

        if (isWideScreen) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Line Chart Card
                SystemHealthLineChartCard(
                    healthHistory = healthHistory,
                    modifier = Modifier.weight(1.2f)
                )

                // Bar Chart Card
                FaultFrequencyBarChartCard(
                    events = faultEvents,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            SystemHealthLineChartCard(
                healthHistory = healthHistory,
                modifier = Modifier.fillMaxWidth()
            )
            FaultFrequencyBarChartCard(
                events = faultEvents,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Historic log feed table
        HistoricalLogTable(events = faultEvents, modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp))
    }
}

@Composable
fun SummaryStatsRow(
    totalFaults: Int,
    mostCommon: String,
    avgConfidence: Double,
    uptime: Double,
    isWide: Boolean,
    modifier: Modifier = Modifier
) {
    if (isWide) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(title = "Total Faults Recorded", value = "$totalFaults Incidents", icon = Icons.Default.Warning, iconColor = NeonRed, modifier = Modifier.weight(1f))
            StatCard(title = "Most Frequent Type", value = mostCommon, icon = Icons.Default.TrendingUp, iconColor = NeonAmber, modifier = Modifier.weight(1.2f))
            StatCard(title = "Average Classification Confidence", value = "${String.format(Locale.US, "%.1f", avgConfidence)}%", icon = Icons.Default.OfflineBolt, iconColor = NeonBlue, modifier = Modifier.weight(1f))
            StatCard(title = "Site Safe Uptime", value = "${String.format(Locale.US, "%.2f", uptime)}%", icon = Icons.Default.GppGood, iconColor = NeonGreen, modifier = Modifier.weight(1f))
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard(title = "Total Faults", value = "$totalFaults Incidents", icon = Icons.Default.Warning, iconColor = NeonRed, modifier = Modifier.weight(1f))
                StatCard(title = "Safe Uptime", value = "${String.format(Locale.US, "%.2f", uptime)}%", icon = Icons.Default.GppGood, iconColor = NeonGreen, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard(title = "Most Frequent", value = mostCommon, icon = Icons.Default.TrendingUp, iconColor = NeonAmber, modifier = Modifier.weight(1.2f))
                StatCard(title = "Avg Confidence", value = "${String.format(Locale.US, "%.1f", avgConfidence)}%", icon = Icons.Default.OfflineBolt, iconColor = NeonBlue, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        shape = RoundedCornerShape(12.dp),
        border = CardBorder()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.labelSmall, color = TextLabelMuted)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
fun SystemHealthLineChartCard(
    healthHistory: List<Int>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        shape = RoundedCornerShape(12.dp),
        border = CardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "SYSTEM HEALTH VECTOR PROFILE (0-100)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0C0F16))
                    .border(1.dp, GridDarkLine, RoundedCornerShape(8.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Draw grid reference background lines (quarter lines)
                    val lines = 4
                    for (i in 1 until lines) {
                        val ly = (h / lines) * i
                        drawLine(
                            color = GridDarkLine,
                            start = Offset(0f, ly),
                            end = Offset(w, ly),
                            strokeWidth = 1f
                        )
                    }

                    if (healthHistory.isNotEmpty()) {
                        val numPoints = healthHistory.size
                        val xStep = if (numPoints > 1) w / (numPoints - 1) else w

                        val path = Path()
                        healthHistory.forEachIndexed { i, health ->
                            val cx = i * xStep
                            // Remap 0-100 to canvas height h-0 (invert)
                            val cy = h - (h * (health / 100.0f))

                            if (i == 0) {
                                path.moveTo(cx, cy)
                            } else {
                                path.lineTo(cx, cy)
                            }
                        }

                        // Glow fill translucent gradient
                        val gradientPath = Path().apply {
                            addPath(path)
                            lineTo(w, h)
                            lineTo(0f, h)
                            close()
                        }

                        drawPath(
                            path = gradientPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(NeonGreen.copy(alpha = 0.3f), Color.Transparent),
                                startY = 0f,
                                endY = h
                            ),
                            style = Fill
                        )

                        drawPath(
                            path = path,
                            color = NeonGreen,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FaultFrequencyBarChartCard(
    events: List<FaultEvent>,
    modifier: Modifier = Modifier
) {
    val nonNormalEvents = events.filter { it.faultType != FaultType.NORMAL }
    val total = nonNormalEvents.size.coerceAtLeast(1)

    val shortCircuitCount = nonNormalEvents.count { it.faultType == FaultType.SHORT_CIRCUIT }
    val overloadCount = nonNormalEvents.count { it.faultType == FaultType.OVERLOAD }
    val imbalanceCount = nonNormalEvents.count { it.faultType == FaultType.PHASE_IMBALANCE }

    val counts = listOf(shortCircuitCount, overloadCount, imbalanceCount)
    val labels = listOf("Short Circuit", "Overload", "Phase Imb.")
    val maxCount = counts.maxOrNull()?.coerceAtLeast(5) ?: 5

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        shape = RoundedCornerShape(12.dp),
        border = CardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "INCIDENT FREQUENCY SPECTRUM",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    val numBars = counts.size
                    val spacing = 50f
                    val barWidth = (w - (spacing * (numBars + 1))) / numBars

                    counts.forEachIndexed { i, count ->
                        val ratio = count.toFloat() / maxCount.toFloat()
                        val barHeight = h * ratio
                        val barX = spacing + i * (barWidth + spacing)

                        // Dual tone bar colors
                        val colors = when (i) {
                            0 -> listOf(NeonRed, NeonRed.copy(alpha = 0.4f))
                            1 -> listOf(NeonAmber, NeonAmber.copy(alpha = 0.4f))
                            else -> listOf(NeonBlue, NeonBlue.copy(alpha = 0.4f))
                        }

                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = colors,
                                startY = h - barHeight,
                                endY = h
                            ),
                            topLeft = Offset(barX, h - barHeight),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Labels Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                counts.forEachIndexed { i, c ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = labels[i],
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "$c",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = TextLabelMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoricalLogTable(
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
            Text(
                text = "HISTORICAL FAULT INTRUSION ARCHIVE",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (events.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ARCHIVE EMPTY — NO EVENT RECORD RETRIEVED",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextLabelMuted
                    )
                }
            } else {
                // Table header row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SlateSurfaceVariant, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "TIME", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextLabelMuted)
                    Text(text = "CLASSIFICATION", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextLabelMuted)
                    Text(text = "PHASE", modifier = Modifier.weight(0.7f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextLabelMuted)
                    Text(text = "CONF.", modifier = Modifier.weight(0.7f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextLabelMuted, textAlign = TextAlign.End)
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Scrollable table items inside a small scroll container instead of double Nested Scrolling conflicts
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    events.forEach { e ->
                        val logTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(e.timestamp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = logTime,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                                color = Color.White
                            )
                            Text(
                                text = e.faultType.name.replace("_", " "),
                                modifier = Modifier.weight(1.5f),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (e.faultType == FaultType.SHORT_CIRCUIT) NeonRed else if (e.faultType == FaultType.OVERLOAD) NeonAmber else NeonBlue
                            )
                            Text(
                                text = e.affectedPhase,
                                modifier = Modifier.weight(0.7f),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = Color.White
                            )
                            Text(
                                text = "${e.confidenceScore}%",
                                modifier = Modifier.weight(0.7f),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = Color.White,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }
}
