package com.example.appwatch.domain.repository

import com.example.appwatch.data.local.entity.AppDataUsageEntity
import kotlinx.coroutines.flow.Flow

interface AppDataUsageRepository {
    // Screen par list dikhane ke liye
    fun getUsageByDate(date: String): Flow<List<AppDataUsageEntity>>

    // Dashboard par total number dikhane ke liye
    fun getTotalUsageForDay(date: String): Flow<Long?>

    // Naya data system se uthakar DB mein save karne ke liye
    suspend fun updateUsageData(usage: AppDataUsageEntity)

    // Purana data clear karne ke liye
    suspend fun clearOldData(expiryDate: String)
}