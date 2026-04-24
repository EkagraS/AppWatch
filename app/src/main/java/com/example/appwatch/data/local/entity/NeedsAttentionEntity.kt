package com.example.appwatch.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "needs_attention")
data class NeedsAttentionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val eventType: String,
    val timestamp: Long,
    val extraInfo: String? = null 
)