package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["routineId"]),
        Index(value = ["dateString"]),
        Index(value = ["scheduledTime"])
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val isCompleted: Boolean = false,
    val scheduledTime: Long = 0L,
    val notes: String? = null,
    val routineId: Long? = null,
    val dateCreated: Long = System.currentTimeMillis(),
    val dateString: String = "" // "YYYY-MM-DD"
)
