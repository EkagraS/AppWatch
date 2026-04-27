package com.example.appwatch.ui.screens

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController

enum class SheetType {
    USAGE_INFO,
    PERMISSION_INFO,
    STORAGE_INFO,
    NOTIFICATION_INFO,
    PRIVACY_INSIGHTS_INFO,
    BACKGROUND_INFO,
    PRIVACY,
    TERMS,
    LICENSES,
    CONTACT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    var activeSheet by remember { mutableStateOf<SheetType?>(null) }
    var showRevokeDialog by remember { mutableStateOf<Pair<String, String>?>(null) }

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
                context, android.Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    val hasNotificationPermission = remember {
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
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
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {

            // ── 1. About ──────────────────────────────────────────────────────
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
                                .background(Color(0xFF6366F1).copy(alpha = 0.15f), CircleShape),
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
                                "AppWatch helps you understand what's happening on your phone. " +
                                        "See which apps use your camera, drain your storage, or sit unused " +
                                        "with sensitive permissions. Everything runs on your device and " +
                                        "nothing is ever shared.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // ── 2. Data Info Card ─────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF06B6D4).copy(alpha = 0.07f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF06B6D4),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "AppWatch builds your privacy report over time. The app cannot access many of the previous data and it starts collecting them the day you install it. " +
                                    "If you restart your phone today's live data resets but if you reinstall the app, everything stored is lost.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // ── 3. Permissions ────────────────────────────────────────────────
            item {
                SettingsSectionHeader("Permissions", Color(0xFFD97706))
            }

            item {
                PermissionSettingsRow(
                    title = "App Usage Access",
                    description = "Needed to show screen time, app launches and weekly activity.",
                    icon = Icons.Default.QueryStats,
                    iconColor = Color(0xFF6366F1),
                    isGranted = hasUsageAccess,
                    onActionClick = {
                        if (hasUsageAccess) {
                            showRevokeDialog = Pair(
                                Settings.ACTION_USAGE_ACCESS_SETTINGS,
                                "Turning this off will stop screen time tracking and most app analysis features."
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

            item {
                PermissionSettingsRow(
                    title = "Media Storage Access",
                    description = "Optional. Lets AppWatch show how much space your Photos, Videos and Music are using.",
                    icon = Icons.Default.PermMedia,
                    iconColor = Color(0xFF10B981),
                    isGranted = hasStoragePermission,
                    isOptional = true,
                    onActionClick = {
                        if (hasStoragePermission) {
                            showRevokeDialog = Pair(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                "Turning this off will hide the Photos, Videos and Music storage breakdown."
                            )
                        } else {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                            )
                        }
                    }
                )
            }

            item {
                PermissionSettingsRow(
                    title = "Notification Count",
                    description = "Counted using Android's usage system. No notification content is ever read.",
                    icon = Icons.Default.Notifications,
                    iconColor = Color(0xFFEF4444),
                    isGranted = hasNotificationPermission,
                    isOptional = false,
                    onActionClick = {
                        if (hasNotificationPermission) {
                            showRevokeDialog = Pair(
                                Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS,
                                "Turning this off will stop tracking how many notifications apps send you."
                            )
                        } else {
                            try {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                })
                            } catch (e: Exception) {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                            }
                        }
                    }
                )
            }

            item {
                InfoSettingsRow(
                    title = "Installed Apps Visibility",
                    description = "Granted automatically. Lets AppWatch see all apps on your device.",
                    icon = Icons.Default.Apps,
                    iconColor = Color(0xFF8B5CF6)
                )
            }

            item {
                InfoSettingsRow(
                    title = "App Permission Reading",
                    description = "Granted automatically. Used to check what permissions each app has.",
                    icon = Icons.Default.Security,
                    iconColor = Color(0xFFF59E0B)
                )
            }

            item {
                InfoSettingsRow(
                    title = "Data Usage",
                    description = "Reads mobile and Wi-Fi data used by each app. No network traffic is monitored.",
                    icon = Icons.Default.NetworkCheck,
                    iconColor = Color(0xFF06B6D4)
                )
            }

            // ── 4. How AppWatch Works ─────────────────────────────────────────
            item {
                SettingsSectionHeader("How AppWatch Works", Color(0xFF059669))
            }

            item {
                AnalysisSettingsRow(
                    title = "App Usage Tracking",
                    subtitle = "How we measure your screen time and app activity",
                    icon = Icons.Default.BarChart,
                    iconColor = Color(0xFF6366F1),
                    onClick = { activeSheet = SheetType.USAGE_INFO }
                )
            }
            item {
                AnalysisSettingsRow(
                    title = "Permission Monitoring",
                    subtitle = "How we find which apps have sensitive access",
                    icon = Icons.Default.Policy,
                    iconColor = Color(0xFFEF4444),
                    onClick = { activeSheet = SheetType.PERMISSION_INFO }
                )
            }
            item {
                AnalysisSettingsRow(
                    title = "Storage Analysis",
                    subtitle = "How we calculate how much space each app is using",
                    icon = Icons.Default.PieChart,
                    iconColor = Color(0xFF10B981),
                    onClick = { activeSheet = SheetType.STORAGE_INFO }
                )
            }
            item {
                AnalysisSettingsRow(
                    title = "Notifications & Unlocks",
                    subtitle = "How we count your daily phone unlocks and notifications",
                    icon = Icons.Default.Notifications,
                    iconColor = Color(0xFFF59E0B),
                    onClick = { activeSheet = SheetType.NOTIFICATION_INFO }
                )
            }
            item {
                AnalysisSettingsRow(
                    title = "Privacy Insights",
                    subtitle = "How we identify apps that may be a risk to your privacy",
                    icon = Icons.Default.Shield,
                    iconColor = Color(0xFF8B5CF6),
                    onClick = { activeSheet = SheetType.PRIVACY_INSIGHTS_INFO }
                )
            }
            item {
                AnalysisSettingsRow(
                    title = "Background Updates",
                    subtitle = "How AppWatch keeps your data fresh without draining battery",
                    icon = Icons.Default.Sync,
                    iconColor = Color(0xFF06B6D4),
                    onClick = { activeSheet = SheetType.BACKGROUND_INFO }
                )
            }

            // ── 5. Legal ──────────────────────────────────────────────────────
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }

            item { SettingsSectionHeader("Legal", Color(0xFF6366F1)) }

            item {
                FooterRow(
                    title = "Privacy Policy",
                    icon = Icons.Default.PrivacyTip,
                    onClick = { activeSheet = SheetType.PRIVACY }
                )
            }
            item {
                FooterRow(
                    title = "Terms of Use",
                    icon = Icons.Default.Description,
                    onClick = { activeSheet = SheetType.TERMS }
                )
            }
            item {
                FooterRow(
                    title = "Open Source Licenses",
                    icon = Icons.Default.Code,
                    onClick = { activeSheet = SheetType.LICENSES }
                )
            }

            // ── 6. Developer ──────────────────────────────────────────────────
            item { SettingsSectionHeader("Developer", Color(0xFF6B7280)) }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF6366F1).copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            "Ekagra Shandilya",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Android Developer",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                ContactLinkRow(
                    icon = Icons.Default.Code,
                    label = "GitHub — AppWatch",
                    value = "View source code",
                    color = Color(0xFF333333),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                    onClick = {
                        Toast.makeText(context, "Feature currently unavailable", Toast.LENGTH_LONG).show()
//                        context.startActivity(
//                            Intent(
//                                Intent.ACTION_VIEW,
//                                Uri.parse("https://github.com/shandilya-ekagra/AppWatch")
//                            ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
//                        )
                    }
                )
            }

