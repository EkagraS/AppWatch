package com.example.appwatch.data.repository

import android.util.Log
import com.example.appwatch.data.local.dao.UsageDao
import com.example.appwatch.data.local.entity.UsageEntity
import com.example.appwatch.domain.model.AppUsage
import com.example.appwatch.domain.repository.UsageRepository
import com.example.appwatch.system.UsageStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

class UsageRepositoryImpl @Inject constructor(
    private val usageDao: UsageDao,
    private val usageStatsHelper: UsageStatsHelper
) : UsageRepository {

    private fun getTodayTimestamp(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    override fun getDailyUsage(): Flow<List<AppUsage>> {
        val today = getTodayTimestamp()

        return usageDao.getUsageByDate(today).map { entities ->
            entities.map { entity ->
                AppUsage(
                    packageName = entity.packageName,
                    appName = entity.appName,
                    usageTimeString = usageStatsHelper.formatDuration(entity.totalTimeInForeground),
                    usagePercentage = calculatePercentage(entity.totalTimeInForeground, entities),
                    lastUsedString = "Active today"
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun syncDailyUsage() {
        withContext(Dispatchers.IO) {
            val today = getTodayTimestamp() // ← same method as getDailyUsage

            val liveEntities = usageStatsHelper.getDailyAppUsage()
            val normalizedUsage = liveEntities.map { entity ->
                entity.copy(usageDate = today)
            }
            if (normalizedUsage.isNotEmpty()) {
                usageDao.insertUsageList(normalizedUsage)
            }
        }
    }

    private fun calculatePercentage(current: Long, all: List<UsageEntity>): Float {
        val total = all.sumOf { it.totalTimeInForeground }
        return if (total > 0) current.toFloat() / total else 0f
    }

    override fun getTotalScreenTimeToday(): Flow<String> = flow {
        val totalMillis = usageStatsHelper.getTotalScreenTimeToday()
        emit(usageStatsHelper.formatDuration(totalMillis))
    }

    override fun getWeeklyActivity(): Flow<List<Float>> = flow {
        while (true) {
            val weeklyData = mutableListOf<Float>()
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
                    usageStatsHelper.getTotalScreenTimeToday()
                } else {
                    usageDao.getTotalScreenTime(dayTimestamp).firstOrNull() ?: 0L
                }
                weeklyData.add(totalForDay / (1000f * 60 * 60))
            }
            emit(weeklyData)
            kotlinx.coroutines.delay(60000)
        }
    }


    override suspend fun saveUsageSnapshot(usage: AppUsage) {
        val entity = UsageEntity(
            packageName = usage.packageName,
            appName = usage.appName,
            usageDate = getTodayTimestamp(),
            totalTimeInForeground = 0L,
            lastTimeUsed = System.currentTimeMillis()
        )
    }
}