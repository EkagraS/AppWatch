package com.example.appwatch.system

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import com.example.appwatch.data.local.entity.UsageEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The "Statistician" of the app.
 * Interacts with Android's UsageStatsManager to calculate screen time,
 * track app launches, and generate weekly activity charts.
 */
@Singleton
class UsageStatsHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    /**
     * Fetches usage data for all apps used today.
     * Maps raw system data into our UsageEntity for the database.
     */

    fun getDailyAppUsage(): List<UsageEntity> {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startTime = calendar.timeInMillis

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: emptyList()

        val todayTimestamp = startTime

        return stats.filter { it.totalTimeInForeground > 0 }.map { usageStat ->
            UsageEntity(
                packageName = usageStat.packageName,
                usageDate = todayTimestamp,
                totalTimeInForeground = usageStat.totalTimeInForeground,
                lastTimeUsed = usageStat.lastTimeUsed,
                appName = usageStat.packageName
            )
        }
    }

    /**
     * Calculates the total screen time in milliseconds for the current day.
     * This powers the "7 hrs" hero card on the Dashboard.
     */
    fun getTotalScreenTimeToday(): Long {
        return getDailyAppUsage().sumOf { it.totalTimeInForeground }
    }

    /**
     * Gets screen time totals for the last 7 days.
     * Used to populate the "Weekly Activity" bar chart.
     */
    fun getWeeklyActivityData(): List<Float> {
        val weeklyData = mutableListOf<Float>()

        // We fetch 7 days of data ending today
        for (i in 6 downTo 0) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -i)

            // Set to start of that specific day
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val start = calendar.timeInMillis

            // Set to end of that specific day
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            val end = calendar.timeInMillis

            // Use INTERVAL_DAILY for precise 24h blocks
            val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)

            val totalMillis = stats?.sumByDouble { it.totalTimeInForeground.toDouble() }?.toLong() ?: 0L
            // Convert to hours.
            // Cap it at 24h just in case of system bugs, but 0-10h is our UI scale.
            val hours = totalMillis / (1000f * 60 * 60)
            weeklyData.add(hours)
        }
        return weeklyData
    }

    /**
     * Helper to format milliseconds into "1h 22m" style strings for the UI.
     */
    fun formatDuration(millis: Long): String {
        val hours = millis / (1000 * 60 * 60)
        val minutes = (millis / (1000 * 60)) % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }

    fun getAppUsageToday(packageName: String): Long {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startTime = calendar.timeInMillis

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        )
        return stats?.find { it.packageName == packageName }?.totalTimeInForeground ?: 0L
    }

    fun getAppUsageThisWeek(packageName: String): Long {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val startTime = calendar.timeInMillis

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_WEEKLY, startTime, endTime
        )
        return stats?.find { it.packageName == packageName }?.totalTimeInForeground ?: 0L
    }

    fun getAppLaunchesToday(packageName: String): Int {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        val startTime = calendar.timeInMillis

        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = android.app.usage.UsageEvents.Event()
        var count = 0
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.packageName == packageName &&
                event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) {
                count++
            }
        }
        return count
    }

    fun getLastUsedString(packageName: String): String {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - (1000L * 60 * 60 * 24 * 90) // 90 days back

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST, // ← INTERVAL_BEST not INTERVAL_DAILY
            startTime,
            endTime
        )

        val lastUsed = stats
            ?.filter { it.packageName == packageName }
            ?.maxByOrNull { it.lastTimeUsed }
            ?.lastTimeUsed ?: 0L

        if (lastUsed == 0L) return "Never"

        val diff = System.currentTimeMillis() - lastUsed
        val minutes = diff / (1000 * 60)
        val hours = minutes / 60
        val days = hours / 24

        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            else -> "${days}d ago"
        }
    }
}