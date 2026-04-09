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
        val calendar = Calendar.getInstance()

        // Loop through last 7 days
        for (i in 6 downTo 0) {
            val dayCalendar = Calendar.getInstance()
            dayCalendar.add(Calendar.DAY_OF_YEAR, -i)

            dayCalendar.set(Calendar.HOUR_OF_DAY, 0)
            dayCalendar.set(Calendar.MINUTE, 0)
            val start = dayCalendar.timeInMillis

            dayCalendar.set(Calendar.HOUR_OF_DAY, 23)
            dayCalendar.set(Calendar.MINUTE, 59)
            val end = dayCalendar.timeInMillis

            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST,
                start,
                end
            )

            val totalMillis = stats?.sumOf { it.totalTimeInForeground } ?: 0L
            // Convert to hours for the chart (e.g., 3.5f hours)
            weeklyData.add(totalMillis / (1000f * 60 * 60))
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
}