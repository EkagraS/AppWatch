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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.appwatch.presentation.viewmodel.AppsWithPermissionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsWithPermissionScreen(
    navController: NavController,
    permissionType: String?,
    viewModel: AppsWithPermissionViewModel = hiltViewModel()
) {
    val apps by viewModel.apps.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

//    LaunchedEffect(permissionType) {
//        if (permissionType != null) {
//            viewModel.loadAppsWithPermission(permissionType)
//        }
//    }
//
    val permissionColor = getPermissionTypeColor(permissionType ?: "")
    val permissionIcon = getPermissionTypeIcon(permissionType ?: "")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "${getPermissionFriendlyName(permissionType ?: "")} Access",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Header info card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = permissionColor.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(permissionColor.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            permissionIcon,
                            contentDescription = null,
                            tint = permissionColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            getPermissionFriendlyName(permissionType ?: ""),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "${apps.size} apps have this permission",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = permissionColor)
                    }
                }
                apps.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No apps have this permission",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(apps, key = { it.packageName }) { app ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    navController.navigate("app_detail/${app.packageName}")
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // App icon
                                    if (app.iconDrawable != null) {
                                        AsyncImage(
                                            model = app.iconDrawable,
                                            contentDescription = app.appName,
                                            modifier = Modifier
                                                .size(46.dp)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .background(
                                                    permissionColor.copy(alpha = 0.1f),
                                                    RoundedCornerShape(10.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Apps,
                                                contentDescription = null,
                                                tint = permissionColor
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            app.appName,
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodyLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            app.packageName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // System badge
                                    if (app.isSystemApp) {
                                        Surface(
                                            color = Color(0xFF6366F1).copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                "System",
                                                modifier = Modifier.padding(
                                                    horizontal = 6.dp,
                                                    vertical = 2.dp
                                                ),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF6366F1)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getPermissionFriendlyName(permissionType: String): String = when (permissionType) {
    "LOCATION" -> "Location"
    "CAMERA" -> "Camera"
    "RECORD_AUDIO" -> "Microphone"
    "CONTACTS" -> "Contacts"
    "CALL_LOG" -> "Phone & Calls"
    "SMS" -> "SMS"
    else -> permissionType
}

private fun getPermissionTypeColor(permissionType: String): Color = when (permissionType) {
    "LOCATION" -> Color(0xFFEF4444)
    "CAMERA" -> Color(0xFF8B5CF6)
    "RECORD_AUDIO" -> Color(0xFF06B6D4)
    "CONTACTS" -> Color(0xFF10B981)
    "CALL_LOG" -> Color(0xFFF97316)
    "SMS" -> Color(0xFF84CC16)
    else -> Color(0xFF6366F1)
}

private fun getPermissionTypeIcon(permissionType: String): ImageVector = when (permissionType) {
    "LOCATION" -> Icons.Default.LocationOn
    "CAMERA" -> Icons.Default.Camera
    "RECORD_AUDIO" -> Icons.Default.Mic
    "CONTACTS" -> Icons.Default.Person
    "CALL_LOG" -> Icons.Default.Phone
    "SMS" -> Icons.Default.Sms
    else -> Icons.Default.Shield
}