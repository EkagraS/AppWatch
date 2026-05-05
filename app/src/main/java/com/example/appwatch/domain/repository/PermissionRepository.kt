package com.example.appwatch.domain.repository

import com.example.appwatch.domain.model.PermissionInfo
import com.example.appwatch.domain.model.SensitiveAccess
import com.example.appwatch.domain.model.AppInfo
import kotlinx.coroutines.flow.Flow

interface PermissionRepository {

    fun getPermissionsForApp(packageName: String): Flow<List<PermissionInfo>>

    fun getSensitiveAccessHistory(): Flow<List<SensitiveAccess>>

    fun getAppsWithPermission(permissionName: String): Flow<List<AppInfo>>

    suspend fun logAccessEvent(access: SensitiveAccess)
}