package com.example.appwatch.domain.model

data class AppInfo(
    val packageName: String,
    val appName: String,
    val iconUri: String? = null, // Path to the icon
    val isSystemApp: Boolean,
    val totalPermissions: Int,
    val riskLevel: RiskLevel = RiskLevel.LOW
)

enum class RiskLevel { LOW, MEDIUM, HIGH }