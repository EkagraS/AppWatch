package com.example.appwatch.system

import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import com.example.appwatch.data.local.entity.UsageEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
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

    fun getAppUsageLastTimestamp(packageName: String): Long {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.YEAR, -1)
        val startTime = calendar.timeInMillis

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        val lastUsed = stats?.find { it.packageName == packageName }?.lastTimeUsed ?: 0L

        // FIX: If lastUsed is 0, the system has no record.
        // We return 0 so the UseCase can ignore it or use Install Time instead.
        return stats
        ?.filter { it.packageName == packageName }
            ?.maxOfOrNull { it.lastTimeUsed } ?: 0L
    }

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
                appName = usageStat.packageName,
                totalTimeInForeground = usageStat.totalTimeInForeground,
                lastTimeUsed = usageStat.lastTimeUsed,
                appUnlocks = 0,
                notificationCount = 0,
                lastEventTimestamp = 0L
            )
        }
    }

    fun getTotalScreenTimeToday(): Long {
        return getDailyAppUsage().sumOf { it.totalTimeInForeground }
    }

    fun formatDuration(millis: Long): String {
        val hours = millis / (1000 * 60 * 60)
        val minutes = (millis / (1000 * 60)) % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
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

    fun getTodayStreaks(): Pair<String, String> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()

        var longestSession = 0L
        var longestBreak = 0L

        var sessionStart = 0L
        var breakStart = startTime // Assume midnight se pehle phone lock tha

        var bestSessionStart = 0L
        var bestSessionEnd = 0L
        var bestBreakStart = startTime
        var bestBreakEnd = 0L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val timestamp = event.timeStamp

            // 15 = SCREEN_INTERACTIVE (Phone unlock hua / chalu hua)
            if (event.eventType == 15) {
                if (breakStart > 0L) {
                    val breakDuration = timestamp - breakStart
                    if (breakDuration > longestBreak) {
                        longestBreak = breakDuration
                        bestBreakStart = breakStart
                        bestBreakEnd = timestamp
                    }
                }
                sessionStart = timestamp
                breakStart = 0L // Break khatam
            }
            // 16 = SCREEN_NON_INTERACTIVE (Phone lock hua / band hua)
            else if (event.eventType == 16) {
                if (sessionStart > 0L) {
                    val sessionDuration = timestamp - sessionStart
                    if (sessionDuration > longestSession) {
                        longestSession = sessionDuration
                        bestSessionStart = sessionStart
                        bestSessionEnd = timestamp
                    }
                }
                breakStart = timestamp
                sessionStart = 0L // Session khatam
            }
        }

        // Loop ke baad check karo: Agar abhi tak session ya break chal hi raha hai
        if (sessionStart > 0L) {
            val sessionDuration = endTime - sessionStart
            if (sessionDuration > longestSession) {
                bestSessionStart = sessionStart
                bestSessionEnd = endTime
            }
        }
        if (breakStart > 0L) {
            val breakDuration = endTime - breakStart
            if (breakDuration > longestBreak) {
                bestBreakStart = breakStart
                bestBreakEnd = endTime
            }
        }

        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())

        val activeString = if (bestSessionStart == 0L) "No Data" else "${sdf.format(Date(bestSessionStart))} - ${sdf.format(Date(bestSessionEnd))}"
        val inactiveString = if (bestBreakEnd == 0L) "No Data" else "${sdf.format(Date(bestBreakStart))} - ${sdf.format(Date(bestBreakEnd))}"

        return Pair(activeString, inactiveString)
    }
}