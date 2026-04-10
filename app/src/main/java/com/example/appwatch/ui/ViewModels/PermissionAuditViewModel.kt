package com.example.appwatch.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appwatch.domain.usecase.GetSensitiveAccessUseCase
import com.example.appwatch.domain.usecase.GetUnusedPermissionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PermissionAuditViewModel @Inject constructor(
    getSensitiveAccessUseCase: GetSensitiveAccessUseCase,
    getUnusedPermissionsUseCase: GetUnusedPermissionsUseCase
) : ViewModel() {

    val recentAccessHistory = getSensitiveAccessUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val highRiskStaleApps = getUnusedPermissionsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}