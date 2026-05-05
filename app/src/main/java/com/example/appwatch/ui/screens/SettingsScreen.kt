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
import androidx.compose.foundation.rememberScrollState // Added for sheets
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll // Added for sheets
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.appwatch.R
import com.example.appwatch.ui.theme.BackgroundLight
import com.example.appwatch.ui.theme.Teal50
import com.example.appwatch.ui.theme.TextPrimary

enum class SheetType {
    USAGE_INFO,
    PERMISSION_INFO,
    STORAGE_INFO,
    NOTIFICATION_INFO,
    BACKGROUND_INFO,
    PRIVACY,
    TERMS,
    LICENSES,
    CONTACT,
    ABOUT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val toastMsg = stringResource(R.string.settings_toast_unavailable)

    val context = LocalContext.current
    var activeSheet by remember { mutableStateOf<SheetType?>(null) }
    var showRevokeDialog by remember { mutableStateOf<Pair<String, Int>?>(null) }

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

    var hasNotificationPermission = remember {
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationPermission =
                    NotificationManagerCompat.getEnabledListenerPackages(context)
                        .contains(context.packageName)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        rememberTopAppBarState()
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = BackgroundLight,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BackgroundLight,
                    scrolledContainerColor = Teal50,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary,
                    actionIconContentColor = TextPrimary
                )
            )
        }
    ) { padding ->
        // LazyColumn already provides scrolling, so no changes made here
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
                                stringResource(R.string.app_name),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF6366F1)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.settings_about_desc),
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
                            stringResource(R.string.settings_data_info_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // ── 3. Permissions ────────────────────────────────────────────────
            item {
                SettingsSectionHeader(stringResource(R.string.settings_header_permissions), Color(0xFFD97706))
            }

            item {
                PermissionSettingsRow(
                    title = stringResource(R.string.settings_perm_usage_title),
                    description = stringResource(R.string.settings_perm_usage_desc),
                    icon = Icons.Default.QueryStats,
                    iconColor = Color(0xFF6366F1),
                    isGranted = hasUsageAccess,
                    onActionClick = {
                        if (hasUsageAccess) {
                            showRevokeDialog = Pair(
                                Settings.ACTION_USAGE_ACCESS_SETTINGS,
                                R.string.revoke_msg_usage
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
                    title = stringResource(R.string.settings_perm_storage_title),
                    description = stringResource(R.string.settings_perm_storage_desc),
                    icon = Icons.Default.PermMedia,
                    iconColor = Color(0xFF10B981),
                    isGranted = hasStoragePermission,
                    onActionClick = {
                        if (hasStoragePermission) {
                            showRevokeDialog = Pair(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                R.string.revoke_msg_storage
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
                    title = stringResource(R.string.settings_perm_notif_title),
                    description = stringResource(R.string.settings_perm_notif_desc),
                    icon = Icons.Default.Notifications,
                    iconColor = Color(0xFFEF4444),
                    isGranted = hasNotificationPermission,
                    onActionClick = {
                        if (hasNotificationPermission) {
                            showRevokeDialog = Pair(
                                Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS,
                                R.string.revoke_msg_notif
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
                    title = stringResource(R.string.settings_perm_apps_title),
                    description = stringResource(R.string.settings_perm_apps_desc),
                    icon = Icons.Default.Apps,
                    iconColor = Color(0xFF8B5CF6)
                )
            }

            item {
                InfoSettingsRow(
                    title = stringResource(R.string.settings_perm_reading_title),
                    description = stringResource(R.string.settings_perm_reading_desc),
                    icon = Icons.Default.Security,
                    iconColor = Color(0xFFF59E0B)
                )
            }

            item {
                InfoSettingsRow(
                    title = stringResource(R.string.settings_perm_data_title),
                    description = stringResource(R.string.settings_perm_data_desc),
                    icon = Icons.Default.NetworkCheck,
                    iconColor = Color(0xFF06B6D4)
                )
            }

            // ── 4. How AppWatch Works ─────────────────────────────────────────
            item {
                SettingsSectionHeader(stringResource(R.string.settings_header_how_it_works), Color(0xFF059669))
            }

            item {
                AnalysisSettingsRow(
                    title = stringResource(R.string.settings_how_usage_title),
                    subtitle = stringResource(R.string.settings_how_usage_subtitle),
                    icon = Icons.Default.BarChart,
                    iconColor = Color(0xFF6366F1),
                    onClick = { activeSheet = SheetType.USAGE_INFO }
                )
            }
            item {
                AnalysisSettingsRow(
                    title = stringResource(R.string.settings_how_perm_title),
                    subtitle = stringResource(R.string.settings_how_perm_subtitle),
                    icon = Icons.Default.Policy,
                    iconColor = Color(0xFFEF4444),
                    onClick = { activeSheet = SheetType.PERMISSION_INFO }
                )
            }
            item {
                AnalysisSettingsRow(
                    title = stringResource(R.string.settings_how_storage_title),
                    subtitle = stringResource(R.string.settings_how_storage_subtitle),
                    icon = Icons.Default.PieChart,
                    iconColor = Color(0xFF10B981),
                    onClick = { activeSheet = SheetType.STORAGE_INFO }
                )
            }
            item {
                AnalysisSettingsRow(
                    title = stringResource(R.string.settings_how_notif_title),
                    subtitle = stringResource(R.string.settings_how_notif_subtitle),
                    icon = Icons.Default.Notifications,
                    iconColor = Color(0xFFF59E0B),
                    onClick = { activeSheet = SheetType.NOTIFICATION_INFO }
                )
            }
            item {
                AnalysisSettingsRow(
                    title = stringResource(R.string.settings_how_background_title),
                    subtitle = stringResource(R.string.settings_how_background_subtitle),
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

            item { SettingsSectionHeader(stringResource(R.string.settings_header_legal), Color(0xFF6366F1)) }

            item {
                FooterRow(
                    title = stringResource(R.string.legal_privacy),
                    icon = Icons.Default.PrivacyTip,
                    onClick = { activeSheet = SheetType.PRIVACY }
                )
            }
            item {
                FooterRow(
                    title = stringResource(R.string.legal_terms),
                    icon = Icons.Default.Description,
                    onClick = { activeSheet = SheetType.TERMS }
                )
            }
            item {
                FooterRow(
                    title = stringResource(R.string.legal_licenses),
                    icon = Icons.Default.Code,
                    onClick = { activeSheet = SheetType.LICENSES }
                )
            }

            // ── 6. Developer ──────────────────────────────────────────────────
            item { SettingsSectionHeader(stringResource(R.string.settings_header_developer), Color(0xFF6B7280)) }

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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Ekagra Shandilya", //
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = stringResource(R.string.settings_dev_role), //
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(onClick = {
                                    try {
                                        val intent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://github.com/EkagraS/AppWatch/")
                                        )
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Code, // GitHub ke liye
                                        contentDescription = "GitHub",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                IconButton(onClick = {
                                    try {
                                        val intent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://www.linkedin.com/in/ekagra-shandilya-3944a0256") // Apna actual GitHub link daal dena
                                        )
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Link,
                                        contentDescription = "LinkedIn",
                                        tint = Color(0xFF0077B5),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                ContactLinkRow(
                    icon = Icons.Default.Info,
                    label = "About AppWatch",
                    value = "Version v1",
                    color = Color(0xFF6366F1),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                    onClick = { activeSheet = SheetType.ABOUT }
                )
                ContactLinkRow(
                    icon = Icons.Default.Code,
                    label = stringResource(R.string.settings_dev_github_label),
                    value = stringResource(R.string.settings_dev_github_value),
                    color = Color(0xFF333333),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                    onClick = {
                        try {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/EkagraS/") // Apna actual GitHub link daal dena
                            )
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                ContactLinkRow(
                    icon = Icons.Default.Chat, // Feedback ke liye chat icon
                    label = "Feedback & Suggestions",
                    value = "Report a bug or suggest a feature",
                    color = Color(0xFF10B981), // Solid Green
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                    onClick = {
                        try {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://forms.gle/your_link") // Tera Google Form link
                            )
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot open form", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // ── 7. Version ────────────────────────────────────────────────────
            item {
                Text(
                    text = stringResource(R.string.settings_version_text, stringResource(R.string.app_version)),
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
            title = { Text(stringResource(R.string.dialog_revoke_title), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    stringResource(showRevokeDialog!!.second),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val action = showRevokeDialog!!.first
                    showRevokeDialog = null

                    val intent = Intent(action).apply {
                        if (action == Settings.ACTION_APPLICATION_DETAILS_SETTINGS) {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                }) {
                    Text(stringResource(R.string.btn_go_to_settings), color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRevokeDialog = null }) {
                    Text(stringResource(R.string.btn_cancel))
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
                SheetType.BACKGROUND_INFO -> BackgroundDetailSheet()
                SheetType.PRIVACY -> PrivacyPolicySheet()
                SheetType.TERMS -> TermsSheet()
                SheetType.LICENSES -> LicensesSheet()
                SheetType.CONTACT -> {}
                SheetType.ABOUT -> AboutDetailSheet()
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
            Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
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
                text = if (isGranted) stringResource(R.string.btn_revoke) else stringResource(R.string.btn_enable),
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

// ─── Bottom Sheets (Added verticalScroll to handle overflow) ──────────────────

@Composable
fun UsageDetailSheet() {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier
        .padding(horizontal = 24.dp)
        .padding(bottom = 40.dp)
        .verticalScroll(scrollState)
    ) {
        Text(stringResource(R.string.settings_how_usage_title), fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text(stringResource(R.string.sheet_usage_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(20.dp))
        SheetDetailPoint(Icons.Default.Timer, Color(0xFF6366F1), stringResource(R.string.sheet_usage_point1_title),
            stringResource(R.string.sheet_usage_point1_desc))
        SheetDetailPoint(Icons.Default.TrendingUp, Color(0xFF8B5CF6), stringResource(R.string.sheet_usage_point2_title),
            stringResource(R.string.sheet_usage_point2_desc))
        SheetDetailPoint(Icons.Default.History, Color(0xFF10B981), stringResource(R.string.sheet_usage_point3_title),
            stringResource(R.string.sheet_usage_point3_desc))
        SheetDetailPoint(Icons.Default.DateRange, Color(0xFFF59E0B), stringResource(R.string.sheet_usage_point4_title),
            stringResource(R.string.sheet_usage_point4_desc))
    }
}

@Composable
fun PermissionDetailSheet() {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier
        .padding(horizontal = 24.dp)
        .padding(bottom = 40.dp)
        .verticalScroll(scrollState)
    ) {
        Text(stringResource(R.string.settings_how_perm_title), fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text(stringResource(R.string.sheet_perm_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(20.dp))
        SheetDetailPoint(Icons.Default.Security, Color(0xFFEF4444), stringResource(R.string.sheet_perm_point1_title),
            stringResource(R.string.sheet_perm_point1_desc))
        SheetDetailPoint(Icons.Default.AdminPanelSettings, Color(0xFF8B5CF6), stringResource(R.string.sheet_perm_point2_title),
            stringResource(R.string.sheet_perm_point2_desc))
        SheetDetailPoint(Icons.Default.NewReleases, Color(0xFF10B981), stringResource(R.string.sheet_perm_point3_title),
            stringResource(R.string.sheet_perm_point3_desc))
    }
}

@Composable
fun StorageDetailSheet() {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier
        .padding(horizontal = 24.dp)
        .padding(bottom = 40.dp)
        .verticalScroll(scrollState)
    ) {
        Text(stringResource(R.string.settings_how_storage_title), fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text(stringResource(R.string.sheet_storage_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(20.dp))
        SheetDetailPoint(Icons.Default.Storage, Color(0xFF10B981), stringResource(R.string.sheet_storage_point1_title),
            stringResource(R.string.sheet_storage_point1_desc))
        SheetDetailPoint(Icons.Default.Apps, Color(0xFF6366F1), stringResource(R.string.sheet_storage_point2_title),
            stringResource(R.string.sheet_storage_point2_desc))
        SheetDetailPoint(Icons.Default.Cached, Color(0xFFF59E0B), stringResource(R.string.sheet_storage_point3_title),
            stringResource(R.string.sheet_storage_point3_desc))
        SheetDetailPoint(Icons.Default.Photo, Color(0xFFEF4444), stringResource(R.string.sheet_storage_point4_title),
            stringResource(R.string.sheet_storage_point4_desc))
    }
}

@Composable
fun NotificationDetailSheet() {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier
        .padding(horizontal = 24.dp)
        .padding(bottom = 40.dp)
        .verticalScroll(scrollState)
    ) {
        Text(stringResource(R.string.settings_how_notif_title), fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text(stringResource(R.string.sheet_notif_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(20.dp))
        SheetDetailPoint(Icons.Default.LockOpen, Color(0xFF6366F1), stringResource(R.string.sheet_notif_point1_title),
            stringResource(R.string.sheet_notif_point1_desc))
        SheetDetailPoint(Icons.Default.Notifications, Color(0xFFF59E0B), stringResource(R.string.sheet_notif_point2_title),
            stringResource(R.string.sheet_notif_point2_desc))
        SheetDetailPoint(Icons.Default.NetworkCheck, Color(0xFF10B981), stringResource(R.string.sheet_notif_point3_title),
            stringResource(R.string.sheet_notif_point3_desc))
        SheetDetailPoint(Icons.Default.Info, Color(0xFF8B5CF6), stringResource(R.string.sheet_notif_point4_title),
            stringResource(R.string.sheet_notif_point4_desc))
    }
}

@Composable
fun BackgroundDetailSheet() {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier
        .padding(horizontal = 24.dp)
        .padding(bottom = 40.dp)
        .verticalScroll(scrollState)
    ) {
        Text(stringResource(R.string.settings_how_background_title), fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text(stringResource(R.string.sheet_bg_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(20.dp))
        SheetDetailPoint(Icons.Default.Sync, Color(0xFF06B6D4), stringResource(R.string.sheet_bg_point1_title),
            stringResource(R.string.sheet_bg_point1_desc))
        SheetDetailPoint(Icons.Default.BatteryFull, Color(0xFF10B981), stringResource(R.string.sheet_bg_point2_title),
            stringResource(R.string.sheet_bg_point2_desc))
        SheetDetailPoint(Icons.Default.Speed, Color(0xFF6366F1), stringResource(R.string.sheet_bg_point3_title),
            stringResource(R.string.sheet_bg_point3_desc))
        SheetDetailPoint(Icons.Default.WifiOff, Color(0xFFF59E0B), stringResource(R.string.sheet_bg_point4_title),
            stringResource(R.string.sheet_bg_point4_desc))
    }
}

@Composable
fun PrivacyPolicySheet() {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier
        .padding(horizontal = 24.dp)
        .padding(bottom = 40.dp)
        .verticalScroll(scrollState)
    ) {
        Text(stringResource(R.string.legal_privacy), fontSize = 20.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(16.dp))
        SheetDetailPoint(Icons.Default.PhoneAndroid, Color(0xFF6366F1), stringResource(R.string.sheet_policy_point1_title),
            stringResource(R.string.sheet_policy_point1_desc))
        SheetDetailPoint(Icons.Default.Block, Color(0xFFEF4444), stringResource(R.string.sheet_policy_point2_title),
            stringResource(R.string.sheet_policy_point2_desc))
        SheetDetailPoint(Icons.Default.WifiOff, Color(0xFF10B981), stringResource(R.string.sheet_policy_point3_title),
            stringResource(R.string.sheet_policy_point3_desc))
        SheetDetailPoint(Icons.Default.DeleteForever, Color(0xFFF59E0B), stringResource(R.string.sheet_policy_point4_title),
            stringResource(R.string.sheet_policy_point4_desc))
        SheetDetailPoint(Icons.Default.EnhancedEncryption, Color(0xFF06B6D4), stringResource(R.string.sheet_policy_point5_title),
            stringResource(R.string.sheet_policy_point5_desc))
        SheetDetailPoint(Icons.Default.Fingerprint, Color(0xFF8B5CF6), stringResource(R.string.sheet_policy_point6_title),
            stringResource(R.string.sheet_policy_point6_desc))
    }
}

@Composable
fun TermsSheet() {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier
        .padding(horizontal = 24.dp)
        .padding(bottom = 40.dp)
        .verticalScroll(scrollState)
    ) {
        Text(stringResource(R.string.legal_terms), fontSize = 20.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(16.dp))
        SheetDetailPoint(Icons.Default.CheckCircle, Color(0xFF6366F1), stringResource(R.string.sheet_terms_point1_title),
            stringResource(R.string.sheet_terms_point1_desc))
        SheetDetailPoint(Icons.Default.Info, Color(0xFF10B981), stringResource(R.string.sheet_terms_point2_title),
            stringResource(R.string.sheet_terms_point2_desc))
        SheetDetailPoint(Icons.Default.Warning, Color(0xFFF59E0B), stringResource(R.string.sheet_terms_point3_title),
            stringResource(R.string.sheet_terms_point3_desc))
        SheetDetailPoint(Icons.Default.Security, Color(0xFFEF4444), stringResource(R.string.sheet_terms_point4_title),
            stringResource(R.string.sheet_terms_point4_desc))
        SheetDetailPoint(Icons.Default.Code, Color(0xFF6B7280), stringResource(R.string.sheet_terms_point5_title),
            stringResource(R.string.sheet_terms_point5_desc))
        SheetDetailPoint(Icons.Default.AccountCircle, Color(0xFFF59E0B), stringResource(R.string.sheet_terms_point6_title),
            stringResource(R.string.sheet_terms_point6_desc))
        SheetDetailPoint(Icons.Default.Link, Color(0xFF6366F1), stringResource(R.string.sheet_terms_point7_title),
            stringResource(R.string.sheet_terms_point7_desc))
    }
}

@Composable
fun LicensesSheet() {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier
        .padding(horizontal = 24.dp)
        .padding(bottom = 40.dp)
        .verticalScroll(scrollState)
    ) {
        Text(stringResource(R.string.legal_licenses), fontSize = 20.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            stringResource(R.string.sheet_licenses_desc),
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
            "Kotlin Coroutines" to "Apache License 2.0",
            "SQLCipher" to "BSD 3-Clause License",
            "DataStore" to "Apache License 2.0",
            "Material Icons" to "Apache License 2.0"
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
            append(stringResource(R.string.sheet_licenses_footer, ""))
            withLink(
                LinkAnnotation.Url(
                    url = "https://www.apache.org/licenses/LICENSE-2.0",
                    styles = TextLinkStyles(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    )
                )
            ) {
                append(stringResource(R.string.click_here))
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

@Composable
fun AboutDetailSheet() {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .padding(bottom = 40.dp)
            .verticalScroll(scrollState)
    ) {
        Text("About", fontSize = 22.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Thanks for trying out AppWatch! This is a solo developed app currently in its v1 (Beta) stage.\n\n" +
                    "Since Android handles usage data differently across various manufacturers and devices, you might encounter some unexpected behavior or minor bugs. I’m constantly refining the app to ensure it works smoothly for everyone.\n\n" +
                    "If you hit a snag, notice something 'rough around the edges,' or have an idea to make the app better, please share your thoughts through our Feedback Form. Your insights are exactly what I need to make AppWatch better for everyone!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
        )
    }
}