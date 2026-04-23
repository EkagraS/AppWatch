package com.example.appwatch.data.repository

import android.content.Context
import com.example.appwatch.data.local.dao.PermissionAccessDao
import com.example.appwatch.data.local.entity.PermissionAccessEntity
import com.example.appwatch.domain.model.AppInfo
import com.example.appwatch.domain.model.PermissionInfo
import com.example.appwatch.domain.model.SensitiveAccess
import com.example.appwatch.domain.repository.PermissionRepository
import com.example.appwatch.system.PackageManagerHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class PermissionRepositoryImpl @Inject constructor(
    private val permissionAccessDao: PermissionAccessDao,
    private val packageManagerHelper: PackageManagerHelper,
    @ApplicationContext private val context: Context
) : PermissionRepository {

    override fun getPermissionsForApp(packageName: String): Flow<List<PermissionInfo>> {
        return permissionAccessDao.getEventsForApp(packageName).map { entities ->
            entities.map { entity ->
                PermissionInfo(
                    permissionName = entity.permissionName,
                    isSensitive = true,
                    description = "System se logged access",
                    granted = true
                )
            }
        }
    }

    override fun getSensitiveAccessHistory(): Flow<List<SensitiveAccess>> {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        return permissionAccessDao.getEventsForApp("").map { entities ->
            entities.map { entity ->
                SensitiveAccess(
                    packageName = entity.packageName,
                    appName = "App Name",
                    accessType = entity.permissionName,
                    timestampString = sdf.format(Date(entity.accessTimestamp)),
                    isRealTime = false
                )
            }
        }
    }

    override fun getAppsWithPermission(permissionName: String): Flow<List<AppInfo>> {
        return permissionAccessDao.getEventsByPermission(permissionName).map { entities ->
            entities.mapNotNull { entity ->
                packageManagerHelper.getAppInfo(entity.packageName)
            }
        }
    }
    override suspend fun logAccessEvent(access: SensitiveAccess) {
        val entity = PermissionAccessEntity(
            packageName = access.packageName,
            permissionName = access.accessType,
            accessTimestamp = System.currentTimeMillis(),
            isGranted = true
        )
        permissionAccessDao.insertAccessEvent(entity)
    }
}