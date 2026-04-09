package com.example.appwatch.domain.usecase

import com.example.appwatch.domain.model.PermissionInfo
import com.example.appwatch.domain.repository.PermissionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use Case to fetch the detailed permission audit for a specific app.
 * Used when a user taps an app in the list to see its risks.
 */
class GetPermissionsUseCase @Inject constructor(
    private val repository: PermissionRepository
) {
    /**
     * Returns a list of all permissions requested by the app,
     * highlighting which ones are "Sensitive" or "Dangerous".
     */
    operator fun invoke(packageName: String): Flow<List<PermissionInfo>> {
        return repository.getPermissionsForApp(packageName)
    }
}