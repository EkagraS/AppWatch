package com.example.appwatch.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_info")
data class AppInfoEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val totalPermissions: Int,
    val sensitivePermissionsCount: Int,
    val isSystemApp: Boolean,
    val installedAt: Long,
    val lastUpdatedAt: Long = System.currentTimeMillis()
)