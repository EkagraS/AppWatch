package com.example.appwatch.data.repository

import android.util.Log
import com.example.appwatch.data.local.dao.AppDataUsageDao
import com.example.appwatch.data.local.entity.AppDataUsageEntity
import com.example.appwatch.domain.repository.AppDataUsageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AppDataUsageRepositoryImpl @Inject constructor(
    private val usageDao: AppDataUsageDao
) : AppDataUsageRepository {

    override fun getUsageByDate(date: String): Flow<List<AppDataUsageEntity>> {
        return usageDao.getUsageByDate(date)
    }

    override fun getTotalUsageForDay(date: String): Flow<Long?> {
        return usageDao.getTotalUsageForDay(date)
    }

    override suspend fun updateUsageData(usage: AppDataUsageEntity) {
        try {
            usageDao.insertOrUpdateUsage(usage)
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Failed to update usage data", e)
        }
    }

    override suspend fun clearOldData(expiryDate: String) {
        try {
            usageDao.deleteOldUsageData(expiryDate)
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Failed to clear old usage data", e)
        }
    }
}