package com.ekagra.privascope.worker

import android.content.Context
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ekagra.privascope.data.local.dao.UsageDao
import com.ekagra.privascope.data.local.dao.VitalsDao
import com.ekagra.privascope.data.local.entity.VitalsEntity
import com.ekagra.privascope.system.UsageStatsHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar

@HiltWorker
class UsageSnapshotWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val usageStatsHelper: UsageStatsHelper,
    private val vitalsDao: VitalsDao,
    private val usageDao: UsageDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val today = getTodayMidnight()

            val screenTime = usageStatsHelper.getTotalScreenTimeToday()
            val (unlocks, notifications) = usageStatsHelper.getTodayDeviceVitals()
            val dataUsage = usageStatsHelper.getTodayTotalDataUsage()

            // Security Patch fetch (Repository wala logic yahan bhi use kar sakte ho)
            val patch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else "Unknown"

            val dailySnapshot = VitalsEntity(
                date = today,
                totalScreenTime = screenTime,
                totalUnlocks = unlocks,
                totalNotifications = notifications,
                totalDataUsage = dataUsage,
                securityPatch = patch
            )
            vitalsDao.insertVitals(dailySnapshot)

            val usageList = usageStatsHelper.getDailyAppUsage()
            usageList.forEach { entity ->
                usageDao.insertUsage(entity.copy(usageDate = today))
            }

            val fourteenDaysInMillis = 14L * 24 * 60 * 60 * 1000
            val threshold = today - fourteenDaysInMillis
            vitalsDao.deleteOldVitals(threshold)
            usageDao.deleteOldUsageData(threshold)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
    private fun getTodayMidnight(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}