package com.example.appwatch.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.appwatch.presentation.viewmodel.AppDetailViewModel
import com.example.appwatch.presentation.viewmodel.RiskTier
import com.example.appwatch.ui.theme.* @OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionScreen(
    navController: NavController,
    packageName: String?,
    viewModel: AppDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedFilter by remember { mutableStateOf("All") }

    LaunchedEffect(packageName) {
        if (packageName != null) {
            viewModel.loadAppDetails(packageName)
        }
    }

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(text = "All Permissions", fontWeight = FontWeight.Bold, color = TextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
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
        val permissions = uiState.permissions
        val highRiskCount = permissions.count { it.riskTier == RiskTier.HIGH }
        val sensitiveCount = permissions.count { it.riskTier == RiskTier.SENSITIVE }

        val filteredPermissions = when (selectedFilter) {
            "High Risk" -> permissions.filter { it.riskTier == RiskTier.HIGH }
            "Sensitive" -> permissions.filter { it.riskTier == RiskTier.SENSITIVE }
            else -> permissions
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 20.dp)
        ) {
            item {
                Text(
                    text = uiState.appInfo?.appName ?: packageName ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            // 1. Summary Chips with LIGHT COLORS (Updated)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryChip(
                        label = "Total",
                        value = "${permissions.size}",
                        color = Blue600,
                        bgColor = Blue50, // Halka blue background
                        modifier = Modifier.weight(1f)
                    )
                    SummaryChip(
                        label = "High Risk",
                        value = "$highRiskCount",
                        color = Red600,
                        bgColor = Red50, // Halka red background
                        modifier = Modifier.weight(1f)
                    )
                    SummaryChip(
                        label = "Sensitive",
                        value = "$sensitiveCount",
                        color = Amber600,
                        bgColor = Amber50, // Halka amber background
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 2. Filter Chips
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All", "High Risk", "Sensitive").forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Blue600.copy(alpha = 0.1f),
                                selectedLabelColor = Blue600,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedFilter == filter,
                                borderColor = DividerColor,
                                selectedBorderColor = Blue600.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }

            if (filteredPermissions.isEmpty() && !uiState.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No permissions found", color = TextSecondary)
                    }
                }
            } else {
                items(filteredPermissions) { perm ->
                    PermissionDetailRow(
                        name = perm.name,
                        tier = perm.riskTier,
                        lastAccess = perm.lastAccess
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryChip(label: String, value: String, color: Color, bgColor: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = bgColor), // LIGHT BACKGROUND
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f)) // Subtl matching border
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.8f), // Text bhi color se match karega
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PermissionDetailRow(name: String, tier: RiskTier, lastAccess: String) {
    val (iconColor, bgColor) = when (tier) {
        RiskTier.HIGH -> Red600 to Red50
        RiskTier.SENSITIVE -> Amber600 to Amber50
        else -> Blue600 to Blue50
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, DividerColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(bgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name.replace("android.permission.", "").replace("_", " ").replace("com.",""),
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (lastAccess.isNotBlank()) {
                    Text(
                        text = lastAccess,
                        style = MaterialTheme.typography.bodySmall,
                        color = Blue600
                    )
                }
            }

            if (tier != RiskTier.STANDARD) {
                Surface(
                    color = iconColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (tier == RiskTier.HIGH) "High" else "Sensitive",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = iconColor,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}