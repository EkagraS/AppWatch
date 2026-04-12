package com.example.appwatch.domain.model

data class DashboardSummary(
    val totalApps: Int,
    val highRiskApps: Int,
    val usedStorage: String = "-- GB",
    val totalStorage: String = "-- GB",
    val totalScreenTime: String,

    // Privacy Insights
    val locationAppsCount: Int,
    val cameraAppsCount: Int,
    val micAppsCount: Int,
    val contactAppsCount: Int,
    val phoneAppsCount: Int,
    val SmsAppsCount: Int,


    // Dynamic Lists
    val attentionItems: List<AttentionItem>,
    val recentActivity: List<ActivityItem>
)

data class AttentionItem(
    val packageName: String,
    val appName: String,
    val reason: String,
    val severity: String
)

data class ActivityItem(
    val title: String,
    val description: String,
    val iconType: String // e.g., "INSTALL", "ACTIVE", "INACTIVE"
)