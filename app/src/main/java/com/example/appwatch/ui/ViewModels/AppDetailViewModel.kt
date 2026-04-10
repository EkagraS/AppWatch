package com.example.appwatch.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appwatch.domain.model.PermissionInfo
import com.example.appwatch.domain.usecase.GetPermissionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class AppDetailViewModel @Inject constructor(
    private val getPermissionsUseCase: GetPermissionsUseCase
) : ViewModel() {

    private val _packageName = MutableStateFlow<String?>(null)

    val permissionAudit: StateFlow<List<PermissionInfo>> = _packageName
        .filterNotNull()
        .flatMapLatest { getPermissionsUseCase(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadAppDetails(packageName: String) {
        _packageName.value = packageName
    }
}