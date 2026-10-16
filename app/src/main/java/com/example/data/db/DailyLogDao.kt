package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.DailyLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyLogDao {
    @Query("SELECT * FROM daily_logs ORDER BY dateString DESC")
    fun getAllDailyLogs(): Flow<List<DailyLogEntity>>

    @Query("SELECT * FROM daily_logs")
    suspend fun getAllDailyLogsList(): List<DailyLogEntity>

    @Query("SELECT * FROM daily_logs WHERE dateString = :dateString")
    suspend fun getDailyLog(dateString: String): DailyLogEntity?

    @Query("SELECT * FROM daily_logs WHERE dateString = :dateString")
    fun getDailyLogFlow(dateString: String): Flow<DailyLogEntity?>

    @Query("SELECT * FROM daily_logs WHERE dateString BETWEEN :startDate AND :endDate ORDER BY dateString ASC")
    fun getDailyLogsInRange(startDate: String, endDate: String): Flow<List<DailyLogEntity>>

    @Query("SELECT * FROM daily_logs WHERE dateString BETWEEN :startDate AND :endDate ORDER BY dateString ASC")
    suspend fun getDailyLogsInRangeList(startDate: String, endDate: String): List<DailyLogEntity>

    @Query("SELECT * FROM daily_logs ORDER BY dateString DESC LIMIT :limit")
    fun getRecentLogs(limit: Int): Flow<List<DailyLogEntity>>

    @Query("SELECT * FROM daily_logs ORDER BY dateString DESC LIMIT :limit")
    suspend fun getRecentLogsList(limit: Int): List<DailyLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(log: DailyLogEntity)
}
