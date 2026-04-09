package com.example.appwatch.data.repository

import com.example.appwatch.data.local.dao.AppInfoDao
import com.example.appwatch.data.local.entity.AppInfoEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AppInfoRepositoryImpl @Inject constructor(private val appInfoDao: AppInfoDao) {

    fun getAllApps(sortByPermissions: Boolean): Flow<List<AppInfoEntity>> {
        return if (sortByPermissions) {
            appInfoDao.getAppsByPermissionCount()
        } else {
            appInfoDao.getAllAppsAlphabetical()
        }
    }

    suspend fun refreshMetadata(metadataList: List<AppInfoEntity>) {
        appInfoDao.insertAllMetadata(metadataList)
    }

    suspend fun getSingleAppMetadata(packageName: String): AppInfoEntity? {
        return appInfoDao.getAppMetadata(packageName)
    }
}