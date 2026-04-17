package com.example.appwatch.ui.screens

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController

enum class SheetType {
    USAGE_INFO,
    PERMISSION_INFO,
    STORAGE_INFO,
    PRIVACY,
    CONTACT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    var activeSheet by remember { mutableStateOf<SheetType?>(null) }
    var showRevokeDialog by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Check permission states
    val hasUsageAccess = remember {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        mode == AppOpsManager.MODE_ALLOWED
    }

    val hasStoragePermission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {

            // 1. App Description
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF6366F1).copy(alpha = 0.08f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    Color(0xFF6366F1).copy(alpha = 0.15f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color(0xFF6366F1),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "AppWatch",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF6366F1)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "A system-level privacy monitor that tracks app usage, " +
                                        "audits permissions and analyzes storage. " +
                                        "Everything runs locally — your data never leaves your device.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // 2. System Access & Permissions
            item {
                SettingsSectionHeader(
                    title = "System Access",
                    color = Color(0xFFD97706)
                )
            }

            // Usage Access
            item {
                PermissionSettingsRow(
                    title = "App Usage Access",
                    description = "Required to track screen time, launch count and app activity.",
                    icon = Icons.Default.QueryStats,
                    iconColor = Color(0xFF6366F1),
                    isGranted = hasUsageAccess,
                    onActionClick = {
                        if (hasUsageAccess) {
                            showRevokeDialog = Pair(
                                Settings.ACTION_USAGE_ACCESS_SETTINGS,
                                "Revoking this will disable screen time tracking and usage statistics."
                            )
                        } else {
                            context.startActivity(
                                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                            )
                        }
                    }
                )
            }

            // Storage Permission
            item {
                PermissionSettingsRow(
                    title = "Media Storage Access",
                    description = "Optional. Enables breakdown of Photos, Videos and Music storage.",
                    icon = Icons.Default.Storage,
                    iconColor = Color(0xFF10B981),
                    isGranted = hasStoragePermission,
                    isOptional = true,
                    onActionClick = {
                        if (hasStoragePermission) {
                            showRevokeDialog = Pair(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                "Revoking this will hide the media storage breakdown (Photos, Videos, Music)."
                            )
                        } else {
                            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            } else {
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            }
                            context.startActivity(
                                Intent(permissions).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                            )
                        }
                    }
                )
            }

            // Auto permissions — no button, just info
            item {
                InfoSettingsRow(
                    title = "Installed Apps Visibility",
                    description = "Automatically granted. Used to scan and list all installed packages.",
                    icon = Icons.Default.Apps,
                    iconColor = Color(0xFF8B5CF6)
                )
            }
            item {
                InfoSettingsRow(
                    title = "App Permission Reading",
                    description = "Automatically granted. Used to read what permissions each app has declared.",
                    icon = Icons.Default.Security,
                    iconColor = Color(0xFFF59E0B)
                )
            }
            item {
                InfoSettingsRow(
                    title = "Background Processing",
                    description = "Used by WorkManager to refresh app data daily without draining battery.",
                    icon = Icons.Default.Sync,
                    iconColor = Color(0xFF06B6D4)
                )
            }

            // 3. How We Analyze
            item {
                SettingsSectionHeader(
                    title = "How We Analyze",
                    color = Color(0xFF059669)
                )
            }

            item {
                AnalysisSettingsRow(
                    title = "Usage Statistics",
                    subtitle = "Screen time, launches and activity tracking",
                    icon = Icons.Default.BarChart,
                    iconColor = Color(0xFF6366F1),
                    onClick = { activeSheet = SheetType.USAGE_INFO }
                )
            }
            item {
                AnalysisSettingsRow(
                    title = "Permission Monitoring",
                    subtitle = "Background sensor and sensitive access detection",
                    icon = Icons.Default.Policy,
                    iconColor = Color(0xFFEF4444),
                    onClick = { activeSheet = SheetType.PERMISSION_INFO }
                )
            }
            item {
                AnalysisSettingsRow(
                    title = "Storage Analytics",
                    subtitle = "App size, data, cache and media breakdown",
                    icon = Icons.Default.PieChart,
                    iconColor = Color(0xFF10B981),
                    onClick = { activeSheet = SheetType.STORAGE_INFO }
                )
            }

