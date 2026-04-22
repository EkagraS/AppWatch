package com.example.appwatch.data.local.dao

import androidx.room.*
import com.example.appwatch.data.local.entity.AppDataUsageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDataUsageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUsage(usage: AppDataUsageEntity)

    // Aaj ka poora data breakdown dekhne ke liye
    @Query("SELECT * FROM app_data_usage WHERE date = :date ORDER BY (mobileUsageBytes + wifiUsageBytes) DESC")
    fun getUsageByDate(date: String): Flow<List<AppDataUsageEntity>>

    // Dashboard ke liye: Aaj ka total data (Mobile + WiFi)
    @Query("SELECT SUM(mobileUsageBytes + wifiUsageBytes) FROM app_data_usage WHERE date = :date")
    fun getTotalUsageForDay(date: String): Flow<Long?>

    @Query("DELETE FROM app_data_usage WHERE date < :expiryDate")
    suspend fun deleteOldUsageData(expiryDate: String)
}