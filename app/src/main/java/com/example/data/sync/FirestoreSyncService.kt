package com.example.data.sync

import android.content.Context
import com.example.data.db.BadgeDao
import com.example.data.db.DailyLogDao
import com.example.data.db.RoutineDao
import com.example.data.db.TaskDao
import com.example.data.db.UserGamificationDao
import com.example.data.model.BadgeEntity
import com.example.data.model.DailyLogEntity
import com.example.data.model.RoutineEntity
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.data.model.UserGamificationEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

data class SyncResult(
    val success: Boolean,
    val message: String,
    val tasksCount: Int = 0,
    val routinesCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

class FirestoreSyncService(
    private val context: Context? = null,
    private val taskDao: TaskDao,
    private val routineDao: RoutineDao,
    private val dailyLogDao: DailyLogDao,
    private val badgeDao: BadgeDao,
    private val userGamificationDao: UserGamificationDao
) {
    private val firestore: FirebaseFirestore? by lazy {
        try {
            if (context != null && FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun backupToCloud(userId: String): Result<SyncResult> {
        val currentFirestore = firestore ?: return Result.failure(Exception("Cloud Firestore is not initialized. Please check network connection."))
        return try {
            val userDocRef = currentFirestore.collection("users").document(userId)


            // 1. Tasks
            val tasks = taskDao.getAllTasksList()
            val tasksColl = userDocRef.collection("tasks")
            for (task in tasks) {
                val taskMap = hashMapOf(
                    "id" to task.id,
                    "title" to task.title,
                    "priority" to task.priority.name,
                    "isCompleted" to task.isCompleted,
                    "scheduledTime" to task.scheduledTime,
                    "notes" to task.notes,
                    "routineId" to task.routineId,
                    "dateCreated" to task.dateCreated,
                    "dateString" to task.dateString
                )
                tasksColl.document(task.id.toString()).set(taskMap, SetOptions.merge()).await()
            }

            // 2. Routines
            val routines = routineDao.getAllRoutinesList()
            val routinesColl = userDocRef.collection("routines")
            for (routine in routines) {
                val routineMap = hashMapOf(
                    "id" to routine.id,
                    "title" to routine.title,
                    "targetDays" to routine.targetDays,
                    "targetTimeOfDay" to routine.targetTimeOfDay,
                    "defaultPriority" to routine.defaultPriority.name,
                    "notes" to routine.notes,
                    "isActive" to routine.isActive
                )
                routinesColl.document(routine.id.toString()).set(routineMap, SetOptions.merge()).await()
            }

            // 3. Daily Logs
            val logs = dailyLogDao.getAllDailyLogsList()
            val logsColl = userDocRef.collection("daily_logs")
            for (log in logs) {
                val logMap = hashMapOf(
                    "dateString" to log.dateString,
                    "tasksCompleted" to log.tasksCompleted,
                    "tasksTotal" to log.tasksTotal,
                    "consistencyScore" to log.consistencyScore
                )
                logsColl.document(log.dateString).set(logMap, SetOptions.merge()).await()
            }

            // 4. Badges and Gamification Profile
            val gamification = userGamificationDao.getGamification("local_user")
            if (gamification != null) {
                val gamificationMap = hashMapOf(
                    "totalXp" to gamification.totalXp,
                    "currentLevel" to gamification.currentLevel,
                    "levelTitle" to gamification.levelTitle,
                    "tasksCompletedCount" to gamification.tasksCompletedCount,
                    "highPriorityCompletedCount" to gamification.highPriorityCompletedCount,
                    "longestStreak" to gamification.longestStreak,
                    "flawlessDaysCount" to gamification.flawlessDaysCount,
                    "aiOptimizationsApplied" to gamification.aiOptimizationsApplied,
                    "lastUpdated" to System.currentTimeMillis()
                )
                userDocRef.collection("profile").document("gamification")
                    .set(gamificationMap, SetOptions.merge()).await()
            }

            val badges = badgeDao.getAllBadgesList()
            val badgesColl = userDocRef.collection("badges")
            for (badge in badges) {
                val badgeMap = hashMapOf(
                    "id" to badge.id,
                    "name" to badge.name,
                    "description" to badge.description,
                    "iconUrl" to badge.iconUrl,
                    "category" to badge.category,
                    "requiredCount" to badge.requiredCount,
                    "currentProgress" to badge.currentProgress,
                    "isUnlocked" to badge.isUnlocked,
                    "unlockedAt" to badge.unlockedAt
                )
                badgesColl.document(badge.id).set(badgeMap, SetOptions.merge()).await()
            }

            // 5. Update Meta
            val metaMap = hashMapOf(
                "lastBackupTimestamp" to System.currentTimeMillis(),
                "totalTasks" to tasks.size,
                "totalRoutines" to routines.size,
                "backupStatus" to "SUCCESS"
            )
            userDocRef.set(metaMap, SetOptions.merge()).await()

            Result.success(
                SyncResult(
                    success = true,
                    message = "Successfully backed up ${tasks.size} tasks and ${routines.size} routines to cloud.",
                    tasksCount = tasks.size,
                    routinesCount = routines.size
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreFromCloud(userId: String): Result<SyncResult> {
        val currentFirestore = firestore ?: return Result.failure(Exception("Cloud Firestore is not initialized. Please check network connection."))
        return try {
            val userDocRef = currentFirestore.collection("users").document(userId)

            // 1. Tasks
            val taskDocs = userDocRef.collection("tasks").get().await()
            val restoredTasks = mutableListOf<TaskEntity>()
            for (doc in taskDocs.documents) {
                val id = (doc.get("id") as? Number)?.toLong() ?: doc.id.toLongOrNull() ?: 0L
                val title = doc.getString("title") ?: "Untitled Task"
                val priorityStr = doc.getString("priority") ?: "MEDIUM"
                val priority = try { TaskPriority.valueOf(priorityStr) } catch (_: Exception) { TaskPriority.MEDIUM }
                val isCompleted = doc.getBoolean("isCompleted") ?: false
                val scheduledTime = (doc.get("scheduledTime") as? Number)?.toLong() ?: 0L
                val notes = doc.getString("notes") ?: ""
                val routineId = (doc.get("routineId") as? Number)?.toLong()
                val dateCreated = (doc.get("dateCreated") as? Number)?.toLong() ?: System.currentTimeMillis()
                val dateString = doc.getString("dateString") ?: ""

                restoredTasks.add(
                    TaskEntity(
                        id = id,
                        title = title,
                        priority = priority,
                        isCompleted = isCompleted,
                        scheduledTime = scheduledTime,
                        notes = notes,
                        routineId = routineId,
                        dateCreated = dateCreated,
                        dateString = dateString
                    )
                )
            }

            for (task in restoredTasks) {
                taskDao.insertTask(task)
            }

            // 2. Routines
            val routineDocs = userDocRef.collection("routines").get().await()
            val restoredRoutines = mutableListOf<RoutineEntity>()
            for (doc in routineDocs.documents) {
                val id = (doc.get("id") as? Number)?.toLong() ?: doc.id.toLongOrNull() ?: 0L
                val title = doc.getString("title") ?: "Routine"
                @Suppress("UNCHECKED_CAST")
                val daysList = (doc.get("targetDays") as? List<Number>)?.map { it.toInt() } ?: listOf(1, 2, 3, 4, 5)
                val targetTimeOfDay = doc.getString("targetTimeOfDay") ?: "08:00"
                val priorityStr = doc.getString("defaultPriority") ?: "MEDIUM"
                val priority = try { TaskPriority.valueOf(priorityStr) } catch (_: Exception) { TaskPriority.MEDIUM }
                val notes = doc.getString("notes") ?: ""
                val isActive = doc.getBoolean("isActive") ?: true

                restoredRoutines.add(
                    RoutineEntity(
                        id = id,
                        title = title,
                        targetDays = daysList,
                        targetTimeOfDay = targetTimeOfDay,
                        defaultPriority = priority,
                        notes = notes,
                        isActive = isActive
                    )
                )
            }

            for (routine in restoredRoutines) {
                routineDao.insertRoutine(routine)
            }

            // 3. Daily Logs
            val logDocs = userDocRef.collection("daily_logs").get().await()
            for (doc in logDocs.documents) {
                val dateString = doc.getString("dateString") ?: doc.id
                val completed = (doc.get("tasksCompleted") as? Number)?.toInt() ?: 0
                val total = (doc.get("tasksTotal") as? Number)?.toInt() ?: 0
                val consistency = (doc.get("consistencyScore") as? Number)?.toFloat() ?: 0f

                dailyLogDao.insertOrUpdate(
                    DailyLogEntity(
                        dateString = dateString,
                        tasksCompleted = completed,
                        tasksTotal = total,
                        consistencyScore = consistency
                    )
                )
            }

            // 4. Badges
            val badgeDocs = userDocRef.collection("badges").get().await()
            for (doc in badgeDocs.documents) {
                val id = doc.getString("id") ?: doc.id
                val name = doc.getString("name") ?: ""
                val desc = doc.getString("description") ?: ""
                val iconUrl = doc.getString("iconUrl") ?: ""
                val cat = doc.getString("category") ?: "General"
                val required = (doc.get("requiredCount") as? Number)?.toInt() ?: 1
                val progress = (doc.get("currentProgress") as? Number)?.toInt() ?: 0
                val unlocked = doc.getBoolean("isUnlocked") ?: false
                val unlockedAt = (doc.get("unlockedAt") as? Number)?.toLong()

                badgeDao.insertOrUpdate(
                    BadgeEntity(
                        id = id,
                        name = name,
                        description = desc,
                        iconUrl = iconUrl,
                        category = cat,
                        requiredCount = required,
                        currentProgress = progress,
                        isUnlocked = unlocked,
                        unlockedAt = unlockedAt
                    )
                )
            }

            // 5. Gamification profile
            val gamificationDoc = userDocRef.collection("profile").document("gamification").get().await()
            if (gamificationDoc.exists()) {
                val totalXp = (gamificationDoc.get("totalXp") as? Number)?.toInt() ?: 0
                val level = (gamificationDoc.get("currentLevel") as? Number)?.toInt() ?: 1
                val title = gamificationDoc.getString("levelTitle") ?: "Novice Striver"
                val tasksCount = (gamificationDoc.get("tasksCompletedCount") as? Number)?.toInt() ?: 0
                val highPriorityCount = (gamificationDoc.get("highPriorityCompletedCount") as? Number)?.toInt() ?: 0
                val longestStreak = (gamificationDoc.get("longestStreak") as? Number)?.toInt() ?: 0
                val flawlessDays = (gamificationDoc.get("flawlessDaysCount") as? Number)?.toInt() ?: 0
                val aiCount = (gamificationDoc.get("aiOptimizationsApplied") as? Number)?.toInt() ?: 0

                userGamificationDao.insertOrUpdate(
                    UserGamificationEntity(
                        userId = "local_user",
                        totalXp = totalXp,
                        currentLevel = level,
                        levelTitle = title,
                        tasksCompletedCount = tasksCount,
                        highPriorityCompletedCount = highPriorityCount,
                        longestStreak = longestStreak,
                        flawlessDaysCount = flawlessDays,
                        aiOptimizationsApplied = aiCount,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            }

            Result.success(
                SyncResult(
                    success = true,
                    message = "Successfully synchronized ${restoredTasks.size} tasks and ${restoredRoutines.size} routines from cloud.",
                    tasksCount = restoredTasks.size,
                    routinesCount = restoredRoutines.size
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
