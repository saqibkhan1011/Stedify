package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiSchedulerClient
import com.example.ai.OptimizedTimeBlock
import com.example.ai.ScheduleOptimizationResult
import com.example.data.model.TaskEntity
import com.example.data.repository.StedifyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AiOptimizerUiState {
    object Idle : AiOptimizerUiState
    object Loading : AiOptimizerUiState
    data class Success(
        val result: ScheduleOptimizationResult,
        val applied: Boolean = false
    ) : AiOptimizerUiState
    data class Error(val message: String) : AiOptimizerUiState
}

class AiOptimizerViewModel(
    private val repository: StedifyRepository,
    private val geminiClient: GeminiSchedulerClient = GeminiSchedulerClient()
) : ViewModel() {

    val todayTasks: StateFlow<List<TaskEntity>> = repository.getTasksForToday()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow<AiOptimizerUiState>(AiOptimizerUiState.Idle)
    val uiState: StateFlow<AiOptimizerUiState> = _uiState.asStateFlow()

    fun optimizeSchedule() {
        val tasks = todayTasks.value
        if (tasks.isEmpty()) {
            _uiState.value = AiOptimizerUiState.Error("No tasks available for today. Please add tasks before optimizing!")
            return
        }

        _uiState.value = AiOptimizerUiState.Loading

        viewModelScope.launch {
            val result = geminiClient.optimizeSchedule(tasks)
            result.onSuccess { optimizationResult ->
                _uiState.value = AiOptimizerUiState.Success(optimizationResult)
            }.onFailure { error ->
                _uiState.value = AiOptimizerUiState.Error(error.localizedMessage ?: "Failed to generate schedule optimization.")
            }
        }
    }

    fun applySchedule(blocks: List<OptimizedTimeBlock>) {
        val currentState = _uiState.value
        if (currentState is AiOptimizerUiState.Success) {
            viewModelScope.launch {
                val updates = blocks.map { it.taskId to it.suggestedTimestamp }
                repository.applyOptimizedSchedule(updates)
                _uiState.value = currentState.copy(applied = true)
            }
        }
    }

    companion object {
        fun provideFactory(
            repository: StedifyRepository,
            client: GeminiSchedulerClient = GeminiSchedulerClient()
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AiOptimizerViewModel(repository, client) as T
                }
            }
    }
}
