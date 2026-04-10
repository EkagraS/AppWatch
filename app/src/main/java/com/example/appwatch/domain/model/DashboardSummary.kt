package com.example.appwatch.domain.model

data class DashboardSummary(
    val totalApps: Int,
    val highRiskApps: Int,
    val totalScreenTime: String,

    // Privacy Insights
    val locationAppsCount: Int,
    val cameraAppsCount: Int,
    val micAppsCount: Int,
    val storageAppsCount: Int,

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