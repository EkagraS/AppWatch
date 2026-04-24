package com.example.appwatch.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.appwatch.presentation.viewmodel.DashboardViewModel
import androidx.compose.runtime.remember
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.appwatch.domain.model.DashboardSummary
import com.example.appwatch.ui.components.DashboardInitialLoader

@Composable
fun DashboardScreen(navController: NavController, viewModel: DashboardViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val totalUnlocks = viewModel.totalUnlocksToday.collectAsState().value
    val totalNotifications = viewModel.totalNotificationsToday.collectAsState().value
    val dataUsageBytes = viewModel.totalDataUsageBytes.collectAsState().value
    val formattedDataUsage = viewModel.formatDataUsage(dataUsageBytes)
    val patch = viewModel.systemUpdate.collectAsState().value

    if (uiState.summary == null) {
        DashboardInitialLoader()
    } else {
        DashboardContent(
            navController = navController,
            summary = uiState.summary!!,
            totalUnlocks = totalUnlocks,
            totalNotifications = totalNotifications,
            formattedDataUsage = formattedDataUsage,
            patch = patch,
            isRefreshing = uiState.isRefreshing
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    navController: NavController,
    summary: DashboardSummary,
    totalUnlocks: Int,
    totalNotifications: Int,
    formattedDataUsage: String,
    patch: String,
    isRefreshing: Boolean
) {
    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "AppWatch Dashboard",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate("settings") }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )

                if (isRefreshing) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = Color(0xFF6366F1),
                        trackColor = Color(0xFF6366F1).copy(alpha = 0.1f)
                    )
                }
            }
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
            // 1. Overview Stats Row
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Overview",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isRefreshing) {
                            Text(
                                "Syncing system data...",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF6366F1)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            label = "Installed Apps",
                            value = summary.totalApps.toString(),
                            icon = Icons.Default.Apps,
                            color = Color(0xFF6366F1),
                            modifier = Modifier.weight(1f)
                                .clickable { navController.navigate("app_list") }
                        )
                        StatCard(
                            label = "Storage",
                            value = summary.usedStorage,
                            subValue = summary.totalStorage,
                            icon = Icons.Default.SdStorage,
                            color = Color(0xFF10B981),
                            modifier = Modifier.weight(1f)
                                .clickable { navController.navigate("storage_detail") }
                        )
                        StatCard(
                            label = "Last device update",
                            value = patch,
                            icon = Icons.Default.TrendingUp,
                            color = Color(0xFFF59E0B),
                            modifier = Modifier.weight(1f)
                                .clickable { navController.navigate("usage_stats") }
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Today's activity",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp), // Thoda spacing upar se
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InsightCard(
                        title = "Phone Unlocks",
                        count = "Device unlocked $totalUnlocks times",
                        icon = Icons.Default.LockOpen,
                        color = Color(0xFF3B82F6),
                        modifier = Modifier.weight(1f),
                        onClick = { /* Optional: Navigate to detail if you want */ }
                    )

                    // Right Card: Notifications
                    InsightCard(
                        title = "Notifications",
                        count = "$totalNotifications notifications received", // Yahan apna 'totalNotifications' state pass kar dena
                        icon = Icons.Default.Notifications,
                        color = Color(0xFFEAB308), // Amber/Yellow theme
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("notification_screen") }
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp), // Thoda spacing upar se
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InsightCard(
                        title = "Screen time",
                        count = summary.totalScreenTime,
                        icon = Icons.Default.TrendingUp,
                        color = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("usage_stats") }
                    )

                    // Right Card: Notifications
                    InsightCard(
                        title = "Data usage",
                        count = formattedDataUsage, // Yahan apna 'totalNotifications' state pass kar dena
                        icon = Icons.Default.SwapVert,
                        color = Color(0xFF10B981), // Amber/Yellow theme
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("data_usage") }
                    )
                }
            }

            // 4. Activity & Insights (Same as before, but now UI knows they are refreshing)
            item {
                RecentActivitySection(recentItems = summary.recentActivity, isUpdating = isRefreshing ,onNavigateToEventScreen = { type ->
                    navController.navigate("recentEventScreen/$type")
                })
                NeedsAttentionSection(attentionItems = summary.attentionItems, isUpdating = isRefreshing, onItemClick = {type ->
                    navController.navigate("needs_attention/$type")
                })
            }


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
                        com.example.appwatch.ui.screens.InsightCard(
                            title = "Background Location",
                            count = "${summary?.locationAppsCount ?: 0} apps",
                            icon = Icons.Default.LocationOn,
                            color = Color(0xFFEF4444),
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate("apps_with_permission/LOCATION") }
                        )
                        com.example.appwatch.ui.screens.InsightCard(
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
                        com.example.appwatch.ui.screens.InsightCard(
                            title = "Microphone",
                            count = "${summary?.micAppsCount ?: 0} apps",
                            icon = Icons.Default.Mic,
                            color = Color(0xFF06B6D4),
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate("apps_with_permission/RECORD_AUDIO") }
                        )
                        com.example.appwatch.ui.screens.InsightCard(
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
                        com.example.appwatch.ui.screens.InsightCard(
                            title = "Contacts",
                            count = "${summary?.phoneAppsCount ?: 0} apps",
                            icon = Icons.Default.Mic,
                            color = Color(0xFF10B981),
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate("apps_with_permission/CONTACTS") }
                        )
                        com.example.appwatch.ui.screens.InsightCard(
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
        }
    }
}

@Composable
fun StatCard(label: String, value: String, subValue: String = "", icon: ImageVector, color: Color, modifier: Modifier = Modifier, ) {
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
fun InsightCard(title: String, count: String, icon: ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
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
fun AppAttentionItem(packageName: String, appName: String, reason: String, severity: String, onActionClick: () -> Unit) {
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
fun ActivityCard(title: String, description: String, icon: ImageVector, onClick: () -> Unit) {
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