package com.example.appwatch.domain.repository

import com.example.appwatch.domain.model.AppInfo
import kotlinx.coroutines.flow.Flow

interface AppInfoRepository {

    fun getAllApps(sortByRisk: Boolean = false): Flow<List<AppInfo>>

    fun searchApps(query: String): Flow<List<AppInfo>>

    fun getAppDetails(packageName: String): Flow<AppInfo?>

    suspend fun refreshAppCache()
}