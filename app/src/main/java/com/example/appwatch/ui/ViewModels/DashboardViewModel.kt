package com.example.appwatch.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    init {
        // Step 1: Start observing Room
        viewModelScope.launch {
            dashboardRepository.getDashboardSummaryFlow()
                .catch { e -> _uiState.update { it.copy(error = e.message, isLoadingFromRoom = false) } }
                .collect { summary ->
                    _uiState.update { it.copy(summary = summary, isLoadingFromRoom = summary.totalApps == 0) }
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