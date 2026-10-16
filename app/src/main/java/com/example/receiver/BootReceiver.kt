package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.db.AppDatabase
import com.example.data.repository.StedifyRepository
import com.example.notification.TaskAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val database = AppDatabase.getDatabase(context)
            val scheduler = TaskAlarmScheduler(context)
            val repository = StedifyRepository(
                taskDao = database.taskDao(),
                routineDao = database.routineDao(),
                dailyLogDao = database.dailyLogDao(),
                alarmScheduler = scheduler
            )

            CoroutineScope(Dispatchers.IO).launch {
                // Spawn daily routines if morning/new day
                repository.spawnDailyRoutinesForToday()

                // Reschedule upcoming active uncompleted tasks
                val upcoming = database.taskDao().getUpcomingUncompletedTasks(System.currentTimeMillis())
                upcoming.forEach { task ->
                    scheduler.scheduleTask(task)
                }
            }
        }
    }
}
