package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.db.AppDatabase
import com.example.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return

        val notificationHelper = NotificationHelper(context)
        val database = AppDatabase.getDatabase(context)

        CoroutineScope(Dispatchers.IO).launch {
            val task = database.taskDao().getTaskById(taskId)
            if (task != null && !task.isCompleted) {
                notificationHelper.showTaskReminder(task)
            }
        }
    }
}
