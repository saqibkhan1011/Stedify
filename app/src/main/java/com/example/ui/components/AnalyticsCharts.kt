package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GeoPriorityHighBg
import com.example.ui.theme.GeoPriorityHighText
import com.example.ui.theme.GeoPurpleContainer
import com.example.ui.theme.GeoPurpleDark
import com.example.ui.theme.GeoPurpleLight
import com.example.ui.theme.GeoPurplePrimary
import com.example.ui.viewmodel.CalendarDayModel
import com.example.ui.viewmodel.DayCompletionStat

@Composable
fun CompletionBarChart(
    stats: List<DayCompletionStat>,
    modifier: Modifier = Modifier
) {
    if (stats.isEmpty()) return

    val isDark = isSystemInDarkTheme()
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(stats) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 700))
    }

    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("completion_bar_chart")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Completion Velocity",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LegendItem(color = if (isDark) GeoPurpleLight else GeoPurplePrimary, label = "Rate %")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val bottomPadding = 32.dp.toPx()
                val topPadding = 24.dp.toPx()
                val chartHeight = canvasHeight - bottomPadding - topPadding

                // Draw background guidelines (0%, 50%, 100%)
                val steps = listOf(0f, 0.5f, 1f)
                steps.forEach { step ->
                    val y = topPadding + chartHeight * (1f - step)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    )
                }

                val barCount = stats.size
                val totalSlotWidth = canvasWidth / barCount
                val barWidth = (totalSlotWidth * 0.55f).coerceIn(6.dp.toPx(), 28.dp.toPx())

                stats.forEachIndexed { index, stat ->
                    val centerX = totalSlotWidth * index + totalSlotWidth / 2f
                    val animatedRate = stat.completionRate * animationProgress.value
                    val barHeight = chartHeight * animatedRate
                    val topY = topPadding + chartHeight - barHeight
                    val leftX = centerX - barWidth / 2f

                    // Draw bar background (light track)
                    val trackColor = if (isDark) Color(0xFF2B2930) else GeoPurpleContainer.copy(alpha = 0.5f)
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(leftX, topPadding),
                        size = Size(barWidth, chartHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )

                    // Draw actual filled bar
                    if (animatedRate > 0.01f) {
                        val barBrush = Brush.verticalGradient(
                            colors = if (isDark) {
                                listOf(GeoPurpleLight, Color(0xFF9A82DB))
                            } else {
                                listOf(GeoPurpleLight, GeoPurplePrimary)
                            },
                            startY = topY,
                            endY = topPadding + chartHeight
                        )

                        drawRoundRect(
                            brush = barBrush,
                            topLeft = Offset(leftX, topY),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                        )
                    }

                    // Draw label
                    val paint = android.graphics.Paint().apply {
                        color = labelColor
                        textSize = if (barCount <= 7) 32f else 24f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }

                    drawContext.canvas.nativeCanvas.drawText(
                        stat.dayLabel,
                        centerX,
                        canvasHeight - 6.dp.toPx(),
                        paint
                    )
                }
            }
        }
    }
}

@Composable
fun MonthlyCalendarGrid(
    days: List<CalendarDayModel>,
    selectedDay: CalendarDayModel?,
    onSelectDay: (CalendarDayModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val dayNames = listOf("M", "T", "W", "T", "F", "S", "S")

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_calendar_grid")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Day of week headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                dayNames.forEach { name ->
                    Box(
                        modifier = Modifier.width(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 7 columns grid (5 or 6 rows)
            val chunkedDays = days.chunked(7)
            chunkedDays.forEach { week ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    week.forEach { day ->
                        CalendarDayCell(
                            day = day,
                            isSelected = selectedDay?.dateString == day.dateString,
                            onClick = { onSelectDay(day) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = GeoPurplePrimary, label = "≥ 80%")
                LegendItem(color = GeoPurpleContainer, label = "40-79%")
                LegendItem(color = GeoPriorityHighBg, label = "< 40%")
                LegendItem(color = MaterialTheme.colorScheme.surfaceVariant, label = "No tasks")
            }
        }
    }
}

@Composable
fun CalendarDayCell(
    day: CalendarDayModel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    val (bgColor, textColor) = when {
        !day.isCurrentMonth -> Pair(Color.Transparent, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f))
        day.total == 0 -> Pair(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        day.completionPercentage >= 80f -> Pair(
            if (isDark) GeoPurpleLight else GeoPurplePrimary,
            if (isDark) GeoPurpleDark else Color.White
        )
        day.completionPercentage >= 40f -> Pair(
            if (isDark) Color(0xFF4F378B) else GeoPurpleContainer,
            if (isDark) GeoPurpleContainer else GeoPurpleDark
        )
        else -> Pair(
            if (isDark) Color(0xFF601410) else GeoPriorityHighBg,
            if (isDark) Color(0xFFF9DEDC) else GeoPriorityHighText
        )
    }

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (day.isCurrentMonth) bgColor else Color.Transparent)
            .clickable(enabled = day.isCurrentMonth) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(2.dp, if (isDark) GeoPurpleLight else GeoPurplePrimary),
                modifier = Modifier.fillMaxWidth().aspectRatio(1f)
            ) {}
        }

        Text(
            text = "${day.dayOfMonth}",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (day.isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp
            ),
            color = if (day.isCurrentMonth) textColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
    }
}

@Composable
fun LegendItem(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
