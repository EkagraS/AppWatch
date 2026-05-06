package com.ekagra.privascope.data.local.entity

import androidx.room.Entity

@Entity(tableName = "needs_attention",
    primaryKeys = ["packageName", "eventType"]
)
data class NeedsAttentionEntity(
    val packageName: String,
    val eventType: String,
    val timestamp: Long,
    val daysUnused: Int? = null,
    val permissionName: String? = null,
    val extraInfo: String? = null 
)