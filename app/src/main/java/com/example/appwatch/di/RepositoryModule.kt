package com.example.appwatch.di

import com.example.appwatch.data.repository.AppInfoRepositoryImpl
import com.example.appwatch.data.repository.DashboardRepositoryImpl
import com.example.appwatch.data.repository.PermissionRepositoryImpl
import com.example.appwatch.data.repository.UsageRepositoryImpl
import com.example.appwatch.domain.repository.AppInfoRepository
import com.example.appwatch.domain.repository.DashboardRepository
import com.example.appwatch.domain.repository.PermissionRepository
import com.example.appwatch.domain.repository.UsageRepository
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
        impl: UsageRepositoryImpl
    ): UsageRepository

    @Binds
    @Singleton
    abstract fun bindDashboardRepository(
        dashboardRepositoryImpl: DashboardRepositoryImpl
    ): DashboardRepository
}