            // ── 7. Version ────────────────────────────────────────────────────
            item {
                Text(
                    text = "AppWatch v1.0.0",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }

    // ── Revoke Dialog ─────────────────────────────────────────────────────────
    if (showRevokeDialog != null) {
        AlertDialog(
            onDismissRequest = { showRevokeDialog = null },
            icon = {
                Icon(Icons.Default.Warning, null, tint = Color(0xFFEF4444))
            },
            title = { Text("Turn Off Permission?", fontWeight = FontWeight.Bold) },
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
                    Text("Go to Settings", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRevokeDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ── Bottom Sheets ─────────────────────────────────────────────────────────
    if (activeSheet != null) {
        ModalBottomSheet(
            onDismissRequest = { activeSheet = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            when (activeSheet) {
                SheetType.USAGE_INFO -> UsageDetailSheet()
                SheetType.PERMISSION_INFO -> PermissionDetailSheet()
                SheetType.STORAGE_INFO -> StorageDetailSheet()
                SheetType.NOTIFICATION_INFO -> NotificationDetailSheet()
                SheetType.PRIVACY_INSIGHTS_INFO -> PrivacyInsightsDetailSheet()
                SheetType.BACKGROUND_INFO -> BackgroundDetailSheet()
                SheetType.PRIVACY -> PrivacyPolicySheet()
                SheetType.TERMS -> TermsSheet()
                SheetType.LICENSES -> LicensesSheet()
                SheetType.CONTACT -> {}
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

// ─── Permission Row ───────────────────────────────────────────────────────────

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
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
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
            color = if (isGranted) Color(0xFFEF4444).copy(alpha = 0.1f)
            else Color(0xFF6366F1).copy(alpha = 0.1f),
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

// ─── Info Row ─────────────────────────────────────────────────────────────────

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
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(Color(0xFF10B981), CircleShape)
        )
    }
}

// ─── Analysis Row ─────────────────────────────────────────────────────────────

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
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                null,
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
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
    }
}

// ─── Contact Link Row ─────────────────────────────────────────────────────────

@Composable
fun ContactLinkRow(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
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
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = color, modifier = Modifier.size(16.dp))
    }
}

// ─── Shared Sheet Point ───────────────────────────────────────────────────────

@Composable
fun SheetDetailPoint(icon: ImageVector, color: Color, title: String, desc: String) {
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
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, color = color, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(2.dp))
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
        }
    }
}

