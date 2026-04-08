package com.example.appwatch.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.appwatch.data.local.entity.AppMetadataEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface AppMetadataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetadata(metadata: AppMetadataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMetadata(metadataList: List<AppMetadataEntity>)

    @Query("SELECT * FROM app_metadata ORDER BY appName ASC")
    fun getAllAppsAlphabetical(): Flow<List<AppMetadataEntity>>

    @Query("SELECT * FROM app_metadata ORDER BY totalPermissions DESC")
    fun getAppsByPermissionCount(): Flow<List<AppMetadataEntity>>

    @Query("SELECT * FROM app_metadata WHERE isSystemApp = 0 ORDER BY appName ASC")
    fun getUserApps(): Flow<List<AppMetadataEntity>>

    @Query("SELECT * FROM app_metadata WHERE packageName = :packageName LIMIT 1")
    suspend fun getAppMetadata(packageName: String): AppMetadataEntity?
}