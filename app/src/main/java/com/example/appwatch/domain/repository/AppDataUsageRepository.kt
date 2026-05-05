package com.example.appwatch.domain.repository

import com.example.appwatch.data.local.entity.AppDataUsageEntity
import kotlinx.coroutines.flow.Flow

interface AppDataUsageRepository {

    fun getUsageByDate(date: String): Flow<List<AppDataUsageEntity>>

    fun getTotalUsageForDay(date: String): Flow<Long?>

    suspend fun updateUsageData(usage: AppDataUsageEntity)

    suspend fun clearOldData(expiryDate: String)
}