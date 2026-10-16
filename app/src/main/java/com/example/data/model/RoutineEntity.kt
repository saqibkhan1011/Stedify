package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val targetDays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7), // 1 = Monday, 7 = Sunday
    val targetTimeOfDay: String = "08:00", // "HH:mm" 24-hour format
    val isActive: Boolean = true,
    val defaultPriority: TaskPriority = TaskPriority.MEDIUM,
    val notes: String? = null
)
