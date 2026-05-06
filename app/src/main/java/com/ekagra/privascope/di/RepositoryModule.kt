package com.ekagra.privascope.di

import com.ekagra.privascope.data.repository.AppDataUsageRepositoryImpl
import com.ekagra.privascope.data.repository.AppInfoRepositoryImpl
import com.ekagra.privascope.data.repository.AppNotificationRepositoryImpl
import com.ekagra.privascope.data.repository.DashboardRepositoryImpl
import com.ekagra.privascope.data.repository.PermissionRepositoryImpl
import com.ekagra.privascope.data.repository.UsageRepositoryImpl
import com.ekagra.privascope.data.repository.VitalsRepositoryImpl
import com.ekagra.privascope.domain.repository.AppDataUsageRepository
import com.ekagra.privascope.domain.repository.AppInfoRepository
import com.ekagra.privascope.domain.repository.AppNotificationRepository
import com.ekagra.privascope.domain.repository.DashboardRepository
import com.ekagra.privascope.domain.repository.PermissionRepository
import com.ekagra.privascope.domain.repository.UsageRepository
import com.ekagra.privascope.domain.repository.VitalsRepository
import dagger.Binds
import dagger.Module
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