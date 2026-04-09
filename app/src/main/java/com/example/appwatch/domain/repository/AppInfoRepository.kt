package com.example.appwatch.domain.repository

import com.example.appwatch.domain.model.AppInfo
import kotlinx.coroutines.flow.Flow

/**
 * Contract for managing application-level information and metadata.
 */
interface AppInfoRepository {
    /**
     * Retrieves all installed apps.
     * @param sortByRisk If true, results are prioritized by risk level.
     */
    fun getAllApps(sortByRisk: Boolean = false): Flow<List<AppInfo>>

    /**
     * Filters apps based on name or package name.
     */
    fun searchApps(query: String): Flow<List<AppInfo>>

    /**
     * Fetches detailed information for a specific application.
     */
    fun getAppDetails(packageName: String): Flow<AppInfo?>

    /**
     * Triggers a system scan to refresh the cached app metadata.
     */
    suspend fun refreshAppCache()
}