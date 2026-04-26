package com.example.appwatch.presentation.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appwatch.data.local.dao.AppInfoDao
import com.example.appwatch.system.AppStorageInfo
import com.example.appwatch.system.DeviceStorageInfo
import com.example.appwatch.system.PackageManagerHelper
import com.example.appwatch.system.StorageStatsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class MediaStorageInfo(
    val photosBytes: Long = 0L,
    val videosBytes: Long = 0L,
    val musicBytes: Long = 0L,
    val downloadsBytes: Long = 0L,
    val hasPermission: Boolean = false
)

data class StorageUiState(
    val deviceStorage: DeviceStorageInfo? = null,
    val userApps: List<AppStorageInfo> = emptyList(),
    val systemApps: List<AppStorageInfo> = emptyList(),
    val totalUserAppsBytes: Long = 0L,
    val totalSystemAppsBytes: Long = 0L,
    val mediaStorage: MediaStorageInfo = MediaStorageInfo(),
    val isLoadingFromRoom: Boolean = true,
    val isRefreshing: Boolean = false,
    val lastUpdated: Long = 0L
)

@HiltViewModel
class StorageViewModel @Inject constructor(
    private val storageStatsHelper: StorageStatsHelper,
    private val appInfoDao: AppInfoDao,
    private val packageManagerHelper: PackageManagerHelper,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(StorageUiState())
    val uiState: StateFlow<StorageUiState> = _uiState

    init {
        loadFromRoom()
        checkMediaPermission()
    }

    private fun loadFromRoom() {
        viewModelScope.launch {
            val initialDeviceStorage = withContext(Dispatchers.IO) {
                storageStatsHelper.getDeviceStorageInfo()
            }
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
                if (!uiState.value.isRefreshing && apps.isNotEmpty()) {
                    refreshInBackground()
                } else if (apps.isEmpty()) {
                    // Agar DB ekdum khali hai (First run), toh turant refresh karo
                    refreshInBackground()
                }
            }
        }
    }

    fun refreshInBackground() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isRefreshing = true) }

            // Get device storage
            val deviceStorage = storageStatsHelper.getDeviceStorageInfo()

            // Check if Room has apps, if not fetch from PackageManager first
            val roomApps = appInfoDao.getAllAppsAlphabetical().first()
            val allEntities = if (roomApps.isEmpty()) {
                // Room is empty — fetch live from PackageManager
                val liveApps = packageManagerHelper.getInstalledAppsMetadata()
                appInfoDao.insertAllMetadata(liveApps) // Save to Room
                liveApps
            } else {
                roomApps
            }

            // Fetch fresh storage for each app
            val now = System.currentTimeMillis()
            allEntities.forEach { entity ->
                val storageInfo = storageStatsHelper.getAppStorageInfo(entity.packageName)
                if (storageInfo != null) {
                    appInfoDao.updateAppStorage(
                        packageName = entity.packageName,
                        appSize = storageInfo.appSizeBytes,
                        dataSize = storageInfo.dataSizeBytes,
                        cacheSize = storageInfo.cacheSizeBytes,
                        totalSize = storageInfo.totalSizeBytes,
                        updatedAt = now
                    )
                }
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
    fun checkMediaPermission() {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

        if (hasPermission) {
            loadMediaStorage()
        } else {
            _uiState.update { it.copy(mediaStorage = MediaStorageInfo(hasPermission = false)) }
        }
    }

    fun loadMediaStorage() {
        viewModelScope.launch(Dispatchers.IO) {
            val photos = getFolderSize(Environment.DIRECTORY_PICTURES) +
                    getFolderSize(Environment.DIRECTORY_DCIM)
            val videos = getFolderSize(Environment.DIRECTORY_MOVIES)
            val music = getFolderSize(Environment.DIRECTORY_MUSIC)
            val downloads = getFolderSize(Environment.DIRECTORY_DOWNLOADS)

            _uiState.update {
                it.copy(
                    mediaStorage = MediaStorageInfo(
                        photosBytes = photos,
                        videosBytes = videos,
                        musicBytes = music,
                        downloadsBytes = downloads,
                        hasPermission = true
                    )
                )
            }
        }
    }

    private fun getFolderSize(directory: String): Long {
        return try {
            val folder = Environment.getExternalStoragePublicDirectory(directory)
            calculateFolderSize(folder)
        } catch (e: Exception) {
            0L
        }
    }

    private fun calculateFolderSize(folder: java.io.File): Long {
        if (!folder.exists()) return 0L
        var size = 0L
        folder.walkTopDown().forEach { file ->
            if (file.isFile) size += file.length()
        }
        return size
    }
}