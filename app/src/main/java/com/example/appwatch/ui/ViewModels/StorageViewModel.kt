package com.example.appwatch.presentation.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
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
    val uiState: StateFlow<StorageUiState> = _uiState.asStateFlow()

    init {
        refreshAllData()
    }

    fun refreshAllData() {
        // Pattern: Agar data pehle se hai toh loading skip (UsageStats pattern)
        if (_uiState.value.userApps.isNotEmpty()) {
            _uiState.update { it.copy(isLoadingFromRoom = false) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingFromRoom = true) }

            refreshInBackground()

            // 2. Load Stats (Pattern: marathon/pace logic)
            checkMediaPermission()

            // 3. Fetch from Room (Pattern: fetchAllDaysData)
            fetchRoomData()

            _uiState.update { it.copy(isLoadingFromRoom = false) }
        }
    }

    private suspend fun fetchRoomData() {
        // Pattern: getUsageForDayInternal logic using firstOrNull
        val entities = appInfoDao.getAppsByStorageSize().firstOrNull() ?: emptyList()
        val deviceStorage = withContext(Dispatchers.IO) { storageStatsHelper.getDeviceStorageInfo() }

        val userApps = entities.filter { !it.isSystemApp }.map { entity ->
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

        val systemApps = entities.filter { it.isSystemApp }.map { entity ->
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
                deviceStorage = deviceStorage,
                totalUserAppsBytes = deviceStorage.totalUserAppsBytes,
                totalSystemAppsBytes = deviceStorage.totalSystemAppsBytes,
                lastUpdated = entities.firstOrNull()?.storageLastUpdated ?: 0L
            )
        }
    }

    fun refreshInBackground() {
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isRefreshing = true) }

            val roomApps = appInfoDao.getAllAppsAlphabetical().firstOrNull() ?: emptyList()
            val allEntities = if (roomApps.isEmpty()) {
                val liveApps = packageManagerHelper.getInstalledAppsMetadata()
                appInfoDao.insertAllMetadata(liveApps)
                liveApps
            } else {
                roomApps
            }

            val now = System.currentTimeMillis()
            allEntities.filter { !it.isSystemApp }.forEach { entity ->
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

            // Sync khatam hone ke baad data refresh
            fetchRoomData()

            _uiState.update { it.copy(isRefreshing = false) }
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