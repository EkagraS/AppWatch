package com.example.appwatch.data.repository

import android.util.Log
import com.example.appwatch.data.local.dao.UsageDao
import com.example.appwatch.data.local.entity.UsageEntity
import com.example.appwatch.domain.model.AppUsage
import com.example.appwatch.domain.repository.UsageRepository
import com.example.appwatch.system.UsageStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
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

    private fun getTimestampDaysAgo(days: Int): Long {
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -days)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    override fun getTopAppForRange(days: Int, limit: Int): Flow<List<AppUsage>> {
        val startDate = getTimestampDaysAgo(days)

        return usageDao.getTopAppForRange(startDate, limit).map { entities ->
            entities.map { entity ->
                // Yahan ho rahi hai asali mapping
                AppUsage(
                    packageName = entity.packageName,
                    appName = entity.appName,
                    usageTimeString = formatDuration(entity.totalTimeInForeground),
                    usagePercentage = 0f,
                    appOpenCount = entity.appUnlocks, // <--- YE MISSING THA
                    lastUsedString = "Active"
                )
            }
        }
    }
    override fun getTotalStatsForRange(days: Int): Flow<Pair<Long, Int>> {
        val startDate = getTimestampDaysAgo(days)

        // Total time aur Unlocks ko combine karke ek single stream bhejenge
        return combine(
            usageDao.getTotalTimeForRange(startDate),
            usageDao.getTotalUnlocksForRange(startDate)
        ) { time, unlocks ->
            (time ?: 0L) to (unlocks ?: 0)
        }
    }

    override fun getActiveStreak(): Flow<String> = flow {
        while (true) {
            val streaks = usageStatsHelper.getTodayStreaks()
            emit(streaks.first)
            delay(60000)
        }
    }.flowOn(Dispatchers.IO)

    override fun getInActiveStreak(): Flow<String> = flow {
        while (true) {
            val streaks = usageStatsHelper.getTodayStreaks()
            emit(streaks.second)
            delay(60000)
        }
    }.flowOn(Dispatchers.IO)

    override fun getDailyUsage(): Flow<List<AppUsage>> {
        val today = getTodayTimestamp()

        return usageDao.getUsageByDate(today).map { entities ->
            entities.map { entity ->
                AppUsage(
                    packageName = entity.packageName,
                    appName = entity.appName,
                    usageTimeString = usageStatsHelper.formatDuration(entity.totalTimeInForeground),
                    usagePercentage = calculatePercentage(entity.totalTimeInForeground, entities),
                    appOpenCount = entity.appUnlocks,
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

    override fun getHighNoiseApps(limit: Int): Flow<List<UsageEntity>> {
        val sevenDaysAgo = getTimestampDaysAgo(7)

        return usageDao.getUsageStatsForNoiseAnalysis(sevenDaysAgo).map { list ->
            list.groupBy { it.packageName }
                .map { (packageName, dailyRecords) ->
                    // Saare 7 din ke data ko merge karke ek single entity bana rahe hain presentation ke liye
                    UsageEntity(
                        packageName = packageName,
                        usageDate = dailyRecords.first().usageDate,
                        appName = dailyRecords.first().appName,
                        totalTimeInForeground = dailyRecords.sumOf { it.totalTimeInForeground },
                        lastTimeUsed = dailyRecords.maxOf { it.lastTimeUsed },
                        appUnlocks = dailyRecords.sumOf { it.appUnlocks },
                        notificationCount = dailyRecords.sumOf { it.notificationCount }
                    )
                }
                // Sorting Logic: Most Notifications vs Least Time
                .filter { it.notificationCount > 0 }
                .sortedWith(compareByDescending<UsageEntity> { it.notificationCount }
                    .thenBy { it.totalTimeInForeground }
                    .thenBy { it.appUnlocks })
                .take(limit)
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
private fun formatDuration(millis: Long): String {
    val hours = millis / (1000 * 60 * 60)
    val minutes = (millis / (1000 * 60)) % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}