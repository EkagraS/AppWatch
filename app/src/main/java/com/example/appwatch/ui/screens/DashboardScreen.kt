package com.example.appwatch.ui.screens

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.content.MediaType.Companion.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role.Companion.Image
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.ContextCompat.startActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.appwatch.presentation.viewmodel.DashboardViewModel
import androidx.compose.runtime.remember
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val summary by viewModel.dashboardSummary.collectAsStateWithLifecycle()

    // Usage Stats Permission Check
    LaunchedEffect(Unit) {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        if (mode != AppOpsManager.MODE_ALLOWED) {
            context.startActivity(Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "AppWatch Dashboard",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate("settings")
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(vertical = 16.dp, horizontal = 0.dp)
        ) {
            // 1. Overview Stats Row - UPDATED WITH DATA
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Overview",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            label = "Installed Apps",
                            value = summary?.totalApps?.toString() ?: "0",
                            icon = Icons.Default.Apps,
                            color = Color(0xFF6366F1),
                            modifier = Modifier.weight(1f).clickable(onClick = {navController.navigate("app_list")})
                        )
                        StatCard(
                            label = "Storage",
                            value = summary?.usedStorage ?: "--",
                            subValue = summary?.totalStorage ?: "--",
                            icon = Icons.Default.SdStorage,
                            color = Color(0xFF10B981),
                            modifier = Modifier.weight(1f).clickable(onClick = {navController.navigate("storage_detail")})
                        )
                        StatCard(
                            label = "Screen time",
                            value = summary?.totalScreenTime ?: "0m",
                            icon = Icons.Default.TrendingUp,
                            color = Color(0xFFF59E0B),
                            modifier = Modifier.weight(1f).clickable(onClick = {navController.navigate("usage_stats")})
                        )
                    }
                }
            }

            // 2. Privacy Insights - UPDATED WITH DATA
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Privacy Insights",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        InsightCard(
                            title = "Background Location",
                            count = "${summary?.locationAppsCount ?: 0} apps",
                            icon = Icons.Default.LocationOn,
                            color = Color(0xFFEF4444),
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate("apps_with_permission/LOCATION") }
                        )
                        InsightCard(
                            title = "Camera Access",
                            count = "${summary?.cameraAppsCount ?: 0} apps",
                            icon = Icons.Default.Camera,
                            color = Color(0xFF8B5CF6),
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate("apps_with_permission/CAMERA") }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        InsightCard(
                            title = "Microphone",
                            count = "${summary?.micAppsCount ?: 0} apps",
                            icon = Icons.Default.Mic,
                            color = Color(0xFF06B6D4),
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate("apps_with_permission/RECORD_AUDIO") }
                        )
                        InsightCard(
                            title = "Phone & Calls",
                            count = "${summary?.contactAppsCount ?: 0} apps",
                            icon = Icons.Default.Storage,
                            color = Color(0xFFF97316),
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate("apps_with_permission/CALL_LOG") }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        InsightCard(
                            title = "Contacts",
                            count = "${summary?.phoneAppsCount ?: 0} apps",
                            icon = Icons.Default.Mic,
                            color = Color(0xFF10B981),
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate("apps_with_permission/CONTACTS") }
                        )
                        InsightCard(
                            title = "SMS",
                            count = "${summary?.SmsAppsCount ?: 0} apps",
                            icon = Icons.Default.Storage,
                            color = Color(0xFF84CC16),
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate("apps_with_permission/SMS") }
                        )
                    }
                }
            }

            // 3. Needs Attention Section - UPDATED WITH DYNAMIC DATA
// ... inside LazyColumn
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Needs Attention",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF111827)
                    )
                    TextButton(onClick = { navController.navigate("permission_audit") }) {
                        Text("Audit All", color = Color(0xFF6366F1), fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(16.dp))
                    }
                }
            }

            val attentionItems = summary?.attentionItems ?: emptyList()

            if (attentionItems.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Shield, null, tint = Color(0xFF10B981), modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "System Secure",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937)
                            )
                            Text(
                                "No immediate privacy risks detected.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            } else {
                items(attentionItems) { item ->
                    AppAttentionItem(
                        appName = item.appName,
                        reason = item.reason,
                        severity = item.severity,
                        packageName = item.packageName,
                        onActionClick = {
                            navController.navigate("app_detail/${item.packageName}")
                        }
                    )
                }
            }

            // 4. Recent Activity - UPDATED WITH DYNAMIC DATA
            item {
                Text(
                    text = "Recent Activity",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            items(summary?.recentActivity ?: emptyList()) { activity ->
                ActivityCard(
                    title = activity.title,
                    description = activity.description,
                    icon = when(activity.iconType) {
                        "INSTALL" -> Icons.Default.TrendingUp
                        "ACTIVE" -> Icons.Default.History
                        "INACTIVE" -> Icons.Default.Block
                        else -> Icons.Default.History
                    },
                    onClick = { navController.navigate("app_list") }
                )
            }

            // 5. Quick Navigation Grid - REMAINS READY
            item {
                Text(
                    text = "Tools & Features",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NavigationRow(
                        title = "Permission Audit",
                        subtitle = "Cross-app privacy analysis",
                        icon = Icons.Default.Security,
                        iconColor = Color(0xFF6366F1),
                        onClick = { navController.navigate("permission_audit") }
                    )
                    NavigationRow(
                        title = "Usage Statistics",
                        subtitle = "Screen time and app behavior",
                        icon = Icons.Default.DataUsage,
                        iconColor = Color(0xFF10B981),
                        onClick = { navController.navigate("usage_stats") }
                    )
                    NavigationRow(
                        title = "App List",
                        subtitle = "Manage all installed packages",
                        icon = Icons.Default.Apps,
                        iconColor = Color(0xFF8B5CF6),
                        onClick = { navController.navigate("app_list") }
                    )
                    NavigationRow(
                        title = "Storage",
                        subtitle = "Manage your storage",
                        icon = Icons.Default.Storage,
                        iconColor = Color(0xFF8B5CF6),
                        onClick = { navController.navigate("storage_detail") }
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    subValue: String = "",
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxHeight(), // Fill height to match the Row's tallest item
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center // Center content vertically
        ) {
            // Icon Circle
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(color.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = color
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Primary Value
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            // Sub-Value (Occupies space even if empty to keep labels aligned)
            Box(modifier = Modifier.height(16.dp), contentAlignment = Alignment.Center) {
                if (subValue.isNotEmpty()) {
                    Text(
                        text = "of $subValue",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Label
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun InsightCard(
    title: String,
    count: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = color
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = count,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AppAttentionItem(
    packageName: String,
    appName: String,
    reason: String,
    severity: String,
    onActionClick: () -> Unit
) {
    val statusColor = when (severity) {
        "High" -> Color(0xFFEF4444)   // Red for Background Actors
        "Medium" -> Color(0xFFF59E0B) // Amber for Ghost Risks
        else -> Color(0xFF6366F1)     // Indigo for Data Hogs/New Apps
    }
    val context = LocalContext.current
    val appIcon = remember(packageName) {
        try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) { null }
    }

    Surface(
        onClick = onActionClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Background
            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center
            ) {
                if (appIcon != null) {
                    Image(
                        bitmap = appIcon.toBitmap().asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    // Fallback to your colored box if icon fails
                    Box(modifier = Modifier.fillMaxSize().background(statusColor.copy(0.1f), CircleShape))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF1F2937)
                )
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ActivityCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun NavigationRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = iconColor
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun permissions(){
}