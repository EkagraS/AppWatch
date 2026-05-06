package com.ekagra.privascope.domain.repository

import com.ekagra.privascope.data.local.entity.NeedsAttentionEntity
import com.ekagra.privascope.data.local.entity.RecentEventEntity
import com.ekagra.privascope.domain.model.DashboardSummary
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

    fun getTodayNotificationCount(): Flow<Int>
}