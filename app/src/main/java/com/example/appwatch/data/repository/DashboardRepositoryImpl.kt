package com.example.appwatch.data.repository

import android.app.admin.SystemUpdateInfo
import android.os.Build
import com.example.appwatch.data.local.dao.AppInfoDao
import com.example.appwatch.data.local.dao.RecentEventDao
import com.example.appwatch.data.local.dao.UsageDao
import com.example.appwatch.data.local.datastore.VitalsDataStore
import com.example.appwatch.data.local.entity.AppInfoEntity
import com.example.appwatch.data.local.entity.RecentEventEntity
import com.example.appwatch.domain.model.ActivityItem
import com.example.appwatch.domain.model.DashboardSummary
import com.example.appwatch.domain.model.RecentItem
import com.example.appwatch.domain.repository.DashboardRepository
import com.example.appwatch.system.AppOpsHelper
import com.example.appwatch.system.PackageManagerHelper
import com.example.appwatch.system.StorageStatsHelper
import com.example.appwatch.system.UsageStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val appInfoDao: AppInfoDao,
    private val recentEventDao: RecentEventDao,
    private val packageManagerHelper: PackageManagerHelper,
    private val storageStatsHelper: StorageStatsHelper,
    private val usageStatsHelper: UsageStatsHelper,
    private val appOpsHelper: AppOpsHelper,
    private val vitalsDataStore: VitalsDataStore,
) : DashboardRepository {

    // Caching system data to prevent 20s lag on every Room emission
    private var cachedSummary: DashboardSummary? = null
    private var cachedScreenTime: String = "0m"
    private var cachedUsedStorage: String = "0 B"
    private var cachedTotalStorage: String = "0 B"

    override fun getDashboardSummaryFlow(): Flow<DashboardSummary> {
        val sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
        return combine(
            appInfoDao.getAllAppsAlphabetical(),
            recentEventDao.getRecentEventsFlow(sevenDaysAgo)
        ) { apps, recentEvents -> // 🔴 FIX: Ab dono list aage bhej rahe hain
            if (apps.isEmpty()) {
                emptyDashboard()
            } else {
                // Ab events direct database-flow se aayenge
                calculateSummaryFull(apps, recentEvents, cachedScreenTime)
            }
        }.flowOn(Dispatchers.IO)
    }
    private fun calculateSummaryFast(entities: List<AppInfoEntity>): DashboardSummary {
        val userApps = entities.filter { !it.isSystemApp }

        // Fast Attention Items: Sirf permissions aur new apps par base karke (No System Calls)
        val fastAttention = entities
            .filter { !it.isSystemApp && it.totalPermissions >= 15 }
            .take(3)
            .map {
                ActivityItem(it.packageName, it.appName, "High Permission Count (${it.totalPermissions})")
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
            attentionItems = fastAttention,
            recentActivity = listOf(
                // Naya RecentItem data class use kiya as requested
                RecentItem("PRIVACY", "Privacy Insights", "Scanning background access...", false),
                RecentItem("INSTALL", "New Installations", "${userApps.count { System.currentTimeMillis() - it.installedAt < 7L*24*60*60*1000 }} this week", false)
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

                var formattedPatch = "Unknown"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val rawDate = Build.VERSION.SECURITY_PATCH
                    if (rawDate.isNotEmpty()) {
                        try {
                            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val formatter = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                            val date = parser.parse(rawDate)
                            if (date != null) formattedPatch = formatter.format(date)
                        } catch (e: Exception) { formattedPatch = rawDate }
                    }
                }
                val vitals = usageStatsHelper.getTodayDeviceVitals()
                val currentUnlocks = vitals.first
                val currentNotifications = vitals.second
                val currentDataUsageBytes = usageStatsHelper.getTodayTotalDataUsage()

                vitalsDataStore.saveVitals(currentUnlocks, currentNotifications, currentDataUsageBytes, formattedPatch)

                // 2. Refresh App Metadata
                val liveApps = packageManagerHelper.getInstalledAppsMetadata()
//                cachedSummary = calculateSummaryFast(liveApps, cachedScreenTime)
                appInfoDao.insertAllMetadata(liveApps)
                syncRecentEventsFromOS(liveApps)

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

                // 5. Cleanup Database for Recent Events (Jahan need hai)
                val sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
                recentEventDao.deleteOldEvents(sevenDaysAgo)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // TERA COMPLETE ORIGINAL LOGIC (Fully Preserved & Debugged)
    private fun calculateSummaryFull(
        entities: List<AppInfoEntity>,
        recentEvents: List<RecentEventEntity>,
        screenTime: String
    ): DashboardSummary {
        val userApps = entities.filter { !it.isSystemApp }

        // 🔴 NAYE EVENTS COUNTING
        val installsCount = recentEvents.count { it.eventType == "INSTALL" }
        val updatesCount = recentEvents.count { it.eventType == "UPDATE" }
        val uninstallsCount = recentEvents.count { it.eventType == "UNINSTALL" } // Agar aage chal ke daala

        // Data hogs aur Sideloaded ki list
        val dataHogs = recentEvents.filter { it.eventType == "DATA_HOG" }
        val sideloaded = recentEvents.filter { it.eventType == "SIDELOADED_APK" }

        // Dynamic list creation as RecentItem
        val dynamicActivityList = mutableListOf<RecentItem>()

        // 1. Privacy / Security: Sideloaded Apps (Top Priority - Bottom Sheet)
        if (sideloaded.isNotEmpty()) {
            dynamicActivityList.add(RecentItem("SIDELOADED_APK", "Unknown Sources", "${sideloaded.size} apps installed outside Play Store", true))
        }

        // 2. Data Usage: Heavy Data Consumers (Bottom Sheet)
        if (dataHogs.isNotEmpty()) {
            val topHog = dataHogs.first()

            val actualAppName = entities.find { it.packageName == topHog.packageName }?.appName ?: "An app"
            val topAppSize = topHog.extraInfo ?: "massive data"

            dynamicActivityList.add(
                RecentItem(
                    eventType = "DATA_HOG",
                    title = "Highest Data Usage",
                    description = "$actualAppName consumed $topAppSize recently",
                    isTimeline = true
                )
            )
        }

        // 3. New Installations (Screen)
        if (installsCount > 0) {
            dynamicActivityList.add(RecentItem("INSTALL", "New Installations", "$installsCount apps installed this week", false))
        }

        // 4. App Updates (Bottom Sheet)
        if (updatesCount > 0) {
            dynamicActivityList.add(RecentItem("UPDATE", "App Updates", "$updatesCount apps updated this week", false))
        }

        // Fallback for uninstalls
        if (uninstallsCount > 0) {
            dynamicActivityList.add(RecentItem("UNINSTALL", "Apps Removed", "$uninstallsCount apps deleted this week", false))
        }

        val ghostRisks = mutableListOf<ActivityItem>()
        val backgroundActors = mutableListOf<ActivityItem>()
        val dataHogsList = mutableListOf<ActivityItem>()

        val finalAttentionList = mutableListOf<ActivityItem>()
        backgroundActors.firstOrNull()?.let { finalAttentionList.add(it) }

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
            recentActivity = dynamicActivityList,
            attentionItems = finalAttentionList
        )
    }
    private suspend fun syncRecentEventsFromOS(entities: List<AppInfoEntity>) {
        val sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
        val newEvents = mutableListOf<RecentEventEntity>()

        // 1. Heavy Data consumers nikal lo (Map of PackageName to Bytes)
        val heavyDataApps = usageStatsHelper.getHeavyDataConsumers(sevenDaysAgo)
        val userAppPackages = entities.filter { !it.isSystemApp }.map { it.packageName }.toSet()

        entities.filter { !it.isSystemApp }.forEach { app ->

            // --- FEATURE 1: Nayi Installations ---
            if (app.installedAt > sevenDaysAgo) {
                newEvents.add(RecentEventEntity(
                    packageName = app.packageName,
                    eventType = "INSTALL",
                    timestamp = app.installedAt
                ))
            }

            // --- FEATURE 2: App Updates ---
            if (app.lastUpdatedAt > sevenDaysAgo && app.lastUpdatedAt > (app.installedAt + 60000)) {
                newEvents.add(RecentEventEntity(
                    packageName = app.packageName,
                    eventType = "UPDATE",
                    timestamp = app.lastUpdatedAt
                ))
            }

            // --- FEATURE 3: Sideloaded / Unknown Sources ---
            // Ise humesha check karenge, kyunki sideloaded app risky hoti hai.
            // Timestamp mein install time daalenge.
            if (packageManagerHelper.isAppSideloaded(app.packageName)) {
                newEvents.add(RecentEventEntity(
                    packageName = app.packageName,
                    eventType = "SIDELOADED_APK",
                    timestamp = app.installedAt,
                    extraInfo = "Installed from unknown source"
                ))
            }

            // --- FEATURE 4: Heavy Data Hogs ---
            val topDataConsumer = heavyDataApps
                .filterKeys { it in userAppPackages } // Sirf user apps mein check karo
                .maxByOrNull { it.value } // Jiski value sabse badi hai, usko utha lo

            // Minimum 10MB ka limit rakha hai taaki 5-10 KB use karne wali app faltu mein na dikhe
            if (topDataConsumer != null && topDataConsumer.value > (10L * 1024 * 1024)) {
                val formattedSize = storageStatsHelper.formatSize(topDataConsumer.value)
                newEvents.add(RecentEventEntity(
                    packageName = topDataConsumer.key,
                    eventType = "DATA_HOG",
                    timestamp = System.currentTimeMillis(),
                    extraInfo = formattedSize // Extra info mein humne size save kar liya (e.g., "1.2 GB")
                ))
            }
        }

//        recentEventDao.clearSyncedEvents()
        if (newEvents.isNotEmpty()) {
            recentEventDao.insertEvents(newEvents)
        }
    }

    override fun getEventsByType(eventType: String): Flow<List<RecentEventEntity>> {
        return recentEventDao.getEventsByType(eventType)
    }

    override fun getTodayTotalUnlocks(): Flow<Int> = vitalsDataStore.vitalsFlow.map { it.unlocks }

    override fun getTodayTotalNotifications(): Flow<Int> = vitalsDataStore.vitalsFlow.map { it.notifications }

    override fun getTodayDataUsage(): Flow<Long> = vitalsDataStore.vitalsFlow.map { it.dataUsage }

    override fun getSystemUpdateInfo(): Flow<String> = vitalsDataStore.vitalsFlow.map { it.patchDate }


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