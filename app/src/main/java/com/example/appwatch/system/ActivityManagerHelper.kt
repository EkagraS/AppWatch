package com.example.appwatch.system

import android.app.ActivityManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityManagerHelper @Inject constructor(@ApplicationContext private val context: Context){
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    fun isAppRunning(packageName: String): Boolean {
        val runningProcesses = activityManager.runningAppProcesses ?: return false
        return runningProcesses.any { it.processName == packageName }
    }

    fun getMemoryInfo(): ActivityManager.MemoryInfo {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo
    }

    fun getRamUsagePercentage(): Int {
        val memInfo = getMemoryInfo()
        val usedMem = memInfo.totalMem - memInfo.availMem
        return ((usedMem.toDouble() / memInfo.totalMem) * 100).toInt()
    }
}