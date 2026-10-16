package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.model.TaskPriority

class Converters {
    @TypeConverter
    fun fromPriority(priority: TaskPriority?): String {
        return priority?.name ?: TaskPriority.MEDIUM.name
    }

    @TypeConverter
    fun toPriority(value: String?): TaskPriority {
        return TaskPriority.fromString(value)
    }

    @TypeConverter
    fun fromIntList(list: List<Int>?): String {
        return list?.joinToString(",") ?: ""
    }

    @TypeConverter
    fun toIntList(data: String?): List<Int> {
        if (data.isNullOrEmpty()) return emptyList()
        return data.split(",").mapNotNull { it.trim().toIntOrNull() }
    }
}
