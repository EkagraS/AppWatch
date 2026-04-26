package com.example.appwatch.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "needs_attention",
    primaryKeys = ["packageName", "eventType"]
)
data class NeedsAttentionEntity(
    val packageName: String,
    val eventType: String,
    val timestamp: Long,
    val extraInfo: String? = null 
)