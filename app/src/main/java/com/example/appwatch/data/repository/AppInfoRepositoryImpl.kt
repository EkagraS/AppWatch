package com.example.appwatch.data.repository

import com.example.appwatch.data.local.dao.AppInfoDao
import com.example.appwatch.domain.model.AppInfo
import com.example.appwatch.domain.model.RiskLevel
import com.example.appwatch.domain.repository.AppInfoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AppInfoRepositoryImpl @Inject constructor(
    private val appInfoDao: AppInfoDao
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
                    riskLevel = if (entity.totalPermissions > 10) RiskLevel.HIGH else RiskLevel.LOW
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
    }
}