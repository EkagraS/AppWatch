package com.example.appwatch.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appwatch.data.local.dao.UsageDao
import com.example.appwatch.data.local.entity.UsageEntity
import com.example.appwatch.domain.model.AppUsage
import com.example.appwatch.domain.repository.UsageRepository
import com.example.appwatch.domain.usecase.GetAppUsageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class UsageStatsViewModel @Inject constructor(
    private val getAppUsageUseCase: GetAppUsageUseCase,
    private val usageRepository: UsageRepository,
    private val usageDao: UsageDao
) : ViewModel() {

    val dailyUsageList = getAppUsageUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklyChartData = getAppUsageUseCase.getWeeklyStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            // Sync first, THEN Room flow auto-updates UI
            usageRepository.syncDailyUsage()
        }
    }

    // Returns AppUsage directly — no system calls in screen
    suspend fun getUsageForDay(dateTimestamp: Long): List<AppUsage> {
        val entities = usageDao.getUsageByDate(dateTimestamp).firstOrNull() ?: emptyList()
        val total = entities.sumOf { it.totalTimeInForeground }.toFloat().coerceAtLeast(1f)
        return entities
            .filter { it.totalTimeInForeground >= 60000 }
            .sortedByDescending { it.totalTimeInForeground }
            .map { entity ->
                AppUsage(
                    packageName = entity.packageName,
                    appName = entity.appName,
                    usageTimeString = formatDuration(entity.totalTimeInForeground),
                    usagePercentage = entity.totalTimeInForeground / total,
                    lastUsedString = "Active"
                )
            }
        getTodayUsage()
    }

    // Today also comes from Room after sync
    suspend fun getTodayUsage(): List<AppUsage> {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return getUsageForDay(today)
    }

    private fun formatDuration(millis: Long): String {
        val hours = millis / (1000 * 60 * 60)
        val minutes = (millis / (1000 * 60)) % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }
}