package com.example.appwatch.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appwatch.domain.model.DashboardSummary
import com.example.appwatch.domain.usecase.GetDashboardSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import javax.inject.Inject

// --- State Interface ---
sealed interface DashboardUiState {
    object Loading : DashboardUiState
    data class Success(val data: DashboardSummary) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getDashboardSummaryUseCase: GetDashboardSummaryUseCase
) : ViewModel() {

    // Direct Flow Transformation: Summary -> UiState
    val uiState: StateFlow<DashboardUiState> = getDashboardSummaryUseCase()
        .map { summary ->
            if (summary != null) {
                DashboardUiState.Success(summary)
            } else {
                DashboardUiState.Loading
            }
        }
        .catch { e ->
            emit(DashboardUiState.Error(e.message ?: "Something went wrong"))
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardUiState.Loading
        )
}