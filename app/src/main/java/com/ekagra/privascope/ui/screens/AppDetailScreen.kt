package com.ekagra.privascope.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ekagra.privascope.ui.viewModels.AppDetailViewModel
import com.ekagra.privascope.ui.viewModels.StorageViewModel
import com.ekagra.privascope.ui.viewModels.RiskTier
import com.ekagra.privascope.ui.theme.*
import com.ekagra.privascope.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    navController: NavController,
    packageName: String?,
    viewModel: AppDetailViewModel = hiltViewModel(),
    storageViewModel: StorageViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val storageUiState by storageViewModel.uiState.collectAsStateWithLifecycle()

    val appStorage = storageUiState.userApps.find { it.packageName == packageName }
        ?: storageUiState.systemApps.find { it.packageName == packageName }

    LaunchedEffect(packageName) {
        if (packageName != null) {
            viewModel.loadAppDetails(packageName)
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState()
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = BackgroundLight,
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.detail_title_analysis),
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back), tint = TextPrimary)
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = BackgroundLight,
                        scrolledContainerColor = SurfaceWhite,
                        titleContentColor = TextPrimary
                    ),
                    actions = {
                        IconButton(onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", packageName ?: "", null)
                            }
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.cd_system_settings), tint = TextPrimary)
                        }
                    }
                )
                if (uiState.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = Blue600,
                        trackColor = Blue100
                    )
                }
            }
        }
    ) { padding ->
        val app = uiState.appInfo
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                AppDetailHeader(
                    appName = app?.appName ?: stringResource(R.string.detail_status_unknown),
                    packageName = app?.packageName ?: packageName ?: "",
                    status = if (app?.isSystemApp == true) stringResource(R.string.detail_status_system) else stringResource(R.string.detail_status_user),
                    accentColor = Blue600,
                    totalStorageBytes = appStorage?.totalSizeBytes ?: 0L
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = stringResource(R.string.detail_header_usage), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DetailMetricCard(stringResource(R.string.detail_label_today), uiState.usageToday, Icons.Default.History, Blue600, Modifier.weight(1f))
                        DetailMetricCard(stringResource(R.string.detail_label_week), uiState.usageWeek, Icons.Default.DateRange, Purple600, Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DetailMetricCard(stringResource(R.string.detail_label_avg), uiState.weeklyAverage, Icons.Default.BarChart, Green600, Modifier.weight(1f))
                        DetailMetricCard(stringResource(R.string.detail_label_monthly), uiState.monthlyTotal, Icons.Default.Insights, Cyan600, Modifier.weight(1f))
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = stringResource(R.string.detail_header_perms), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)

                    if (uiState.permissions.isEmpty() && !uiState.isLoading) {
                        SafeStateBanner()
                    } else {
                        uiState.permissions.take(3).forEach { perm ->
                            PermissionItemRow(
                                name = perm.name,
                                tier = perm.riskTier,
                                lastAccess = perm.lastAccess
                            )
                        }

                        if (uiState.permissions.size > 3) {
                            TextButton(
                                onClick = { navController.navigate("app_permissions/${packageName}") },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text(
                                    text = stringResource(R.string.detail_more_perms, uiState.permissions.size - 3),
                                    color = Blue600,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = stringResource(R.string.detail_header_behavior), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        BehaviorStatCard(stringResource(R.string.detail_label_launches), stringResource(R.string.detail_val_launches_count, uiState.launchesToday), Icons.Default.TrendingUp, Cyan600, Modifier.weight(1f))
                        BehaviorStatCard(stringResource(R.string.detail_label_last_activity), uiState.lastUsed, Icons.Default.AccessTime, Amber600, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionItemRow(name: String, tier: RiskTier, lastAccess: String) {
    val (iconColor, bgColor) = when (tier) {
        RiskTier.HIGH -> Red600 to Red50
        RiskTier.SENSITIVE -> Amber600 to Amber50
        RiskTier.STANDARD -> Blue600 to Blue50
    }

    val icon = when {
        name.contains("CAMERA", true) -> Icons.Default.CameraAlt
        name.contains("LOCATION", true) -> Icons.Default.LocationOn
        name.contains("AUDIO", true) || name.contains("MIC", true) -> Icons.Default.Mic
        name.contains("SMS", true) -> Icons.Default.Sms
        name.contains("CONTACTS", true) -> Icons.Default.Person
        name.contains("STORAGE", true) -> Icons.Default.Folder
        name.contains("PHONE", true) || name.contains("CALL", true) -> Icons.Default.Call
        name.contains("ACCESSIBILITY", true) -> Icons.Default.AccessibilityNew
        name.contains("NOTIFICATION", true) -> Icons.Default.NotificationsActive
        name.contains("OVERLAY", true) || name.contains("SYSTEM_ALERT", true) -> Icons.Default.Layers
        else -> Icons.Default.Shield
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, DividerColor)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(42.dp).background(bgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name.replace("android.permission.", "").replace("_", " "),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = lastAccess,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }

            if (tier != RiskTier.STANDARD) {
                Surface(
                    color = iconColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (tier == RiskTier.HIGH) stringResource(R.string.detail_risk_high) else stringResource(R.string.detail_risk_sensitive),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = iconColor,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }
    }
}

@Composable
fun SafeStateBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Green50,
        border = BorderStroke(1.dp, Green100)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, null, tint = Green600, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.detail_safe_banner),
                color = Green600,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AppDetailHeader(appName: String, packageName: String, status: String, accentColor: Color, totalStorageBytes: Long) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, DividerColor)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(try { context.packageManager.getApplicationIcon(packageName) } catch (e: Exception) { null }).crossfade(true).build(),
                contentDescription = null,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)),
                error = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Apps)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 28.sp
                )
                Text(
                    text = packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(color = accentColor.copy(alpha = 0.1f), shape = CircleShape) {
                    Text(
                        text = status,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
            if (totalStorageBytes > 0L) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatDetailedByteSize(totalStorageBytes),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = accentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                    Text(
                        text = stringResource(R.string.detail_label_storage),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }
    }
}

@Composable
fun DetailMetricCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = SurfaceWhite), border = BorderStroke(1.dp, DividerColor)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}

@Composable
fun BehaviorStatCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = SurfaceWhite), border = BorderStroke(1.dp, DividerColor)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}

fun formatDetailedByteSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}