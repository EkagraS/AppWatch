package com.example.appwatch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.appwatch.data.local.entity.RecentEventEntity
import com.example.appwatch.presentation.viewmodel.DashboardViewModel
import androidx.compose.foundation.lazy.items
import com.example.appwatch.data.local.entity.NeedsAttentionEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeedsAttentionScreen(
    navController: NavController,
    auditType: String,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val auditFilter by viewModel.auditFilter.collectAsState()
    val events by remember(auditType, auditFilter) {
        val dbType = when (auditType.uppercase()) {
            "UNUSED" -> "AUDIT_UNUSED_$auditFilter"
            "STALE" -> "AUDIT_STALE_PERMS"
            "SPECIAL" -> "AUDIT_SPECIAL_ACCESS"
            else -> auditType
        }
        viewModel.getNeedsAttentionEventsByType(dbType)
    }.collectAsState(initial = emptyList())


    val uiState by viewModel.uiState.collectAsState()
    val isLoading = uiState.isRefreshing && events.isEmpty()
    val context = LocalContext.current

    // State for Unused Filter (30, 60, 90)
    var selectedDayFilter by remember { mutableStateOf("30") }
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // Screen Titles & Colors
    val screenTitle = when (auditType) {
        "UNUSED" -> "Unused Applications"
        "STALE" -> "Stale Permissions"
        "SPECIAL" -> "Highly Sensitive Access"
        else -> "Security Audit"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (auditType == "SPECIAL") {
                        IconButton(onClick = { showBottomSheet = true }) {
                            Icon(Icons.Default.Info, contentDescription = "Help", tint = Color(0xFF6366F1))
                        }
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
            // --- Innovative Filter (Only for Unused Apps) ---
            if (auditType == "UNUSED") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("30", "60", "90").forEach { day ->
                        FilterChip(
                            selected = selectedDayFilter == day,
                            onClick = {
                                selectedDayFilter = day
                                viewModel.updateUnusedFilter(day)
                            },
                            label = { Text("$day+ Days") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF6366F1).copy(alpha = 0.15f),
                                selectedLabelColor = Color(0xFF6366F1)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // --- Info Header for Stale/Special ---
            if (auditType != "UNUSED") {
                Surface(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = when(auditType) {
                            "STALE" -> "Apps that have sensitive permissions (Camera, Mic, etc.) but haven't been opened in 30+ days."
                            else -> "Apps with high-level system control. These can monitor your screen, notifications, or files."
                        },
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // --- List of Apps ---
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF6366F1))
                }
            } else if (events.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No apps found in this category", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(events, key = { "${it.packageName}_${it.eventType}" }) { event ->
                        AuditAppItem(event = event, auditType = auditType)
                    }
                }
            }
        }

        // --- Highly Sensitive Permissions Explanation (Bottom Sheet) ---
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                SpecialPermissionsExplanations()
            }
        }
    }
}

@Composable
fun AuditAppItem(event: NeedsAttentionEntity, auditType: String) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(try { context.packageManager.getApplicationIcon(event.packageName) } catch (e: Exception) { null })
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.size(50.dp).clip(RoundedCornerShape(10.dp)),
                error = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Apps)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.packageName.split(".").last().replaceFirstChar { it.uppercase() }, // Simple fallback app name
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = event.extraInfo ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (auditType == "SPECIAL") Color(0xFFEF4444) else Color(0xFF6366F1),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Right side badge for Unused days
            if (auditType == "UNUSED") {
                Surface(
                    color = Color.Gray.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Dormant",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun SpecialPermissionsExplanations() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text("Highly Sensitive Permissions", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("These permissions grant apps deep access to your system:", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))

        val perms = listOf(
            "Accessibility" to "Can read screen content and perform actions on your behalf.",
            "Notification Access" to "Can read all incoming messages, including private OTPs.",
            "Display Over Apps" to "Can show fake screens over other apps to steal passwords.",
            "Device Admin" to "Can wipe phone data or change screen locks remotely.",
            "Install Unknown Apps" to "Can install other malicious apps without your permission.",
            "Write Settings" to "Can modify core system behaviors and network settings.",
            "VPN Service" to "Can monitor and redirect all your internet traffic."
        )

        perms.forEach { (title, desc) ->
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(title, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444), fontSize = 14.sp)
                Text(desc, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}