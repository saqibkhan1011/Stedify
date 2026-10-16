package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    object Dashboard : Screen(
        route = "dashboard",
        title = "Today",
        icon = Icons.Default.DateRange,
        testTag = "nav_dashboard"
    )

    object Routines : Screen(
        route = "routines",
        title = "Routines",
        icon = Icons.Default.Repeat,
        testTag = "nav_routines"
    )

    object Analytics : Screen(
        route = "analytics",
        title = "Analytics",
        icon = Icons.Default.BarChart,
        testTag = "nav_analytics"
    )

    object Achievements : Screen(
        route = "achievements",
        title = "Badges",
        icon = Icons.Default.EmojiEvents,
        testTag = "nav_achievements"
    )

    object AiOptimizer : Screen(
        route = "ai_optimizer",
        title = "AI Day",
        icon = Icons.Default.AutoAwesome,
        testTag = "nav_ai_optimizer"
    )

    companion object {
        val items: List<Screen>
            get() = listOf(Dashboard, Routines, Analytics, Achievements, AiOptimizer)
    }
}
