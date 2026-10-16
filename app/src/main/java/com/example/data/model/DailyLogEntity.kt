package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_logs")
data class DailyLogEntity(
    @PrimaryKey
    val dateString: String, // "YYYY-MM-DD"
    val tasksCompleted: Int = 0,
    val tasksTotal: Int = 0,
    val consistencyScore: Float = 0.0f // percentage 0.0 to 100.0
)
