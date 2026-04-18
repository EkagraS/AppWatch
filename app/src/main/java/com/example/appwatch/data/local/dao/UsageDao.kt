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

    @Query("SELECT SUM(appUnlocks) FROM app_usage WHERE usageDate = :date")
    fun getTotalUnlocksToday(date: Long): Flow<Int?>

    @Query("SELECT * FROM app_usage WHERE usageDate >= :startDate ORDER BY totalTimeInForeground DESC LIMIT 3")
    fun getTopChamps(startDate: Long): Flow<List<UsageEntity>>

    @Query("""
        SELECT SUM(totalTimeInForeground) as total, usageDate 
        FROM app_usage 
        WHERE usageDate >= :startDate 
        GROUP BY usageDate 
        ORDER BY usageDate ASC
    """)
    fun getWeeklyStats(startDate: Long): Flow<List<DayUsageTuple>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageList(usageList: List<UsageEntity>) // Yeh line missing thi

    @Query("SELECT * FROM app_usage WHERE packageName = :packageName AND usageDate = :date LIMIT 1")
    fun getUsageByPackageAndDate(packageName: String, date: Long): Flow<UsageEntity?>

    @Query("""
    SELECT * FROM app_usage 
    WHERE usageDate >= :startDate 
    GROUP BY packageName 
    ORDER BY SUM(totalTimeInForeground) DESC 
    LIMIT :limit
""")
    fun getTopAppForRange(startDate: Long, limit: Int): Flow<List<UsageEntity>>

    // 2. Specific range ka total screen time (Know Your Usage ke liye)
    @Query("SELECT SUM(totalTimeInForeground) FROM app_usage WHERE usageDate >= :startDate")
    fun getTotalTimeForRange(startDate: Long): Flow<Long?>

    // 3. Specific range ke total unlocks
    @Query("SELECT SUM(appUnlocks) FROM app_usage WHERE usageDate >= :startDate")
    fun getTotalUnlocksForRange(startDate: Long): Flow<Int?>

    // 4. Streak ke liye last event timestamp (Current active/inactive check)
    @Query("SELECT MAX(lastEventTimestamp) FROM app_usage")
    suspend fun getLastSystemEventTime(): Long
// Separate data class for grouping



    data class DayUsageTuple(val total: Long, val usageDate: Long)
    data class DayTotal(val total: Long, val usageDate: Long)
}