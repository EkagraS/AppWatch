package com.example.appwatch.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_usage")
data class UsageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val packageName: String,
    val appName: String,
    val totalTimeInForeground: Long, // Duration in milliseconds
    val lastTimeUsed: Long,         // Epoch timestamp
    val usageDate: Long
)