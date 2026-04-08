package com.example.appwatch.data.repository

import com.example.appwatch.data.local.dao.PermissionAccessDao
import com.example.appwatch.data.local.entity.PermissionAccessEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class PermissionRepositoryImpl @Inject constructor(private val permissionAccessDao: PermissionAccessDao) {

    suspend fun logAccessEvent(event: PermissionAccessEntity) {
        permissionAccessDao.insertAccessEvent(event)
    }

    fun getAppHistory(packageName: String): Flow<List<PermissionAccessEntity>> {
        return permissionAccessDao.getEventsForApp(packageName)
    }

    fun getAccessesByType(permissionName: String): Flow<List<PermissionAccessEntity>> {
        return permissionAccessDao.getEventsByPermission(permissionName)
    }

    suspend fun cleanOldLogs(retentionPeriodMs: Long) {
        val threshold = System.currentTimeMillis() - retentionPeriodMs
        permissionAccessDao.deleteOldLogs(threshold)
    }
}