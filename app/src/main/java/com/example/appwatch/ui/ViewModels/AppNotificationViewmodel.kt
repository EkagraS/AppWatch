package com.example.appwatch.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appwatch.data.local.entity.AppNotificationEntity
import com.example.appwatch.domain.repository.AppNotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

// 🔴 UI State ab isi file mein hai
data class TodayActivityState(
    val isLoading: Boolean = false,
    val notificationList: List<AppNotificationEntity> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class AppNotificationViewmodel @Inject constructor(
    private val notificationRepo: AppNotificationRepository
) : ViewModel() {

    private val todayDate = LocalDate.now().toString()

    private val _uiState = MutableStateFlow(TodayActivityState())
    val uiState: StateFlow<TodayActivityState> = _uiState.asStateFlow()

    init {
        loadTodayStats()
    }

    private fun loadTodayStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // DB se live flow collect karenge
            notificationRepo.getNotificationsByDate(todayDate)
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = e.message)
                    }
                }
                .collect { list ->
                    _uiState.update {
                        it.copy(isLoading = false, notificationList = list)
                    }
                }
        }
    }

    fun refreshStats() {
        loadTodayStats()
    }
}