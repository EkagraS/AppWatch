package com.example.appwatch.worker
//
//import android.content.Context
//import androidx.hilt.work.HiltWorker
//import androidx.work.CoroutineWorker
//import androidx.work.WorkerParameters
//import com.example.appwatch.data.local.dao.AppInfoDao
//import com.example.appwatch.system.PackageManagerHelper
//import com.example.appwatch.system.StorageStatsHelper
//import dagger.assisted.Assisted
//import dagger.assisted.AssistedInject
//
//@HiltWorker
//class AppDiscoveryWorker @AssistedInject constructor(
//    @Assisted context: Context,
//    @Assisted workerParams: WorkerParameters,
//    private val packageManagerHelper: PackageManagerHelper,
//    private val storageStatsHelper: StorageStatsHelper,
//    private val appInfoDao: AppInfoDao
//) : CoroutineWorker(context, workerParams) {
//
//    override suspend fun doWork(): Result {
//        return try {
//            // 1. Fetch and save app metadata
//            val appEntities = packageManagerHelper.getInstalledAppsMetadata()
//            appInfoDao.insertAllMetadata(appEntities)
//
//            // 2. Fetch and save storage for each app
//            val now = System.currentTimeMillis()
//            appEntities.forEach { entity ->
//                val storageInfo = storageStatsHelper.getAppStorageInfo(entity.packageName)
//                if (storageInfo != null) {
//                    appInfoDao.updateAppStorage(
//                        packageName = entity.packageName,
//                        appSize = storageInfo.appSizeBytes,
//                        dataSize = storageInfo.dataSizeBytes,
//                        cacheSize = storageInfo.cacheSizeBytes,
//                        totalSize = storageInfo.totalSizeBytes,
//                        updatedAt = now
//                    )
//                }
//            }
//
//            Result.success()
//        } catch (e: Exception) {
//            Result.retry()
//        }
//    }
//}