package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.data.repository.StedifyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class TaskFilter {
    ALL,
    PENDING,
    COMPLETED
}

data class DashboardUiState(
    val tasks: List<TaskEntity> = emptyList(),
    val currentStreak: Int = 0,
    val rollingConsistencyScore: Float = 100f,
    val selectedFilter: TaskFilter = TaskFilter.ALL,
    val todayFormattedDate: String = "",
    val completedCount: Int = 0,
    val totalCount: Int = 0
)

class DashboardViewModel(
    private val repository: StedifyRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(TaskFilter.ALL)
    val selectedFilter: StateFlow<TaskFilter> = _selectedFilter.asStateFlow()

    private val rawTodayTasks = repository.getTasksForToday()
    private val streakFlow = repository.observeStreakCount()
    private val consistencyFlow = repository.observe14DayConsistencyScore()

    val uiState: StateFlow<DashboardUiState> = combine(
        rawTodayTasks,
        streakFlow,
        consistencyFlow,
        _selectedFilter
    ) { tasks, streak, consistency, filter ->
        val filtered = when (filter) {
            TaskFilter.ALL -> tasks
            TaskFilter.PENDING -> tasks.filter { !it.isCompleted }
            TaskFilter.COMPLETED -> tasks.filter { it.isCompleted }
        }

        val completed = tasks.count { it.isCompleted }
        val total = tasks.size

        val dateDisplay = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())

        DashboardUiState(
            tasks = filtered,
            currentStreak = streak,
            rollingConsistencyScore = consistency,
            selectedFilter = filter,
            todayFormattedDate = dateDisplay,
            completedCount = completed,
            totalCount = total
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    init {
        viewModelScope.launch {
            repository.spawnDailyRoutinesForToday()
        }
    }

    fun setFilter(filter: TaskFilter) {
        _selectedFilter.value = filter
    }

    fun toggleTask(taskId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.toggleTaskCompletion(taskId, isCompleted)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun addTask(
        title: String,
        priority: TaskPriority,
        timeOfDay: String, // "HH:mm"
        notes: String?
    ) {
        viewModelScope.launch {
            val timestamp = calculateTimestampForTime(timeOfDay)
            val task = TaskEntity(
                title = title.trim(),
                priority = priority,
                isCompleted = false,
                scheduledTime = timestamp,
                notes = notes?.trim()?.ifBlank { null },
                dateString = repository.getTodayDateString(),
                dateCreated = System.currentTimeMillis()
            )
            repository.insertTask(task)
        }
    }

    private fun calculateTimestampForTime(timeOfDay: String): Long {
        val calendar = Calendar.getInstance()
        try {
            val parts = timeOfDay.split(":")
            if (parts.size >= 2) {
                val hour = parts[0].toIntOrNull() ?: 9
                val min = parts[1].toIntOrNull() ?: 0
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, min)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
        } catch (_: Exception) {
            calendar.set(Calendar.HOUR_OF_DAY, 9)
            calendar.set(Calendar.MINUTE, 0)
        }
        return calendar.timeInMillis
    }

    companion object {
        fun provideFactory(repository: StedifyRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DashboardViewModel(repository) as T
                }
            }
    }
}
