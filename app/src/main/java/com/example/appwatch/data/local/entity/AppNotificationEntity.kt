package com.example.appwatch.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "app_notification_stats",
    indices = [Index(value = ["packageName", "date"], unique = true)]
)
data class AppNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val date: String,
    val count: Int,
    val lastUpdated: Long = System.currentTimeMillis()
)