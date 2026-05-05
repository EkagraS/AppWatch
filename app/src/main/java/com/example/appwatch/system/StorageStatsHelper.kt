package com.example.appwatch.system

import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
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
        val pm = context.packageManager

        return try {
            val total = ssm.getTotalBytes(StorageManager.UUID_DEFAULT)
            val free = ssm.getFreeBytes(StorageManager.UUID_DEFAULT)

            val userHandle = android.os.Process.myUserHandle()
            val totalAppsStats = ssm.queryStatsForUser(StorageManager.UUID_DEFAULT, userHandle)
            val allAppsTotal = totalAppsStats.appBytes +
                    totalAppsStats.dataBytes +
                    totalAppsStats.cacheBytes

            // Fix shared UID problem — track UIDs already counted
            val installedApps = pm.getInstalledApplications(0)
            var userAppsSum = 0L
            val countedUids = mutableSetOf<Int>()

            for (app in installedApps) {
                val isSystem = (app.flags and
                        android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                if (!isSystem && !countedUids.contains(app.uid)) {
                    try {
                        val stats = ssm.queryStatsForUid(
                            StorageManager.UUID_DEFAULT, app.uid
                        )
                        userAppsSum += (stats.appBytes +
                                stats.dataBytes +
                                stats.cacheBytes)
                        countedUids.add(app.uid)
                    } catch (e: Exception) {}
                }
            }

            DeviceStorageInfo(
                totalBytes = total,
                usedBytes = total - free,
                freeBytes = free,
                totalUserAppsBytes = userAppsSum,
                totalSystemAppsBytes = allAppsTotal - userAppsSum
            )
        } catch (e: Exception) {
            val statFs = StatFs(Environment.getDataDirectory().path)
            DeviceStorageInfo(
                totalBytes = statFs.totalBytes,
                usedBytes = statFs.totalBytes - statFs.availableBytes,
                freeBytes = statFs.availableBytes,
                totalUserAppsBytes = 0L,
                totalSystemAppsBytes = 0L
            )
        }
    }

    fun getAppStorageInfo(packageName: String): AppStorageInfo? {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val isSystemApp = (appInfo.flags and
                    android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0

            if (isSystemApp) {
                return AppStorageInfo(
                    packageName = packageName,
                    appName = pm.getApplicationLabel(appInfo).toString(),
                    appSizeBytes = 0L,
                    dataSizeBytes = 0L,
                    cacheSizeBytes = 0L,
                    totalSizeBytes = 0L,
                    isSystemApp = true
                )
            }

            val uid = appInfo.uid
            val storageStats = storageStatsManager.queryStatsForUid(
                StorageManager.UUID_DEFAULT, uid
            )

            AppStorageInfo(
                packageName = packageName,
                appName = pm.getApplicationLabel(appInfo).toString(),
                appSizeBytes = storageStats.appBytes,
                dataSizeBytes = storageStats.dataBytes,
                cacheSizeBytes = storageStats.cacheBytes,
                totalSizeBytes = storageStats.appBytes +
                        storageStats.dataBytes +
                        storageStats.cacheBytes,
                isSystemApp = false
            )
        } catch (e: Exception) {
            null
        }
    }
    fun formatSize(bytes: Long): String {
        return android.text.format.Formatter.formatFileSize(context, bytes)
    }
}