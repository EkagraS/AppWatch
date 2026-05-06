package com.ekagra.privascope

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.ekagra.privascope.ui.activities.CrashRecoveryActivity
import com.ekagra.privascope.worker.UsageSnapshotWorker
//import com.example.appwatch.worker.AppDiscoveryWorker
//import com.example.appwatch.worker.UsageSnapshotWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import java.util.Calendar
import java.util.Date
import kotlin.jvm.java
import kotlin.system.exitProcess

@HiltAndroidApp
class PrivaScopeApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        try {
            System.loadLibrary("sqlcipher")
        } catch (e: UnsatisfiedLinkError) {
            Log.e("AppWatch", "SQLCipher load failed", e)
        }
        setupCrashReporting()
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
//            scheduleAppDiscovery()
            scheduleUsageSnapshot()
        }
    }

    private fun scheduleUsageSnapshot() {
        // Runs every night at approximately midnight to save daily usage to Room
        val request = PeriodicWorkRequestBuilder<UsageSnapshotWorker>(1, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .setInitialDelay(calculateDelayUntilMidnight(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "usage_snapshot",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )

        // Also run immediately once to populate today's data
        val immediateRequest = OneTimeWorkRequestBuilder<UsageSnapshotWorker>().build()
        WorkManager.getInstance(this).enqueueUniqueWork(
            "usage_snapshot_immediate",
            ExistingWorkPolicy.KEEP,
            immediateRequest
        )
    }

    private fun setupCrashReporting() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            try {
                val sharedPrefs = getSharedPreferences("app_watch_prefs", Context.MODE_PRIVATE)

                // 1. Current state read karo
                val lastCrashTimestamp = sharedPrefs.getLong("last_crash_timestamp", 0L)
                val currentTime = System.currentTimeMillis()
                val currentCount = sharedPrefs.getInt("continuous_crash_count", 0)

                // 2. Logic: Kya yeh crash pichle 3 min ke andar hua? (Window reset logic)
                val isRecentCrash = (currentTime - lastCrashTimestamp) < (3 * 60 * 1000)
                val updatedCount = if (isRecentCrash) currentCount + 1 else 1

                if (updatedCount > 5) {
                    defaultHandler?.uncaughtException(thread, exception)
                    return@setDefaultUncaughtExceptionHandler
                }

                // 3. Logic: Kya yeh Critical Loop hai? (1 min ke andar multiple crashes)
                val timeDiffMinutes = (currentTime - lastCrashTimestamp) / 60000.0
                val isCriticalLoop = updatedCount >= 2 && timeDiffMinutes < 1.0

                // 4. Stack Trace aur Report taiyar karo
                val stackTrace = Log.getStackTraceString(exception)
                val errorReport = """
                Time: ${Date()}
                Device: ${Build.MANUFACTURER} ${Build.MODEL}
                OS: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
                
                Stack Trace:
                $stackTrace
            """.trimIndent()

                sharedPrefs.edit().apply {
                    putString("last_crash_log", errorReport)
                    putInt("continuous_crash_count", updatedCount)
                    putLong("last_crash_timestamp", currentTime)
                    // Activity isi flag ko check karegi
                    putBoolean("is_critical_loop", isCriticalLoop || updatedCount >= 4)
                    commit()
                }

                val intent = Intent(this, CrashRecoveryActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)

            } catch (e: Exception) {
                Log.e("AppWatch", "Error in crash handler", e)
                defaultHandler?.uncaughtException(thread, exception)
            } finally {
                exitProcess(10)
            }
        }
    }

    private fun calculateDelayUntilMidnight(): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 45)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }
}