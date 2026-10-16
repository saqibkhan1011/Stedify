package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GeoPurpleBorder
import com.example.ui.theme.GeoPurpleContainer
import com.example.ui.theme.GeoPurpleDark
import com.example.ui.theme.GeoPurpleLight
import com.example.ui.theme.GeoPurplePrimary
import com.example.ui.theme.GeoStreakContainer
import com.example.ui.theme.GeoStreakDarkContainer
import com.example.ui.theme.GeoStreakDarkOnContainer
import com.example.ui.theme.GeoStreakOnContainer
import java.util.Locale

import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.IconButton

@Composable
fun DashboardHeaderCard(
    dateString: String,
    streakCount: Int,
    consistencyScore: Float,
    completedTasks: Int,
    totalTasks: Int,
    modifier: Modifier = Modifier,
    onOpenAuthSheet: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_header_card")
    ) {
        // Top row: Date subtitle + Stedify Title, Right: Streak pill and Cloud profile button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Stedify",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StreakBadge(
                    streak = streakCount,
                    modifier = Modifier.testTag("current_streak_badge")
                )

                Surface(
                    onClick = onOpenAuthSheet,
                    shape = CircleShape,
                    color = if (isDark) MaterialTheme.colorScheme.surfaceVariant else GeoPurpleContainer,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outlineVariant else GeoPurpleBorder),
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("cloud_profile_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Cloud Backup & Account",
                            tint = if (isDark) MaterialTheme.colorScheme.onSurface else GeoPurplePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Geometric Consistency Score Card
        val cardBg = if (isDark) MaterialTheme.colorScheme.surfaceVariant else GeoPurpleContainer
        val cardBorder = if (isDark) MaterialTheme.colorScheme.outlineVariant else GeoPurpleBorder
        val textColor = if (isDark) MaterialTheme.colorScheme.onSurface else GeoPurpleDark

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "CONSISTENCY SCORE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = textColor
                    )

                    Text(
                        text = String.format(Locale.getDefault(), "%.0f%%", consistencyScore),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black
                        ),
                        color = textColor
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Progress Bar
                ConsistencyProgressBar(
                    score = consistencyScore,
                    isDark = isDark,
                    modifier = Modifier.testTag("consistency_progress_bar")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle: Tasks breakdown / momentum status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusText = if (totalTasks > 0) {
                        "$completedTasks of $totalTasks tasks completed today"
                    } else {
                        "No tasks scheduled yet today"
                    }

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = textColor.copy(alpha = 0.8f)
                    )

                    if (totalTasks > 0) {
                        val rate = (completedTasks.toFloat() / totalTasks.toFloat()) * 100f
                        Text(
                            text = String.format(Locale.getDefault(), "%.0f%% today", rate),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StreakBadge(
    streak: Int,
    modifier: Modifier = Modifier
) {
    val isZero = streak == 0
    val isDark = isSystemInDarkTheme()

    val containerColor = if (isDark) {
        if (!isZero) GeoStreakDarkContainer else MaterialTheme.colorScheme.surfaceVariant
    } else {
        if (!isZero) GeoStreakContainer else MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = if (isDark) {
        if (!isZero) GeoStreakDarkOnContainer else MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        if (!isZero) GeoStreakOnContainer else MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (!isZero) Icons.Default.LocalFireDepartment else Icons.Default.AcUnit,
                contentDescription = "Streak",
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )

            Text(
                text = "$streak DAY STREAK",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = contentColor
            )
        }
    }
}

@Composable
fun ConsistencyProgressBar(
    score: Float,
    isDark: Boolean = false,
    modifier: Modifier = Modifier
) {
    val progress = (score / 100f).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800),
        label = "progress_anim"
    )

    val trackColor = if (isDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.3f) else GeoPurpleBorder
    val fillColor = if (isDark) GeoPurpleLight else GeoPurplePrimary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(CircleShape)
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .clip(CircleShape)
                .background(fillColor)
        )
    }
}
