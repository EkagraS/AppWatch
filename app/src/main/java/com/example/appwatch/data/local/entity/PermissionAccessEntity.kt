package com.example.appwatch.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "permission_access")
data class PermissionAccessEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val packageName: String,
    val permissionName: String,
    val accessTimestamp: Long,
    val isGranted: Boolean
)