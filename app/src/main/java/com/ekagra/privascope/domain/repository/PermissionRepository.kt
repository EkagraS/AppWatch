package com.ekagra.privascope.domain.repository

import com.ekagra.privascope.domain.model.PermissionInfo
import com.ekagra.privascope.domain.model.SensitiveAccess
import com.ekagra.privascope.domain.model.AppInfo
import kotlinx.coroutines.flow.Flow

interface PermissionRepository {

    fun getPermissionsForApp(packageName: String): Flow<List<PermissionInfo>>

    fun getSensitiveAccessHistory(): Flow<List<SensitiveAccess>>

    fun getAppsWithPermission(permissionName: String): Flow<List<AppInfo>>

    suspend fun logAccessEvent(access: SensitiveAccess)
}