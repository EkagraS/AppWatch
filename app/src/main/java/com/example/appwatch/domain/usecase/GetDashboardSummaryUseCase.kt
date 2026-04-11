package com.example.appwatch.domain.usecase

import com.example.appwatch.domain.model.*
import com.example.appwatch.domain.repository.UsageRepository
import com.example.appwatch.system.PackageManagerHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetDashboardSummaryUseCase @Inject constructor(
    private val usageRepository: UsageRepository,
    private val packageManagerHelper: PackageManagerHelper
) {
    operator fun invoke(): Flow<DashboardSummary> {
        val appsFlow = flow {
            emit(packageManagerHelper.getInstalledAppsMetadata())
        }

        return combine(
            appsFlow,
            usageRepository.getTotalScreenTimeToday()
        ) { entities, screenTime ->

            val userApps = entities.filter { !it.isSystemApp }
            val highRiskApps = userApps.filter { it.totalPermissions >= 15 }

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
                attentionItems = attention,
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
        }
    }
}