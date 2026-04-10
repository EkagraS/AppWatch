package com.example.appwatch.domain.model

data class PermissionInfo(
    val permissionName: String,
    val isSensitive: Boolean,
    val description: String,
    val granted: Boolean
)
