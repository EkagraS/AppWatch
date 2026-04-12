package com.example.appwatch.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appwatch.domain.model.AppInfo
import com.example.appwatch.domain.model.RiskLevel
import com.example.appwatch.system.PackageManagerHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppsWithPermissionViewModel @Inject constructor(
    private val packageManagerHelper: PackageManagerHelper
) : ViewModel() {

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadAppsWithPermission(permissionType: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val allApps = packageManagerHelper.getInstalledAppsMetadata()
            _apps.value = allApps
                .filter { entity ->
                    packageManagerHelper.hasPermission(entity.packageName, permissionType)
                }
                .map { entity ->
                    AppInfo(
                        packageName = entity.packageName,
                        appName = entity.appName,
                        isSystemApp = entity.isSystemApp,
                        totalPermissions = entity.totalPermissions,
                        iconDrawable = packageManagerHelper.getAppIcon(entity.packageName),
                        riskLevel = when {
                            entity.totalPermissions >= 15 -> RiskLevel.HIGH
                            entity.totalPermissions >= 5 -> RiskLevel.MEDIUM
                            else -> RiskLevel.LOW
                        }
                    )
                }
                .sortedWith(compareBy({ it.isSystemApp }, { it.appName }))
            _isLoading.value = false
        }
    }
}