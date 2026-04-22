package com.example.appwatch.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recent_events",
    indices = [Index(value = ["packageName", "eventType"], unique = true)]
)
data class RecentEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val eventType: String,
    val timestamp: Long,
    val extraInfo: String? = null
)