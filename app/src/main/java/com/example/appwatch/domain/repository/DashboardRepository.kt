package com.example.appwatch.domain.repository

import com.example.appwatch.data.local.entity.NeedsAttentionEntity
import com.example.appwatch.data.local.entity.RecentEventEntity
import com.example.appwatch.domain.model.DashboardSummary
import kotlinx.coroutines.flow.Flow

interface DashboardRepository {
    fun getDashboardSummaryFlow(): Flow<DashboardSummary>

    suspend fun refreshAllData()

    fun getTodayTotalUnlocks(): Flow<Int>

    fun getTodayTotalNotifications(): Flow<Int>

    fun getTodayDataUsage(): Flow<Long>

    fun getSystemUpdateInfo(): Flow<String>

    fun getEventsByType(eventType: String): Flow<List<RecentEventEntity>>

    fun getNeedsAttentionEventsByType(eventType: String): Flow<List<NeedsAttentionEntity>>

}