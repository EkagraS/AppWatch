package com.ekagra.privascope.domain.repository

import com.ekagra.privascope.domain.model.AppInfo
import kotlinx.coroutines.flow.Flow

interface AppInfoRepository {

    fun getAllApps(sortByRisk: Boolean = false): Flow<List<AppInfo>>

    fun searchApps(query: String): Flow<List<AppInfo>>

    fun getAppDetails(packageName: String): Flow<AppInfo?>

    suspend fun refreshAppCache()
}