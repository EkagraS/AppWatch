package com.example.appwatch.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "app_usage",
    primaryKeys = ["packageName", "usageDate"]
)
data class UsageEntity(
    val packageName: String,
    val usageDate: Long,
    val appName: String,
    val totalTimeInForeground: Long,
    val lastTimeUsed: Long,
    val appUnlocks: Int = 0,
    val notificationCount: Int = 0,
    val lastEventTimestamp: Long = 0L
)