// ─── Bottom Sheets ────────────────────────────────────────────────────────────

@Composable
fun UsageDetailSheet() {
    Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 40.dp)) {
        Text("App Usage Tracking", fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text("How we measure your screen time", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(20.dp))
        SheetDetailPoint(Icons.Default.Timer, Color(0xFF6366F1), "Daily Screen Time",
            "We track how long each app is open on your screen every day. This data gets updated the moment you open the app and shown in your weekly chart.")
        SheetDetailPoint(Icons.Default.TrendingUp, Color(0xFF8B5CF6), "App Launches",
            "Every time you open an app, we count it. This helps you see which apps you reach for the most.")
        SheetDetailPoint(Icons.Default.History, Color(0xFF10B981), "Inactive Apps",
            "If an app hasn't been opened in 30, 60 or 90 days, we flag it. These apps often have sensitive permissions.")
        SheetDetailPoint(Icons.Default.DateRange, Color(0xFFF59E0B), "Active and Inactive Sessions",
            "We find your longest continuous screen-on and screen-off periods to show your phone usage patterns throughout the day.")
    }
}

@Composable
fun PermissionDetailSheet() {
    Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 40.dp)) {
        Text("Permission Monitoring", fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text("How we find which apps have sensitive access", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(20.dp))
        SheetDetailPoint(Icons.Default.Security, Color(0xFFEF4444), "What Each App Can Access",
            "We read every permission each app has been given that includes Camera, Microphone, Location and Contacts and show you the full list.")
        SheetDetailPoint(Icons.Default.AdminPanelSettings, Color(0xFF8B5CF6), "High Risk Apps",
            "Apps with special system access like reading notifications or permission to install other apps or accessibility permissions are highlighted separately as they have more power over your device.")
        SheetDetailPoint(Icons.Default.NewReleases, Color(0xFF10B981), "New Apps",
            "Any app installed in the last 7 days is listed separately.")
    }
}

@Composable
fun StorageDetailSheet() {
    Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 40.dp)) {
        Text("Storage Analysis", fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text("How we calculate how much space apps are using", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(20.dp))
        SheetDetailPoint(Icons.Default.Storage, Color(0xFF10B981), "Device Storage",
            "We read your phone's total, used and free storage directly.")
        SheetDetailPoint(Icons.Default.Apps, Color(0xFF6366F1), "Per App Breakdown",
            "For each app we calculate three things: the app's install size, data accumulated by the app, and its temporary cache files.")
        SheetDetailPoint(Icons.Default.Cached, Color(0xFFF59E0B), "Cache Files",
            "Cache is temporary data apps save to load faster. It is safe to clear and does not delete any of your personal files or settings.")
        SheetDetailPoint(Icons.Default.Photo, Color(0xFFEF4444), "Photos, Videos and Music",
            "With optional storage permission, we also show how much space your media files are taking up and are broken down by type.")
    }
}

@Composable
fun NotificationDetailSheet() {
    Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 40.dp)) {
        Text("Notifications & Unlocks", fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text("How we count your daily activity", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(20.dp))
        SheetDetailPoint(Icons.Default.LockOpen, Color(0xFF6366F1), "Phone Unlocks",
            "Every time you unlock your screen, we count it. This gives you a simple picture of how often you pick up your phone.")
        SheetDetailPoint(Icons.Default.Notifications, Color(0xFFF59E0B), "Notification Count",
            "We count how many notifications each app sends you using Android's usage system. We never read or store the content of any notification.")
        SheetDetailPoint(Icons.Default.NetworkCheck, Color(0xFF10B981), "Data Usage",
            "We show how much mobile and Wi-Fi data each app has used today. No actual network traffic is monitored or stored.")
        SheetDetailPoint(Icons.Default.Info, Color(0xFF8B5CF6), "Today Only",
            "Unlock and notification counts reset every day. They show your activity for the current day only.")
    }
}

