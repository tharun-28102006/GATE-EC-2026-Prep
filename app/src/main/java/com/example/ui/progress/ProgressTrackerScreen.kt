package com.example.ui.progress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.DayActivityPoint
import com.example.ui.ProgressSummary
import com.example.ui.SectionMastery
import com.example.ui.theme.GateIcons
import com.example.ui.theme.IbmPlexMonoFamily
import com.example.ui.theme.LocalGateColors

@Composable
fun ProgressTrackerScreen(
    summary: ProgressSummary
) {
    val gateColors = LocalGateColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Institutional Header
        Surface(
            color = gateColors.examHeader,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Consistency & Trends",
                        style = MaterialTheme.typography.titleLarge,
                        color = gateColors.examHeaderContent,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "GATE 2026 EC Performance Analytics • Synced",
                        style = MaterialTheme.typography.labelSmall,
                        color = gateColors.examHeaderContent.copy(alpha = 0.85f)
                    )
                }
                Icon(
                    imageVector = GateIcons.TrendingUp,
                    contentDescription = "Performance trends",
                    tint = gateColors.examHeaderContent
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top KPI Grid: Streak + Accuracy + Net Score + Today Goal
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricKpiCard(
                        title = "Daily Streak",
                        value = "${summary.currentStreak}d",
                        subtitle = "Best: ${summary.longestStreak}d",
                        accentColor = gateColors.streak,
                        modifier = Modifier.weight(1f)
                    )
                    MetricKpiCard(
                        title = "Accuracy",
                        value = "${summary.overallAccuracyPercent}%",
                        subtitle = "${summary.totalCorrect}/${summary.totalAttempted} correct",
                        accentColor = gateColors.answered,
                        modifier = Modifier.weight(1f)
                    )
                    MetricKpiCard(
                        title = "Net Score",
                        value = "%.1f".format(summary.netGateScore),
                        subtitle = "GATE marks",
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Today's Daily Goal Progress
            item {
                val goalProgress = (summary.todayCount.toFloat() / summary.dailyGoal.coerceAtLeast(1))
                    .coerceIn(0f, 1f)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, gateColors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = GateIcons.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = gateColors.streak,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Today's Consistency Goal",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "${summary.todayCount} / ${summary.dailyGoal} Qs",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { goalProgress },
                            color = if (goalProgress >= 1f) gateColors.answered else MaterialTheme.colorScheme.primary,
                            trackColor = gateColors.unvisited,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        if (summary.notificationAnsweredCount > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = GateIcons.NotificationsActive,
                                    contentDescription = null,
                                    tint = gateColors.review,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "${summary.notificationAnsweredCount} questions answered directly from lock-screen notifications",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 30-Day Consistency Heatmap
            item {
                ConsistencyHeatmapCard(last30Days = summary.last30Days)
            }

            // Accuracy Performance Trend Chart (Lightweight Canvas)
            item {
                PerformanceTrendChartCard(recentDays = summary.recentTrendDays)
            }

            // GATE 2026 EC 9-Section Syllabus Mastery
            item {
                SyllabusMasteryCard(sections = summary.sectionMastery)
            }
        }
    }
}

@Composable
private fun MetricKpiCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val gateColors = LocalGateColors.current
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, gateColors.border),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = IbmPlexMonoFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = accentColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ConsistencyHeatmapCard(last30Days: List<DayActivityPoint>) {
    val gateColors = LocalGateColors.current
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, gateColors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "30-Day Consistency Heatmap",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${last30Days.count { it.attempted > 0 }}/30 active days",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 3 rows x 10 columns clean grid
            val rows = last30Days.chunked(10)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowItems.forEach { day ->
                            val tileColor = when {
                                day.attempted == 0 -> gateColors.unvisited
                                day.attempted <= 2 -> gateColors.answered.copy(alpha = 0.35f)
                                day.attempted <= 5 -> gateColors.answered.copy(alpha = 0.65f)
                                else -> gateColors.answered
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(tileColor)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PerformanceTrendChartCard(recentDays: List<DayActivityPoint>) {
    val gateColors = LocalGateColors.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = gateColors.border

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, gateColors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Accuracy Trend (Recent Active Days)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (recentDays.isEmpty()) {
                Text(
                    text = "Answer questions in the Practice Desk or from lock-screen notifications to chart your GATE EC accuracy trend.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .padding(top = 8.dp, bottom = 4.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val padH = 14.dp.toPx()
                    val padV = 12.dp.toPx()
                    val chartW = (w - padH * 2).coerceAtLeast(1f)
                    val chartH = (h - padV * 2).coerceAtLeast(1f)

                    // Horizontal guide lines (0%, 50%, 100%)
                    listOf(0f, 0.5f, 1f).forEach { frac ->
                        val y = padV + chartH * (1f - frac)
                        drawLine(
                            color = gridColor,
                            start = Offset(padH, y),
                            end = Offset(w - padH, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    val points = recentDays.mapIndexed { idx, pt ->
                        val x = if (recentDays.size == 1) {
                            padH + chartW / 2f
                        } else {
                            padH + (idx.toFloat() / (recentDays.size - 1)) * chartW
                        }
                        val y = padV + chartH * (1f - (pt.accuracyPercent / 100f).coerceIn(0f, 1f))
                        Offset(x, y)
                    }

                    if (points.size >= 2) {
                        val path = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            points.drop(1).forEach { lineTo(it.x, it.y) }
                        }
                        drawPath(
                            path = path,
                            color = primaryColor,
                            style = Stroke(
                                width = 2.5.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }

                    points.forEach { pt ->
                        drawCircle(
                            color = primaryColor,
                            radius = 4.5.dp.toPx(),
                            center = pt
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = recentDays.first().label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Latest: ${recentDays.last().accuracyPercent.toInt()}% (${recentDays.last().label})",
                        style = MaterialTheme.typography.labelSmall,
                        color = primaryColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SyllabusMasteryCard(sections: List<SectionMastery>) {
    val gateColors = LocalGateColors.current
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, gateColors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "GATE 2026 EC Section Mastery",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            sections.forEach { sec ->
                val progress = if (sec.attempted == 0) 0f else sec.accuracyPercent / 100f
                val barColor = when {
                    sec.attempted == 0 -> gateColors.unvisited
                    sec.accuracyPercent >= 70 -> gateColors.answered
                    sec.accuracyPercent >= 45 -> gateColors.streak
                    else -> gateColors.wrong
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${sec.code} • ${sec.shortName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (sec.attempted == 0) "Not visited" else "${sec.accuracyPercent}% (${sec.correct}/${sec.attempted})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        color = barColor,
                        trackColor = gateColors.unvisited,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }
        }
    }
}
