package com.example.appwatch.di

import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.room.Room
import com.example.appwatch.data.local.AppWatchDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlin.jvm.java
import com.example.appwatch.SecurityManager
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Module
@InstallIn(SingletonComponent::class)
object SystemModule {

    @Provides
    @Singleton
    fun provideStorageStatsManager(@ApplicationContext context: Context): StorageStatsManager {
        return context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
    }

    @Provides
    @Singleton
    fun provideUsageStatsManager(@ApplicationContext context: Context): UsageStatsManager {
        return context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }

    @Provides
    @Singleton
    fun provideAppOpsManager(@ApplicationContext context: Context): AppOpsManager {
        return context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    }

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        securityManager: SecurityManager
    ): AppWatchDatabase {

        val passphrase = securityManager.getDatabasePassphrase()
        val factory = SupportOpenHelperFactory(passphrase)

        return Room.databaseBuilder(
            context,
            AppWatchDatabase::class.java,
            "app_watch_database"
        )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .setJournalMode(androidx.room.RoomDatabase.JournalMode.TRUNCATE)
            .build()
    }
}