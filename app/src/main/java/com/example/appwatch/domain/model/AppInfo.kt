package com.example.appwatch.domain.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val appName: String,
    val iconDrawable: Drawable? = null,
    val isSystemApp: Boolean,
    val totalPermissions: Int,
    val riskLevel: RiskLevel = RiskLevel.LOW
)

enum class RiskLevel { LOW, MEDIUM, HIGH }