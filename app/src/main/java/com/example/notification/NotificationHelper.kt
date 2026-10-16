package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.TaskEntity
import com.example.receiver.TaskActionReceiver

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "task_reminders_channel"
        const val CHANNEL_NAME = "Task & Routine Reminders"
        const val CHANNEL_DESC = "High priority notifications for scheduled routines and tasks"

        const val ACTION_MARK_COMPLETE = "com.example.ACTION_MARK_COMPLETE"
        const val ACTION_SNOOZE = "com.example.ACTION_SNOOZE"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showTaskReminder(task: TaskEntity) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            task.id.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Mark Complete Action
        val completeIntent = Intent(context, TaskActionReceiver::class.java).apply {
            action = ACTION_MARK_COMPLETE
            putExtra(EXTRA_TASK_ID, task.id)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            (task.id * 10 + 1).toInt(),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze 15m Action
        val snoozeIntent = Intent(context, TaskActionReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_TASK_TITLE, task.title)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (task.id * 10 + 2).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val priorityText = when (task.priority) {
            com.example.data.model.TaskPriority.HIGH -> "⚡ High Priority"
            com.example.data.model.TaskPriority.MEDIUM -> "🔹 Medium Priority"
            com.example.data.model.TaskPriority.LOW -> "☕ Low Priority"
        }

        val contentText = if (!task.notes.isNullOrBlank()) {
            "$priorityText • ${task.notes}"
        } else {
            "$priorityText • It's time for your scheduled task!"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(task.title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.checkbox_on_background,
                "Mark Complete",
                completePendingIntent
            )
            .addAction(
                android.R.drawable.ic_lock_idle_alarm,
                "Snooze 15m",
                snoozePendingIntent
            )

        notificationManager.notify(task.id.toInt(), builder.build())
    }

    fun cancelNotification(taskId: Long) {
        notificationManager.cancel(taskId.toInt())
    }
}