@Composable
fun PrivacyInsightsDetailSheet() {
    Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 40.dp)) {
        Text("Privacy Insights", fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text("How we identify privacy risks on your phone", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(20.dp))
        SheetDetailPoint(Icons.Default.LocationOn, Color(0xFFEF4444), "Location, Camera and Mic",
            "We show you exactly how many apps have access to your location, camera and microphone. Tapping any of these shows the full list of those apps.")
        SheetDetailPoint(Icons.Default.Contacts, Color(0xFF10B981), "Contacts, SMS and Calls",
            "Apps with access to your contacts, messages and call history are listed separately as these are among the most personal types of data.")
        SheetDetailPoint(Icons.Default.Shield, Color(0xFF8B5CF6), "Needs Attention",
            "We combine unused app data and permission data to highlight the apps that pose the biggest risk like an old app that still has your camera access.")
        SheetDetailPoint(Icons.Default.Timeline, Color(0xFFF59E0B), "Recent Activity",
            "We track which apps were installed, updated or removed this week so you always know what changed on your phone recently.")
    }
}

@Composable
fun BackgroundDetailSheet() {
    Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 40.dp)) {
        Text("Background Updates", fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text("How AppWatch keeps your data fresh", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(20.dp))
        SheetDetailPoint(Icons.Default.Sync, Color(0xFF06B6D4), "Daily Refresh",
            "Once a day, AppWatch quietly updates your app list, storage data and usage history in the background. You don't need to do anything.")
        SheetDetailPoint(Icons.Default.BatteryFull, Color(0xFF10B981), "Battery Friendly",
            "We use Android's WorkManager which is designed to run tasks efficiently without draining your battery or slowing your phone.")
        SheetDetailPoint(Icons.Default.Speed, Color(0xFF6366F1), "Instant Loading",
            "When you open AppWatch, it shows your last saved data immediately while quietly fetching any updates in the background.")
        SheetDetailPoint(Icons.Default.WifiOff, Color(0xFFF59E0B), "Works Offline",
            "AppWatch does not need internet to work. All data is read directly from your device and stored locally.")
    }
}

@Composable
fun PrivacyPolicySheet() {
    Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 40.dp)) {
        Text("Privacy Policy", fontSize = 20.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(16.dp))
        SheetDetailPoint(Icons.Default.PhoneAndroid, Color(0xFF6366F1), "Everything Stays on Your Phone",
            "All analysis including screen time, permissions, storage happens entirely on your device. Nothing is ever sent anywhere.")
        SheetDetailPoint(Icons.Default.Block, Color(0xFFEF4444), "No Tracking at All",
            "AppWatch has no third-party SDKs. We have no way to track or store any data.")
        SheetDetailPoint(Icons.Default.WifiOff, Color(0xFF10B981), "No Internet Permission",
            "AppWatch does not have internet permission. It is technically impossible for it to send any data anywhere.")
        SheetDetailPoint(Icons.Default.DeleteForever, Color(0xFFF59E0B), "Delete Anytime",
            "Uninstalling AppWatch removes everything. No data remains on your phone or anywhere else after uninstall.")
    }
}

@Composable
fun TermsSheet() {
    Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 40.dp)) {
        Text("Terms of Use", fontSize = 20.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(16.dp))
        SheetDetailPoint(Icons.Default.CheckCircle, Color(0xFF6366F1), "Free to Use",
            "AppWatch is completely free. There are no subscriptions, no in-app purchases and no hidden charges.")
        SheetDetailPoint(Icons.Default.Info, Color(0xFF10B981), "For Information Only",
            "AppWatch shows you information about your device. It does not make any changes to your apps, settings or data on your behalf.")
        SheetDetailPoint(Icons.Default.Warning, Color(0xFFF59E0B), "No Guarantee",
            "We do our best to keep data accurate but some readings may vary depending on your device model or Android version.")
        SheetDetailPoint(Icons.Default.Security, Color(0xFFEF4444), "Your Responsibility",
            "Any action you take based on AppWatch's information like revoking a permission is your own decision. We are not responsible for any outcome.")
    }
}

@Composable
fun LicensesSheet() {
    Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 40.dp)) {
        Text("Open Source Licenses", fontSize = 20.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "AppWatch is built using the following open source libraries. We are grateful to their developers.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        listOf(
            "Jetpack Compose" to "Apache License 2.0",
            "Hilt" to "Apache License 2.0",
            "Room" to "Apache License 2.0",
            "WorkManager" to "Apache License 2.0",
            "Coil" to "Apache License 2.0",
            "Kotlin" to "Apache License 2.0",
            "Kotlin Coroutines" to "Apache License 2.0"
        ).forEach { (library, license) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(library, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                Text(license, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }

        Spacer(modifier = Modifier.height(16.dp))
        val annotatedString = buildAnnotatedString {
            append("All libraries above are licensed under Apache License 2.0, to view full license ")
            withLink(
                LinkAnnotation.Url(
                    url = "https://www.apache.org/licenses/LICENSE-2.0",
                    styles = TextLinkStyles(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary, // Link color
                            textDecoration = TextDecoration.Underline // Underline for clarity
                        )
                    )
                )
            ) {
                append("click here")
            }
        }
        Text(
            text = annotatedString,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
        )
    }
}