package com.ekagra.privascope.domain.model

import androidx.room.PrimaryKey

/**
 * Clean UI model for an individual App.
 */
data class AppInfo(
    @PrimaryKey val id: Int,
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val totalPermissions: Int,
    val installedAt: Long = 0L
)