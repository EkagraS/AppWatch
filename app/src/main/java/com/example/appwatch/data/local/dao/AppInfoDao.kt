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

    @Query("SELECT * FROM app_info ORDER BY totalSizeBytes DESC")
    fun getAppsByStorageSize(): Flow<List<AppInfoEntity>>

    @Query("SELECT SUM(totalSizeBytes) FROM app_info WHERE isSystemApp = 0")
    fun getTotalUserAppsSize(): Flow<Long?>

    @Query("SELECT SUM(totalSizeBytes) FROM app_info WHERE isSystemApp = 1")
    fun getTotalSystemAppsSize(): Flow<Long?>

    @Query("""
    UPDATE app_info 
    SET appSizeBytes = :appSize, dataSizeBytes = :dataSize, cacheSizeBytes = :cacheSize, totalSizeBytes = :totalSize, storageLastUpdated = :updatedAt
    WHERE packageName = :packageName""")
    suspend fun updateAppStorage(packageName: String, appSize: Long, dataSize: Long, cacheSize: Long, totalSize: Long, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM app_info")
    fun getTotalAppsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM app_info WHERE hasLocation = 1")
    fun getLocationAppsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM app_info WHERE hasCamera = 1")
    fun getCameraAppsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM app_info WHERE hasMic = 1")
    fun getMicAppsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM app_info WHERE hasContacts = 1")
    fun getContactAppsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM app_info WHERE hasSms = 1")
    fun getSmsAppsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM app_info WHERE hasPhone = 1")
    fun getPhoneAppsCount(): Flow<Int>

}