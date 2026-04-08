package com.example.appwatch.data.repository

import com.example.appwatch.data.local.dao.AppMetadataDao
import com.example.appwatch.data.local.entity.AppMetadataEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AppMetadataRepositoryImpl @Inject constructor(
    private val appMetadataDao: AppMetadataDao
) {
    fun getAllApps(sortByPermissions: Boolean): Flow<List<AppMetadataEntity>> {
        return if (sortByPermissions) {
            appMetadataDao.getAppsByPermissionCount()
        } else {
            appMetadataDao.getAllAppsAlphabetical()
        }
    }

    suspend fun refreshMetadata(metadataList: List<AppMetadataEntity>) {
        appMetadataDao.insertAllMetadata(metadataList)
    }

    suspend fun getSingleAppMetadata(packageName: String): AppMetadataEntity? {
        return appMetadataDao.getAppMetadata(packageName)
    }
}