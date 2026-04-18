package com.example.appwatch.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "app_usage",
    primaryKeys = ["packageName", "usageDate"] // Composite Key
)
data class UsageEntity(
    val packageName: String,
    val usageDate: Long, // normalized to start of day (12 AM)
    val appName: String,
    val totalTimeInForeground: Long,
    val lastTimeUsed: Long,
    val appUnlocks: Int = 0 // NAYA: Unlocks store karne ke liye
)