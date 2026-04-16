package com.example.appwatch.domain.usecase

import com.example.appwatch.domain.model.*
import com.example.appwatch.domain.repository.UsageRepository
import com.example.appwatch.system.AppOpsHelper
import com.example.appwatch.system.PackageManagerHelper
import com.example.appwatch.system.StorageStatsHelper
import com.example.appwatch.system.UsageStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class GetDashboardSummaryUseCase @Inject constructor(
    private val usageRepository: UsageRepository,
    private val packageManagerHelper: PackageManagerHelper,
    private val storageStatsHelper: StorageStatsHelper,
    private val usageStatsHelper: UsageStatsHelper, // Add this
    private val appOpsHelper: AppOpsHelper
) {
    operator fun invoke(): Flow<DashboardSummary> {
        val appsFlow = flow {
            emit(packageManagerHelper.getInstalledAppsMetadata())
        }

        val deviceStorage = storageStatsHelper.getDeviceStorageInfo()
        return combine(
            appsFlow,
            usageRepository.getTotalScreenTimeToday()
        ) { entities, screenTime ->

            val userApps = entities.filter { !it.isSystemApp }
            val attentionItems = mutableListOf<AttentionItem>()
            val highRiskApps = userApps.filter { it.totalPermissions >= 15 }

            val ghostRisks = mutableListOf<AttentionItem>()
            val backgroundActors = mutableListOf<AttentionItem>()
            val dataHogs = mutableListOf<AttentionItem>()

            for (app in userApps) {
                val lastUsed = usageStatsHelper.getAppUsageLastTimestamp(app.packageName)

                // IGNORE apps with 0 timestamp to prevent the 20558 days bug
                val daysUnused = if (lastUsed == 0L) 999L else (System.currentTimeMillis() - lastUsed) / (1000 * 60 * 60 * 24)

                // --- NEW FORMATTING LOGIC START ---
                val daysText = when {
                    lastUsed == 0L -> "Never used"
                    daysUnused == 0L -> "Used today"
                    daysUnused == 1L -> "Used yesterday"
                    daysUnused > 90 -> "90+ days"
                    else -> "Last used $daysUnused days ago"
                }

                val permCount = app.sensitivePermissionsCount
                val permText = if (permCount == 1) "1 sensitive permission" else "$permCount sensitive permissions"

                // 1. GHOST BUCKET
                if (daysUnused >=7 && app.sensitivePermissionsCount > 0) {
                    attentionItems.add(AttentionItem(
                        packageName = app.packageName,
                        appName = app.appName,
                        reason = "Unused for $daysText with $permText", // Using the new strings here
                        severity = "Medium"
                    ))
                }
// 2. BACKGROUND ACTOR (Improved Logic)
                val lastCamera = appOpsHelper.getLastPermissionAccess(app.packageName, "CAMERA")
                val lastMic = appOpsHelper.getLastPermissionAccess(app.packageName, "RECORD_AUDIO")
                val latestSensorAccess = maxOf(lastCamera, lastMic)

// If sensor was used in the last 24 hours
                val isRecentSensorUse = (System.currentTimeMillis() - latestSensorAccess) < (24 * 60 * 60 * 1000)

                if (latestSensorAccess != 0L && isRecentSensorUse) {
                    attentionItems.add(AttentionItem(
                        packageName = app.packageName,
                        appName = app.appName,
                        reason = "Recently accessed Camera/Mic",
                        severity = "High"
                    ))
                }
                
                // 3. DATA HOG / NEW INSTALL BUCKET
                val isNew = System.currentTimeMillis() - app.installedAt < 7L * 24 * 60 * 60 * 1000
                if (isNew && app.totalPermissions >= 10) {
                    dataHogs.add(AttentionItem(
                        app.packageName, app.appName,
                        "New app requesting broad access", "Low"
                    ))
                }
            }

            // --- SELECT ONE OF EACH ---
            val finalAttentionList = mutableListOf<AttentionItem>()

            // Priority 1: High Risk (Background Actor)
            backgroundActors.firstOrNull()?.let { finalAttentionList.add(it) }

            // Priority 2: Medium Risk (Ghost)
            ghostRisks.firstOrNull { it.packageName !in finalAttentionList.map { a -> a.packageName } }
                ?.let { finalAttentionList.add(it) }

            // Priority 3: Data Hog or another High Risk
            if (finalAttentionList.size < 3) {
                dataHogs.firstOrNull { it.packageName !in finalAttentionList.map { a -> a.packageName } }
                    ?.let { finalAttentionList.add(it) }
            }

            // Fill remaining slots if any bucket was empty
            val fallbacks = (backgroundActors + ghostRisks + dataHogs)
                .distinctBy { it.packageName }
                .filter { it.packageName !in finalAttentionList.map { a -> a.packageName } }

            finalAttentionList.addAll(fallbacks.take(3 - finalAttentionList.size))

            val newInstalls = userApps.filter {
                System.currentTimeMillis() - it.installedAt < 7L * 24 * 60 * 60 * 1000
            }

            val attention = userApps
                .sortedByDescending { it.totalPermissions }
                .take(3)
                .map {
                    AttentionItem(
                        packageName = it.packageName,
                        appName = it.appName,
                        reason = "Access to ${it.totalPermissions} permissions",
                        severity = when {
                            it.totalPermissions >= 15 -> "High"
                            it.totalPermissions >= 10 -> "Medium"
                            else -> "Low"
                        }
                    )
                }

            DashboardSummary(
                totalApps = userApps.size,
                highRiskApps = highRiskApps.size,
                totalScreenTime = screenTime,
                locationAppsCount = packageManagerHelper.getAppsWithPermission("ACCESS_FINE_LOCATION"),
                cameraAppsCount = packageManagerHelper.getAppsWithPermission("CAMERA"),
                micAppsCount = packageManagerHelper.getAppsWithPermission("RECORD_AUDIO"),
                contactAppsCount = packageManagerHelper.getAppsWithPermission("READ_CONTACTS"),
                phoneAppsCount = packageManagerHelper.getAppsWithPermission("READ_CALL_NUMBERS"),
                SmsAppsCount = packageManagerHelper.getAppsWithPermission("READ_SMS"),
                usedStorage = storageStatsHelper.formatSize(deviceStorage.freeBytes),
                totalStorage = storageStatsHelper.formatSize(deviceStorage.totalBytes),
                attentionItems = finalAttentionList,
                recentActivity = listOf(
                    ActivityItem(
                        "New Installations",
                        "${newInstalls.size} apps installed this week",
                        "INSTALL"
                    ),
                    ActivityItem(
                        "Active Apps",
                        "${userApps.size} user apps found",
                        "ACTIVE"
                    ),
                    ActivityItem(
                        "High Risk",
                        "${highRiskApps.size} apps need attention",
                        "INACTIVE"
                    )
                )
            )
        }.flowOn(Dispatchers.IO)
    }
}