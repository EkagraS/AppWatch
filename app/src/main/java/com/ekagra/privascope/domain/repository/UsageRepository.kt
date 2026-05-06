package com.ekagra.privascope.domain.repository

import com.ekagra.privascope.data.local.entity.UsageEntity
import com.ekagra.privascope.domain.model.AppUsage
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

    suspend fun getUnlockPace(): Int

    suspend fun getMarathonSession(): Pair<String, Long>?
}
