package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.db.AppDatabase
import com.example.data.model.DailyLogEntity
import com.example.notification.NotificationHelper
import com.example.notification.TaskAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return

        val notificationHelper = NotificationHelper(context)
        val database = AppDatabase.getDatabase(context)
        val scheduler = TaskAlarmScheduler(context)

        when (intent.action) {
            NotificationHelper.ACTION_MARK_COMPLETE -> {
                notificationHelper.cancelNotification(taskId)
                scheduler.cancelTask(taskId)
                CoroutineScope(Dispatchers.IO).launch {
                    val task = database.taskDao().getTaskById(taskId)
                    if (task != null) {
                        val updated = task.copy(isCompleted = true)
                        database.taskDao().updateTask(updated)

                        // Update DailyLog
                        val tasksForDate = database.taskDao().getTasksForDateList(task.dateString)
                        val total = tasksForDate.size
                        val completed = tasksForDate.count { it.isCompleted }
                        val score = if (total > 0) (completed.toFloat() / total.toFloat()) * 100f else 100f
                        database.dailyLogDao().insertOrUpdate(
                            DailyLogEntity(
                                dateString = task.dateString,
                                tasksCompleted = completed,
                                tasksTotal = total,
                                consistencyScore = score
                            )
                        )
                    }
                }
            }

            NotificationHelper.ACTION_SNOOZE -> {
                val title = intent.getStringExtra(NotificationHelper.EXTRA_TASK_TITLE) ?: "Task Reminder"
                notificationHelper.cancelNotification(taskId)
                scheduler.scheduleSnooze(taskId, title, delayMillis = 15 * 60 * 1000L)
            }
        }
    }
}
