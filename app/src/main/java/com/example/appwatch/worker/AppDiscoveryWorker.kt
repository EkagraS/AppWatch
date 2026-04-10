package com.example.appwatch.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.appwatch.data.local.dao.AppInfoDao
import com.example.appwatch.system.PackageManagerHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class AppDiscoveryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val packageManagerHelper: PackageManagerHelper,
    private val appInfoDao: AppInfoDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // 1. Fetch metadata from the system
            val appEntities = packageManagerHelper.getInstalledAppsMetadata()

            // 2. Insert into Room database
            appInfoDao.insertAllMetadata(appEntities)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}