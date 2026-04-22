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
    val recentActivity: List<RecentItem>,
    val attentionItems: List<ActivityItem>
)

data class RecentItem(
    val eventType: String,
    val title: String,
    val description: String,
    val isTimeline: Boolean
)

data class ActivityItem(
    val title: String,
    val description: String,
    val iconType: String
)