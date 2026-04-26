package com.example.appwatch.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.appwatch.data.local.entity.NeedsAttentionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NeedsAttentionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<NeedsAttentionEntity>)

    @Query("SELECT * FROM needs_attention WHERE eventType = :type ORDER BY timestamp DESC")
    fun getEventsByType(type: String): Flow<List<NeedsAttentionEntity>>

    @Query("SELECT * FROM needs_attention ORDER BY timestamp DESC") // 👈 Filter hata diya
    fun getNeedsAttentionFlow(): Flow<List<NeedsAttentionEntity>>

    @Query("DELETE FROM needs_attention WHERE timestamp < :threshold")
    suspend fun deleteOldEvents(threshold: Long)

    @Query("DELETE FROM needs_attention WHERE eventType LIKE 'AUDIT_%'")
    suspend fun clearAuditEvents()

    @Query("DELETE FROM needs_attention WHERE packageName NOT IN (:packageNames)")
    suspend fun deleteRemovedApps(packageNames: List<String>)
}