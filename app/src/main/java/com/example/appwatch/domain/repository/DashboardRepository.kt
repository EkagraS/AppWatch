package com.example.appwatch.domain.repository

import com.example.appwatch.domain.model.DashboardSummary
import kotlinx.coroutines.flow.Flow

interface DashboardRepository {
    fun getDashboardSummaryFlow(): Flow<DashboardSummary>
    suspend fun refreshAllData()
}