            // 4. Divider
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }

            // 5. Footer rows
            item {
                FooterRow(
                    title = "Privacy Policy",
                    icon = Icons.Default.PrivacyTip,
                    onClick = { activeSheet = SheetType.PRIVACY }
                )
            }
            item {
                FooterRow(
                    title = "Contact Developer",
                    icon = Icons.Default.Person,
                    onClick = { activeSheet = SheetType.CONTACT }
                )
            }

            // Version
            item {
                Text(
                    text = "AppWatch v1.0.0",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }

    // Revoke confirmation dialog
    if (showRevokeDialog != null) {
        AlertDialog(
            onDismissRequest = { showRevokeDialog = null },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFEF4444)
                )
            },
            title = { Text("Revoke Permission?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    showRevokeDialog!!.second,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val action = showRevokeDialog!!.first
                    showRevokeDialog = null
                    context.startActivity(
                        Intent(action).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    )
                }) {
                    Text("GO TO SETTINGS", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRevokeDialog = null }) {
                    Text("CANCEL")
                }
            }
        )
    }

    // Bottom sheets
    if (activeSheet != null) {
        ModalBottomSheet(
            onDismissRequest = { activeSheet = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            when (activeSheet) {
                SheetType.USAGE_INFO -> UsageDetailSheet()
                SheetType.PERMISSION_INFO -> PermissionDetailSheet()
                SheetType.STORAGE_INFO -> StorageDetailSheet()
                SheetType.PRIVACY -> PrivacyPolicySheet()
                SheetType.CONTACT -> ContactSheet(context)
                else -> {}
            }
        }
    }
}

// ─── Section Header ───────────────────────────────────────────────────────────

@Composable
fun SettingsSectionHeader(title: String, color: Color) {
    Text(
        text = title,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp, end = 16.dp),
        color = color,
        fontWeight = FontWeight.Black,
        style = MaterialTheme.typography.labelLarge,
        letterSpacing = 0.5.sp
    )
}

// ─── Permission Row (with Enable/Revoke button) ───────────────────────────────

@Composable
fun PermissionSettingsRow(
    title: String,
    description: String,
    icon: ImageVector,
    iconColor: Color,
    isGranted: Boolean,
    isOptional: Boolean = false,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconColor.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (isOptional) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "Optional",
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Surface(
            onClick = onActionClick,
            color = if (isGranted)
                Color(0xFFEF4444).copy(alpha = 0.1f)
            else
                Color(0xFF6366F1).copy(alpha = 0.1f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = if (isGranted) "Revoke" else "Enable",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isGranted) Color(0xFFEF4444) else Color(0xFF6366F1)
            )
        }
    }
}

// ─── Info Row (no button, auto-granted) ──────────────────────────────────────

@Composable
fun InfoSettingsRow(
    title: String,
    description: String,
    icon: ImageVector,
    iconColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconColor.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        // Green dot indicating auto-granted
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(Color(0xFF10B981), CircleShape)
        )
    }
}

// ─── Analysis Row (opens bottom sheet) ───────────────────────────────────────

@Composable
fun AnalysisSettingsRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(iconColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ─── Footer Row ───────────────────────────────────────────────────────────────

@Composable
fun FooterRow(title: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            title,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}

// ─── Bottom Sheets ────────────────────────────────────────────────────────────

@Composable
fun UsageDetailSheet() {
    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .padding(bottom = 40.dp)
    ) {
        Text("Usage Statistics", fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text(
            "How AppWatch tracks your app activity",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))
        SheetDetailPoint(
            icon = Icons.Default.Timer,
            color = Color(0xFF6366F1),
            title = "Daily Screen Time",
            desc = "Uses Android's UsageStatsManager to measure exactly how long each app is in the foreground every day. Data is snapshotted nightly and stored locally."
        )
        SheetDetailPoint(
            icon = Icons.Default.TrendingUp,
            color = Color(0xFF8B5CF6),
            title = "Launch Count",
            desc = "Tracks how many times you open each app per day using UsageEvents. Helps identify habitually overused apps."
        )
        SheetDetailPoint(
            icon = Icons.Default.History,
            color = Color(0xFF10B981),
            title = "Inactive App Detection",
            desc = "Apps not opened in 30, 60 or 90 days are flagged. These often retain permissions they no longer need."
        )
        SheetDetailPoint(
            icon = Icons.Default.FiberNew,
            color = Color(0xFFF59E0B),
            title = "Recent Installations",
            desc = "PackageManager provides the exact install date of every app. New installs within 7 days are highlighted for review."
        )
    }
}

