package com.example.appwatch.data.repository

import com.example.appwatch.data.local.dao.VitalsDao
import com.example.appwatch.data.local.entity.VitalsEntity
import com.example.appwatch.domain.repository.VitalsRepository
import jakarta.inject.Inject

class VitalsRepositoryImpl @Inject constructor(
    private val vitalsDao: VitalsDao
) : VitalsRepository {

    override fun getLast14DaysVitals(today: Long): kotlinx.coroutines.flow.Flow<List<VitalsEntity>> {
        val fourteenDaysInMillis = 14L * 24 * 60 * 60 * 1000
        val threshold = today - fourteenDaysInMillis
        return vitalsDao.getVitalsRange(threshold)
    }

    override suspend fun saveDailyVitals(vitals: VitalsEntity) {
        vitalsDao.insertVitals(vitals)
    }

    override suspend fun cleanup(threshold: Long) {
        vitalsDao.deleteOldVitals(threshold)
    }
}