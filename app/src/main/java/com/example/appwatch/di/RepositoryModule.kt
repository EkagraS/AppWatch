package com.example.appwatch.di

import com.example.appwatch.data.local.AppWatchDatabase
import com.example.appwatch.data.local.dao.RecentEventDao
import com.example.appwatch.data.repository.AppDataUsageRepositoryImpl
import com.example.appwatch.data.repository.AppInfoRepositoryImpl
import com.example.appwatch.data.repository.AppNotificationRepositoryImpl
import com.example.appwatch.data.repository.DashboardRepositoryImpl
import com.example.appwatch.data.repository.PermissionRepositoryImpl
import com.example.appwatch.data.repository.UsageRepositoryImpl
import com.example.appwatch.data.repository.VitalsRepositoryImpl
import com.example.appwatch.domain.repository.AppDataUsageRepository
import com.example.appwatch.domain.repository.AppInfoRepository
import com.example.appwatch.domain.repository.AppNotificationRepository
import com.example.appwatch.domain.repository.DashboardRepository
import com.example.appwatch.domain.repository.PermissionRepository
import com.example.appwatch.domain.repository.UsageRepository
import com.example.appwatch.domain.repository.VitalsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAppInfoRepository(
        impl: AppInfoRepositoryImpl
    ): AppInfoRepository

    @Binds
    @Singleton
    abstract fun bindPermissionRepository(
        impl: PermissionRepositoryImpl
    ): PermissionRepository

    @Binds
    @Singleton
    abstract fun bindUsageRepository(
        usageRepositoryImpl: UsageRepositoryImpl
    ): UsageRepository

    @Binds
    @Singleton
    abstract fun bindDashboardRepository(
        dashboardRepositoryImpl: DashboardRepositoryImpl
    ): DashboardRepository

    @Binds
    @Singleton
    abstract fun bindVitalsRepository(
        vitalsRepositoryImpl: VitalsRepositoryImpl
    ): VitalsRepository

    @Binds
    @Singleton
    abstract fun bindAppNotificationRepository(
        impl: AppNotificationRepositoryImpl
    ): AppNotificationRepository

    @Binds
    @Singleton
    abstract fun bindAppDataUsageRepository(
        impl: AppDataUsageRepositoryImpl
    ): AppDataUsageRepository
}