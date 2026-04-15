package com.example.appwatch.data.repository

import com.example.appwatch.data.local.dao.UsageDao
import com.example.appwatch.data.local.entity.UsageEntity
import com.example.appwatch.domain.model.AppUsage
import com.example.appwatch.domain.repository.UsageRepository
import com.example.appwatch.system.UsageStatsHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject

class UsageRepositoryImpl @Inject constructor(
    private val usageDao: UsageDao,
    private val usageStatsHelper: UsageStatsHelper
) : UsageRepository {

    override fun getDailyUsage(): Flow<List<AppUsage>> {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        return usageDao.getUsageByDate(today).map { entities ->
            entities.map { entity ->
                AppUsage(
                    packageName = entity.packageName,
                    appName = entity.appName,
                    usageTimeString = usageStatsHelper.formatDuration(entity.totalTimeInForeground),
                    usagePercentage = 0.5f,
                    lastUsedString = "Active today"
                )
            }
        }
    }

    override fun getTotalScreenTimeToday(): Flow<String> = flow {
        val totalMillis = usageStatsHelper.getTotalScreenTimeToday()
        emit(usageStatsHelper.formatDuration(totalMillis))
    }

    override fun getWeeklyActivity(): Flow<List<Float>> = flow {
        while(true) { // Keeps the chart updated if data changes
            val weeklyData = mutableListOf<Float>()

            // Loop through last 7 days (6 days ago -> Today)
            for (i in 6 downTo 0) {
                val calendar = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -i)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val dayTimestamp = calendar.timeInMillis

                val totalForDay = if (i == 0) {
                    // TODAY: Get live data from System Stats Manager
                    usageStatsHelper.getTotalScreenTimeToday()
                } else {
                    // PAST: Get from Room DB.
                    // We use firstOrNull because it's a Flow and we want the current snapshot
                    usageDao.getTotalScreenTime(dayTimestamp).firstOrNull() ?: 0L
                }

                // Convert ms to hours (e.g., 2.5f hours)
                val hours = totalForDay / (1000f * 60 * 60)
                weeklyData.add(hours)
            }

            emit(weeklyData)
            kotlinx.coroutines.delay(60000) // Refresh every minute for the "Today" bar
        }
    }

    override suspend fun saveUsageSnapshot(usage: AppUsage) {
        val entity = UsageEntity(
            packageName = usage.packageName,
            appName = usage.appName,
            usageDate = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis,
            totalTimeInForeground = 0L,
            lastTimeUsed = System.currentTimeMillis()
        )
        usageDao.insertUsage(entity)
    }
}