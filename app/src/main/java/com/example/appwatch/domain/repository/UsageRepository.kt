package com.example.appwatch.domain.repository

import com.example.appwatch.data.local.entity.UsageEntity
import com.example.appwatch.domain.model.AppUsage
import com.example.appwatch.domain.model.DashboardSummary
import kotlinx.coroutines.flow.Flow

interface UsageRepository {

    fun getDailyUsage(): Flow<List<AppUsage>>

    fun getTotalScreenTimeToday(): Flow<String>

    fun getWeeklyActivity(): Flow<List<Float>>

    suspend fun saveUsageSnapshot(usage: AppUsage)

    suspend fun syncDailyUsage()

    fun getTopAppForRange(days: Int, limit: Int): Flow<List<AppUsage>>

    fun getTotalStatsForRange(days: Int): Flow<Pair<Long, Int>> // ScreenTime to Unlocks

    fun getActiveStreak(): Flow<String>

    fun getInActiveStreak(): Flow<String>

    fun getHighNoiseApps(limit: Int): Flow<List<UsageEntity>>

    suspend fun getUnlockPace(): Double

    suspend fun getMarathonSession(): Pair<String, Long>?
}
