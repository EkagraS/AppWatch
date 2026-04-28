package com.example.appwatch.domain.model

data class SensitiveAccess(
    val id: Int = 0,
    val packageName: String,
    val appName: String,
    val accessType: String,
    val timestampString: String,
    val isRealTime: Boolean
)