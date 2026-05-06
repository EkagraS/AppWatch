package com.ekagra.privascope.data.repository

import android.util.Log
import com.ekagra.privascope.data.local.dao.VitalsDao
import com.ekagra.privascope.data.local.entity.VitalsEntity
import com.ekagra.privascope.domain.repository.VitalsRepository
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
        try {
            vitalsDao.insertVitals(vitals)
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Failed to save daily vitals", e)
        }
    }

    override suspend fun cleanup(threshold: Long) {
        try {
            vitalsDao.deleteOldVitals(threshold)
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Failed to cleanup vitals", e)
        }
    }
}