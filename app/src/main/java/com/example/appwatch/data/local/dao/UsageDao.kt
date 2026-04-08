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
}