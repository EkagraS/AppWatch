package com.example.appwatch.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appwatch.data.local.dao.AppInfoDao
import com.example.appwatch.system.AppStorageInfo
import com.example.appwatch.system.DeviceStorageInfo
import com.example.appwatch.system.StorageStatsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StorageUiState(
    val deviceStorage: DeviceStorageInfo? = null,
    val userApps: List<AppStorageInfo> = emptyList(),
    val systemApps: List<AppStorageInfo> = emptyList(),
    val totalUserAppsBytes: Long = 0L,
    val totalSystemAppsBytes: Long = 0L,
    val isLoadingFromRoom: Boolean = true,
    val isRefreshing: Boolean = false,
    val lastUpdated: Long = 0L
)

@HiltViewModel
class StorageViewModel @Inject constructor(
    private val storageStatsHelper: StorageStatsHelper,
    private val appInfoDao: AppInfoDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(StorageUiState())
    val uiState: StateFlow<StorageUiState> = _uiState

    init {
        loadFromRoom()
        refreshInBackground()
    }

    private fun loadFromRoom() {
        viewModelScope.launch {
            combine(
                appInfoDao.getAppsByStorageSize(),
                appInfoDao.getTotalUserAppsSize(),
                appInfoDao.getTotalSystemAppsSize()
            ) { apps, userTotal, systemTotal ->
                Triple(apps, userTotal, systemTotal)
            }.collect { (apps, userTotal, systemTotal) ->
                val userApps = apps.filter { !it.isSystemApp }.map { entity ->
                    AppStorageInfo(
                        packageName = entity.packageName,
                        appName = entity.appName,
                        appSizeBytes = entity.appSizeBytes,
                        dataSizeBytes = entity.dataSizeBytes,
                        cacheSizeBytes = entity.cacheSizeBytes,
                        totalSizeBytes = entity.totalSizeBytes,
                        isSystemApp = false
                    )
                }
                val systemApps = apps.filter { it.isSystemApp }.map { entity ->
                    AppStorageInfo(
                        packageName = entity.packageName,
                        appName = entity.appName,
                        appSizeBytes = entity.appSizeBytes,
                        dataSizeBytes = entity.dataSizeBytes,
                        cacheSizeBytes = entity.cacheSizeBytes,
                        totalSizeBytes = entity.totalSizeBytes,
                        isSystemApp = true
                    )
                }
                _uiState.update { current ->
                    current.copy(
                        userApps = userApps,
                        systemApps = systemApps,
                        totalUserAppsBytes = userTotal ?: 0L,
                        totalSystemAppsBytes = systemTotal ?: 0L,
                        isLoadingFromRoom = false,
                        lastUpdated = apps.firstOrNull()?.storageLastUpdated ?: 0L
                    )
                }
            }
        }
    }

    fun refreshInBackground() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isRefreshing = true) }

            // Get device storage
            val deviceStorage = storageStatsHelper.getDeviceStorageInfo()

            // Get all apps from Room
            val allEntities = appInfoDao.getAllAppsAlphabetical().first()

            // Fetch fresh storage for each app
            val freshStorageList = storageStatsHelper.getAllAppsStorageInfo(allEntities)

            // Update Room with fresh data
            val now = System.currentTimeMillis()
            freshStorageList.forEach { storageInfo ->
                appInfoDao.updateAppStorage(
                    packageName = storageInfo.packageName,
                    appSize = storageInfo.appSizeBytes,
                    dataSize = storageInfo.dataSizeBytes,
                    cacheSize = storageInfo.cacheSizeBytes,
                    totalSize = storageInfo.totalSizeBytes,
                    updatedAt = now
                )
            }

            _uiState.update { current ->
                current.copy(
                    deviceStorage = deviceStorage,
                    isRefreshing = false,
                    lastUpdated = now
                )
            }
        }
    }
}