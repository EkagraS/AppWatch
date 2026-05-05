package com.example.appwatch.data.repository

import android.os.Build
import com.example.appwatch.data.local.dao.AppInfoDao
import com.example.appwatch.data.local.dao.AppNotificationDao
import com.example.appwatch.data.local.dao.NeedsAttentionDao
import com.example.appwatch.data.local.dao.RecentEventDao
import com.example.appwatch.data.local.dao.UsageDao
import com.example.appwatch.data.local.dao.VitalsDao
import com.example.appwatch.data.local.entity.AppInfoEntity
import com.example.appwatch.data.local.entity.NeedsAttentionEntity
import com.example.appwatch.data.local.entity.RecentEventEntity
import com.example.appwatch.data.local.entity.VitalsEntity
import com.example.appwatch.domain.model.ActivityItem
import com.example.appwatch.domain.model.DashboardSummary
import com.example.appwatch.domain.model.RecentItem
import com.example.appwatch.domain.repository.DashboardRepository
import com.example.appwatch.system.PackageManagerHelper
import com.example.appwatch.system.StorageStatsHelper
import com.example.appwatch.system.UsageStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val appInfoDao: AppInfoDao,
    private val recentEventDao: RecentEventDao,
    private val needsAttentionDao: NeedsAttentionDao,
    private val packageManagerHelper: PackageManagerHelper,
    private val storageStatsHelper: StorageStatsHelper,
    private val usageStatsHelper: UsageStatsHelper,
    private val usageDao: UsageDao,
    private val vitalsDao: VitalsDao,
    private val appNotificationDao: AppNotificationDao,

    ) : DashboardRepository {

    // Caching system data to prevent 20s lag on every Room emission
    private var cachedScreenTime: String = "0m"
    private var cachedUsedStorage: String = "0 B"
    private var cachedTotalStorage: String = "0 B"

    private fun getTodayMidnight(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    override fun getDashboardSummaryFlow(): Flow<DashboardSummary> {
        val sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
        return combine(
            appInfoDao.getAllAppsAlphabetical().onStart { emit(emptyList()) },
            recentEventDao.getRecentEventsFlow(sevenDaysAgo).onStart { emit(emptyList()) },
            needsAttentionDao.getNeedsAttentionFlow().onStart { emit(emptyList()) },
        ) { apps, recentEvents, needsAttention ->
            if (apps.isEmpty()) {
                emptyDashboard()
            } else {
                calculateSummaryFull(entities = apps,recentEvents= recentEvents,needsAttention= needsAttention, screenTime = cachedScreenTime)
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun refreshAllData() {
        val todayMidnight = getTodayMidnight()
        withContext(Dispatchers.IO) {
            try {
                // 1. Fetch Heavy System Data (Slow, runs in background)
                val screenTimeMillis = usageStatsHelper.getTotalScreenTimeToday()
                cachedScreenTime = usageStatsHelper.formatDuration(screenTimeMillis)

                val deviceStorage = storageStatsHelper.getDeviceStorageInfo()
                cachedUsedStorage = storageStatsHelper.formatSize(deviceStorage.usedBytes)
                cachedTotalStorage = storageStatsHelper.formatSize(deviceStorage.totalBytes)

                val vitals = usageStatsHelper.getTodayDeviceVitals()
                val currentUnlocks = vitals.first
                val currentNotifications = vitals.second
                val currentDataUsageBytes = usageStatsHelper.getTodayTotalDataUsage()

                var formattedPatch = "Unknown"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val rawDate = Build.VERSION.SECURITY_PATCH
                    formattedPatch = formatSecurityPatch(rawDate) // Tera purana logic function mein daal dena
                }

                // B. Save Daily Vitals to Room (For Main Graph)
                val dailyVitals = VitalsEntity(
                    date = todayMidnight,
                    totalScreenTime = screenTimeMillis,
                    totalUnlocks = currentUnlocks,
                    totalNotifications = currentNotifications,
                    totalDataUsage = currentDataUsageBytes,
                    securityPatch = formattedPatch
                )
                vitalsDao.insertVitals(dailyVitals)

                val usageList = usageStatsHelper.getDailyAppUsage()
                usageList.forEach { entity ->
                    usageDao.insertUsage(entity.copy(usageDate = todayMidnight))
                }

                // C. Cleanup (14-Day Window)
                val threshold = todayMidnight - (14L * 24 * 60 * 60 * 1000)
                usageDao.deleteOldUsageData(threshold)
                vitalsDao.deleteOldVitals(threshold)

                // 2. Refresh App Metadata
                val liveApps = packageManagerHelper.getInstalledAppsMetadata()
//                cachedSummary = calculateSummaryFast(liveApps, cachedScreenTime)
                appInfoDao.insertAllMetadata(liveApps)
                syncRecentEventsFromOS(liveApps)
                syncNeedsAttentionFromOS(liveApps)

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
        needsAttention: List<NeedsAttentionEntity>,
        screenTime: String
    ): DashboardSummary {
        val userApps = entities.filter { !it.isSystemApp }

        // 🔴 NAYE EVENTS COUNTING
        val installsCount = recentEvents.count { it.eventType == "INSTALL" }
        val updatesCount = recentEvents.count { it.eventType == "UPDATE" }
        val uninstallsCount =
            recentEvents.count { it.eventType == "UNINSTALL" } // Agar aage chal ke daala

        // Data hogs aur Sideloaded ki list
        val dataHogs = recentEvents.filter { it.eventType == "DATA_HOG" }
        val sideloaded = recentEvents.filter { it.eventType == "SIDELOADED_APK" }

        // Dynamic list creation as RecentItem
        val dynamicActivityList = mutableListOf<RecentItem>()

        // 1. Privacy / Security: Sideloaded Apps (Top Priority - Bottom Sheet)
        if (sideloaded.isNotEmpty()) {
            val appName = entities.find {
                it.packageName == sideloaded.first().packageName
            }?.appName ?: sideloaded.first().packageName

            dynamicActivityList.add(
                RecentItem(
                    eventType = "SIDELOADED_APK",
                    title = "Unknown Sources",
                    description = if (sideloaded.size == 1)
                        "$appName installed from unknown source"
                    else
                        "${sideloaded.size} apps installed outside Play Store",
                    isTimeline = sideloaded.size > 5 // bottom sheet for <=5, screen for >5
                )
            )
        }

        // 2. Data Usage: Heavy Data Consumers (Bottom Sheet)
        if (dataHogs.isNotEmpty()) {
            val topHog = dataHogs.first()

            val actualAppName =
                entities.find { it.packageName == topHog.packageName }?.appName ?: "An app"
            val topAppSize = topHog.extraInfo ?: "massive data"

            dynamicActivityList.add(
                RecentItem(
                    eventType = "DATA_HOG",
                    title = "Highest Data Usage",
                    description = "$actualAppName consumed $topAppSize in past 7 days",
                    isTimeline = true
                )
            )
        }

        // 3. New Installations (Screen)
        if (installsCount > 0) {
            dynamicActivityList.add(
                RecentItem(
                    "INSTALL",
                    "New Installations",
                    "$installsCount apps installed this week",
                    false
                )
            )
        }

        // 4. App Updates (Bottom Sheet)
        if (updatesCount > 0) {
            dynamicActivityList.add(
                RecentItem(
                    "UPDATE",
                    "App Updates",
                    "$updatesCount apps updated this week",
                    false
                )
            )
        }

        // Fallback for uninstalls
        if (uninstallsCount > 0) {
            dynamicActivityList.add(
                RecentItem(
                    "UNINSTALL",
                    "Apps Removed",
                    "$uninstallsCount apps deleted this week",
                    false
                )
            )
        }

        //Needs Attention

        val unused30 = needsAttention.count { it.eventType == "AUDIT_UNUSED_30" }
        val unused60 = needsAttention.count { it.eventType == "AUDIT_UNUSED_60" }
        val unused90 = needsAttention.count { it.eventType == "AUDIT_UNUSED_90" }
        val totalUnused = unused30 + unused60 + unused90

        val staleCount = needsAttention.count { it.eventType == "AUDIT_STALE_PERMS" }
        val specialCount = needsAttention.count { it.eventType == "AUDIT_SPECIAL_ACCESS" }

        val attentionItems = mutableListOf<ActivityItem>()

        // Add items to list if counts > 0 (as you wrote before)
        if (totalUnused > 0) attentionItems.add(ActivityItem("Unused Apps", "$totalUnused apps", "UNUSED"))
        if (staleCount > 0) attentionItems.add(ActivityItem("Stale Permissions", "$staleCount apps", "STALE"))
        if (specialCount > 0) attentionItems.add(ActivityItem("Highly Sensitive", "$specialCount apps", "SPECIAL"))

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
            attentionItems = attentionItems
        )
    }

    private suspend fun syncRecentEventsFromOS(entities: List<AppInfoEntity>) {
        val now = System.currentTimeMillis()
        val dayMillis = 24L * 60 * 60 * 1000
        val sevenDaysAgo = now - dayMillis
        val newEvents = mutableListOf<RecentEventEntity>()

        val heavyDataApps = usageStatsHelper.getHeavyDataConsumers(sevenDaysAgo)
        val userAppPackages = entities.filter { !it.isSystemApp }.map { it.packageName }.toSet()

        entities.filter { !it.isSystemApp }.forEach { app ->
            // --- FEATURE 1: Nayi Installations ---
            if (app.installedAt > sevenDaysAgo) {
                val existing = recentEventDao.countExistingEvent(
                    app.packageName, "INSTALL", sevenDaysAgo
                )
                if(existing==0) {
                    newEvents.add(
                        RecentEventEntity(
                            packageName = app.packageName,
                            eventType = "INSTALL",
                            timestamp = app.installedAt
                        )
                    )
                }
            }

            // --- FEATURE 2: App Updates ---
            if (app.lastUpdatedAt > sevenDaysAgo && app.lastUpdatedAt > (app.installedAt + 60000)) {
                newEvents.add(
                    RecentEventEntity(
                        packageName = app.packageName,
                        eventType = "UPDATE",
                        timestamp = app.lastUpdatedAt
                    )
                )
            }

            // --- FEATURE 3: Sideloaded / Unknown Sources ---
            if (packageManagerHelper.isAppSideloaded(app.packageName) && app.installedAt > sevenDaysAgo) {
                newEvents.add(
                    RecentEventEntity(
                        packageName = app.packageName,
                        eventType = "SIDELOADED_APK",
                        timestamp = app.installedAt,
                        extraInfo = "Installed from unknown source"
                    )
                )
            }
        } // <-- Loop yahan khatam hota hai

        val topDataConsumer = heavyDataApps
            .filterKeys { it in userAppPackages }
            .maxByOrNull { it.value }

        if (topDataConsumer != null && topDataConsumer.value > (10L * 1024 * 1024)) {
            val formattedSize = storageStatsHelper.formatSize(topDataConsumer.value)
            newEvents.add(
                RecentEventEntity(
                    packageName = topDataConsumer.key,
                    eventType = "DATA_HOG",
                    timestamp = System.currentTimeMillis(),
                    extraInfo = formattedSize
                )
            )
        }

        if (newEvents.isNotEmpty()) {
            recentEventDao.insertEvents(newEvents)
        }
    }

    private suspend fun syncNeedsAttentionFromOS(entities: List<AppInfoEntity>) {
        val now = System.currentTimeMillis()
        val dayMillis = 24L * 60 * 60 * 1000
        val newEvents = mutableListOf<NeedsAttentionEntity>()

        needsAttentionDao.clearAuditEvents()

        entities.filter { !it.isSystemApp }.forEach { app ->

            // 1. Highly Sensitive Access (Special Permissions)
            val specialPerms = packageManagerHelper.getSpecialPermissions(app.packageName)
            if (specialPerms.isNotEmpty()) {
                val permsJoined = specialPerms.joinToString(", ")
                newEvents.add(
                    NeedsAttentionEntity(
                        packageName = app.packageName,
                        eventType = "AUDIT_SPECIAL_ACCESS",
                        timestamp = now,
                        permissionName = permsJoined, // Exact names like OVERLAY, ACCESSIBILITY
                        extraInfo = "App has high-level system control"
                    )
                )
            }

            // 2. Unused & Stale Logic
            val lastUsed = usageStatsHelper.getAppLastUsedTime(app.packageName)
            if (lastUsed != 0L) {
                val daysDiff = ((now - lastUsed) / dayMillis).toInt()

                // --- STALE PERMISSIONS LOGIC ---
                // Pehle check karo kaun-kaun si sensitive permissions hain
                val sensitiveList = mutableListOf<String>()
                if (app.hasLocation) sensitiveList.add("Location")
                if (app.hasCamera) sensitiveList.add("Camera")
                if (app.hasMic) sensitiveList.add("Microphone")
                if (app.hasSms) sensitiveList.add("SMS")
                if (app.hasContacts) sensitiveList.add("Contacts")

                // Agar 30+ days se use nahi hui aur sensitive perms hain
                if (sensitiveList.isNotEmpty() && daysDiff >= 30) {
                    newEvents.add(
                        NeedsAttentionEntity(
                            packageName = app.packageName,
                            eventType = "AUDIT_STALE_PERMS",
                            timestamp = lastUsed,
                            daysUnused = daysDiff,
                            permissionName = sensitiveList.joinToString(", "),
                            extraInfo = "Sensitive access active but app dormant"
                        )
                    )
                }

                // --- UNUSED APPS LOGIC ---
                val unusedType = when {
                    daysDiff >= 90 -> "AUDIT_UNUSED_90"
                    daysDiff >= 60 -> "AUDIT_UNUSED_60"
                    daysDiff >= 30 -> "AUDIT_UNUSED_30"
                    else -> null
                }

                if (unusedType != null) {
                    newEvents.add(
                        NeedsAttentionEntity(
                            packageName = app.packageName,
                            eventType = unusedType,
                            timestamp = lastUsed,
                            daysUnused = daysDiff,
                            extraInfo = "Not used in $daysDiff days"
                        )
                    )
                }
            }
        }

        // Database operations
        if (newEvents.isNotEmpty()) {
            needsAttentionDao.insertEvents(newEvents)
        }

        val currentPackages = entities.map { it.packageName }.toSet()
        needsAttentionDao.deleteRemovedApps(currentPackages.toList())
    }

    override fun getEventsByType(eventType: String): Flow<List<RecentEventEntity>> {
            return recentEventDao.getEventsByType(eventType)
        }

    override fun getNeedsAttentionEventsByType(eventType: String): Flow<List<NeedsAttentionEntity>> {
        return needsAttentionDao.getEventsByType(eventType)
    }

    override fun getTodayNotificationCount(): Flow<Int> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return appNotificationDao.getTotalCountByDate(today).map { it ?: 0 }
    }

    override fun getTodayTotalUnlocks(): Flow<Int> =
        vitalsDao.getVitalsByDate(getTodayMidnight()).map { it?.totalUnlocks ?: 0 }

    override fun getTodayTotalNotifications(): Flow<Int> =
        vitalsDao.getVitalsByDate(getTodayMidnight()).map { it?.totalNotifications ?: 0 }

    override fun getTodayDataUsage(): Flow<Long> =
        vitalsDao.getVitalsByDate(getTodayMidnight()).map { it?.totalDataUsage ?: 0L }

    override fun getSystemUpdateInfo(): Flow<String> =
        vitalsDao.getVitalsByDate(getTodayMidnight()).map { it?.securityPatch ?: "Unknown" }


    private fun formatSecurityPatch(rawDate: String): String {
        if (rawDate.isEmpty()) return "Unknown"

        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val formatter = SimpleDateFormat("MMM yyyy", Locale.getDefault())
            val date = parser.parse(rawDate)
            if (date != null) formatter.format(date) else rawDate
        } catch (e: Exception) {
            rawDate
        }
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