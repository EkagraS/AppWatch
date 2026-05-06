package com.ekagra.privascope.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ekagra.privascope.data.local.entity.AppNotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppNotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(notification: AppNotificationEntity)

    @Query("SELECT * FROM app_notification_stats WHERE packageName = :packageName AND date = :date LIMIT 1")
    suspend fun getSpecificStat(packageName: String, date: String): AppNotificationEntity?

    @Query("SELECT * FROM app_notification_stats WHERE date = :todayDate ORDER BY postedCount DESC")
    fun getNotificationsByDate(todayDate: String): Flow<List<AppNotificationEntity>>

    @Query("SELECT SUM(postedCount) FROM app_notification_stats WHERE date = :date")
    fun getTotalCountByDate(date: String): Flow<Int?>

    // 🔴 UPSERT LOGIC USING TRANSACTION
    // Ye function check karega: Agar data hai toh count++ karega, warna naya daalega.
    @Transaction
    suspend fun incrementNotificationCount(packageName: String, date: String) {
        val existingStat = getSpecificStat(packageName, date)
        val timestamp = System.currentTimeMillis()

        if (existingStat == null) {
            insertOrUpdate(
                AppNotificationEntity(
                    packageName = packageName,
                    date = date,
                    postedCount = 1,
                    lastUpdated = timestamp
                )
            )
        } else {
            insertOrUpdate(
                existingStat.copy(
                    postedCount = existingStat.postedCount + 1,
                    lastUpdated = timestamp
                )
            )
        }
    }

    @Transaction
    suspend fun updateNotificationStats(packageName: String, date: String, type: String) {
        val existing = getSpecificStat(packageName, date) ?: AppNotificationEntity(packageName = packageName, date = date)

        val updated = when(type) {
            "POSTED" -> existing.copy(postedCount = existing.postedCount + 1)
            "OPENED" -> existing.copy(openedCount = existing.openedCount + 1)
            "DISMISSED" -> existing.copy(dismissedCount = existing.dismissedCount + 1)
            else -> existing
        }
        insertOrUpdate(updated.copy(lastUpdated = System.currentTimeMillis()))
    }

    @Query("DELETE FROM app_notification_stats WHERE date < :expiryDate")
    suspend fun deleteOldNotifications(expiryDate: String)
}