@Composable
fun PermissionDetailSheet() {
    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .padding(bottom = 40.dp)
    ) {
        Text("Permission Monitoring", fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text(
            "How AppWatch audits sensitive access",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))
        SheetDetailPoint(
            icon = Icons.Default.Security,
            color = Color(0xFFEF4444),
            title = "Permission Grants",
            desc = "PackageManager reads every permission each app has declared and whether it is currently granted or denied by the user."
        )
        SheetDetailPoint(
            icon = Icons.Default.Camera,
            color = Color(0xFF8B5CF6),
            title = "Sensor Access History",
            desc = "AppOpsManager tracks the last time an app accessed Camera, Microphone, Location, Contacts or SMS. This reveals hidden background activity."
        )
        SheetDetailPoint(
            icon = Icons.Default.Warning,
            color = Color(0xFFF59E0B),
            title = "Unused Permission Detection",
            desc = "If an app holds a sensitive permission but hasn't used it in 30+ days, it is flagged. These are prime candidates for manual revocation."
        )
        SheetDetailPoint(
            icon = Icons.Default.Visibility,
            color = Color(0xFF06B6D4),
            title = "Background Activity",
            desc = "ActivityManager detects apps running foreground services — processes that stay active even when you aren't using the app."
        )
    }
}

@Composable
fun StorageDetailSheet() {
    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .padding(bottom = 40.dp)
    ) {
        Text("Storage Analytics", fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text(
            "How AppWatch measures your storage",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))
        SheetDetailPoint(
            icon = Icons.Default.Storage,
            color = Color(0xFF10B981),
            title = "Device Storage",
            desc = "StatFs reads your device's total, used and free storage directly from the filesystem in real time."
        )
        SheetDetailPoint(
            icon = Icons.Default.Apps,
            color = Color(0xFF6366F1),
            title = "Per-App Breakdown",
            desc = "StorageStatsManager (requires Usage Access) calculates the exact APK size, personal data and cache for every installed app."
        )
        SheetDetailPoint(
            icon = Icons.Default.Photo,
            color = Color(0xFFEF4444),
            title = "Media Storage",
            desc = "With optional storage permission, AppWatch calculates the total size of your Photos, Videos, Music and Downloads folders."
        )
        SheetDetailPoint(
            icon = Icons.Default.Cached,
            color = Color(0xFFF59E0B),
            title = "Cache Identification",
            desc = "Cache files are temporary data apps create for faster loading. They can safely be cleared from Android Settings without losing personal data."
        )
    }
}

@Composable
fun PrivacyPolicySheet() {
    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .padding(bottom = 40.dp)
    ) {
        Text("Privacy Policy", fontSize = 20.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(16.dp))
        SheetDetailPoint(
            icon = Icons.Default.PhoneAndroid,
            color = Color(0xFF6366F1),
            title = "100% Local Processing",
            desc = "Every analysis — usage, permissions, storage — happens entirely on your device. Nothing is sent to any server."
        )
        SheetDetailPoint(
            icon = Icons.Default.Block,
            color = Color(0xFFEF4444),
            title = "No Tracking",
            desc = "AppWatch has no analytics, no crash reporting tools and no third-party SDKs. There is nothing to track you with."
        )
        SheetDetailPoint(
            icon = Icons.Default.Lock,
            color = Color(0xFF10B981),
            title = "No Internet Permission",
            desc = "AppWatch does not have INTERNET permission in its manifest. It is physically incapable of sending data anywhere."
        )
        SheetDetailPoint(
            icon = Icons.Default.DeleteForever,
            color = Color(0xFFF59E0B),
            title = "Your Data, Your Control",
            desc = "Uninstalling AppWatch permanently deletes all locally stored data. Nothing remains on your device or anywhere else."
        )
    }
}

@Composable
fun ContactSheet(context: Context) {
    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .padding(bottom = 40.dp)
    ) {
        Text("Contact Developer", fontSize = 20.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "Ekagra Shandilya",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Android Developer · LNMIIT Jaipur",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        ContactLinkRow(
            icon = Icons.Default.Person,
            label = "LinkedIn",
            value = "linkedin.com/in/ekagrashandilya",
            color = Color(0xFF0077B5),
            onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://linkedin.com/in/ekagrashandilya")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                )
            }
        )
        ContactLinkRow(
            icon = Icons.Default.Code,
            label = "GitHub",
            value = "github.com/shandilya-ekagra",
            color = Color(0xFF333333),
            onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/shandilya-ekagra")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                )
            }
        )
        ContactLinkRow(
            icon = Icons.Default.Email,
            label = "Email",
            value = "ekagra@lnmiit.ac.in",
            color = Color(0xFFEF4444),
            onClick = {
                context.startActivity(
                    Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:ekagra@lnmiit.ac.in")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                )
            }
        )
    }
}

// ─── Shared Sheet Components ──────────────────────────────────────────────────

@Composable
fun SheetDetailPoint(
    icon: ImageVector,
    color: Color,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(color.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, color = color, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(2.dp))
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
        }
    }
}

@Composable
fun ContactLinkRow(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(color.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
    }
}