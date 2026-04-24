package com.example.appwatch.system

import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import com.example.appwatch.data.local.entity.AppInfoEntity
import com.example.appwatch.domain.model.AppInfo
import com.example.appwatch.presentation.viewmodel.PermissionEvidence
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import android.provider.Settings

@Singleton
class PackageManagerHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storageStatsManager: android.app.usage.StorageStatsManager
) {
    private val packageManager = context.packageManager
    private val userHandle = android.os.Process.myUserHandle()
    private val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    private val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    fun getSpecialPermissions(packageName: String): List<String> {
        val enabledSpecialPerms = mutableListOf<String>()
        val appInfo = try {
            packageManager.getPackageInfo(packageName, 0).applicationInfo
        } catch (e: Exception) { null }

        val uid = appInfo?.uid ?: return emptyList()

        // 1. Accessibility Service
        val enabledAccessibilityServices = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        if (enabledAccessibilityServices.contains(packageName)) {
            enabledSpecialPerms.add("ACCESSIBILITY")
        }

        // 2. Notification Access
        val enabledNotificationListeners = Settings.Secure.getString(
            context.contentResolver, "enabled_notification_listeners"
        ) ?: ""
        if (enabledNotificationListeners.contains(packageName)) {
            enabledSpecialPerms.add("NOTIFICATION_ACCESS")
        }

        // 3. Display Over Other Apps (Overlay)
        if (checkOp(AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW, uid, packageName)) {
            enabledSpecialPerms.add("OVERLAY")
        }

        // 4. Device Admin
        val activeAdmins = dpm.activeAdmins
        val isAdmin = activeAdmins?.any { it.packageName == packageName } ?: false
        if (isAdmin) {
            enabledSpecialPerms.add("DEVICE_ADMIN")
        }

        // 5. Install Unknown Apps
        if (checkOp("android:request_install_packages", uid, packageName)) {
            enabledSpecialPerms.add("INSTALL_UNKNOWN_APPS")
        }

        // 6. Write System Settings
        if (checkOp(AppOpsManager.OPSTR_WRITE_SETTINGS, uid, packageName)) {
            enabledSpecialPerms.add("WRITE_SETTINGS")
        }

        // 7. VPN Service
        // VPN apps must declare a service with BIND_VPN_SERVICE permission.
        // We check if the app has a service that handles 'android.net.VpnService'
        val intent = android.content.Intent(android.net.VpnService.SERVICE_INTERFACE)
        intent.setPackage(packageName)
        val vpnServices = packageManager.queryIntentServices(intent, 0)
        if (vpnServices.isNotEmpty()) {
            enabledSpecialPerms.add("VPN_SERVICE")
        }

        return enabledSpecialPerms
    }

    /**
     * Helper to check AppOps permissions for other apps
     */
    private fun checkOp(op: String, uid: Int, packageName: String): Boolean {
        return try {
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(op, uid, packageName)
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(op, uid, packageName)
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }
    /**
     * Fetches metadata for all installed apps.
     * Fixed the nullability mismatch for applicationInfo.
     */
    fun getInstalledAppsMetadata(): List<AppInfoEntity> {
        val packages = packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)

        return packages.mapNotNull { packageInfo ->
            val appInfo = packageInfo.applicationInfo ?: return@mapNotNull null
            val permissions = packageInfo.requestedPermissions ?: emptyArray()

            // Skip auditing ourselves
            if (packageInfo.packageName == context.packageName) return@mapNotNull null
            var totalSize = 0L
            var cacheSize = 0L

            try {
                // This is the call that gets the real numbers
                val stats = storageStatsManager.queryStatsForPackage(
                    appInfo.storageUuid,
                    packageInfo.packageName,
                    userHandle
                )
                // App Size + Data Size + Cache Size
                totalSize = stats.appBytes + stats.dataBytes + stats.cacheBytes
                cacheSize = stats.cacheBytes
            } catch (e: Exception) {
                // Fallback to 0 if permission is missing
            }

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
                installedAt = packageInfo.firstInstallTime,
                hasLocation = permissions.contains("android.permission.ACCESS_FINE_LOCATION"),
                hasCamera = permissions.contains("android.permission.CAMERA"),
                hasMic = permissions.contains("android.permission.RECORD_AUDIO"),
                hasContacts = permissions.contains("android.permission.READ_CONTACTS"),
                hasPhone = permissions.contains("android.permission.READ_PHONE_STATE"),
                hasSms = permissions.contains("android.permission.READ_SMS"),
                totalSizeBytes = totalSize,
                cacheSizeBytes = cacheSize
            )
        }
    }

    fun hasPermission(packageName: String, permissionKeyword: String): Boolean {
        return try {
            val packageInfo = packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_PERMISSIONS
            )
            packageInfo.requestedPermissions?.any { permission ->
                permission.contains(permissionKeyword, ignoreCase = true)
            } == true
        } catch (e: Exception) {
            false
        }
    }

    fun getPermissionsForApp(packageName: String): List<PermissionEvidence> {
        return try {
            val packageInfo = packageManager.getPackageInfo(
                packageName, PackageManager.GET_PERMISSIONS
            )
            val sensitiveKeywords = listOf(
                "CAMERA", "RECORD_AUDIO", "LOCATION",
                "CONTACTS", "SMS", "CALL_LOG", "STORAGE"
            )
            packageInfo.requestedPermissions?.map { permission ->
                val shortName = permission.substringAfterLast(".")
                val isSensitive = sensitiveKeywords.any {
                    permission.contains(it, ignoreCase = true)
                }
                val isGranted = (packageInfo.requestedPermissionsFlags?.getOrNull(
                    packageInfo.requestedPermissions?.indexOf(permission) ?: -1
                ) ?: 0) and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0

                PermissionEvidence(
                    name = shortName,
                    lastAccess = if (isGranted) "Granted" else "Not granted",
                    isUnused = false,
                    isSensitive = isSensitive
                )
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getAppInfo(packageName: String): AppInfo? {
        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            val packageInfo = packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_PERMISSIONS
            )
            AppInfo(
                packageName = packageName,
                appName = packageManager.getApplicationLabel(info).toString(),
                isSystemApp = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                totalPermissions = packageInfo.requestedPermissions?.size ?: 0,
                installedAt = packageInfo.firstInstallTime,
            )
        } catch (e: PackageManager.NameNotFoundException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    fun isAppSideloaded(packageName: String): Boolean {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)

            // --- 1. SYSTEM APP CHECK ---
            // Agar check hone wali app khud system app hai (jaise default camera), toh wo safe hai.
            val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            if (isSystemApp) return false

            // --- 2. ANDROID 13+ MODERN CHECK ---
            // Naye Android versions OS level pe track karte hain ki file kahan se aayi.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val source = pm.getInstallSourceInfo(packageName).packageSource
                if (source == PackageInstaller.PACKAGE_SOURCE_DOWNLOADED_FILE ||
                    source == PackageInstaller.PACKAGE_SOURCE_LOCAL_FILE) {
                    return true // OS perfectly caught it
                }
            }

            // --- 3. TERA IDEA: AAB (Split APK) vs Monolithic APK Check ---
            // Play Store (AAB) hamesha splitSourceDirs mein multiple paths dega.
            // Chrome/File Manager se download hui single .apk file mein yeh null ya empty hoga.
            val isSingleMonolithicApk = appInfo.splitSourceDirs.isNullOrEmpty()

            // --- 4. INSTALLER DYNAMICS ---
            val installerPackage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pm.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(packageName)
            }

            // Agar installer null hai -> ADB ya old manual package installer tap.
            if (installerPackage == null) return true

            // Installer ka info nikalo
            val installerInfo = pm.getApplicationInfo(installerPackage, 0)
            val isInstallerSystemApp = (installerInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            // --- THE FINAL DYNAMIC DECISION (ZERO HARDCODING) ---

            // CONDITION A: Agar Chrome jaisi koi System App isko install kar rahi hai,
            // PAR app ek single/monolithic APK hai (AAB nahi hai), toh it's a Chrome Sideload!
            if (isInstallerSystemApp && isSingleMonolithicApk) {
                return true
            }

            // CONDITION B: Agar jis app ne isko install kiya hai (like Telegram/ShareIt),
            // wo khud ek User App hai, toh 100% Sideloaded hai.
            return !isInstallerSystemApp

        } catch (e: Exception) {
            // App uninstall ho gayi ho ya data na mile, safely false return karo
            false
        }
    }
}