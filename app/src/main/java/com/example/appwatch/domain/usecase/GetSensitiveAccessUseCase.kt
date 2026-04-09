package com.example.appwatch.domain.usecase

import com.example.appwatch.domain.model.SensitiveAccess
import com.example.appwatch.domain.repository.PermissionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use Case to drive the "Privacy Insights" and "Recent Activity" timelines.
 */
class GetSensitiveAccessUseCase @Inject constructor(
    private val repository: PermissionRepository
) {
    /**
     * Returns a flow of events where apps accessed Camera, Microphone, or Location.
     */
    operator fun invoke(): Flow<List<SensitiveAccess>> {
        return repository.getSensitiveAccessHistory()
    }
}