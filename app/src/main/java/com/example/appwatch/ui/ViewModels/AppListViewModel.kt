package com.example.appwatch.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appwatch.domain.model.AppInfo
import com.example.appwatch.domain.model.RiskLevel
import com.example.appwatch.system.PackageManagerHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppListViewModel @Inject constructor(
    private val packageManagerHelper: PackageManagerHelper
) : ViewModel() {

    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _selectedFilter = MutableStateFlow("All")
    private val _isLoading = MutableStateFlow(true)

    val searchQuery: StateFlow<String> = _searchQuery
    val selectedFilter: StateFlow<String> = _selectedFilter
    val isLoading: StateFlow<Boolean> = _isLoading

    val apps: StateFlow<List<AppInfo>> = combine(
        _allApps, _searchQuery, _selectedFilter
    ) { apps, query, filter ->
        var result = apps

        // Apply filter
        result = when (filter) {
            "User Apps" -> result.filter { !it.isSystemApp }
            "System Apps" -> result.filter { it.isSystemApp }
            "High Risk" -> result
                .filter { it.riskLevel == RiskLevel.HIGH }
                .sortedByDescending { it.totalPermissions }
            else -> result.sortedBy { it.appName }
        }

        // Apply search on top of filter
        if (query.isNotEmpty()) {
            result = result.filter {
                it.appName.contains(query, ignoreCase = true) ||
                        it.packageName.contains(query, ignoreCase = true)
            }
        }

        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val entities = packageManagerHelper.getInstalledAppsMetadata()
            _allApps.value = entities.map { entity ->
                AppInfo(
                    packageName = entity.packageName,
                    appName = entity.appName,
                    isSystemApp = entity.isSystemApp,
                    totalPermissions = entity.totalPermissions,
                    iconDrawable = packageManagerHelper.getAppIcon(entity.packageName),
                    riskLevel = when {
                        entity.totalPermissions > 10 -> RiskLevel.HIGH
                        entity.totalPermissions > 5 -> RiskLevel.MEDIUM
                        else -> RiskLevel.LOW
                    }
                )
            }
            _isLoading.value = false
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterChange(filter: String) {
        _selectedFilter.value = filter
    }
}