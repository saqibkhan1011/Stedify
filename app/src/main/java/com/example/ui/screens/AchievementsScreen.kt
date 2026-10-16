package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Fireplace
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.BadgeEntity
import com.example.ui.theme.GeoAmber
import com.example.ui.theme.GeoAmberContainer
import com.example.ui.theme.GeoBluePrimary
import com.example.ui.theme.GeoCoral
import com.example.ui.theme.GeoPurpleContainer
import com.example.ui.theme.GeoPurpleDark
import com.example.ui.theme.GeoPurplePrimary
import com.example.ui.viewmodel.AchievementsUiState
import com.example.ui.viewmodel.AchievementsViewModel
import com.example.ui.viewmodel.BadgeFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AchievementsScreen(
    uiState: AchievementsUiState,
    viewModel: AchievementsViewModel,
    onOpenAuthSheet: () -> Unit = {}
) {
    val gamification = uiState.gamification

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("achievements_screen"),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- 1. HERO LEVEL CARD (Full Width Span) ---
        item(span = { GridItemSpan(2) }) {
            LevelHeroCard(
                uiState = uiState,
                onRefresh = { viewModel.refreshAchievements() }
            )
        }

        // --- 2. STATS SUMMARY ROW (Full Width Span) ---
        item(span = { GridItemSpan(2) }) {
            GamificationStatsRow(uiState = uiState)
        }

        // --- 3. FILTER TABS (Full Width Span) ---
        item(span = { GridItemSpan(2) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = uiState.currentFilter == BadgeFilter.ALL,
                    onClick = { viewModel.setFilter(BadgeFilter.ALL) },
                    label = { Text("All (${uiState.totalBadgesCount})") },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GeoPurplePrimary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("filter_all_badges")
                )
                FilterChip(
                    selected = uiState.currentFilter == BadgeFilter.UNLOCKED,
                    onClick = { viewModel.setFilter(BadgeFilter.UNLOCKED) },
                    label = { Text("Unlocked (${uiState.unlockedCount})") },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GeoPurplePrimary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("filter_unlocked_badges")
                )
                FilterChip(
                    selected = uiState.currentFilter == BadgeFilter.IN_PROGRESS,
                    onClick = { viewModel.setFilter(BadgeFilter.IN_PROGRESS) },
                    label = { Text("In Progress (${uiState.totalBadgesCount - uiState.unlockedCount})") },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GeoPurplePrimary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("filter_locked_badges")
                )
            }
        }

        // --- 4. BADGES GRID ITEMS (2 Columns) ---
        items(
            items = uiState.filteredBadges,
            key = { it.id }
        ) { badge ->
            BadgeGridCard(
                badge = badge,
                onClick = { viewModel.selectBadge(badge) }
            )
        }

        // Bottom spacer
        item(span = { GridItemSpan(2) }) {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // --- Badge Details Dialog ---
    if (uiState.selectedBadge != null) {
        BadgeDetailsDialog(
            badge = uiState.selectedBadge,
            onDismiss = { viewModel.selectBadge(null) }
        )
    }
}

@Composable
private fun LevelHeroCard(
    uiState: AchievementsUiState,
    onRefresh: () -> Unit
) {
    val gamification = uiState.gamification

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = GeoPurpleDark
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("level_hero_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            GeoPurplePrimary.copy(alpha = 0.85f),
                            GeoPurpleDark
                        ),
                        radius = 600f
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .border(1.5.dp, GeoAmber, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "L${gamification.currentLevel}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = GeoAmber
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "LEVEL ${gamification.currentLevel}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                    color = GeoAmber
                                )
                            )
                            Text(
                                text = gamification.levelTitle,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }

                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.testTag("refresh_gamification_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Achievements",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // XP Progress Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "${gamification.totalXp} Total XP",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "${uiState.xpNeededForNextLevel} XP to Level ${gamification.currentLevel + 1}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(uiState.progressWithinLevel)
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(GeoAmber, Color(0xFFFFD54F))
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Earn XP by completing daily tasks (+25 XP), high-priority focus (+35 XP), maintaining streaks (+100 XP), and unlocking badges (+200 XP).",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun GamificationStatsRow(uiState: AchievementsUiState) {
    val gamification = uiState.gamification

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatPill(
            icon = Icons.Default.CheckCircle,
            label = "Tasks",
            value = "${gamification.tasksCompletedCount}",
            tint = GeoPurplePrimary,
            modifier = Modifier.weight(1f)
        )
        StatPill(
            icon = Icons.Default.Whatshot,
            label = "Streak",
            value = "${gamification.longestStreak}d",
            tint = GeoCoral,
            modifier = Modifier.weight(1f)
        )
        StatPill(
            icon = Icons.Default.Star,
            label = "100% Days",
            value = "${gamification.flawlessDaysCount}",
            tint = GeoAmber,
            modifier = Modifier.weight(1f)
        )
        StatPill(
            icon = Icons.Default.MilitaryTech,
            label = "Badges",
            value = "${uiState.unlockedCount}/${uiState.totalBadgesCount}",
            tint = GeoBluePrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatPill(
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BadgeGridCard(
    badge: BadgeEntity,
    onClick: () -> Unit
) {
    val isUnlocked = badge.isUnlocked

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isUnlocked) 2.dp else 0.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isUnlocked) 1.5.dp else 1.dp,
                color = if (isUnlocked) GeoAmber.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .testTag("badge_item_${badge.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Badge Icon / Image
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(
                        if (isUnlocked) GeoAmberContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .border(
                        width = 1.dp,
                        color = if (isUnlocked) GeoAmber else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isUnlocked) {
                    if (badge.iconUrl.isNotBlank()) {
                        AsyncImage(
                            model = badge.iconUrl,
                            contentDescription = badge.name,
                            modifier = Modifier.size(36.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = GeoAmber,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked Badge",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Category tag
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isUnlocked) GeoPurpleContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badge.category.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (isUnlocked) GeoPurplePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Badge Name
            Text(
                text = badge.name,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                ),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Description
            Text(
                text = badge.description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Progress or Unlocked status
            if (isUnlocked) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = GeoAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val dateText = if (badge.unlockedAt != null) {
                        val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
                        sdf.format(Date(badge.unlockedAt))
                    } else "Unlocked"
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = GeoAmber
                        )
                    )
                }
            } else {
                val progressFraction = if (badge.requiredCount > 0) {
                    (badge.currentProgress.toFloat() / badge.requiredCount.toFloat()).coerceIn(0f, 1f)
                } else 0f

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(6.dp)
                            .clip(CircleShape),
                        color = GeoPurplePrimary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${badge.currentProgress} / ${badge.requiredCount}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun BadgeDetailsDialog(
    badge: BadgeEntity,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GeoPurplePrimary
                ),
                modifier = Modifier.testTag("close_badge_dialog_button")
            ) {
                Text("Got It")
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (badge.isUnlocked) GeoAmberContainer else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (badge.isUnlocked) {
                        AsyncImage(
                            model = badge.iconUrl,
                            contentDescription = badge.name,
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = badge.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${badge.category} Achievement",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = badge.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "XP Reward",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "+200 XP",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = GeoAmber
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Status",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = if (badge.isUnlocked) "Unlocked" else "In Progress (${badge.currentProgress}/${badge.requiredCount})",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (badge.isUnlocked) Color(0xFF2E7D32) else GeoPurplePrimary
                                )
                            )
                        }

                        if (badge.unlockedAt != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            val sdf = SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.getDefault())
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Earned On",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = sdf.format(Date(badge.unlockedAt)),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}
