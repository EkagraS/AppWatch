package com.example.appwatch.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appwatch.data.local.dao.UsageDao
import com.example.appwatch.data.local.entity.UsageEntity
import com.example.appwatch.domain.model.AppUsage
import com.example.appwatch.domain.repository.UsageRepository
import com.example.appwatch.domain.usecase.GetAppUsageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    var marathonAppName by mutableStateOf("None")
    var marathonTime by mutableStateOf("0m")
    var unlockPace by mutableStateOf(0.0)
    var isLoading by mutableStateOf(true)

    // 2. The "Clean" Seven Day State
    // UI ab isse direct use karegi, loop nahi chalayegi
    private val _allDaysUsage = MutableStateFlow<Map<Int, List<AppUsage>>>(emptyMap())
    val allDaysUsage: StateFlow<Map<Int, List<AppUsage>>> = _allDaysUsage.asStateFlow()

    // 3. Existing Flows (Kept as is for your components)
    val dailyUsageList = getAppUsageUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklyChartData = getAppUsageUseCase.getWeeklyStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayActiveStreak = usageRepository.getActiveStreak()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "00:00 - 00:00")

    val todayInactiveStreak = usageRepository.getInActiveStreak()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "00:00 - 00:00")
    // 2. Weekly Top App

    val top3AppsWeekly = usageRepository.getTopAppForRange(days = 7, limit = 3)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<AppUsage>())

    val top3AppsMonthly = usageRepository.getTopAppForRange(days = 30, limit = 3)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<AppUsage>())

    val highNoiseApps = usageRepository.getHighNoiseApps(limit = 5)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    init {
        refreshAllData()
    }

    fun refreshAllData() {
        if (allDaysUsage.value.isNotEmpty()) {
            isLoading = false
            return
        }
        viewModelScope.launch {
            isLoading = true
            usageRepository.syncDailyUsage()
            val pace = usageRepository.getUnlockPace()
            val marathon = usageRepository.getMarathonSession()

            unlockPace = pace
            marathonAppName = marathon?.first ?: "None"
            marathonTime = formatDuration(marathon?.second ?: 0L)

            fetchAllDaysData()

            isLoading = false
        }
    }

    private suspend fun fetchAllDaysData() {
        val result = mutableMapOf<Int, List<AppUsage>>()
        for (i in 0..6) {
            val daysAgo = 6 - i
            val timestamp = getStartOfDay(daysAgo)
            result[i] = getUsageForDayInternal(timestamp)
        }
        _allDaysUsage.value = result
    }

    private suspend fun getUsageForDayInternal(dateTimestamp: Long): List<AppUsage> {
        val entities = usageDao.getUsageByDate(dateTimestamp).firstOrNull() ?: emptyList()
        val total = entities.sumOf { it.totalTimeInForeground }.toFloat().coerceAtLeast(1f)

        return entities
            .filter { it.totalTimeInForeground >= 60000 } // 1 min filter
            .sortedByDescending { it.totalTimeInForeground }
            .map { entity ->
                AppUsage(
                    packageName = entity.packageName,
                    appName = entity.appName,
                    usageTimeString = formatDuration(entity.totalTimeInForeground),
                    usagePercentage = entity.totalTimeInForeground / total,
                    appOpenCount = entity.appUnlocks,
                    lastUsedString = "Active"
                )
            }
    }

    private fun getStartOfDay(daysAgo: Int): Long {
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -daysAgo)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
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
                    appOpenCount = entity.appUnlocks,
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