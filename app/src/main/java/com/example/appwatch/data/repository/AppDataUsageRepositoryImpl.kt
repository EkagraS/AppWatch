package com.example.appwatch.data.repository

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
        usageDao.insertOrUpdateUsage(usage)
    }

    override suspend fun clearOldData(expiryDate: String) {
        usageDao.deleteOldUsageData(expiryDate)
    }
}