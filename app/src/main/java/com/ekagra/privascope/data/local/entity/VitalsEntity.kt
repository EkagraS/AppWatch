package com.ekagra.privascope.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vitals_table")
data class VitalsEntity(
    @PrimaryKey
    val date: Long,
    val totalScreenTime: Long,
    val totalUnlocks: Int,
    val totalNotifications: Int,
    val totalDataUsage: Long,
    val securityPatch: String
)