package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BadgeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BadgeDao {
    @Query("SELECT * FROM badges ORDER BY isUnlocked DESC, id ASC")
    fun getAllBadges(): Flow<List<BadgeEntity>>

    @Query("SELECT * FROM badges")
    suspend fun getAllBadgesList(): List<BadgeEntity>

    @Query("SELECT * FROM badges WHERE isUnlocked = 1 ORDER BY unlockedAt DESC")
    fun getUnlockedBadges(): Flow<List<BadgeEntity>>

    @Query("SELECT COUNT(*) FROM badges WHERE isUnlocked = 1")
    suspend fun getUnlockedCount(): Int

    @Query("SELECT * FROM badges WHERE id = :id LIMIT 1")
    suspend fun getBadgeById(id: String): BadgeEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInitialBadges(badges: List<BadgeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(badge: BadgeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(badges: List<BadgeEntity>)

    @Update
    suspend fun updateBadge(badge: BadgeEntity)

    @Query("UPDATE badges SET isUnlocked = 1, unlockedAt = :unlockedAt, currentProgress = requiredCount WHERE id = :badgeId")
    suspend fun unlockBadge(badgeId: String, unlockedAt: Long)

    @Query("UPDATE badges SET currentProgress = :progress WHERE id = :badgeId")
    suspend fun updateProgress(badgeId: String, progress: Int)
}
