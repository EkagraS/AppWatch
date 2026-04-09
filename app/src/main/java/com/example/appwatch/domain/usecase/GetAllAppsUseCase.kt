package com.example.appwatch.domain.usecase

import com.example.appwatch.domain.model.AppInfo
import com.example.appwatch.domain.repository.AppInfoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use Case to retrieve the list of installed applications.
 * Handles sorting logic to separate system apps or prioritize high-risk items.
 */
class GetAllAppsUseCase @Inject constructor(
    private val repository: AppInfoRepository
) {
    /**
     * Executes the use case.
     * @param sortByRisk If true, apps with higher permission counts appear first.
     */
    operator fun invoke(sortByRisk: Boolean = false): Flow<List<AppInfo>> {
        return repository.getAllApps(sortByRisk)
    }

    /**
     * Searches for apps by name.
     */
    fun search(query: String): Flow<List<AppInfo>> {
        return repository.searchApps(query)
    }
}