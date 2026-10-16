package com.example.data.repository

import com.example.data.db.DailyLogDao
import com.example.data.db.RoutineDao
import com.example.data.db.TaskDao
import com.example.data.model.DailyLogEntity
import com.example.data.model.RoutineEntity
import com.example.data.model.TaskEntity
import com.example.notification.TaskAlarmScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StedifyRepository(
    private val taskDao: TaskDao,
    private val routineDao: RoutineDao,
    private val dailyLogDao: DailyLogDao,
    private val alarmScheduler: TaskAlarmScheduler,
    private val gamificationRepository: GamificationRepository? = null
) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun getTodayDateString(): String = dateFormat.format(Date())

    fun formatDate(timestamp: Long): String = dateFormat.format(Date(timestamp))

    fun formatTime(timestamp: Long): String = timeFormat.format(Date(timestamp))

    fun getTasksForToday(): Flow<List<TaskEntity>> {
        val today = getTodayDateString()
        return taskDao.getTasksForDate(today)
    }

    fun getTasksForDate(dateString: String): Flow<List<TaskEntity>> {
        return taskDao.getTasksForDate(dateString)
    }

    fun getAllTasks(): Flow<List<TaskEntity>> = taskDao.getAllTasks()

    suspend fun insertTask(task: TaskEntity): Long {
        val dateString = if (task.dateString.isBlank()) {
            if (task.scheduledTime > 0) dateFormat.format(Date(task.scheduledTime)) else getTodayDateString()
        } else {
            task.dateString
        }
        val taskWithDate = task.copy(dateString = dateString)
        val id = taskDao.insertTask(taskWithDate)
        val savedTask = taskWithDate.copy(id = id)

        if (!savedTask.isCompleted && savedTask.scheduledTime > System.currentTimeMillis()) {
            alarmScheduler.scheduleTask(savedTask)
        }

        recalculateDailyLog(dateString)
        return id
    }

    suspend fun updateTask(task: TaskEntity) {
        taskDao.updateTask(task)
        if (task.isCompleted) {
            alarmScheduler.cancelTask(task.id)
        } else if (task.scheduledTime > System.currentTimeMillis()) {
            alarmScheduler.scheduleTask(task)
        } else {
            alarmScheduler.cancelTask(task.id)
        }
        recalculateDailyLog(task.dateString)
    }

    suspend fun toggleTaskCompletion(taskId: Long, isCompleted: Boolean) {
        val task = taskDao.getTaskById(taskId) ?: return
        val updated = task.copy(isCompleted = isCompleted)
        taskDao.updateTask(updated)
        if (isCompleted) {
            alarmScheduler.cancelTask(taskId)
        } else if (updated.scheduledTime > System.currentTimeMillis()) {
            alarmScheduler.scheduleTask(updated)
        }
        recalculateDailyLog(updated.dateString)
    }

    suspend fun deleteTask(task: TaskEntity) {
        alarmScheduler.cancelTask(task.id)
        taskDao.deleteTask(task)
        recalculateDailyLog(task.dateString)
    }

    suspend fun deleteTaskById(id: Long) {
        val task = taskDao.getTaskById(id)
        alarmScheduler.cancelTask(id)
        taskDao.deleteTaskById(id)
        if (task != null) {
            recalculateDailyLog(task.dateString)
        }
    }

    suspend fun applyOptimizedSchedule(updates: List<Pair<Long, Long>>) {
        for ((taskId, newScheduledTime) in updates) {
            val task = taskDao.getTaskById(taskId)
            if (task != null) {
                val updated = task.copy(
                    scheduledTime = newScheduledTime,
                    dateString = dateFormat.format(Date(newScheduledTime))
                )
                taskDao.updateTask(updated)
                if (!updated.isCompleted && updated.scheduledTime > System.currentTimeMillis()) {
                    alarmScheduler.scheduleTask(updated)
                }
            }
        }
        recalculateDailyLog(getTodayDateString())
        gamificationRepository?.recordAiOptimizationApplied()
    }

    // --- Routine Operations ---

    fun getAllRoutines(): Flow<List<RoutineEntity>> = routineDao.getAllRoutines()

    suspend fun insertRoutine(routine: RoutineEntity): Long {
        val id = routineDao.insertRoutine(routine)
        val saved = routine.copy(id = id)
        // Check if today matches routine and spawn task if active
        if (saved.isActive) {
            checkAndSpawnRoutineForDate(saved, getTodayDateString())
        }
        return id
    }

    suspend fun updateRoutine(routine: RoutineEntity) {
        routineDao.updateRoutine(routine)
        if (routine.isActive) {
            checkAndSpawnRoutineForDate(routine, getTodayDateString())
        }
    }

    suspend fun toggleRoutineActive(routineId: Long, isActive: Boolean) {
        val routine = routineDao.getRoutineById(routineId) ?: return
        val updated = routine.copy(isActive = isActive)
        routineDao.updateRoutine(updated)
        if (isActive) {
            checkAndSpawnRoutineForDate(updated, getTodayDateString())
        }
    }

    suspend fun deleteRoutine(routine: RoutineEntity) {
        routineDao.deleteRoutine(routine)
    }

    suspend fun deleteRoutineById(id: Long) {
        routineDao.deleteRoutineById(id)
    }

    // --- Daily Routine Spawner ---

    suspend fun spawnDailyRoutinesForToday() {
        val today = getTodayDateString()
        val activeRoutines = routineDao.getActiveRoutines()
        for (routine in activeRoutines) {
            checkAndSpawnRoutineForDate(routine, today)
        }
        recalculateDailyLog(today)
    }

    private suspend fun checkAndSpawnRoutineForDate(routine: RoutineEntity, dateString: String) {
        val calendar = Calendar.getInstance()
        val dayOfWeekCalendar = calendar.get(Calendar.DAY_OF_WEEK)
        // Convert Calendar day (Sunday=1, Monday=2..Saturday=7) to 1=Monday..7=Sunday
        val dayOfWeekIso = when (dayOfWeekCalendar) {
            Calendar.SUNDAY -> 7
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            else -> 1
        }

        if (routine.targetDays.contains(dayOfWeekIso)) {
            val existingCount = taskDao.countTasksForRoutineOnDate(routine.id, dateString)
            if (existingCount == 0) {
                val scheduledTimestamp = calculateTimestampForTime(routine.targetTimeOfDay)
                val task = TaskEntity(
                    title = routine.title,
                    priority = routine.defaultPriority,
                    isCompleted = false,
                    scheduledTime = scheduledTimestamp,
                    notes = routine.notes,
                    routineId = routine.id,
                    dateCreated = System.currentTimeMillis(),
                    dateString = dateString
                )
                val taskId = taskDao.insertTask(task)
                val savedTask = task.copy(id = taskId)
                if (savedTask.scheduledTime > System.currentTimeMillis()) {
                    alarmScheduler.scheduleTask(savedTask)
                }
            }
        }
    }

    private fun calculateTimestampForTime(timeOfDay: String): Long {
        val calendar = Calendar.getInstance()
        try {
            val parts = timeOfDay.split(":")
            if (parts.size >= 2) {
                val hour = parts[0].toIntOrNull() ?: 8
                val minute = parts[1].toIntOrNull() ?: 0
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
        } catch (_: Exception) {
            calendar.set(Calendar.HOUR_OF_DAY, 8)
            calendar.set(Calendar.MINUTE, 0)
        }
        return calendar.timeInMillis
    }

    // --- Daily Log & Consistency Analytics ---

    private suspend fun recalculateDailyLog(dateString: String) {
        val tasks = taskDao.getTasksForDateList(dateString)
        val total = tasks.size
        val completed = tasks.count { it.isCompleted }
        val score = if (total > 0) (completed.toFloat() / total.toFloat()) * 100f else 0.0f

        val log = DailyLogEntity(
            dateString = dateString,
            tasksCompleted = completed,
            tasksTotal = total,
            consistencyScore = score
        )
        dailyLogDao.insertOrUpdate(log)
        gamificationRepository?.evaluateGamificationState()
    }

    fun getAllDailyLogs(): Flow<List<DailyLogEntity>> = dailyLogDao.getAllDailyLogs()

    fun getDailyLogsInRange(startDate: String, endDate: String): Flow<List<DailyLogEntity>> =
        dailyLogDao.getDailyLogsInRange(startDate, endDate)

    fun getTodayDailyLog(): Flow<DailyLogEntity?> = dailyLogDao.getDailyLogFlow(getTodayDateString())

    /**
     * Algorithm 2: Rolling Consistency Score
     * Score = (Total Completed Tasks in last 14 days / Total Scheduled Tasks in last 14 days) * 100
     */
    suspend fun calculate14DayConsistencyScore(): Float {
        val calendar = Calendar.getInstance()
        val endDate = dateFormat.format(calendar.time)
        calendar.add(Calendar.DAY_OF_YEAR, -13) // 14 days including today
        val startDate = dateFormat.format(calendar.time)

        val tasks = taskDao.getTasksBetweenDatesList(startDate, endDate)
        val total = tasks.size
        if (total == 0) return 100.0f // Perfect if no tasks scheduled or fresh start
        val completed = tasks.count { it.isCompleted }
        return (completed.toFloat() / total.toFloat()) * 100.0f
    }

    fun observe14DayConsistencyScore(): Flow<Float> {
        val calendar = Calendar.getInstance()
        val endDate = dateFormat.format(calendar.time)
        val pastCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -13) }
        val startDate = dateFormat.format(pastCalendar.time)

        return taskDao.getTasksBetweenDates(startDate, endDate).map { tasks ->
            val total = tasks.size
            if (total == 0) 100.0f
            else {
                val completed = tasks.count { it.isCompleted }
                (completed.toFloat() / total.toFloat()) * 100.0f
            }
        }
    }

    /**
     * Algorithm 3: Streak Counter
     * Count consecutive days where Daily Completion Percentage >= 70%.
     * Include logic for a 1-day "Streak Freeze" if 0 tasks were scheduled on a day.
     */
    suspend fun calculateStreakCount(): Int {
        val recentLogs = dailyLogDao.getRecentLogsList(60) // Check up to 60 days
        val logMap = recentLogs.associateBy { it.dateString }

        val calendar = Calendar.getInstance()
        val todayStr = dateFormat.format(calendar.time)
        val todayLog = logMap[todayStr]

        var streak = 0
        var freezeUsed = false

        // Check if today is qualifying or in-progress
        val todayHasTasks = todayLog != null && todayLog.tasksTotal > 0
        val todayQualifies = todayHasTasks && todayLog!!.consistencyScore >= 70f

        if (todayQualifies) {
            streak++
        }

        // Now traverse backwards from yesterday
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        for (i in 0 until 60) {
            val dateStr = dateFormat.format(calendar.time)
            val log = logMap[dateStr]

            if (log == null || log.tasksTotal == 0) {
                // 0 tasks scheduled -> 1-day streak freeze
                if (!freezeUsed) {
                    freezeUsed = true
                    // Streak freeze maintains the streak without breaking
                } else {
                    // Consecutive 0-task day or second gap breaks the streak
                    break
                }
            } else if (log.consistencyScore >= 70.0f) {
                streak++
                freezeUsed = false // Reset freeze allowance on valid active day
            } else {
                // Less than 70% completion with tasks scheduled -> streak breaks
                break
            }
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }

        return streak
    }

    fun observeStreakCount(): Flow<Int> {
        return dailyLogDao.getAllDailyLogs().map { logs ->
            val logMap = logs.associateBy { it.dateString }
            val calendar = Calendar.getInstance()
            val todayStr = dateFormat.format(calendar.time)
            val todayLog = logMap[todayStr]

            var streak = 0
            var freezeUsed = false

            val todayHasTasks = todayLog != null && todayLog.tasksTotal > 0
            val todayQualifies = todayHasTasks && todayLog!!.consistencyScore >= 70f

            if (todayQualifies) {
                streak++
            }

            calendar.add(Calendar.DAY_OF_YEAR, -1)
            for (i in 0 until 60) {
                val dateStr = dateFormat.format(calendar.time)
                val log = logMap[dateStr]

                if (log == null || log.tasksTotal == 0) {
                    if (!freezeUsed) {
                        freezeUsed = true
                    } else {
                        break
                    }
                } else if (log.consistencyScore >= 70.0f) {
                    streak++
                    freezeUsed = false
                } else {
                    break
                }
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            }
            streak
        }
    }
}
