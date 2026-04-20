package com.example.appwatch.data.local.datastore // Apne folder ke hisaab se rakh

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// DataStore ka instance create kiya
private val Context.dataStore by preferencesDataStore("vitals_prefs")

data class DailyVitals(
    val unlocks: Int,
    val notifications: Int,
    val dataUsage: Long,
    val patchDate: String
)

@Singleton
class VitalsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Keys (Data pehchanne ke liye)
    private val UNLOCKS_KEY = intPreferencesKey("today_unlocks")
    private val NOTIFICATIONS_KEY = intPreferencesKey("today_notifications")
    private val DATA_USAGE_KEY = longPreferencesKey("today_data_usage")
    private val SECURITY_PATCH_KEY = stringPreferencesKey("security_patch")

    // 1. Data Save karne ka function
    suspend fun saveVitals(unlocks: Int, notifications: Int, dataUsageBytes: Long, patchDate: String) {
        context.dataStore.edit { prefs ->
            prefs[UNLOCKS_KEY] = unlocks
            prefs[NOTIFICATIONS_KEY] = notifications
            prefs[DATA_USAGE_KEY] = dataUsageBytes
            prefs[SECURITY_PATCH_KEY] = patchDate
        }
    }

    val vitalsFlow: Flow<DailyVitals> = context.dataStore.data.map { prefs ->
        val unlocks = prefs[UNLOCKS_KEY] ?: 0
        val notifications = prefs[NOTIFICATIONS_KEY] ?: 0
        val dataUsage = prefs[DATA_USAGE_KEY] ?: 0L
        val patchDate = prefs[SECURITY_PATCH_KEY] ?: "Unknown" // 4th Nayi chiz

        DailyVitals(unlocks, notifications, dataUsage, patchDate)
    }}