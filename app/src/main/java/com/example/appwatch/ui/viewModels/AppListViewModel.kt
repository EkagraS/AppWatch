package com.example.appwatch.ui.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appwatch.domain.model.AppInfo
import com.example.appwatch.domain.repository.AppInfoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppListViewModel @Inject constructor(
    private val repository: AppInfoRepository
) : ViewModel() {

    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _selectedFilter = MutableStateFlow("All")
    private val _isLoading = MutableStateFlow(true)

    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Reason: Spam rokne ke liye refresh job ko track karenge
    private var refreshJob: Job? = null

    // Dashboard style Combine logic
    val apps: StateFlow<List<AppInfo>> = combine(
        _allApps, _searchQuery, _selectedFilter
    ) { apps, query, filter ->
        var result = apps

        // Apply filter (RiskLevel Filter Removed)
        result = when (filter) {
            "User Apps" -> result.filter { !it.isSystemApp }
            "System Apps" -> result.filter { it.isSystemApp }
            else -> result.sortedBy { it.appName }
        }

        if (query.isNotEmpty()) {
            result = result.filter {
                it.appName.contains(query, ignoreCase = true) ||
                        it.packageName.contains(query, ignoreCase = true)
            }
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Step 1: Room ko observe karo (With Crash Protection)
        viewModelScope.launch {
            repository.getAllApps(false)
                .catch { e ->
                    // Reason: Database read error par app freeze nahi hogi
                    _isLoading.value = false
                    _allApps.value = emptyList()
                }
                .collect { appsFromRoom ->
                    _allApps.value = appsFromRoom
                    if (appsFromRoom.isNotEmpty()) _isLoading.value = false
                }
        }

        // Step 2: Background refresh (Sync OS -> Room)
        refreshApps()
    }

    fun refreshApps() {
        // Reason: Agar user jaldi-jaldi refresh dabaye, toh purani job cancel karke nayi shuru karo. CPU bachega.
        refreshJob?.cancel()

        refreshJob = viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = _allApps.value.isEmpty()
            try {
                repository.refreshAppCache()
            } catch (e: CancellationException) {
                throw e // Coroutine safe-exit
            } catch (e: Exception) {
                // Reason: PackageManager ya OS error aaye toh ignore karo, crash nahi.
                e.printStackTrace()
            } finally {
                // Hamesha loading false karo, chahe error aaye ya success
                _isLoading.value = false
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterChange(filter: String) {
        _selectedFilter.value = filter
    }
}