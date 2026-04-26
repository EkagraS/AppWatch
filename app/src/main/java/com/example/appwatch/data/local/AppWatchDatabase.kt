package com.example.appwatch.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.appwatch.data.local.dao.AppDataUsageDao
import com.example.appwatch.data.local.dao.AppInfoDao
import com.example.appwatch.data.local.dao.AppNotificationDao
import com.example.appwatch.data.local.dao.NeedsAttentionDao
import com.example.appwatch.data.local.dao.PermissionAccessDao
import com.example.appwatch.data.local.dao.RecentEventDao
import com.example.appwatch.data.local.dao.UsageDao
import com.example.appwatch.data.local.entity.AppDataUsageEntity
import com.example.appwatch.data.local.entity.AppInfoEntity
import com.example.appwatch.data.local.entity.AppNotificationEntity
import com.example.appwatch.data.local.entity.NeedsAttentionEntity
import com.example.appwatch.data.local.entity.PermissionAccessEntity
import com.example.appwatch.data.local.entity.RecentEventEntity
import com.example.appwatch.data.local.entity.UsageEntity

@Database(
    entities = [
        UsageEntity::class,
        PermissionAccessEntity::class,
        AppInfoEntity::class,
        RecentEventEntity::class,
        NeedsAttentionEntity::class,
        AppNotificationEntity::class,
        AppDataUsageEntity::class
    ],
    version = 15,
    exportSchema = false
)

abstract class AppWatchDatabase: RoomDatabase() {

    abstract fun usageDao(): UsageDao
    abstract fun permissionAccessDao(): PermissionAccessDao
    abstract fun appInfoDao(): AppInfoDao
    abstract fun recentEventDao(): RecentEventDao
    abstract fun appNotificationDao(): AppNotificationDao
    abstract fun appDataUsageDao(): AppDataUsageDao
    abstract fun needsAttentionDao(): NeedsAttentionDao




    companion object {
        @Volatile
        private var INSTANCE: AppWatchDatabase? = null

        fun getDatabase(context: Context): AppWatchDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppWatchDatabase::class.java,
                    "app_watch_database"
                )
                    .fallbackToDestructiveMigration() // Useful during development
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}