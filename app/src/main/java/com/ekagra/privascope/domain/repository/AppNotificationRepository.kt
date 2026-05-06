package com.ekagra.privascope.domain.repository

import com.ekagra.privascope.data.local.entity.AppNotificationEntity
import kotlinx.coroutines.flow.Flow

interface AppNotificationRepository {
    fun getNotificationsByDate(date: String): Flow<List<AppNotificationEntity>>

    suspend fun incrementNotificationCount(packageName: String, date: String)

    suspend fun cleanOldNotifications(expiryDate: String)

    suspend fun updateNotificationStats(packageName: String, date: String, type: String)
}