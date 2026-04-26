package com.example.appwatch.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appwatch.data.local.entity.NeedsAttentionEntity
import com.example.appwatch.data.local.entity.RecentEventEntity
import com.example.appwatch.domain.model.DashboardSummary
import com.example.appwatch.domain.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val summary: DashboardSummary? = null,
    val isLoadingFromRoom: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    val totalUnlocksToday: StateFlow<Int> = dashboardRepository.getTodayTotalUnlocks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // 5 seconds baad inactive
            initialValue = 0
        )

    val totalNotificationsToday: StateFlow<Int> = dashboardRepository.getTodayTotalNotifications()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val totalDataUsageBytes: StateFlow<Long> = dashboardRepository.getTodayDataUsage()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val systemUpdate: StateFlow<String> = dashboardRepository.getSystemUpdateInfo()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "Loading..."
        )

    fun getEventsByType(eventType: String): Flow<List<RecentEventEntity>> {
        return dashboardRepository.getEventsByType(eventType)
    }

    fun getNeedsAttentionEventsByType(eventType: String): Flow<List<NeedsAttentionEntity>> {
        return dashboardRepository.getNeedsAttentionEventsByType(eventType)
    }

    private val _auditFilter = MutableStateFlow("30") // Unused apps ke liye default (30, 60, 90)
    val auditFilter: StateFlow<String> = _auditFilter.asStateFlow()

    fun updateUnusedFilter(days: String) {
        _auditFilter.value = days
    }


    fun formatDataUsage(bytes: Long): String {
        if (bytes == 0L) return "0 MB"
        val megabytes = bytes / (1024 * 1024).toDouble()
        return if (megabytes > 1024) {
            String.format("%.1f GB", megabytes / 1024)
        } else {
            String.format("%.0f MB", megabytes)
        }
    }
    init {
        // Step 1: Start observing Room
        viewModelScope.launch {
            dashboardRepository.getDashboardSummaryFlow()
                .catch { e -> _uiState.update { it.copy(error = e.message, isLoadingFromRoom = false) } }
                .collect { summary ->
                    _uiState.update {
                        it.copy(summary = summary, isLoadingFromRoom = summary.totalApps == 0)
                    }
                }
        }
        // Step 2: Refresh system data & cache
        refreshInBackground()
    }

    fun refreshInBackground() {
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            dashboardRepository.refreshAllData()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }
}