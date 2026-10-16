package com.example.data.model

enum class TaskPriority {
    HIGH,
    MEDIUM,
    LOW;

    companion object {
        fun fromString(value: String?): TaskPriority {
            return when (value?.uppercase()) {
                "HIGH" -> HIGH
                "LOW" -> LOW
                else -> MEDIUM
            }
        }
    }
}
