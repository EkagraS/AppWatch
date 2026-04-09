package com.example.appwatch.domain.usecase

import com.example.appwatch.domain.model.AppUsage
import com.example.appwatch.domain.repository.UsageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use Case to retrieve screen time statistics for the Usage Stats screen.
 */
class GetAppUsageUseCase @Inject constructor(
    private val repository: UsageRepository
) {
    /**
     * Returns a flow of ranked app usage for the current day.
     */
    operator fun invoke(): Flow<List<AppUsage>> {
        return repository.getDailyUsage()
    }

    /**
     * Returns historical screen time data for the weekly bar chart.
     */
    fun getWeeklyStats(): Flow<List<Float>> {
        return repository.getWeeklyActivity()
    }
}