package com.example.appwatch.domain.usecase

import com.example.appwatch.domain.model.DashboardSummary
import com.example.appwatch.domain.repository.UsageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use Case that aggregates data from various sources to populate
 * the Dashboard "Hero" cards (Total Apps, Permissions, Screen Time).
 */
class GetDashboardSummaryUseCase @Inject constructor(
    private val usageRepository: UsageRepository
) {
    /**
     * Returns a Flow containing the combined summary data for the Dashboard.
     */
    operator fun invoke(): Flow<DashboardSummary> {
        return usageRepository.getDashboardSummary()
    }
}