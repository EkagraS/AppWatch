package com.example.appwatch.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.appwatch.data.local.entity.AppInfoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppInfoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetadata(metadata: AppInfoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMetadata(metadataList: List<AppInfoEntity>)

    // All queries now correctly point to "app_info"
    @Query("SELECT * FROM app_info ORDER BY appName ASC")
    fun getAllAppsAlphabetical(): Flow<List<AppInfoEntity>>

    @Query("SELECT * FROM app_info ORDER BY totalPermissions DESC")
    fun getAppsByPermissionCount(): Flow<List<AppInfoEntity>>

    @Query("SELECT * FROM app_info WHERE isSystemApp = 0 ORDER BY appName ASC")
    fun getUserApps(): Flow<List<AppInfoEntity>>

    @Query("SELECT * FROM app_info WHERE packageName = :packageName LIMIT 1")
    suspend fun getAppMetadata(packageName: String): AppInfoEntity?
}