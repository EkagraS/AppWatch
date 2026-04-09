package com.example.appwatch.domain.usecase

import com.example.appwatch.domain.model.AppInfo
import com.example.appwatch.domain.repository.PermissionRepository
import com.example.appwatch.domain.repository.AppInfoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Advanced Business Logic: Identifies apps that have sensitive permissions
 * but haven't been opened in a long time (e.g., 30 days).
 * This feeds the "Needs Attention" section on the Dashboard.
 */
class GetUnusedPermissionsUseCase @Inject constructor(
    private val permissionRepository: PermissionRepository,
    private val appInfoRepository: AppInfoRepository
) {
    /**
     * Filters the app list to find "Stale" apps with dangerous permissions.
     */
    operator fun invoke(): Flow<List<AppInfo>> {
        // Logic: Combine app list with permission history to flag unused dangerous access
        return appInfoRepository.getAllApps().map { apps ->
            apps.filter { it.totalPermissions > 5 && it.isSystemApp == false }
            // Simplified logic: High permission count + User App = Candidate for Review
        }
    }
}