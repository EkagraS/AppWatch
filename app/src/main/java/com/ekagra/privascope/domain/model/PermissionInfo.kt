package com.ekagra.privascope.domain.model

data class PermissionInfo(
    val permissionName: String,
    val isSensitive: Boolean,
    val description: String,
    val granted: Boolean
)
