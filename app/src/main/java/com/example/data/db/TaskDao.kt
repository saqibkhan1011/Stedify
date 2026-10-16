package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE dateString = :dateString ORDER BY scheduledTime ASC, priority ASC, id ASC")
    fun getTasksForDate(dateString: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dateString = :dateString ORDER BY scheduledTime ASC, priority ASC, id ASC")
    suspend fun getTasksForDateList(dateString: String): List<TaskEntity>

    @Query("SELECT * FROM tasks ORDER BY scheduledTime ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksList(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE dateString BETWEEN :startDate AND :endDate ORDER BY dateString ASC, scheduledTime ASC")
    fun getTasksBetweenDates(startDate: String, endDate: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dateString BETWEEN :startDate AND :endDate")
    suspend fun getTasksBetweenDatesList(startDate: String, endDate: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Query("SELECT COUNT(*) FROM tasks WHERE routineId = :routineId AND dateString = :dateString")
    suspend fun countTasksForRoutineOnDate(routineId: Long, dateString: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>): List<Long>

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND scheduledTime > :currentTime ORDER BY scheduledTime ASC")
    suspend fun getUpcomingUncompletedTasks(currentTime: Long): List<TaskEntity>
}
