package com.example.appwatch.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.appwatch.data.local.entity.PermissionAccessEntity
import com.example.appwatch.data.local.entity.UsageEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface PermissionAccessDao{
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAccessEvent(event: PermissionAccessEntity)

    @Query("SELECT * FROM permission_access WHERE packageName = :packageName ORDER BY accessTimestamp DESC")
    fun getEventsForApp(packageName: String): Flow<List<PermissionAccessEntity>>

    @Query("SELECT * FROM permission_access WHERE permissionName = :permission ORDER BY accessTimestamp DESC")
    fun getEventsByPermission(permission: String): Flow<List<PermissionAccessEntity>>

    @Query("DELETE FROM permission_access WHERE accessTimestamp < :threshold")
    suspend fun deleteOldLogs(threshold: Long)
}