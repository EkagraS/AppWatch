package com.example.appwatch.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.appwatch.data.local.dao.UsageDao
import com.example.appwatch.system.UsageStatsHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar

@HiltWorker
class UsageSnapshotWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val usageStatsHelper: UsageStatsHelper,
    private val usageDao: UsageDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val usageList = usageStatsHelper.getDailyAppUsage()
            usageList.forEach { entity ->
                usageDao.insertUsage(entity.copy(usageDate = today))
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}