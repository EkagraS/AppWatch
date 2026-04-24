package com.example.appwatch.di

import android.content.Context
import com.example.appwatch.data.local.AppWatchDatabase
import com.example.appwatch.data.local.dao.AppDataUsageDao
import com.example.appwatch.data.local.dao.AppInfoDao
import com.example.appwatch.data.local.dao.AppNotificationDao
import com.example.appwatch.data.local.dao.NeedsAttentionDao
import com.example.appwatch.data.local.dao.PermissionAccessDao
import com.example.appwatch.data.local.dao.RecentEventDao
import com.example.appwatch.data.local.dao.UsageDao
import com.example.appwatch.data.repository.AppDataUsageRepositoryImpl
import com.example.appwatch.data.repository.AppNotificationRepositoryImpl
import com.example.appwatch.domain.repository.AppDataUsageRepository
import com.example.appwatch.domain.repository.AppNotificationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppWatchDatabase {
        return AppWatchDatabase.getDatabase(context)
    }

    @Provides
    fun provideAppInfoDao(db: AppWatchDatabase): AppInfoDao = db.appInfoDao()

    @Provides
    fun providePermissionAccessDao(db: AppWatchDatabase): PermissionAccessDao = db.permissionAccessDao()

    @Provides
    fun provideUsageDao(db: AppWatchDatabase): UsageDao = db.usageDao()

    @Provides
    @Singleton
    fun provideRecentEventDao(db: AppWatchDatabase): RecentEventDao {
        return db.recentEventDao()
    }

    @Provides
    @Singleton
    fun provideNeedsAttentionDao(db: AppWatchDatabase): NeedsAttentionDao {
        return db.needsAttentionDao()
    }

    @Provides
    @Singleton
    fun provideNotificationDao(db: AppWatchDatabase): AppNotificationDao {
        return db.appNotificationDao()
    }

    @Provides
    @Singleton
    fun provideNotificationRepository(
        dao: AppNotificationDao
    ): AppNotificationRepository {
        return AppNotificationRepositoryImpl(dao)
    }

    @Provides
    @Singleton
    fun provideDataUsageDao(db: AppWatchDatabase): AppDataUsageDao {
        return db.appDataUsageDao()
    }

    @Provides
    @Singleton
    fun provideDataUsageRepository(dao: AppDataUsageDao): AppDataUsageRepository {
        return AppDataUsageRepositoryImpl(dao)
    }
}