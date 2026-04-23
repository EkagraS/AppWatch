package com.example.appwatch.data.repository

import com.example.appwatch.data.local.dao.AppInfoDao
import com.example.appwatch.domain.model.AppInfo
import com.example.appwatch.domain.repository.AppInfoRepository
import com.example.appwatch.system.PackageManagerHelper
import kotlinx.coroutines.flow.Flow
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

    override fun getAppDetails(packageName: String): Flow<AppInfo?> {
        return getAllApps(false).map { list ->
            list.find { it.packageName == packageName }
        }
    }

    override suspend fun refreshAppCache() {
        val entities = packageManagerHelper.getInstalledAppsMetadata()
        appInfoDao.insertAllMetadata(entities)
    }
}