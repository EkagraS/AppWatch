package com.example.appwatch.domain.repository

import com.example.appwatch.data.local.entity.VitalsEntity

interface VitalsRepository {

    fun getLast14DaysVitals(today: Long): kotlinx.coroutines.flow.Flow<List<VitalsEntity>>

    suspend fun saveDailyVitals(vitals: VitalsEntity)

    suspend fun cleanup(threshold: Long)
}