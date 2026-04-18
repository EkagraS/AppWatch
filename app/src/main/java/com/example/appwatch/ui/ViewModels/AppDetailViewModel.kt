package com.example.appwatch.presentation.viewmodel

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appwatch.data.local.dao.PermissionAccessDao
import com.example.appwatch.data.local.dao.UsageDao
import com.example.appwatch.domain.model.AppInfo
import com.example.appwatch.domain.model.RiskLevel
import com.example.appwatch.domain.repository.AppInfoRepository
import com.example.appwatch.domain.repository.PermissionRepository
import com.example.appwatch.domain.repository.UsageRepository
import com.example.appwatch.system.PackageManagerHelper
import com.example.appwatch.system.UsageStatsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import java.util.Calendar
import javax.inject.Inject

data class AppDetailUiState(
    val isLoading: Boolean = true,
    val appInfo: AppInfo? = null,
    val usageToday: String = "0m",
    val usageWeek: String = "0m",
    val weeklyAverage: String = "0m",
    val monthlyTotal: String = "0m",
    val launchesToday: Int = 0,
    val lastUsed: String = "Never",
    val permissions: List<PermissionEvidence> = emptyList()
)

data class PermissionEvidence(
    val name: String,
    val lastAccess: String,
    val isUnused: Boolean,
    val isSensitive: Boolean
)

@HiltViewModel
class AppDetailViewModel @Inject constructor(
    private val packageManagerHelper: PackageManagerHelper,
    private val usageStatsHelper: UsageStatsHelper,
    private val usageDao: UsageDao,
    private val permissionAccessDao: PermissionAccessDao
) : ViewModel() {

    private val _packageName = MutableStateFlow<String?>(null)

    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis


    val uiState: StateFlow<AppDetailUiState> = _packageName
        .filterNotNull()
        .flatMapLatest { pkg ->

            val monthStart = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -30)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.timeInMillis

            val weekStart = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -7)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.timeInMillis

            combine(
                usageDao.getUsageForApp(pkg).onStart { emit(emptyList()) },
                usageDao.getUsageByPackageAndDate(pkg, today).onStart { emit(null) },
                usageDao.getTotalTimeForApp(pkg, weekStart).onStart { emit(null) },
                usageDao.getMonthlyTotalForApp(pkg).onStart { emit(null) },
                permissionAccessDao.getEventsForApp(pkg).onStart { emit(emptyList()) }
            ) { appUsageHistory, todayEntity, weekTotal, monthTotal, permEvents ->

                // Live data from system
                val appInfo = packageManagerHelper.getAppInfo(pkg)
                val usageTodayMs = todayEntity?.totalTimeInForeground ?: 0L
                val launchesToday = usageStatsHelper.getAppLaunchesToday(pkg)
                val lastUsed = usageStatsHelper.getLastUsedString(pkg)

                // Historical data from Room
                val weekTotalMs = weekTotal ?: usageStatsHelper.getAppUsageThisWeek(pkg)
                val monthTotalMs = monthTotal ?: 0L
                val weeklyAvgMs = if (appUsageHistory.isNotEmpty()) {
                    appUsageHistory.take(7).sumOf { it.totalTimeInForeground } / 7
                } else {
                    weekTotalMs / 7
                }

                // Permissions — live from PackageManager + access history from Room
                val permissions = packageManagerHelper.getPermissionsForApp(pkg).map { perm ->
                    val lastAccess = permEvents.find {
                        it.permissionName.contains(perm.name, ignoreCase = true)
                    }
                    perm.copy(
                        lastAccess = when {
                            lastAccess != null -> {
                                val diff = System.currentTimeMillis() - lastAccess.accessTimestamp
                                val hours = diff / (1000 * 60 * 60)
                                val days = hours / 24
                                when {
                                    hours < 1 -> "Used recently"
                                    hours < 24 -> "${hours}h ago"
                                    days < 30 -> "${days}d ago"
                                    else -> "Not used in 30+ days"
                                }
                            }
                            perm.lastAccess == "Granted" -> "No recent access recorded"
                            else -> perm.lastAccess
                        },
                        isUnused = lastAccess == null && perm.lastAccess == "Granted"
                    )
                }

                AppDetailUiState(
                    isLoading = false,
                    appInfo = appInfo,
                    usageToday = usageStatsHelper.formatDuration(usageTodayMs),
                    usageWeek = usageStatsHelper.formatDuration(weekTotalMs),
                    weeklyAverage = usageStatsHelper.formatDuration(weeklyAvgMs),
                    monthlyTotal = usageStatsHelper.formatDuration(monthTotalMs),
                    launchesToday = launchesToday,
                    lastUsed = lastUsed,
                    permissions = permissions
                )
            }.flowOn(Dispatchers.IO)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppDetailUiState(isLoading = true)
        )

    fun loadAppDetails(packageName: String) {
        if (_packageName.value != packageName) {
            _packageName.value = packageName
        }
    }
}