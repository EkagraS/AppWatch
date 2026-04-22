package com.example.appwatch.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.appwatch.data.local.entity.RecentEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: RecentEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<RecentEventEntity>)

    @Query("SELECT * FROM recent_events WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    suspend fun getRecentEvents(sinceTimestamp: Long): List<RecentEventEntity>

    @Query("SELECT * FROM recent_events WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    fun getRecentEventsFlow(sinceTimestamp: Long): Flow<List<RecentEventEntity>>

    // RecentEventDao.kt mein
    @Query("SELECT * FROM recent_events WHERE eventType = :type ORDER BY timestamp DESC")
    fun getEventsByType(type: String): Flow<List<RecentEventEntity>>

    @Query("DELETE FROM recent_events WHERE timestamp < :olderThanTimestamp")
    suspend fun deleteOldEvents(olderThanTimestamp: Long)

//    @Query("DELETE FROM recent_events WHERE eventType IN ('INSTALL', 'UPDATE', 'SIDELOADED_APK', 'DATA_HOG')")
//    suspend fun clearSyncedEvents()
}