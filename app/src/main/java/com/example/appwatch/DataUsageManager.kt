package com.example.appwatch.data.manager

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.NetworkCapabilities
import com.example.appwatch.data.local.entity.AppDataUsageEntity
import com.example.appwatch.domain.repository.AppDataUsageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class DataUsageManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AppDataUsageRepository
) {
    private val networkStatsManager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
    private val packageManager = context.packageManager

    suspend fun trackTodayUsage() {
        val today = LocalDate.now().toString()
        val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val currentTime = System.currentTimeMillis()

        // 1. Sabse pehle Mobile aur WiFi ka saara data ek baar mein fetch kar lo (Optimized)
        val mobileStatsMap = getNetworkUsageMap(NetworkCapabilities.TRANSPORT_CELLULAR, startOfDay, currentTime)
        val wifiStatsMap = getNetworkUsageMap(NetworkCapabilities.TRANSPORT_WIFI, startOfDay, currentTime)

        // 2. Installed apps ki list nikaalo
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        for (app in installedApps) {
            val uid = app.uid
            val mobileBytes = mobileStatsMap[uid] ?: 0L
            val wifiBytes = wifiStatsMap[uid] ?: 0L

            // 3. Wahi variables use kar raha hoon jo Entity mein hain
            if (mobileBytes > 0 || wifiBytes > 0) {
                repository.updateUsageData(
                    AppDataUsageEntity(
                        packageName = app.packageName,
                        date = today,
                        mobileUsageBytes = mobileBytes,
                        wifiUsageBytes = wifiBytes
                    )
                )
            }
        }
    }

    private fun getNetworkUsageMap(networkType: Int, startTime: Long, endTime: Long): Map<Int, Long> {
        val usageMap = mutableMapOf<Int, Long>()
        try {
            // 🔴 Yahan 'querySummary' use hoga, jo 'NetworkStats' (Iterator) return karta hai
            val networkStats = networkStatsManager.querySummary(networkType, null, startTime, endTime)
            val bucket = NetworkStats.Bucket()

            while (networkStats.hasNextBucket()) {
                networkStats.getNextBucket(bucket)
                val uid = bucket.uid
                val bytes = bucket.rxBytes + bucket.txBytes
                usageMap[uid] = (usageMap[uid] ?: 0L) + bytes
            }
            networkStats.close() // 🔴 Ab 'close' chalega
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return usageMap
    }
}