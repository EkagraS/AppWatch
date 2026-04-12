package com.example.appwatch.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.appwatch.presentation.viewmodel.StorageViewModel
import com.example.appwatch.system.AppStorageInfo
import com.example.appwatch.system.StorageStatsHelper
import androidx.hilt.navigation.compose.hiltViewModel
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageDetailScreen(
    navController: NavController,
    viewModel: StorageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("User Apps", "System Apps")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage Analysis", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 16.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF6366F1)
                        )
                    } else {
                        IconButton(onClick = { viewModel.refreshInBackground() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoadingFromRoom && uiState.userApps.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFF6366F1))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Analyzing storage...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // 1. Refresh status banner
                if (uiState.isRefreshing) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF6366F1).copy(alpha = 0.1f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF6366F1)
                                )
                                Text(
                                    "Updating storage data...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF6366F1)
                                )
                            }
                        }
                    }
                }

                // 2. Device Storage Overview Card
                item {
                    DeviceStorageCard(
                        totalBytes = uiState.deviceStorage?.totalBytes ?: 0L,
                        usedBytes = uiState.deviceStorage?.usedBytes ?: 0L,
                        freeBytes = uiState.deviceStorage?.freeBytes ?: 0L
                    )
                }

                // 3. Apps Storage Summary
                item {
                    Text(
                        "Apps Storage",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StorageSummaryCard(
                            label = "User Apps",
                            bytes = uiState.totalUserAppsBytes,
                            count = uiState.userApps.size,
                            color = Color(0xFF6366F1),
                            modifier = Modifier.weight(1f)
                        )
                        StorageSummaryCard(
                            label = "System Apps",
                            bytes = uiState.totalSystemAppsBytes,
                            count = uiState.systemApps.size,
                            color = Color(0xFF8B5CF6),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 4. Tab selector
                item {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        contentColor = Color(0xFF6366F1)
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = {
                                    Text(
                                        title,
                                        fontWeight = if (selectedTab == index)
                                            FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }
                }

                // 5. App list based on selected tab
                val displayApps = if (selectedTab == 0) uiState.userApps else uiState.systemApps
                val maxSize = displayApps.firstOrNull()?.totalSizeBytes ?: 1L

                if (displayApps.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No data yet — pull to refresh",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(displayApps, key = { it.packageName }) { app ->
                        AppStorageRow(
                            app = app,
                            maxBytes = maxSize,
                            onClick = {
                                navController.navigate("app_detail/${app.packageName}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceStorageCard(
    totalBytes: Long,
    usedBytes: Long,
    freeBytes: Long
) {
    val usedFraction = if (totalBytes > 0) usedBytes.toFloat() / totalBytes else 0f

    // Animated progress
    val animatedProgress by animateFloatAsState(
        targetValue = usedFraction,
        animationSpec = tween(durationMillis = 800),
        label = "storage_progress"
    )

    val progressColor by animateColorAsState(
        targetValue = when {
            usedFraction > 0.9f -> Color(0xFFEF4444)
            usedFraction > 0.7f -> Color(0xFFF59E0B)
            else -> Color(0xFF6366F1)
        },
        animationSpec = tween(durationMillis = 800),
        label = "storage_color"
    )

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Device Storage",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Progress bar
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Used: ${formatSize(usedBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = progressColor,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Free: ${formatSize(freeBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StorageStatItem(
                    label = "Total",
                    value = formatSize(totalBytes),
                    color = MaterialTheme.colorScheme.onSurface
                )
                StorageStatItem(
                    label = "Used",
                    value = formatSize(usedBytes),
                    color = progressColor
                )
                StorageStatItem(
                    label = "Free",
                    value = formatSize(freeBytes),
                    color = Color(0xFF10B981)
                )
            }
        }
    }
}

@Composable
fun StorageStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StorageSummaryCard(
    label: String,
    bytes: Long,
    count: Int,
    color: Color,
    modifier: Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                formatSize(bytes),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "$count apps",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AppStorageRow(
    app: AppStorageInfo,
    maxBytes: Long,
    onClick: () -> Unit
) {
    val fraction = if (maxBytes > 0) app.totalSizeBytes.toFloat() / maxBytes else 0f

    // Animate the bar width for changed values
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 600),
        label = "app_storage_bar"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App icon placeholder
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Color(0xFF6366F1).copy(alpha = 0.1f),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Apps,
                        contentDescription = null,
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        app.appName,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                    Text(
                        "App: ${formatSize(app.appSizeBytes)} • " +
                                "Data: ${formatSize(app.dataSizeBytes)} • " +
                                "Cache: ${formatSize(app.cacheSizeBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    formatSize(app.totalSizeBytes),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6366F1)
                )
            }

            // Animated progress bar
            LinearProgressIndicator(
                progress = { animatedFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color(0xFF6366F1),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes >= 1024L * 1024 * 1024 -> "%.1f GB".format(bytes / (1024.0 * 1024 * 1024))
        bytes >= 1024L * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
        bytes >= 1024L -> "%.1f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
}