package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.UserGamificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserGamificationDao {
    @Query("SELECT * FROM user_gamification WHERE userId = :userId LIMIT 1")
    fun getGamificationFlow(userId: String = "local_user"): Flow<UserGamificationEntity?>

    @Query("SELECT * FROM user_gamification WHERE userId = :userId LIMIT 1")
    suspend fun getGamification(userId: String = "local_user"): UserGamificationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: UserGamificationEntity)

    @Update
    suspend fun update(entity: UserGamificationEntity)
}
