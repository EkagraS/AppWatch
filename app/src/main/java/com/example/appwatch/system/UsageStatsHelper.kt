package com.example.appwatch.system

import android.app.usage.NetworkStatsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.appwatch.data.local.entity.UsageEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import android.os.Process

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

    fun getAppLastUsedTime(packageName: String): Long {
        val now = System.currentTimeMillis()
        val startTime = now - (90L * 24 * 60 * 60 * 1000)

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            startTime,
            now
        )

        return stats
            ?.filter { it.packageName == packageName && it.lastTimeUsed > 0 }
            ?.maxByOrNull { it.lastTimeUsed }
            ?.lastTimeUsed ?: 0L
    }

    fun getDailyAppUsage(): List<UsageEntity> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()
        val elapsedSinceMidnight = endTime - startTime // Max possible usage right now

        val usageMap = mutableMapOf<String, Long>()
        val appLastResume = mutableMapOf<String, Long>()
//        val handledMidnightOverlap = mutableSetOf<String>()

        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()

        var currentPackage: String? = null
        var currentStart = 0L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName
            val time = event.timeStamp

            when (event.eventType) {

                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    val appInfo = try {
                        context.packageManager.getApplicationInfo(pkg, 0)
                    } catch (e: Exception) {
                        null
                    }

                    if (appInfo == null || (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0) { }

                    if (pkg == currentPackage) { }

                    if (time < currentStart) { }

                    if (currentPackage != null && currentStart > 0 && time > currentStart && currentStart >= startTime) {
                        val duration = time - currentStart
                        usageMap[currentPackage] = (usageMap[currentPackage] ?: 0L) + duration
                    }
                    currentPackage = pkg
                    currentStart = time
                }

                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                }

                else -> {}
            }
        }

        // 🚩 FINAL SANITY CHECK: Kisi bhi app ko 12:00 AM se zyada time mat do
        return usageMap.filter { it.value > 1000 }.map { (pkg, duration) ->
            val finalDuration = Math.min(duration, elapsedSinceMidnight)
            UsageEntity(
                packageName = pkg,
                usageDate = startTime,
                appName = pkg,
                totalTimeInForeground = finalDuration,
                lastTimeUsed = System.currentTimeMillis(),
                appUnlocks = 0,
                notificationCount = 0,
                lastEventTimestamp = 0L
            )
        }.sortedByDescending { it.totalTimeInForeground }
    }

    fun getTotalScreenTimeToday(): Long {
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

        var totalTime = 0L
        var screenOnTime = 0L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            // 15 = SCREEN_INTERACTIVE, 16 = SCREEN_NON_INTERACTIVE
            if (event.eventType == 15) {
                screenOnTime = event.timeStamp
            } else if (event.eventType == 16) {
                if (screenOnTime > 0L) {
                    totalTime += event.timeStamp - screenOnTime
                    screenOnTime = 0L
                }
            }
        }

        if (screenOnTime > 0L) {
            totalTime += endTime - screenOnTime
        }

        return totalTime
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

    fun getTodayDeviceVitals(): Pair<Int, Int> {
        // 1. Aaj raat 12 baje ka time nikalna
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        // 2. System se events ki stream mangna
        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()

        var unlocks = 0
        var notifications = 0

        val lastNotificationTimeMap = mutableMapOf<String, Long>()
        val DEBOUNCE_THRESHOLD = 10000L
        // 3. Ek ek event ko check karna
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)

            when (event.eventType) {
                // Event Type 18: KEYGUARD_HIDDEN (Matlab user ne phone unlock kiya)
                18 -> unlocks++

                // Event Type 12: NOTIFICATION_INTERRUPTION (Matlab koi alert/notification aayi)
                12 -> {
                    val pkg = event.packageName
                    val currentTime = event.timeStamp
                    val lastTime = lastNotificationTimeMap[pkg] ?: 0L
                    if (currentTime - lastTime > DEBOUNCE_THRESHOLD) {
                        notifications++
                        lastNotificationTimeMap[pkg] = currentTime
                    }
                }
            }
        }

        return Pair(unlocks, notifications)
    }

    fun getMarathonSessionToday(): Pair<String, Long>? {
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

        var maxDuration = 0L
        var marathonApp = ""

        var currentPackage = ""
        var currentSessionStart = 0L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName
            val time = event.timeStamp

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    // Agar app change hui hai, tabhi naya session start karo
                    if (pkg != currentPackage) {
                        // Pichle app ka duration calculate karo
                        if (currentPackage.isNotEmpty() && currentSessionStart > 0) {
                            val duration = time - currentSessionStart
                            if (duration > maxDuration) {
                                maxDuration = duration
                                marathonApp = currentPackage
                            }
                        }
                        currentPackage = pkg
                        currentSessionStart = time
                    }
                }
                // Screen OFF hone par session pakka khatam
                16 -> { // SCREEN_NON_INTERACTIVE
                    if (currentPackage.isNotEmpty() && currentSessionStart > 0) {
                        val duration = time - currentSessionStart
                        if (duration > maxDuration) {
                            maxDuration = duration
                            marathonApp = currentPackage
                        }
                    }
                    currentPackage = ""
                    currentSessionStart = 0L
                }
            }
        }

        // Handle session jo abhi chal raha hai
        if (currentPackage.isNotEmpty() && currentSessionStart > 0) {
            val duration = endTime - currentSessionStart
            if (duration > maxDuration) {
                maxDuration = duration
                marathonApp = currentPackage
            }
        }

        return if (marathonApp.isNotEmpty()) Pair(marathonApp, maxDuration) else null
    }

    // UsageStatsHelper.kt ke andar
    fun getTodayTotalDataUsage(): Long {
        val networkStatsManager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        var totalBytes = 0L
        try {
            // Mobile Data
            val mobileBucket = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE, null, calendar.timeInMillis, System.currentTimeMillis())
            totalBytes += mobileBucket.rxBytes + mobileBucket.txBytes

            // WiFi
            val wifiBucket = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_WIFI, "", calendar.timeInMillis, System.currentTimeMillis())
            totalBytes += wifiBucket.rxBytes + wifiBucket.txBytes
        } catch (e: Exception) { e.printStackTrace() }

        return totalBytes
    }

    fun getHeavyDataConsumers(sevenDaysAgo: Long): Map<String, Long> {
        val networkStatsManager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
        val packageManager = context.packageManager
        val appDataMap = mutableMapOf<Int, Long>()

        try {
            val networkTypes = listOf(NetworkCapabilities.TRANSPORT_WIFI, NetworkCapabilities.TRANSPORT_CELLULAR)

            for (networkType in networkTypes) {
                val bucket = android.app.usage.NetworkStats.Bucket()
                val stats = networkStatsManager.querySummary(networkType, null, sevenDaysAgo, System.currentTimeMillis())

                while (stats.hasNextBucket()) {
                    stats.getNextBucket(bucket)
                    val uid = bucket.uid
                    val totalBytes = bucket.rxBytes + bucket.txBytes

                    // System apps ko ignore kar rahe hain
                    if (uid >= Process.FIRST_APPLICATION_UID) {
                        appDataMap[uid] = appDataMap.getOrDefault(uid, 0L) + totalBytes
                    }
                }
                stats.close()
            }

            val packageDataMap = mutableMapOf<String, Long>()
            appDataMap.forEach { (uid, bytes) ->
                val packages = packageManager.getPackagesForUid(uid)
                if (!packages.isNullOrEmpty()) {
                    packageDataMap[packages[0]] = bytes
                }
            }
            return packageDataMap
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyMap()
        }
    }
}