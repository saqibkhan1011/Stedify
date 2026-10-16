package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.BadgeEntity
import com.example.data.model.DailyLogEntity
import com.example.data.model.RoutineEntity
import com.example.data.model.TaskEntity
import com.example.data.model.UserGamificationEntity

@Database(
    entities = [
        TaskEntity::class,
        RoutineEntity::class,
        DailyLogEntity::class,
        BadgeEntity::class,
        UserGamificationEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun routineDao(): RoutineDao
    abstract fun dailyLogDao(): DailyLogDao
    abstract fun badgeDao(): BadgeDao
    abstract fun userGamificationDao(): UserGamificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "stedify_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
