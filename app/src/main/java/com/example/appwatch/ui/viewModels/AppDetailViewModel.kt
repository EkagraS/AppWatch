package com.example.appwatch.ui.viewModels

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
    private val highlySensitiveNames = listOf(
        "ACCESSIBILITY", "NOTIFICATION_ACCESS", "OVERLAY",
        "DEVICE_ADMIN", "INSTALL_UNKNOWN_APPS", "WRITE_SETTINGS", "VPN_SERVICE"
    )

    // 🟠 SENSITIVE: Direct Manifest Strings from Companion Object + Extras
    private val sensitivePermissions = listOf(
        // Location Group
        "android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_BACKGROUND_LOCATION",
        // SMS Group
        "android.permission.READ_SMS", "android.permission.RECEIVE_SMS", "android.permission.SEND_SMS", "android.permission.RECEIVE_WAP_PUSH",
        "android.permission.RECEIVE_MMS",
        // Contacts Group
        "android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS", "android.permission.GET_ACCOUNTS", "android.permission.MANAGE_ACCOUNTS",
        // Phone Group
        "android.permission.READ_PHONE_STATE", "android.permission.CALL_PHONE", "android.permission.READ_CALL_LOG", "android.permission.WRITE_CALL_LOG",
        "android.permission.ADD_VOICEMAIL", "android.permission.USE_SIP", "android.permission.PROCESS_OUTGOING_CALLS", "android.permission.MANAGE_OWN_CALLS",
        // Core
        "android.permission.CAMERA", "android.permission.RECORD_AUDIO",
        // Extras & Media
        "android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.MANAGE_EXTERNAL_STORAGE",
        "android.permission.READ_MEDIA_IMAGES", "android.permission.READ_MEDIA_VIDEO", "android.permission.READ_MEDIA_AUDIO", "android.permission.BLUETOOTH",
        "android.permission.BLUETOOTH_ADMIN","android.permission.BLUETOOTH_SCAN", "android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_ADVERTISE",
        "android.permission.ACTIVITY_RECOGNITION", "android.permission.READ_CALENDAR", "android.permission.WRITE_CALENDAR", "android.permission.NEARBY_WIFI_DEVICES"
    )
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
        return when (permission) {
            in highlySensitiveNames -> RiskTier.HIGH
            in sensitivePermissions -> RiskTier.SENSITIVE
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

                try {
                    val appInfo = packageManagerHelper.getAppInfo(pkg, 0)
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

                    val manifestPerms = packageManagerHelper.getPermissionsForApp(pkg)

                    val sensitivePerms = manifestPerms.filter { it.name in sensitivePermissions }.map { perm ->
                        PermissionEvidence(
                            name = perm.name,
                            riskTier = RiskTier.SENSITIVE,
                            lastAccess = permEvents.find { it.permissionName == perm.name }?.let {
                                formatLastAccess(it.accessTimestamp)
                            } ?: ""
                        )
                    }

                    val normalPerms = manifestPerms.filter { it.name !in sensitivePermissions }.map { perm ->
                        PermissionEvidence(
                            name = perm.name,
                            riskTier = RiskTier.STANDARD,
                            lastAccess = permEvents.find { it.permissionName == perm.name }?.let {
                                formatLastAccess(it.accessTimestamp)
                            } ?: ""
                        )
                    }

                    val specialPerms = packageManagerHelper.getSpecialPermissions(pkg).map { specName ->
                        PermissionEvidence(
                            name = specName,
                            riskTier = RiskTier.HIGH,
                            lastAccess = ""
                        )
                    }

                    val allPermissions = (normalPerms + sensitivePerms + specialPerms)

                    AppDetailUiState(
                        isLoading = false,
                        appInfo = appInfo,
                        usageToday = usageStatsHelper.formatDuration(usageTodayMs),
                        usageWeek = usageStatsHelper.formatDuration(weekTotalMs),
                        weeklyAverage = usageStatsHelper.formatDuration(weeklyAvgMs),
                        monthlyTotal = usageStatsHelper.formatDuration(monthTotalMs),
                        launchesToday = launchesToday,
                        lastUsed = lastUsed,
                        permissions = allPermissions
                    )
                } catch (e: Exception) {
                    AppDetailUiState(
                        isLoading = false,
                        appInfo = null,
                        lastUsed = "App details not available or uninstalled."
                    )
                }
            }
                .catch { e ->
                    emit(AppDetailUiState(isLoading = false, lastUsed = "Database Error"))
                }
                .flowOn(Dispatchers.IO)
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