package com.example.appwatch.system

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.example.appwatch.data.local.entity.AppInfoEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The "Librarian" of the app.
 * Responsible for scanning the device for installed applications and
 * extracting metadata like permission counts and installation dates.
 */
@Singleton
class PackageManagerHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val packageManager: PackageManager = context.packageManager

    /**
     * Fetches all installed apps and converts them into our Database Entities.
     * This is the heavy lifting used to populate the App List.
     */
    fun getInstalledAppsMetadata(): List<AppInfoEntity> {
        val apps = mutableListOf<AppInfoEntity>()
        val packages = packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)

        for (packageInfo in packages) {
            val appInfo = packageInfo.applicationInfo ?: continue

            // Skip the AppWatch app itself to avoid auditing ourselves
            if (packageInfo.packageName == context.packageName) continue

            apps.add(
                AppInfoEntity(
                    packageName = packageInfo.packageName,
                    appName = packageManager.getApplicationLabel(appInfo).toString(),
                    totalPermissions = packageInfo.requestedPermissions?.size ?: 0,
                    sensitivePermissionsCount = countSensitivePermissions(packageInfo),
                    isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    installedAt = packageInfo.firstInstallTime
                )
            )
        }
        return apps
    }

    /**
     * Returns the icon for a specific package.
     * Used by the UI layer to display app icons in lists.
     */
    fun getAppIcon(packageName: String): Drawable? {
        return try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /**
     * Helper to identify high-risk permission counts for the Dashboard.
     */
    private fun countSensitivePermissions(packageInfo: PackageInfo): Int {
        val sensitiveKeywords = listOf("CAMERA", "RECORD_AUDIO", "LOCATION", "CONTACTS", "SMS", "STORAGE")
        return packageInfo.requestedPermissions?.count { permission ->
            sensitiveKeywords.any { keyword -> permission.contains(keyword, ignoreCase = true) }
        } ?: 0
    }

    /**
     * Gets the count of user-installed apps vs system apps for the Dashboard Overview.
     */
    fun getAppCounts(): Pair<Int, Int> {
        val packages = packageManager.getInstalledPackages(0)
        val systemApps = packages.count { (it.applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM)) != 0 }
        val userApps = packages.size - systemApps
        return Pair(userApps, systemApps)
    }
}