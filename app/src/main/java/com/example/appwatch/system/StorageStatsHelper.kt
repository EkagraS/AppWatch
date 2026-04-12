package com.example.appwatch.system

import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import com.example.appwatch.data.local.entity.AppInfoEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import android.os.storage.StorageManager.UUID_DEFAULT

data class AppStorageInfo(
    val packageName: String,
    val appName: String,
    val appSizeBytes: Long,
    val dataSizeBytes: Long,
    val cacheSizeBytes: Long,
    val totalSizeBytes: Long,
    val isSystemApp: Boolean
)

data class DeviceStorageInfo(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val totalUserAppsBytes: Long,
    val totalSystemAppsBytes: Long
)

@Singleton
class StorageStatsHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val storageStatsManager =
        context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
    private val storageManager =
        context.getSystemService(Context.STORAGE_SERVICE) as StorageManager

    fun getDeviceStorageInfo(): DeviceStorageInfo {
        val ssm = context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager

        return try {
            val total = ssm.getTotalBytes(StorageManager.UUID_DEFAULT)
            val free = ssm.getFreeBytes(StorageManager.UUID_DEFAULT)

            DeviceStorageInfo(
                totalBytes = total,
                usedBytes = total - free,
                freeBytes = free,
                totalUserAppsBytes = 0L, // Skipped for speed
                totalSystemAppsBytes = 0L // Skipped for speed
            )
        } catch (e: Exception) {
            DeviceStorageInfo(0L, 0L, 0L, 0L, 0L)
        }
    }

    fun getAppStorageInfo(packageName: String): AppStorageInfo? {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val uid = appInfo.uid
            val storageUuid = try {
                storageManager.getUuidForPath(
                    Environment.getDataDirectory()
                )
            } catch (e: Exception) {
                StorageManager.UUID_DEFAULT
            }

            val storageStats = storageStatsManager.queryStatsForUid(storageUuid, uid)
            val isSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0

            AppStorageInfo(
                packageName = packageName,
                appName = pm.getApplicationLabel(appInfo).toString(),
                appSizeBytes = storageStats.appBytes,
                dataSizeBytes = storageStats.dataBytes,
                cacheSizeBytes = storageStats.cacheBytes,
                totalSizeBytes = storageStats.appBytes + storageStats.dataBytes + storageStats.cacheBytes,
                isSystemApp = isSystemApp
            )
        } catch (e: Exception) {
            null
        }
    }

    fun getAllAppsStorageInfo(entities: List<AppInfoEntity>): List<AppStorageInfo> {
        return entities.mapNotNull { entity ->
            getAppStorageInfo(entity.packageName)
        }.sortedByDescending { it.totalSizeBytes }
    }

    fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1024L * 1024 * 1024 -> "%.1f GB".format(bytes / (1024.0 * 1024 * 1024))
            bytes >= 1024L * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
            bytes >= 1024L -> "%.1f KB".format(bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}