package com.example.appwatch.domain.model

data class DashboardSummary(
    val totalApps: Int,
    val highRiskApps: Int,
    val totalScreenTime: String,
    val topUsedApp: String,
    val weeklyUsageData: List<Float> // For the Bar Chart
)
