package com.ekagra.privascope.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "permission_access",
    indices = [Index(value = ["packageName", "permissionName"], unique = true)]
)
data class PermissionAccessEntity (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val permissionName: String,
    val accessTimestamp: Long
)