package com.example.appwatch.domain.repository

import com.example.appwatch.domain.model.AppUsage
import com.example.appwatch.domain.model.DashboardSummary
import kotlinx.coroutines.flow.Flow

/**
 * Contract for tracking and retrieving app usage behavior and screen time.
 */
interface UsageRepository {
    /**
     * Returns the ranked list of app usage for the current day.
     */
    fun getDailyUsage(): Flow<List<AppUsage>>

    /**
     * Provides the aggregated stats needed for the Dashboard "Hero" cards.
     */
    fun getTotalScreenTimeToday(): Flow<String>

    /**
     * Provides 7-day historical data for the activity bar chart.
     */
    fun getWeeklyActivity(): Flow<List<Float>>

    /**
     * Persists a newly captured usage snapshot into the database.
     */
    suspend fun saveUsageSnapshot(usage: AppUsage)

    suspend fun syncDailyUsage()

}