package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "badges")
data class BadgeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val iconUrl: String,
    val category: String = "general",
    val requiredCount: Int = 1,
    val currentProgress: Int = 0,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null
)
