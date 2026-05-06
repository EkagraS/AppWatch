package com.ekagra.privascope.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ekagra.privascope.data.local.entity.VitalsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VitalsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVitals(vitals: VitalsEntity)

    @Query("SELECT * FROM vitals_table WHERE date >= :startDate ORDER BY date ASC")
    fun getVitalsRange(startDate: Long): Flow<List<VitalsEntity>>

    @Query("SELECT * FROM vitals_table WHERE date = :date")
    fun getVitalsByDate(date: Long): Flow<VitalsEntity?>

    @Query("DELETE FROM vitals_table WHERE date < :threshold")
    suspend fun deleteOldVitals(threshold: Long)
}