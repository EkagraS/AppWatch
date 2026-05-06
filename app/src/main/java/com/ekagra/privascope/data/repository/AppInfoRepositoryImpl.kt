package com.ekagra.privascope.data.repository

import android.util.Log
import com.ekagra.privascope.data.local.dao.AppInfoDao
import com.ekagra.privascope.domain.model.AppInfo
import com.ekagra.privascope.domain.repository.AppInfoRepository
import com.ekagra.privascope.system.PackageManagerHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AppInfoRepositoryImpl @Inject constructor(
    private val appInfoDao: AppInfoDao,
    private val packageManagerHelper: PackageManagerHelper
) : AppInfoRepository {

    override fun getAllApps(sortByRisk: Boolean): Flow<List<AppInfo>> {
        val flow = if (sortByRisk) appInfoDao.getAppsByPermissionCount()
        else appInfoDao.getAllAppsAlphabetical()

        return flow.map { entities ->
            entities.map { entity ->
                AppInfo(
                    id= entity.id,
                    packageName = entity.packageName,
                    appName = entity.appName,
                    isSystemApp = entity.isSystemApp,
                    totalPermissions = entity.totalPermissions,
                    installedAt = entity.installedAt
                )
            }
        }
    }
    override fun searchApps(query: String): Flow<List<AppInfo>> {
        return getAllApps(false).map { list ->
            list.filter {
                it.appName.contains(query, ignoreCase = true) ||
                        it.packageName.contains(query, ignoreCase = true)
            }
        }
    }

    override fun getAppDetails(packageName: String): Flow<AppInfo?> = flow {
        try {
            val entity = appInfoDao.getAppMetadata(packageName)
            if (entity != null) {
                emit(
                    AppInfo(
                        id= entity.id,
                        packageName = entity.packageName,
                        appName = entity.appName,
                        isSystemApp = entity.isSystemApp,
                        totalPermissions = entity.totalPermissions,
                        installedAt = entity.installedAt
                    )
                )
            } else {
                emit(null)
            }
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Failed to fetch app details for $packageName", e)
            emit(null)
        }
    }
    override suspend fun refreshAppCache() {
        try {
            val entities = packageManagerHelper.getInstalledAppsMetadata()
            appInfoDao.insertAllMetadata(entities)
        } catch (e: Exception) {
            Log.e("REPO_ERROR", "Failed to refresh app cache. OS might be busy.", e)
        }
    }
}