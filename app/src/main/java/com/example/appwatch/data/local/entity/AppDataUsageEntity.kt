package com.example.appwatch.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "app_data_usage",
    primaryKeys = ["packageName", "date"]
)
data class AppDataUsageEntity(
    val packageName: String,
    val date: String, // Format: yyyy-MM-dd
    val mobileUsageBytes: Long = 0L,
    val wifiUsageBytes: Long = 0L
)