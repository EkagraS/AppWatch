package com.example.appwatch.data.repository

import com.example.appwatch.data.local.dao.UsageDao
import com.example.appwatch.data.local.entity.UsageEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UsageRepositoryImpl@Inject constructor(private val usageDao: UsageDao) {
    suspend fun saveUsageData(usage: UsageEntity) {
        usageDao.insertUsage(usage)
    }
    fun getDailyUsage(date: Long): Flow<List<UsageEntity>> {
        return usageDao.getUsageByDate(date)
    }
    fun getTotalDailyScreenTime(date: Long): Flow<Long?> {
        return usageDao.getTotalScreenTime(date)
    }
}