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
    val lastUpdatedAt: Long = System.currentTimeMillis(),

    val appSizeBytes: Long = 0L,
    val dataSizeBytes: Long = 0L,
    val cacheSizeBytes: Long = 0L,
    val totalSizeBytes: Long = 0L,
    val storageLastUpdated: Long = 0L
)