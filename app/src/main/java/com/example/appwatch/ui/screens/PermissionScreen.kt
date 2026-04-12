package com.example.appwatch.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.appwatch.presentation.viewmodel.AppDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionScreen(
    navController: NavController,
    packageName: String?,
    viewModel: AppDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(packageName) {
        if (packageName != null) {
            viewModel.loadAppDetails(packageName)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "All Permissions",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF6366F1))
            }
        } else {
            val permissions = uiState.permissions
            val unusedCount = permissions.count { it.isUnused }
            val sensitiveCount = permissions.count { it.isSensitive }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // App name header
                item {
                    Text(
                        text = uiState.appInfo?.appName ?: packageName ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Summary cards
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SummaryChip(
                            label = "Total",
                            value = "${permissions.size}",
                            color = Color(0xFF6366F1),
                            modifier = Modifier.weight(1f)
                        )
                        SummaryChip(
                            label = "Sensitive",
                            value = "$sensitiveCount",
                            color = Color(0xFFEF4444),
                            modifier = Modifier.weight(1f)
                        )
                        SummaryChip(
                            label = "Unused",
                            value = "$unusedCount",
                            color = Color(0xFFF59E0B),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Filter chips
                item {
                    var selectedFilter by remember { mutableStateOf("All") }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("All", "Sensitive", "Unused", "Granted").forEach { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text(filter) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF6366F1).copy(alpha = 0.15f),
                                    selectedLabelColor = Color(0xFF6366F1)
                                )
                            )
                        }
                    }
                }

                // Sensitive permissions section
                if (sensitiveCount > 0) {
                    item {
                        Text(
                            "Sensitive Permissions",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444)
                        )
                    }
                    items(permissions.filter { it.isSensitive }) { perm ->
                        PermissionDetailRow(perm)
                    }
                }

                // Other permissions section
                val otherPerms = permissions.filter { !it.isSensitive }
                if (otherPerms.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Other Permissions",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(otherPerms) { perm ->
                        PermissionDetailRow(perm)
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryChip(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
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
}

@Composable
fun PermissionDetailRow(perm: com.example.appwatch.presentation.viewmodel.PermissionEvidence) {
    val color = when {
        perm.name.contains("LOCATION", ignoreCase = true) -> Color(0xFFEF4444)
        perm.name.contains("CAMERA", ignoreCase = true) -> Color(0xFF8B5CF6)
        perm.name.contains("RECORD_AUDIO", ignoreCase = true) -> Color(0xFF06B6D4)
        perm.name.contains("CONTACTS", ignoreCase = true) -> Color(0xFF10B981)
        perm.name.contains("SMS", ignoreCase = true) -> Color(0xFF84CC16)
        perm.name.contains("CALL_LOG", ignoreCase = true) -> Color(0xFFF97316)
        perm.name.contains("STORAGE", ignoreCase = true) -> Color(0xFFF59E0B)
        else -> Color(0xFF6366F1)
    }

    val icon: ImageVector = when {
        perm.name.contains("LOCATION", ignoreCase = true) -> Icons.Default.LocationOn
        perm.name.contains("CAMERA", ignoreCase = true) -> Icons.Default.Camera
        perm.name.contains("RECORD_AUDIO", ignoreCase = true) -> Icons.Default.Mic
        perm.name.contains("CONTACTS", ignoreCase = true) -> Icons.Default.Person
        perm.name.contains("SMS", ignoreCase = true) -> Icons.Default.Sms
        perm.name.contains("CALL_LOG", ignoreCase = true) -> Icons.Default.Phone
        perm.name.contains("STORAGE", ignoreCase = true) -> Icons.Default.Storage
        else -> Icons.Default.Shield
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    perm.name,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    perm.lastAccess,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Status badge
            Surface(
                color = when {
                    perm.isUnused -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                    perm.lastAccess == "Granted" -> Color(0xFF10B981).copy(alpha = 0.15f)
                    else -> Color(0xFF6366F1).copy(alpha = 0.15f)
                },
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = when {
                        perm.isUnused -> "Unused"
                        perm.lastAccess == "Granted" -> "Granted"
                        else -> "Active"
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        perm.isUnused -> Color(0xFFF59E0B)
                        perm.lastAccess == "Granted" -> Color(0xFF10B981)
                        else -> Color(0xFF6366F1)
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}