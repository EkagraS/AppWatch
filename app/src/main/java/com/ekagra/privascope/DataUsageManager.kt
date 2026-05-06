package com.ekagra.privascope.data.manager

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.NetworkCapabilities
import com.ekagra.privascope.data.local.entity.AppDataUsageEntity
import com.ekagra.privascope.domain.repository.AppDataUsageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

class DataUsageManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AppDataUsageRepository
) {
    private val networkStatsManager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
    private val packageManager = context.packageManager

    suspend fun trackTodayUsage() {
        // Reason: Heavy operations (OS IPC aur DB writes) ko IO thread par bheja taaki UI freeze na ho.
        withContext(Dispatchers.IO) {
            try {
                val calendar = Calendar.getInstance()
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.timeInMillis
                val currentTime = System.currentTimeMillis()

                val mobileStatsMap = getNetworkUsageMap(NetworkCapabilities.TRANSPORT_CELLULAR, startOfDay, currentTime)
                val wifiStatsMap = getNetworkUsageMap(NetworkCapabilities.TRANSPORT_WIFI, startOfDay, currentTime)

                val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

                for (app in installedApps) {
                    val uid = app.uid
                    val mobileBytes = mobileStatsMap[uid] ?: 0L
                    val wifiBytes = wifiStatsMap[uid] ?: 0L

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
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    private fun getNetworkUsageMap(networkType: Int, startTime: Long, endTime: Long): Map<Int, Long> {
        val usageMap = mutableMapOf<Int, Long>()
        var networkStats: NetworkStats? = null
        try {
            networkStats = networkStatsManager.querySummary(networkType, null, startTime, endTime)
            val bucket = NetworkStats.Bucket()

            while (networkStats.hasNextBucket()) {
                networkStats.getNextBucket(bucket)
                val uid = bucket.uid
                val bytes = bucket.rxBytes + bucket.txBytes
                usageMap[uid] = (usageMap[uid] ?: 0L) + bytes
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                networkStats?.close()
            } catch (e: Exception) {
            }
        }
        return usageMap
    }
}