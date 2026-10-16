package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_gamification")
data class UserGamificationEntity(
    @PrimaryKey val userId: String = "local_user",
    val totalXp: Int = 0,
    val currentLevel: Int = 1,
    val levelTitle: String = "Novice Striver",
    val tasksCompletedCount: Int = 0,
    val highPriorityCompletedCount: Int = 0,
    val longestStreak: Int = 0,
    val flawlessDaysCount: Int = 0,
    val aiOptimizationsApplied: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)
