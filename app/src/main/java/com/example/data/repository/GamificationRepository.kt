package com.example.data.repository

import com.example.data.db.BadgeDao
import com.example.data.db.DailyLogDao
import com.example.data.db.RoutineDao
import com.example.data.db.TaskDao
import com.example.data.db.UserGamificationDao
import com.example.data.model.BadgeEntity
import com.example.data.model.TaskPriority
import com.example.data.model.UserGamificationEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

class GamificationRepository(
    private val badgeDao: BadgeDao,
    private val gamificationDao: UserGamificationDao,
    private val taskDao: TaskDao,
    private val routineDao: RoutineDao,
    private val dailyLogDao: DailyLogDao
) {

    val allBadges: Flow<List<BadgeEntity>> = badgeDao.getAllBadges()
    val unlockedBadges: Flow<List<BadgeEntity>> = badgeDao.getUnlockedBadges()

    val userGamification: Flow<UserGamificationEntity> =
        gamificationDao.getGamificationFlow("local_user").map { entity ->
            entity ?: UserGamificationEntity()
        }

    suspend fun initializeBadges() {
        val initialBadges = listOf(
            BadgeEntity(
                id = "first_step",
                name = "First Step",
                description = "Complete your first task in Stedify",
                iconUrl = "https://img.icons8.com/fluency/96/ok.png",
                category = "Tasks",
                requiredCount = 1
            ),
            BadgeEntity(
                id = "decathlete",
                name = "Decathlete",
                description = "Complete 10 total tasks successfully",
                iconUrl = "https://img.icons8.com/fluency/96/checklist.png",
                category = "Tasks",
                requiredCount = 10
            ),
            BadgeEntity(
                id = "centurion",
                name = "Centurion",
                description = "Complete 100 total tasks",
                iconUrl = "https://img.icons8.com/fluency/96/trophy.png",
                category = "Tasks",
                requiredCount = 100
            ),
            BadgeEntity(
                id = "week_warrior",
                name = "Week Warrior",
                description = "Complete routines for 7 consecutive days",
                iconUrl = "https://img.icons8.com/fluency/96/fire-element.png",
                category = "Streaks",
                requiredCount = 7
            ),
            BadgeEntity(
                id = "streak_titan",
                name = "Streak Titan",
                description = "Maintain a 14-day unbroken consistency streak",
                iconUrl = "https://img.icons8.com/fluency/96/medal.png",
                category = "Streaks",
                requiredCount = 14
            ),
            BadgeEntity(
                id = "flawless_week",
                name = "Flawless Flow",
                description = "Achieve 100% consistency for a full week (7 consecutive days)",
                iconUrl = "https://img.icons8.com/fluency/96/diamond.png",
                category = "Consistency",
                requiredCount = 7
            ),
            BadgeEntity(
                id = "routine_builder",
                name = "Routine Builder",
                description = "Create and maintain 3 or more active recurring routines",
                iconUrl = "https://img.icons8.com/fluency/96/recurring-appointment.png",
                category = "Routines",
                requiredCount = 3
            ),
            BadgeEntity(
                id = "dawn_striver",
                name = "Dawn Striver",
                description = "Complete a high-priority morning task before 9:00 AM",
                iconUrl = "https://img.icons8.com/fluency/96/sun.png",
                category = "Focus",
                requiredCount = 1
            ),
            BadgeEntity(
                id = "high_priority_ace",
                name = "Priority Ace",
                description = "Complete 15 high-priority focus tasks",
                iconUrl = "https://img.icons8.com/fluency/96/high-priority.png",
                category = "Focus",
                requiredCount = 15
            ),
            BadgeEntity(
                id = "chrono_aligner",
                name = "Chrono Aligner",
                description = "Optimize your day using Gemini AI Chrono-Scheduler",
                iconUrl = "https://img.icons8.com/fluency/96/artificial-intelligence.png",
                category = "AI",
                requiredCount = 1
            )
        )
        badgeDao.insertInitialBadges(initialBadges)

        val current = gamificationDao.getGamification("local_user")
        if (current == null) {
            gamificationDao.insertOrUpdate(UserGamificationEntity(userId = "local_user"))
        }
    }

    /**
     * Evaluates all achievement criteria, updates badge states, awards XP and updates user level.
     */
    suspend fun evaluateGamificationState(): UserGamificationEntity {
        initializeBadges()

        val allTasks = taskDao.getAllTasksList()
        val totalCompleted = allTasks.count { it.isCompleted }
        val highPriorityCompleted = allTasks.count { it.isCompleted && it.priority == TaskPriority.HIGH }
        val activeRoutines = routineDao.getActiveRoutines().size

        // Calculate flawless consecutive days from daily logs
        val recentLogs = dailyLogDao.getRecentLogsList(30)
        var currentFlawlessStreak = 0
        var maxFlawlessStreak = 0
        for (log in recentLogs) {
            if (log.tasksTotal > 0 && log.consistencyScore >= 99.9f) {
                currentFlawlessStreak++
                if (currentFlawlessStreak > maxFlawlessStreak) {
                    maxFlawlessStreak = currentFlawlessStreak
                }
            } else {
                currentFlawlessStreak = 0
            }
        }

        // Calculate early bird completion (before 9 AM)
        val earlyCompletedCount = allTasks.count { task ->
            if (task.isCompleted && task.scheduledTime > 0) {
                val cal = Calendar.getInstance().apply { timeInMillis = task.scheduledTime }
                cal.get(Calendar.HOUR_OF_DAY) < 9
            } else false
        }

        // Streak count
        var activeStreak = 0
        var freezeUsed = false
        for (log in recentLogs) {
            if (log.tasksTotal == 0) {
                if (!freezeUsed) {
                    freezeUsed = true
                } else break
            } else if (log.consistencyScore >= 70f) {
                activeStreak++
                freezeUsed = false
            } else break
        }

        // Update each badge progress and unlock if met
        updateBadgeProgress("first_step", totalCompleted, 1)
        updateBadgeProgress("decathlete", totalCompleted, 10)
        updateBadgeProgress("centurion", totalCompleted, 100)
        updateBadgeProgress("week_warrior", activeStreak, 7)
        updateBadgeProgress("streak_titan", activeStreak, 14)
        updateBadgeProgress("flawless_week", maxFlawlessStreak, 7)
        updateBadgeProgress("routine_builder", activeRoutines, 3)
        updateBadgeProgress("dawn_striver", earlyCompletedCount, 1)
        updateBadgeProgress("high_priority_ace", highPriorityCompleted, 15)

        val currentProfile = gamificationDao.getGamification("local_user") ?: UserGamificationEntity()
        if (currentProfile.aiOptimizationsApplied > 0) {
            updateBadgeProgress("chrono_aligner", currentProfile.aiOptimizationsApplied, 1)
        }

        // Tally XP
        // Task = +25 XP, High Priority = +35 XP bonus, Streak Days = +100 XP per day, Unlocked Badge = +200 XP
        val unlockedCount = calculateUnlockedBadgesCount()
        val totalXp = (totalCompleted * 25) +
                (highPriorityCompleted * 35) +
                (activeStreak * 100) +
                (unlockedCount * 200) +
                (currentProfile.aiOptimizationsApplied * 150)

        val (level, title) = calculateLevelInfo(totalXp)

        val updatedEntity = currentProfile.copy(
            totalXp = totalXp,
            currentLevel = level,
            levelTitle = title,
            tasksCompletedCount = totalCompleted,
            highPriorityCompletedCount = highPriorityCompleted,
            longestStreak = maxOf(currentProfile.longestStreak, activeStreak),
            flawlessDaysCount = maxFlawlessStreak,
            lastUpdated = System.currentTimeMillis()
        )

        gamificationDao.insertOrUpdate(updatedEntity)
        return updatedEntity
    }

    suspend fun recordAiOptimizationApplied() {
        val current = gamificationDao.getGamification("local_user") ?: UserGamificationEntity()
        val updated = current.copy(aiOptimizationsApplied = current.aiOptimizationsApplied + 1)
        gamificationDao.insertOrUpdate(updated)
        evaluateGamificationState()
    }

    private suspend fun updateBadgeProgress(badgeId: String, currentVal: Int, requiredVal: Int) {
        val badge = badgeDao.getBadgeById(badgeId) ?: return
        val clampedProgress = currentVal.coerceAtMost(requiredVal)
        val shouldUnlock = currentVal >= requiredVal

        if (shouldUnlock && !badge.isUnlocked) {
            badgeDao.unlockBadge(badgeId, System.currentTimeMillis())
        } else if (!badge.isUnlocked && clampedProgress != badge.currentProgress) {
            badgeDao.updateProgress(badgeId, clampedProgress)
        }
    }

    private suspend fun calculateUnlockedBadgesCount(): Int {
        return try {
            badgeDao.getUnlockedCount()
        } catch (_: Exception) {
            0
        }
    }

    companion object {
        fun calculateLevelInfo(totalXp: Int): Pair<Int, String> {
            return when {
                totalXp < 200 -> Pair(1, "Novice Striver")
                totalXp < 500 -> Pair(2, "Daily Builder")
                totalXp < 1000 -> Pair(3, "Steady Practitioner")
                totalXp < 1800 -> Pair(4, "Rhythm Architect")
                totalXp < 3000 -> Pair(5, "Habit Master")
                totalXp < 4800 -> Pair(6, "Consistency Champion")
                totalXp < 7200 -> Pair(7, "Chrono Vanguard")
                totalXp < 10500 -> Pair(8, "Momentum Legend")
                totalXp < 15000 -> Pair(9, "Grand Aligner")
                else -> Pair(10, "Apex Chrono-Sage")
            }
        }

        fun getXpThresholdsForLevel(level: Int): Pair<Int, Int> {
            return when (level) {
                1 -> Pair(0, 200)
                2 -> Pair(200, 500)
                3 -> Pair(500, 1000)
                4 -> Pair(1000, 1800)
                5 -> Pair(1800, 3000)
                6 -> Pair(3000, 4800)
                7 -> Pair(4800, 7200)
                8 -> Pair(7200, 10500)
                9 -> Pair(10500, 15000)
                else -> Pair(15000, 25000)
            }
        }
    }
}
