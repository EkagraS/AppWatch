package com.example.appwatch.domain.model

import android.graphics.drawable.Drawable

/**
 * Clean UI model for an individual App.
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    val iconDrawable: Drawable? = null,
    val isSystemApp: Boolean,
    val totalPermissions: Int,
    val installedAt: Long = 0L,
    val riskLevel: RiskLevel = RiskLevel.LOW
)

enum class RiskLevel { LOW, MEDIUM, HIGH }
