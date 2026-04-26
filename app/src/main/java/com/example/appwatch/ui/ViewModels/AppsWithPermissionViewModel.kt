package com.example.appwatch.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appwatch.domain.model.AppInfo
import com.example.appwatch.domain.model.SensitiveAccess
import com.example.appwatch.domain.repository.PermissionRepository
import com.example.appwatch.system.PackageManagerHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppsWithPermissionViewModel @Inject constructor(
    private val packageManagerHelper: PackageManagerHelper,
    private val repository: PermissionRepository,
    savedStateHandle: SavedStateHandle // Navigation se permissionType nikalne ke liye
) : ViewModel() {

    // Navigation route mein jo key use ki hai (e.g., "permission_type") wahi yahan likhna
    private val permissionType: String = savedStateHandle["permissionType"] ?: ""

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        // Step 1: Start observing Room (Jaise DashboardViewModel mein hai)
        viewModelScope.launch {
            repository.getAppsWithPermission(permissionType)
                .catch { _isLoading.value = false }
                .collect { appsFromRoom ->
                    _apps.value = appsFromRoom.sortedWith(
                        compareBy({ it.isSystemApp }, { it.appName })
                    )
                    // Agar Room mein data aa gaya, toh loading false
                    if (appsFromRoom.isNotEmpty()) {
                        _isLoading.value = false
                    }
                }
        }

        // Step 2: Refresh system data & cache (Background Sync)
        refreshPermissionCache()
    }

    private fun refreshPermissionCache() {
        if (permissionType.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            try {
                val allApps = packageManagerHelper.getInstalledAppsMetadata()
                allApps.forEach { entity ->
                    // Permission check logic
                    if (packageManagerHelper.hasPermission(entity.packageName, permissionType)) {
                        repository.logAccessEvent(
                            SensitiveAccess(
                                packageName = entity.packageName,
                                appName = entity.appName,
                                accessType = permissionType,
                                timestampString = "",
                                isRealTime = false
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}