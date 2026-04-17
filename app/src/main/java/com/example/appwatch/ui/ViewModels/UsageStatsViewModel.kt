package com.example.appwatch.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appwatch.data.local.dao.UsageDao
import com.example.appwatch.data.local.entity.UsageEntity
import com.example.appwatch.domain.usecase.GetAppUsageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class UsageStatsViewModel @Inject constructor(
    private val getAppUsageUseCase: GetAppUsageUseCase,
    private val usageDao: UsageDao
) : ViewModel() {

    val dailyUsageList = getAppUsageUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklyChartData = getAppUsageUseCase.getWeeklyStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Returns raw UsageEntity list for a specific day from Room
    suspend fun getUsageForDay(dateTimestamp: Long): List<UsageEntity> {
        return usageDao.getUsageByDate(dateTimestamp).firstOrNull() ?: emptyList()
    }
}