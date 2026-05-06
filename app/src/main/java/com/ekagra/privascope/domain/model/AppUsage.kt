package com.ekagra.privascope.domain.model

data class AppUsage(
    val packageName: String,
    val appName: String,
    val usageTimeString: String,
    val usagePercentage: Float,
    val lastUsedString: String,
    val appOpenCount: Int
)