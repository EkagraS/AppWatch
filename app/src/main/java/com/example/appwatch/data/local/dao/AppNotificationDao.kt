package com.example.appwatch.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.appwatch.data.local.entity.AppNotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppNotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(notification: AppNotificationEntity)

    @Query("SELECT * FROM app_notification_stats WHERE packageName = :packageName AND date = :date LIMIT 1")
    suspend fun getSpecificStat(packageName: String, date: String): AppNotificationEntity?

    @Query("SELECT * FROM app_notification_stats WHERE date = :todayDate ORDER BY count DESC")
    fun getNotificationsByDate(todayDate: String): Flow<List<AppNotificationEntity>>

    // 🔴 UPSERT LOGIC USING TRANSACTION
    // Ye function check karega: Agar data hai toh count++ karega, warna naya daalega.
    @Transaction
    suspend fun incrementNotificationCount(packageName: String, date: String) {
        val existingStat = getSpecificStat(packageName, date)
        val timestamp = System.currentTimeMillis()

        if (existingStat == null) {
            // Nayi entry
            insertOrUpdate(
                AppNotificationEntity(
                    packageName = packageName,
                    date = date,
                    count = 1,
                    lastUpdated = timestamp
                )
            )
        } else {
            // Purani entry update (Count + 1)
            insertOrUpdate(
                existingStat.copy(
                    count = existingStat.count + 1,
                    lastUpdated = timestamp
                )
            )
        }
    }

    @Query("DELETE FROM app_notification_stats WHERE date < :expiryDate")
    suspend fun deleteOldNotifications(expiryDate: String)
}