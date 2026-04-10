package com.example.appwatch.data.repository

import com.example.appwatch.data.local.dao.UsageDao
import com.example.appwatch.domain.model.AppUsage
import com.example.appwatch.domain.model.DashboardSummary
import com.example.appwatch.domain.repository.UsageRepository
import com.example.appwatch.system.UsageStatsHelper
import kotlinx.coroutines.flow.Flow
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
        }.timeInMillis

        return usageDao.getUsageByDate(today).map { entities ->
            entities.map { entity ->
                AppUsage(
                    packageName = entity.packageName,
                    appName = entity.packageName,
                    usageTimeString = usageStatsHelper.formatDuration(entity.totalTimeInForeground),
                    usagePercentage = 0.5f,
                    lastUsedString = "Aaj active tha"
                )
            }
        }
    }

    override fun getDashboardSummary(): Flow<DashboardSummary> {
        return getDailyUsage().map {
            DashboardSummary(
                totalApps = 124,
                highRiskApps = 3,
                totalScreenTime = "4h 22m",
                topUsedApp = "WhatsApp",
                weeklyUsageData = usageStatsHelper.getWeeklyActivityData()
            )
        }
    }

    override fun getWeeklyActivity(): Flow<List<Float>> {
        return usageDao.getUsageByDate(0).map { usageStatsHelper.getWeeklyActivityData() }
    }

    override suspend fun saveUsageSnapshot(usage: AppUsage) {
        // Room mein save karne ka logic
    }
}