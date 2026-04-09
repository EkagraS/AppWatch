package com.example.appwatch.system

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The "Privacy Detective" of the app.
 * It queries the Android AppOpsManager and UsageStatsManager to track
 * sensitive hardware usage and app activity.
 */
@Singleton
class AppOpsHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    val monitorOps = arrayOf(AppOpsManager.OPSTR_CAMERA, AppOpsManager.OPSTR_RECORD_AUDIO, AppOpsManager.OPSTR_FINE_LOCATION, AppOpsManager.OPSTR_COARSE_LOCATION)

    /**
     * Gets the last time an app was active.
     * Since getPackagesForOps is a hidden API, we use UsageStatsManager
     * to provide the "Last used" data for the "30 days" logic.
     */

    fun getLastAppUsageTime(packageName: String): Long {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - (1000L * 60 * 60 * 24 * 30) // 30 days ago

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        return stats?.find { it.packageName == packageName }?.lastTimeUsed ?: 0L
    }

    /**
     * Checks if an app is currently "actively" using a sensor.
     * This is useful for real-time alerts.
     */
    fun isOpActive(opString: String, uid: Int, packageName: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                appOpsManager.isOpActive(opString, uid, packageName)
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }

    /**
     * Translates OP codes into human-readable strings for your UI.
     */
    fun getFriendlyOpName(opString: String): String {
        return when (opString) {
            AppOpsManager.OPSTR_CAMERA -> "Camera"
            AppOpsManager.OPSTR_RECORD_AUDIO -> "Microphone"
            AppOpsManager.OPSTR_FINE_LOCATION -> "Precise Location"
            AppOpsManager.OPSTR_COARSE_LOCATION -> "Approximate Location"
            else -> "System Resource"
        }
    }
}