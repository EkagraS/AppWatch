package com.example.appwatch.data.repository

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
        notificationDao.incrementNotificationCount(packageName, date)
    }

    override suspend fun cleanOldNotifications(expiryDate: String) {
        notificationDao.deleteOldNotifications(expiryDate)
    }
}