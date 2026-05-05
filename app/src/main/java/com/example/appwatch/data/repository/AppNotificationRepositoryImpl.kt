package com.example.appwatch.data.repository

import android.util.Log
import com.example.appwatch.data.local.dao.AppNotificationDao
import com.example.appwatch.data.local.entity.AppNotificationEntity
import com.example.appwatch.domain.repository.AppNotificationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AppNotificationRepositoryImpl @Inject constructor(
    private val notificationDao: AppNotificationDao
) : AppNotificationRepository {

    override fun getNotificationsByDate(date: String): Flow<List<AppNotificationEntity>> {
        return notificationDao.getNotificationsByDate(date)
    }

    override suspend fun incrementNotificationCount(packageName: String, date: String) {
        try {
            notificationDao.incrementNotificationCount(packageName, date)
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Failed to increment notification count", e)
        }
    }

    override suspend fun cleanOldNotifications(expiryDate: String) {
        try {
            notificationDao.deleteOldNotifications(expiryDate)
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Failed to clean old notifications", e)
        }
    }

    override suspend fun updateNotificationStats(packageName: String, date: String, type: String) {
        try {
            notificationDao.updateNotificationStats(packageName, date, type)
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Failed to update notification stats", e)
        }
    }
}