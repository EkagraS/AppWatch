package com.example.appwatch.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.appwatch.data.local.entity.UsageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsage(usage: UsageEntity)

    @Query("SELECT * FROM app_usage WHERE usageDate = :date ORDER BY totalTimeInForeground DESC")
    fun getUsageByDate(date: Long): Flow<List<UsageEntity>>

    @Query("SELECT SUM(totalTimeInForeground) FROM app_usage WHERE usageDate = :date")
    fun getTotalScreenTime(date: Long): Flow<Long?>

    @Query("SELECT * FROM app_usage WHERE packageName = :packageName ORDER BY usageDate DESC")
    fun getUsageForApp(packageName: String): Flow<List<UsageEntity>>

    @Query("SELECT SUM(totalTimeInForeground) FROM app_usage WHERE packageName = :packageName AND usageDate >= :startDate")
    fun getTotalTimeForApp(packageName: String, startDate: Long): Flow<Long?>

    @Query("SELECT * FROM app_usage WHERE usageDate >= :startDate ORDER BY totalTimeInForeground DESC LIMIT 5")
    fun getMostUsedApps(startDate: Long): Flow<List<UsageEntity>>

    @Query("SELECT SUM(totalTimeInForeground) FROM app_usage WHERE packageName = :packageName")
    fun getMonthlyTotalForApp(packageName: String): Flow<Long?>

    @Query("""
    SELECT SUM(totalTimeInForeground) as total, usageDate 
    FROM app_usage 
    WHERE usageDate >= :startDate 
    GROUP BY usageDate 
    ORDER BY usageDate ASC
""")
    suspend fun getWeeklyTotals(startDate: Long): List<DayTotal>
    data class DayTotal(val total: Long, val usageDate: Long)
}