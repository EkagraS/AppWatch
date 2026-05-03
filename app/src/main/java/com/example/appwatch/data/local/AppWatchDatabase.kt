package com.example.appwatch.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.appwatch.data.local.dao.AppDataUsageDao
import com.example.appwatch.data.local.dao.AppInfoDao
import com.example.appwatch.data.local.dao.AppNotificationDao
import com.example.appwatch.data.local.dao.NeedsAttentionDao
import com.example.appwatch.data.local.dao.PermissionAccessDao
import com.example.appwatch.data.local.dao.RecentEventDao
import com.example.appwatch.data.local.dao.UsageDao
import com.example.appwatch.data.local.dao.VitalsDao
import com.example.appwatch.data.local.entity.AppDataUsageEntity
import com.example.appwatch.data.local.entity.AppInfoEntity
import com.example.appwatch.data.local.entity.AppNotificationEntity
import com.example.appwatch.data.local.entity.NeedsAttentionEntity
import com.example.appwatch.data.local.entity.PermissionAccessEntity
import com.example.appwatch.data.local.entity.RecentEventEntity
import com.example.appwatch.data.local.entity.UsageEntity
import com.example.appwatch.data.local.entity.VitalsEntity

@Database(
    entities = [
        UsageEntity::class,
        PermissionAccessEntity::class,
        AppInfoEntity::class,
        RecentEventEntity::class,
        NeedsAttentionEntity::class,
        AppNotificationEntity::class,
        AppDataUsageEntity::class,
        VitalsEntity::class
    ],
    version = 2,
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
    abstract fun vitalsDao(): VitalsDao


    companion object {
        @Volatile
        private var INSTANCE: AppWatchDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Maan le naya column add karna hai kisi table mein
//                database.execSQL("ALTER TABLE stats_table ADD COLUMN lastSyncTime INTEGER DEFAULT 0 NOT NULL")

                // Ya agar nayi table banayi hai
//                database.execSQL("CREATE TABLE IF NOT EXISTS `user_settings` (`id` INTEGER PRIMARY KEY NOT NULL, `theme` TEXT NOT NULL)")
            }
        }

        fun getDatabase(context: Context): AppWatchDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppWatchDatabase::class.java,
                    "app_watch_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}