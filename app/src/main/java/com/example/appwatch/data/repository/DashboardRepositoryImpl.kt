package com.example.appwatch.data.repository

import com.example.appwatch.data.local.dao.AppInfoDao
import com.example.appwatch.data.local.entity.AppInfoEntity
import com.example.appwatch.domain.model.ActivityItem
import com.example.appwatch.domain.model.AttentionItem
import com.example.appwatch.domain.model.DashboardSummary
import com.example.appwatch.domain.repository.DashboardRepository
import com.example.appwatch.system.AppOpsHelper
import com.example.appwatch.system.PackageManagerHelper
import com.example.appwatch.system.StorageStatsHelper
import com.example.appwatch.system.UsageStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val appInfoDao: AppInfoDao,
    private val packageManagerHelper: PackageManagerHelper,
    private val storageStatsHelper: StorageStatsHelper,
    private val usageStatsHelper: UsageStatsHelper,
    private val appOpsHelper: AppOpsHelper
) : DashboardRepository {

    // Caching system data to prevent 20s lag on every Room emission
    private var cachedSummary: DashboardSummary? = null
    private var cachedScreenTime: String = "0m"
    private var cachedUsedStorage: String = "0 B"
    private var cachedTotalStorage: String = "0 B"

    override fun getDashboardSummaryFlow(): Flow<DashboardSummary> {
        return appInfoDao.getAllAppsAlphabetical()
            .conflate()
            .map { apps ->
                if (apps.isEmpty()) {
                    emptyDashboard()
                } else {
                    // FIX: Agar heavy cache ready hai toh wo lo,
                    // warna Room data se "Fast Privacy Insights" generate karo
                    cachedSummary ?: calculateSummaryFast(apps)
                }
            }
            .flowOn(Dispatchers.IO)
    }

    private fun calculateSummaryFast(entities: List<AppInfoEntity>): DashboardSummary {
        val userApps = entities.filter { !it.isSystemApp }

        // Fast Attention Items: Sirf permissions aur new apps par base karke (No System Calls)
        val fastAttention = entities
            .filter { !it.isSystemApp && it.totalPermissions >= 15 }
            .take(3)
            .map {
                AttentionItem(it.packageName, it.appName, "High Permission Count (${it.totalPermissions})", "Medium")
            }

        return DashboardSummary(
            totalApps = userApps.size,
            highRiskApps = entities.count { it.totalPermissions >= 15 },
            totalScreenTime = cachedScreenTime, // "0m" initially
            locationAppsCount = entities.count { it.hasLocation },
            cameraAppsCount = entities.count { it.hasCamera },
            micAppsCount = entities.count { it.hasMic },
            contactAppsCount = entities.count { it.hasContacts },
            phoneAppsCount = entities.count { it.hasPhone },
            SmsAppsCount = entities.count { it.hasSms },
            usedStorage = cachedUsedStorage,
            totalStorage = cachedTotalStorage,
            attentionItems = fastAttention, // Ab yeh khali nahi dikhega!
            recentActivity = listOf(
                ActivityItem("Privacy Insights", "Scanning background access...", "PRIVACY"),
                ActivityItem("New Installations", "${userApps.count { System.currentTimeMillis() - it.installedAt < 7L*24*60*60*1000 }} this week", "INSTALL")
            )
        )
    }

    override suspend fun refreshAllData() {
        withContext(Dispatchers.IO) {
            try {
                // 1. Fetch Heavy System Data (Slow, runs in background)
                val screenTimeMillis = usageStatsHelper.getTotalScreenTimeToday()
                cachedScreenTime = usageStatsHelper.formatDuration(screenTimeMillis)

                val deviceStorage = storageStatsHelper.getDeviceStorageInfo()
                cachedUsedStorage = storageStatsHelper.formatSize(deviceStorage.usedBytes)
                cachedTotalStorage = storageStatsHelper.formatSize(deviceStorage.totalBytes)

                // 2. Refresh App Metadata
                val liveApps = packageManagerHelper.getInstalledAppsMetadata()
                appInfoDao.insertAllMetadata(liveApps)

                // 3. CALCULATION: Tera original heavy logic yahan background mein chalega
                cachedSummary = calculateSummaryFull(liveApps, cachedScreenTime)

                // 4. Update individual storage (Slowest part)
                val now = System.currentTimeMillis()
                liveApps.forEach { app ->
                    val stats = storageStatsHelper.getAppStorageInfo(app.packageName)
                    stats?.let {
                        appInfoDao.updateAppStorage(
                            packageName = app.packageName,
                            appSize = it.appSizeBytes,
                            dataSize = it.dataSizeBytes,
                            cacheSize = it.cacheSizeBytes,
                            totalSize = it.totalSizeBytes,
                            updatedAt = now
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Fast Summary: Shows immediate data from Room
    private fun calculateFastSummary(apps: List<AppInfoEntity>): DashboardSummary {
        val userApps = apps.filter { !it.isSystemApp }
        return emptyDashboard().copy(
            totalApps = userApps.size,
            usedStorage = cachedUsedStorage,
            totalStorage = cachedTotalStorage,
            totalScreenTime = cachedScreenTime
        )
    }

    // TERA COMPLETE ORIGINAL LOGIC (Fully Preserved & Debugged)
    private suspend fun calculateSummaryFull(entities: List<AppInfoEntity>, screenTime: String): DashboardSummary {
        val userApps = entities.filter { !it.isSystemApp }

        val ghostRisks = mutableListOf<AttentionItem>()
        val backgroundActors = mutableListOf<AttentionItem>()
        val dataHogs = mutableListOf<AttentionItem>()

        for (app in userApps) {
            // Check Last Used (Ghost Risks)
            val lastUsed = usageStatsHelper.getAppUsageLastTimestamp(app.packageName)
            val daysUnused = if (lastUsed == 0L) 999L else (System.currentTimeMillis() - lastUsed) / (1000 * 60 * 60 * 24)

            val daysText = when {
                lastUsed == 0L -> "Never used"
                daysUnused == 0L -> "Used today"
                daysUnused == 1L -> "Used yesterday"
                daysUnused > 90 -> "90+ days"
                else -> "Last used $daysUnused days ago"
            }

            if (daysUnused >= 7 && app.sensitivePermissionsCount > 0) {
                ghostRisks.add(AttentionItem(
                    app.packageName, app.appName,
                    "Unused for $daysText with ${app.sensitivePermissionsCount} sensitive permissions",
                    "Medium"
                ))
            }

            // Check Sensor Access (Background Actors)
            val lastCamera = appOpsHelper.getLastPermissionAccess(app.packageName, "CAMERA")
            val lastMic = appOpsHelper.getLastPermissionAccess(app.packageName, "RECORD_AUDIO")
            val latestSensorAccess = maxOf(lastCamera, lastMic)
            val isRecentSensorUse = (System.currentTimeMillis() - latestSensorAccess) < (24 * 60 * 60 * 1000)

            if (latestSensorAccess != 0L && isRecentSensorUse) {
                backgroundActors.add(AttentionItem(app.packageName, app.appName, "Recently accessed Camera/Mic", "High"))
            }

            // Check New High-Risk Apps (Data Hogs)
            val isNew = (System.currentTimeMillis() - app.installedAt) < 7L * 24 * 60 * 60 * 1000
            if (isNew && app.totalPermissions >= 10) {
                dataHogs.add(AttentionItem(app.packageName, app.appName, "New app requesting broad access", "Low"))
            }
        }

        // Attention Priority Logic
        val finalAttentionList = mutableListOf<AttentionItem>()
        backgroundActors.firstOrNull()?.let { finalAttentionList.add(it) }
        ghostRisks.firstOrNull { it.packageName !in finalAttentionList.map { a -> a.packageName } }?.let { finalAttentionList.add(it) }

        if (finalAttentionList.size < 3) {
            dataHogs.firstOrNull { it.packageName !in finalAttentionList.map { a -> a.packageName } }?.let { finalAttentionList.add(it) }
        }

        val fallbacks = (backgroundActors + ghostRisks + dataHogs).distinctBy { it.packageName }.filter { it.packageName !in finalAttentionList.map { a -> a.packageName } }
        finalAttentionList.addAll(fallbacks.take(3 - finalAttentionList.size))

        // Storage logic
        val largestApp = userApps.filter { it.totalSizeBytes > 0 }.maxByOrNull { it.totalSizeBytes }
        val heavyweightDesc = if (largestApp != null) {
            "${largestApp.appName} takes up significant space"
        } else {
            "No large apps detected"
        }

        return DashboardSummary(
            totalApps = userApps.size,
            highRiskApps = entities.count { it.totalPermissions >= 15 },
            totalScreenTime = screenTime,
            locationAppsCount = entities.count { it.hasLocation },
            cameraAppsCount = entities.count { it.hasCamera },
            micAppsCount = entities.count { it.hasMic },
            contactAppsCount = entities.count { it.hasContacts },
            phoneAppsCount = entities.count { it.hasPhone },
            SmsAppsCount = entities.count { it.hasSms },
            usedStorage = cachedUsedStorage,
            totalStorage = cachedTotalStorage,
            attentionItems = finalAttentionList,
            recentActivity = listOf(
                ActivityItem("New Installations", "${userApps.count { System.currentTimeMillis() - it.installedAt < 7L*24*60*60*1000 }} installed this week", "INSTALL"),
                ActivityItem("Storage Heavyweight", heavyweightDesc, "STORAGE_HEAVY"),
                ActivityItem("Cache Accumulation", "Free up ${storageStatsHelper.formatSize(userApps.sumOf { it.cacheSizeBytes })}", "CACHE")
            )
        )
    }

    private fun emptyDashboard() = DashboardSummary(
        totalApps = 0,
        highRiskApps = 0,
        totalScreenTime = "0m",
        locationAppsCount = 0,
        cameraAppsCount = 0,
        micAppsCount = 0,
        contactAppsCount = 0,
        phoneAppsCount = 0,
        SmsAppsCount = 0,
        usedStorage = "0 B",
        totalStorage = "0 B",
        attentionItems = emptyList(),
        recentActivity = emptyList()
    )
}