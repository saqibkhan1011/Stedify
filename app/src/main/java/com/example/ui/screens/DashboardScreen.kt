package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CreateTaskDialog
import com.example.ui.components.DashboardHeaderCard
import com.example.ui.components.TaskItemCard
import com.example.ui.theme.GeoPurpleDark
import com.example.ui.theme.GeoPurplePrimary
import com.example.ui.viewmodel.DashboardViewModel
import com.example.ui.viewmodel.TaskFilter

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToAiOptimizer: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenAuthSheet: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateTaskDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Optimize My Day FAB (Geometric Balance styled)
                ExtendedFloatingActionButton(
                    onClick = onNavigateToAiOptimizer,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White
                        )
                    },
                    text = {
                        Text(
                            text = "Optimize My Day",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    containerColor = GeoPurplePrimary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("optimize_day_fab")
                )

                // Quick Add Task FAB
                FloatingActionButton(
                    onClick = { showCreateTaskDialog = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("add_task_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Task")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("dashboard_task_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Card
            item {
                DashboardHeaderCard(
                    dateString = state.todayFormattedDate,
                    streakCount = state.currentStreak,
                    consistencyScore = state.rollingConsistencyScore,
                    completedTasks = state.completedCount,
                    totalTasks = state.totalCount,
                    onOpenAuthSheet = onOpenAuthSheet
                )
            }

            // Timetable Header & Filter Chips
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TODAY'S TIMETABLE",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = state.selectedFilter == TaskFilter.ALL,
                            onClick = { viewModel.setFilter(TaskFilter.ALL) },
                            label = { Text("All (${state.totalCount})", style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("filter_all")
                        )
                        FilterChip(
                            selected = state.selectedFilter == TaskFilter.PENDING,
                            onClick = { viewModel.setFilter(TaskFilter.PENDING) },
                            label = { Text("Pending", style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("filter_pending")
                        )
                        FilterChip(
                            selected = state.selectedFilter == TaskFilter.COMPLETED,
                            onClick = { viewModel.setFilter(TaskFilter.COMPLETED) },
                            label = { Text("Done", style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("filter_completed")
                        )
                    }
                }
            }

            // Empty State
            if (state.tasks.isEmpty()) {
                item {
                    EmptyTimetableCard(
                        filter = state.selectedFilter,
                        onAddTask = { showCreateTaskDialog = true }
                    )
                }
            } else {
                items(
                    items = state.tasks,
                    key = { it.id }
                ) { task ->
                    TaskItemCard(
                        task = task,
                        onToggleCompleted = { completed ->
                            viewModel.toggleTask(task.id, completed)
                        },
                        onDelete = {
                            viewModel.deleteTask(task)
                        }
                    )
                }
            }
        }
    }

    if (showCreateTaskDialog) {
        CreateTaskDialog(
            onDismiss = { showCreateTaskDialog = false },
            onConfirm = { title, priority, timeOfDay, notes ->
                viewModel.addTask(title, priority, timeOfDay, notes)
            }
        )
    }
}

@Composable
fun EmptyTimetableCard(
    filter: TaskFilter,
    onAddTask: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .testTag("empty_timetable_view")
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = when (filter) {
                    TaskFilter.COMPLETED -> Icons.Default.CheckCircle
                    else -> Icons.Default.Schedule
                },
                contentDescription = null,
                tint = GeoPurplePrimary.copy(alpha = 0.7f),
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = when (filter) {
                    TaskFilter.ALL -> "No tasks scheduled for today"
                    TaskFilter.PENDING -> "All caught up! No pending tasks."
                    TaskFilter.COMPLETED -> "No completed tasks yet today."
                },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Add tasks manually or activate recurring routines to build momentum.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
