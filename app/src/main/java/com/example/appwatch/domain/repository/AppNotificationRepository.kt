package com.example.appwatch.domain.repository

import com.example.appwatch.data.local.entity.AppNotificationEntity
import kotlinx.coroutines.flow.Flow

interface AppNotificationRepository {
    fun getNotificationsByDate(date: String): Flow<List<AppNotificationEntity>>

    suspend fun incrementNotificationCount(packageName: String, date: String)

    suspend fun cleanOldNotifications(expiryDate: String)
}