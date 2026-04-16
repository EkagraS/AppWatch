package com.example.appwatch.system

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

//    val monitorOps = arrayOf(AppOpsManager.OPSTR_CAMERA, AppOpsManager.OPSTR_RECORD_AUDIO, AppOpsManager.OPSTR_FINE_LOCATION, AppOpsManager.OPSTR_COARSE_LOCATION, AppOpsManager.OPSTR_READ_CONTACTS, AppOpsManager.OPSTR_READ_CALENDAR, AppOpsManager.OPSTR_READ_PHONE_NUMBERS, AppOpsManager.OPSTR_READ_SMS)

    /**
     * Gets the last time an app was active.
     * Since getPackagesForOps is a hidden API, we use UsageStatsManager
     * to provide the "Last used" data for the "30 days" logic.
     */
    private val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

    suspend fun getLastPermissionAccess(packageName: String, permission: String): Long = withContext(Dispatchers.IO){
        // AppOps tracking for last access requires Android 11 (API 30)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return@withContext 0L

        val op = when (permission) {
            "CAMERA" -> AppOpsManager.OPSTR_CAMERA
            "RECORD_AUDIO" -> AppOpsManager.OPSTR_RECORD_AUDIO
            "FINE_LOCATION" -> AppOpsManager.OPSTR_FINE_LOCATION
            else -> return@withContext 0L
        }

        try {
            var lastAccess = 0L

            // 1. Get the hidden method 'getPackagesForOps'
            val getPackagesForOpsMethod = appOps.javaClass.getMethod("getPackagesForOps", Array<String>::class.java)
            val packageOps = getPackagesForOpsMethod.invoke(appOps, arrayOf(op)) as? List<*>

            packageOps?.forEach { packageOp ->
                // 2. Get the package name from the hidden PackageOp class
                val pkgName = packageOp?.javaClass?.getMethod("getPackageName")?.invoke(packageOp) as? String

                if (pkgName == packageName) {
                    // 3. Get the list of ops (OpEntry objects)
                    val opsList = packageOp.javaClass.getMethod("getOps").invoke(packageOp) as? List<*>
                    opsList?.forEach { opEntry ->
                        // 4. Check if it's the right operation
                        val opStr = opEntry?.javaClass?.getMethod("getOpStr")?.invoke(opEntry) as? String
                        if (opStr == op) {
                            // 5. Call getLastTimeAccess. '31' is the value for OP_FLAGS_ALL
                            val getLastTimeAccessMethod = opEntry?.javaClass?.getMethod("getLastTimeAccess", Int::class.java)
                            val time = getLastTimeAccessMethod?.invoke(opEntry, 31) as? Long ?: 0L
                            lastAccess = lastAccess.coerceAtLeast(time)
                        }
                    }
                }
            }
            lastAccess
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

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
            AppOpsManager.OPSTR_READ_CONTACTS -> "Contacts"
            AppOpsManager.OPSTR_READ_CALENDAR -> "Calendar"
            AppOpsManager.OPSTR_READ_PHONE_NUMBERS -> "Phone Numbers"
            AppOpsManager.OPSTR_READ_SMS -> "SMS"
            else -> "System Resource"
        }
    }
}