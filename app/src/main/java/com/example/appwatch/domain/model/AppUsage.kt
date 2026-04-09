package com.example.appwatch.domain.model

data class AppUsage(
    val packageName: String,
    val appName: String,
    val usageTimeString: String, // e.g., "2h 15m"
    val usagePercentage: Float,  // For the progress bar
    val lastUsedString: String   // e.g., "Used 5 mins ago"
)