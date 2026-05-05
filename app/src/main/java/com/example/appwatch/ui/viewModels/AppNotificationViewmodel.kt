package com.example.appwatch.ui.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appwatch.data.local.entity.AppNotificationEntity
import com.example.appwatch.domain.repository.AppNotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class TodayActivityState(
    val isLoading: Boolean = false,
    val notificationList: List<AppNotificationEntity> = emptyList(),
    val totalOpened: Int = 0,
    val totalDismissed: Int = 0,
    val errorMessage: String? = null
)

@HiltViewModel
class AppNotificationViewmodel @Inject constructor(
    private val notificationRepo: AppNotificationRepository
) : ViewModel() {

    private val todayDate = LocalDate.now().toString()

    private val _uiState = MutableStateFlow(TodayActivityState())
    val uiState: StateFlow<TodayActivityState> = _uiState.asStateFlow()

    private var collectionJob: Job? = null

    init {
        loadTodayStats()
    }

    private fun loadTodayStats() {
        collectionJob?.cancel()
        collectionJob =viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                notificationRepo.getNotificationsByDate(todayDate)
                    .catch { e ->
                        // Reason: Flow/Database related crash yahan pakda jayega
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = "Database error: ${e.localizedMessage}")
                        }
                    }
                    .collect { list ->
                        val opened = list.sumOf { it.openedCount }
                        val dismissed = list.sumOf { it.dismissedCount }

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                notificationList = list,
                                totalOpened = opened,
                                totalDismissed = dismissed
                            )
                        }
                    }
            } catch (e: CancellationException) {
                // Reason: Coroutine cancellation ko block nahi karna warna deadlock ho jayega
                throw e
            } catch (e: Exception) {
                // Reason: Koi bhi unexpected crash (jaise OS restriction) yahan catch hogi
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Unexpected error: ${e.localizedMessage}")
                }
            }
        }
    }
}