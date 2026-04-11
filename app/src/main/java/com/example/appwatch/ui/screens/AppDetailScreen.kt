package com.example.appwatch.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.appwatch.presentation.viewmodel.AppDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    navController: NavController,
    packageName: String?,
    viewModel: AppDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(packageName) {
        if (packageName != null) {
            viewModel.loadAppDetails(packageName)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("App Analysis", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", packageName ?: "", null)
                        }
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "System Settings")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val app = uiState.appInfo
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // 1. App Header Info
                item {
                    AppDetailHeader(
                        appName = app?.appName ?: "Unknown",
                        packageName = app?.packageName ?: packageName ?: "",
                        status = if (app?.isSystemApp == true) "System Application" else "User Application",
                        accentColor = Color(0xFF6366F1),
                        icon = app?.iconDrawable
                    )
                }

                // 2. Usage Stats Section
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Usage Statistics",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DetailMetricCard(
                                label = "Today",
                                value = uiState.usageToday,
                                icon = Icons.Default.History,
                                color = Color(0xFF6366F1),
                                modifier = Modifier.weight(1f)
                            )
                            DetailMetricCard(
                                label = "This Week",
                                value = uiState.usageWeek,
                                icon = Icons.Default.DateRange,
                                color = Color(0xFF8B5CF6),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DetailMetricCard(
                                label = "Weekly Average",
                                value = uiState.weeklyAverage,
                                icon = Icons.Default.BarChart,
                                color = Color(0xFF10B981),
                                modifier = Modifier.weight(1f)
                            )
                            DetailMetricCard(
                                label = "Monthly Total",
                                value = uiState.monthlyTotal,
                                icon = Icons.Default.BarChart,
                                color = Color(0xFF10B981),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 3. Behavioral Insights
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BehaviorStatCard(
                            title = "Launches Today",
                            value = "${uiState.launchesToday} times",
                            icon = Icons.Default.TrendingUp,
                            color = Color(0xFF06B6D4),
                            modifier = Modifier.weight(1f)
                        )
                        BehaviorStatCard(
                            title = "Last Activity",
                            value = uiState.lastUsed,
                            icon = Icons.Default.AccessTime,
                            color = Color(0xFFF59E0B),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 4. Permission Audit (The "Evidence" Section)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Permission Evidence",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = {
                                    navController.navigate("app_permissions/${uiState.appInfo?.packageName}")
                                }
                            ) {
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = "See all permissions"
                                )
                            }
                        }

                        // Show only first 3
                        uiState.permissions.take(3).forEach { perm ->
                            DetailedPermissionRow(
                                permission = perm.name,
                                detail = perm.lastAccess,
                                icon = when {
                                    perm.name.contains("LOCATION", ignoreCase = true) -> Icons.Default.LocationOn
                                    perm.name.contains("CAMERA", ignoreCase = true) -> Icons.Default.Camera
                                    perm.name.contains("RECORD_AUDIO", ignoreCase = true) -> Icons.Default.Mic
                                    perm.name.contains("STORAGE", ignoreCase = true) -> Icons.Default.Storage
                                    perm.name.contains("CONTACTS", ignoreCase = true) -> Icons.Default.Person
                                    perm.name.contains("SMS", ignoreCase = true) -> Icons.Default.Sms
                                    else -> Icons.Default.Shield
                                },
                                color = getPermissionColor(perm.name),
                                isWarning = perm.isUnused || perm.isSensitive
                            )
                        }

                        // Show count of remaining
                        if (uiState.permissions.size > 3) {
                            TextButton(
                                onClick = {
                                    navController.navigate("app_permissions/${uiState.appInfo?.packageName}")
                                },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text(
                                    "+${uiState.permissions.size - 3} more permissions",
                                    color = Color(0xFF6366F1)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppDetailHeader(
    appName: String,
    packageName: String,
    status: String,
    accentColor: Color,
    icon: android.graphics.drawable.Drawable?
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Image(
                        bitmap = icon.toBitmap().asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                } else {
                    Icon(Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(32.dp), tint = accentColor)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(appName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text(packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = accentColor.copy(alpha = 0.1f),
                    shape = CircleShape
                ) {
                    Text(
                        text = status,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun DetailMetricCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun BehaviorStatCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier) {
    OutlinedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun DetailedPermissionRow(
    permission: String,
    detail: String,
    icon: ImageVector,
    color: Color,
    isWarning: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(permission, fontWeight = FontWeight.Bold)
                    if (isWarning) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Warning, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                    }
                }
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun getPermissionColor(name: String): Color = when(name) {
    "Location" -> Color(0xFFEF4444)
    "Camera" -> Color(0xFF8B5CF6)
    "Microphone" -> Color(0xFF06B6D4)
    "Storage" -> Color(0xFFF97316)
    "Contacts" -> Color(0xFF10B981)
    "SMS" -> Color(0xFF84cc16)
    else -> Color(0xFF6366F1)
}