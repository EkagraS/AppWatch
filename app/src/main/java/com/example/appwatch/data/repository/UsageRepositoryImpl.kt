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
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

class UsageRepositoryImpl @Inject constructor(
    private val usageDao: UsageDao,
    private val usageStatsHelper: UsageStatsHelper,
    @ApplicationContext private val context: Context
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

    override suspend fun getUnlockPace(): Double {
        val vitals = usageStatsHelper.getTodayDeviceVitals()
        val totalUnlocks = vitals.first // Total unlocks today

        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        // Time since midnight in hours (e.g., 2:30 AM = 2.5)
        val hoursElapsed = currentHour + (currentMinute / 60.0)

        // Safety: Division by zero se bachne ke liye (Min 6 mins/0.1 hr)
        val safeHours = if (hoursElapsed < 0.1) 0.1 else hoursElapsed

        return totalUnlocks / safeHours
    }

    override suspend fun getMarathonSession(): Pair<String, Long>? {
        // Continuous max usage ka data Helper se uthaya
        val marathon = usageStatsHelper.getMarathonSessionToday() ?: return null

        val packageName = marathon.first
        val duration = marathon.second

        // Package name ko "Instagram" jaise readable name mein convert karna
        val appName = getAppName(packageName)

        return Pair(appName, duration)
    }

    private fun getAppName(packageName: String): String {
        return try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            // Agar app name na mile toh package ka last part dikhao
            packageName.split(".").last()
        }
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
                entity.copy(usageDate = today, appName = getAppName(entity.packageName))
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
                .filter { entity ->
                    entity.notificationCount >= 5
                }
                .filter { entity ->
                    entity.totalTimeInForeground >= 0
                }
                .sortedByDescending { entity ->
                    val usageMinutes = entity.totalTimeInForeground / (1000f * 60f)
                    val safeMinutes = usageMinutes.coerceAtLeast(0.5f)
                    entity.notificationCount / safeMinutes
                }
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