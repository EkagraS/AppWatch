package com.example.appwatch.domain.repository

import com.example.appwatch.domain.model.PermissionInfo
import com.example.appwatch.domain.model.SensitiveAccess
import com.example.appwatch.domain.model.AppInfo
import kotlinx.coroutines.flow.Flow

/**
 * Contract for auditing app permissions and tracking sensitive resource access.
 */
interface PermissionRepository {
    /**
     * Gets a full audit of permissions for a specific app.
     */
    fun getPermissionsForApp(packageName: String): Flow<List<PermissionInfo>>

    /**
     * Provides a timeline of sensitive accesses (Camera, Mic, etc.) across the device.
     */
    fun getSensitiveAccessHistory(): Flow<List<SensitiveAccess>>

    /**
     * Finds all apps that have a specific permission (e.g., "Location").
     */
    fun getAppsWithPermission(permissionName: String): Flow<List<AppInfo>>

    /**
     * Logs a new access event detected by the system helpers.
     */
    suspend fun logAccessEvent(access: SensitiveAccess)
}