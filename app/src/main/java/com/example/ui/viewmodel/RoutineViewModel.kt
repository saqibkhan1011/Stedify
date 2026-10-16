package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.RoutineEntity
import com.example.data.model.TaskPriority
import com.example.data.repository.StedifyRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RoutineUiState(
    val routines: List<RoutineEntity> = emptyList(),
    val activeCount: Int = 0
)

class RoutineViewModel(
    private val repository: StedifyRepository
) : ViewModel() {

    val uiState: StateFlow<List<RoutineEntity>> = repository.getAllRoutines()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createRoutine(
        title: String,
        targetDays: List<Int>,
        targetTimeOfDay: String,
        priority: TaskPriority = TaskPriority.MEDIUM,
        notes: String? = null
    ) {
        viewModelScope.launch {
            val routine = RoutineEntity(
                title = title.trim(),
                targetDays = if (targetDays.isEmpty()) listOf(1, 2, 3, 4, 5, 6, 7) else targetDays,
                targetTimeOfDay = targetTimeOfDay,
                isActive = true,
                defaultPriority = priority,
                notes = notes?.trim()?.ifBlank { null }
            )
            repository.insertRoutine(routine)
        }
    }

    fun toggleRoutineActive(routineId: Long, isActive: Boolean) {
        viewModelScope.launch {
            repository.toggleRoutineActive(routineId, isActive)
        }
    }

    fun updateRoutine(routine: RoutineEntity) {
        viewModelScope.launch {
            repository.updateRoutine(routine)
        }
    }

    fun deleteRoutine(routine: RoutineEntity) {
        viewModelScope.launch {
            repository.deleteRoutine(routine)
        }
    }

    companion object {
        fun provideFactory(repository: StedifyRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return RoutineViewModel(repository) as T
                }
            }
    }
}
