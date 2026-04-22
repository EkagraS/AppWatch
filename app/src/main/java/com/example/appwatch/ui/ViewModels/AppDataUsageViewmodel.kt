package com.example.appwatch.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appwatch.data.local.entity.AppDataUsageEntity
import com.example.appwatch.data.local.entity.AppNotificationEntity
import com.example.appwatch.data.manager.DataUsageManager
import com.example.appwatch.domain.repository.AppDataUsageRepository
import com.example.appwatch.domain.repository.AppNotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class TodayUIState(
    val isLoading: Boolean = false,
    val notifications: List<AppNotificationEntity> = emptyList(),
    val dataUsage: List<AppDataUsageEntity> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class AppDataUsageViewmodel @Inject constructor(
    private val notificationRepo: AppNotificationRepository,
    private val dataUsageRepo: AppDataUsageRepository,
    private val dataUsageManager: DataUsageManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayUIState())
    val uiState = _uiState.asStateFlow()

    private val todayDate = LocalDate.now().toString()

    init {
        refreshStats()
    }

    fun refreshStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // 1. System se fresh data fetch karo (On-demand)
            try {
                dataUsageManager.trackTodayUsage()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Data fetch failed: ${e.message}") }
            }

            // 2. Room se dono flows ko combine karke UI ko update karo
            combine(
                notificationRepo.getNotificationsByDate(todayDate),
                dataUsageRepo.getUsageByDate(todayDate)
            ) { notifications, usage ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        notifications = notifications,
                        dataUsage = usage
                    )
                }
            }.collect { }
        }
    }
}