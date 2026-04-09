package com.example.appwatch.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The "Scout" of the app.
 * Listens for hardware events like screen unlocks to track
 * "First Pick Up" and "Unlocks Count".
 */
@Singleton
class DeviceEventManager @Inject constructor(@ApplicationContext private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("device_events_prefs", Context.MODE_PRIVATE)

    /**
     * Receiver that catches the "User Present" signal (Unlock).
     * This runs every time the user unlocks their phone.
     */
    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_USER_PRESENT) {
                recordUnlock()
            }
        }
    }

    /**
     * Starts the monitoring process.
     * Usually called from AppWatchApplication or MainActivity.
     */
    fun startMonitoring() {
        val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
        context.registerReceiver(unlockReceiver, filter)
    }

    /**
     * Logic to record the unlock event.
     */
    private fun recordUnlock() {
        val todayKey = getTodayKey()
        val currentTime = System.currentTimeMillis()

        // 1. Record First Pick Up if not already set for today
        if (!prefs.contains("${todayKey}_first_pickup")) {
            prefs.edit().putLong("${todayKey}_first_pickup", currentTime).apply()
        }

        // 2. Increment Unlock Count for the dashboard/stats
        val currentUnlocks = prefs.getInt("${todayKey}_unlock_count", 0)
        prefs.edit().putInt("${todayKey}_unlock_count", currentUnlocks + 1).apply()
    }

    /**
     * Gets the formatted "First Pick Up" time (e.g., "7:15 AM").
     * Used directly by the UsageStatsViewModel.
     */
    fun getFirstPickUpTime(): String {
        val todayKey = getTodayKey()
        val timestamp = prefs.getLong("${todayKey}_first_pickup", 0L)

        return if (timestamp == 0L) {
            "--:--"
        } else {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
        }
    }

    /**
     * Gets the total number of times the phone was unlocked today.
     */
    fun getUnlockCount(): Int {
        return prefs.getInt("${getTodayKey()}_unlock_count", 0)
    }

    private fun getTodayKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
}