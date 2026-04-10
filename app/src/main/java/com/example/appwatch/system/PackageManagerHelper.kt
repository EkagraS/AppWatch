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

@Singleton
class PackageManagerHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val packageManager: PackageManager = context.packageManager

    /**
     * Fetches metadata for all installed apps.
     * Fixed the nullability mismatch for applicationInfo.
     */
    fun getInstalledAppsMetadata(): List<AppInfoEntity> {
        val packages = packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)

        return packages.mapNotNull { packageInfo ->
            val appInfo = packageInfo.applicationInfo ?: return@mapNotNull null

            // Skip auditing ourselves
            if (packageInfo.packageName == context.packageName) return@mapNotNull null

            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            // Count sensitive permissions
            val sensitiveCount = packageInfo.requestedPermissions?.count { perm ->
                val p = perm.uppercase()
                p.contains("CAMERA") || p.contains("RECORD_AUDIO") ||
                        p.contains("LOCATION") || p.contains("SMS") || p.contains("CONTACTS")
            } ?: 0

            AppInfoEntity(
                packageName = packageInfo.packageName,
                appName = packageManager.getApplicationLabel(appInfo).toString(),
                totalPermissions = packageInfo.requestedPermissions?.size ?: 0,
                sensitivePermissionsCount = sensitiveCount,
                isSystemApp = isSystem,
                installedAt = packageInfo.firstInstallTime
            )
        }
    }

    fun getAppIcon(packageName: String): Drawable? {
        return try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun getAppCounts(): Pair<Int, Int> {
        val packages = packageManager.getInstalledPackages(0)
        val systemApps = packages.count { (it.applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM)) != 0 }
        val userApps = packages.size - systemApps
        return Pair(userApps, systemApps)
    }

    /**
     * Helper to check specific counts for the Dashboard Insights grid.
     */
    fun getAppsWithPermission(permissionSubString: String): Int {
        val packages = packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        return packages.count { pkg ->
            pkg.requestedPermissions?.any {
                it.contains(permissionSubString, ignoreCase = true)
            } == true
        }
    }
}