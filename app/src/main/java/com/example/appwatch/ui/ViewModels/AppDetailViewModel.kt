package com.example.appwatch.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appwatch.data.local.dao.PermissionAccessDao
import com.example.appwatch.data.local.dao.UsageDao
import com.example.appwatch.domain.model.AppInfo
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
    val riskTier: RiskTier // High, Sensitive, ya Standard
)

enum class RiskTier { HIGH, SENSITIVE, STANDARD }


@HiltViewModel
class AppDetailViewModel @Inject constructor(
    private val packageManagerHelper: PackageManagerHelper,
    private val usageStatsHelper: UsageStatsHelper,
    private val usageDao: UsageDao,
    private val permissionAccessDao: PermissionAccessDao
) : ViewModel() {

    // Helper to format the timestamp into "Xh ago" etc.
    private fun formatLastAccess(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val minutes = diff / (1000 * 60)
        val hours = minutes / 60
        val days = hours / 24

        return when {
            minutes < 1 -> "Used just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 30 -> "${days}d ago"
            else -> "Not used in 30+ days"
        }
    }

    private fun mapToRiskTier(permission: String): RiskTier {
        val p = permission.uppercase() // Case-insensitive matching ke liye safe side
        return when {
            p.contains("ACCESSIBILITY") ||
                    p.contains("NOTIFICATION_LISTENER") ||
                    p.contains("SYSTEM_ALERT_WINDOW") ||
                    p.contains("BIND_DEVICE_ADMIN") ||
                    p.contains("INSTALL_PACKAGES") ||
                    p.contains("VPN") ||
                    p.contains("WRITE_SETTINGS") -> RiskTier.HIGH

            p.contains("LOCATION") ||
                    p.contains("CAMERA") ||
                    p.contains("RECORD_AUDIO") ||
                    p.contains("SMS") ||
                    p.contains("CONTACTS") ||
                    p.contains("PHONE") ||
                    p.contains("CALL_LOG") ||
                    p.contains("STORAGE") ||
                    p.contains("BLUETOOTH") ||
                    p.contains("ACTIVITY_RECOGNITION") ||
                    p.contains("CALENDAR") -> RiskTier.SENSITIVE

            else -> RiskTier.STANDARD
        }
    }

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

                val appInfo = packageManagerHelper.getAppInfo(pkg)
                val usageTodayMs = todayEntity?.totalTimeInForeground ?: 0L
                val launchesToday = usageStatsHelper.getAppLaunchesToday(pkg)
                val lastUsed = usageStatsHelper.getLastUsedString(pkg)

                val weekTotalMs = weekTotal ?: usageStatsHelper.getAppUsageThisWeek(pkg)
                val monthTotalMs = monthTotal ?: 0L
                val weeklyAvgMs = if (appUsageHistory.isNotEmpty()) {
                    appUsageHistory.take(7).sumOf { it.totalTimeInForeground } / 7
                } else {
                    weekTotalMs / 7
                }

                // Logic: Deterministic Tiering
                val permissions = packageManagerHelper.getPermissionsForApp(pkg).map { perm ->
                    val lastAccessEvent = permEvents.find {
                        it.permissionName.contains(perm.name, ignoreCase = true)
                    }

                    PermissionEvidence(
                        name = perm.name,
                        riskTier = mapToRiskTier(perm.name),
                        lastAccess = when {
                            lastAccessEvent != null -> formatLastAccess(lastAccessEvent.accessTimestamp)
                            else -> ""
                        }
                    )
                }.sortedBy { it.riskTier } // HIGH sabse upar aayenge

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