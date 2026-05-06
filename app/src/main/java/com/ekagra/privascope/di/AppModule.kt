package com.ekagra.privascope.di

import com.ekagra.privascope.data.local.PrivaScopeDatabase
import com.ekagra.privascope.data.local.dao.AppDataUsageDao
import com.ekagra.privascope.data.local.dao.AppInfoDao
import com.ekagra.privascope.data.local.dao.AppNotificationDao
import com.ekagra.privascope.data.local.dao.NeedsAttentionDao
import com.ekagra.privascope.data.local.dao.PermissionAccessDao
import com.ekagra.privascope.data.local.dao.RecentEventDao
import com.ekagra.privascope.data.local.dao.UsageDao
import com.ekagra.privascope.data.local.dao.VitalsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun provideAppInfoDao(db: PrivaScopeDatabase): AppInfoDao = db.appInfoDao()

    @Provides
    fun providePermissionAccessDao(db: PrivaScopeDatabase): PermissionAccessDao = db.permissionAccessDao()

    @Provides
    fun provideUsageDao(db: PrivaScopeDatabase): UsageDao = db.usageDao()

    @Provides
    @Singleton
    fun provideRecentEventDao(db: PrivaScopeDatabase): RecentEventDao {
        return db.recentEventDao()
    }

    @Provides
    @Singleton
    fun provideNeedsAttentionDao(db: PrivaScopeDatabase): NeedsAttentionDao {
        return db.needsAttentionDao()
    }

    @Provides
    @Singleton
    fun provideNotificationDao(db: PrivaScopeDatabase): AppNotificationDao {
        return db.appNotificationDao()
    }

    @Provides
    @Singleton
    fun provideDataUsageDao(db: PrivaScopeDatabase): AppDataUsageDao {
        return db.appDataUsageDao()
    }

    @Provides
    @Singleton
    fun provideVitalsDao(db: PrivaScopeDatabase): VitalsDao {
        return db.vitalsDao()
    }
}