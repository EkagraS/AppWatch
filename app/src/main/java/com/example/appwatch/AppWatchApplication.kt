package com.example.appwatch

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
//import com.example.appwatch.worker.AppDiscoveryWorker
//import com.example.appwatch.worker.UsageSnapshotWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class AppWatchApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
//            scheduleAppDiscovery()
//            scheduleUsageSnapshot()
        }
    }

//    private fun scheduleUsageSnapshot() {
//        // Runs every night at approximately midnight to save daily usage to Room
//        val request = PeriodicWorkRequestBuilder<UsageSnapshotWorker>(1, TimeUnit.DAYS)
//            .setConstraints(
//                Constraints.Builder()
//                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
//                    .build()
//            )
//            .setInitialDelay(calculateDelayUntilMidnight(), TimeUnit.MILLISECONDS)
//            .build()
//
//        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
//            "usage_snapshot",
//            ExistingPeriodicWorkPolicy.KEEP,
//            request
//        )
//
//        // Also run immediately once to populate today's data
//        val immediateRequest = OneTimeWorkRequestBuilder<UsageSnapshotWorker>().build()
//        WorkManager.getInstance(this).enqueueUniqueWork(
//            "usage_snapshot_immediate",
//            ExistingWorkPolicy.KEEP,
//            immediateRequest
//        )
//    }

    private fun calculateDelayUntilMidnight(): Long {
        val now = System.currentTimeMillis()
        val midnight = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        return midnight - now
    }
}