package com.example

import android.app.Application
import android.util.Log
import com.example.data.auth.AuthService
import com.example.data.db.AppDatabase
import com.example.data.repository.GamificationRepository
import com.example.data.repository.StedifyRepository
import com.example.data.sync.FirestoreSyncService
import com.example.notification.NotificationHelper
import com.example.notification.TaskAlarmScheduler
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class StedifyApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { AppDatabase.getDatabase(this) }
    val alarmScheduler by lazy { TaskAlarmScheduler(this) }
    val notificationHelper by lazy { NotificationHelper(this) }

    val gamificationRepository by lazy {
        GamificationRepository(
            badgeDao = database.badgeDao(),
            gamificationDao = database.userGamificationDao(),
            taskDao = database.taskDao(),
            routineDao = database.routineDao(),
            dailyLogDao = database.dailyLogDao()
        )
    }

    val authService by lazy { AuthService(this) }

    val syncService by lazy {
        FirestoreSyncService(
            context = this,
            taskDao = database.taskDao(),
            routineDao = database.routineDao(),
            dailyLogDao = database.dailyLogDao(),
            badgeDao = database.badgeDao(),
            userGamificationDao = database.userGamificationDao()
        )
    }

    val repository by lazy {
        StedifyRepository(
            taskDao = database.taskDao(),
            routineDao = database.routineDao(),
            dailyLogDao = database.dailyLogDao(),
            alarmScheduler = alarmScheduler,
            gamificationRepository = gamificationRepository
        )
    }

    override fun onCreate() {
        super.onCreate()

        // Ensure FirebaseApp is initialized
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) {
            Log.w("StedifyApp", "FirebaseApp initialization: ${e.message}")
        }

        notificationHelper.createNotificationChannel()

        // Initialize gamification badges & spawn daily routines for today
        applicationScope.launch {
            gamificationRepository.initializeBadges()
            repository.spawnDailyRoutinesForToday()
            gamificationRepository.evaluateGamificationState()
        }
    }
}

