package com.example.appwatch.ui.screens

import androidx.compose.foundation.BorderStroke
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
import com.example.appwatch.ui.theme.BackgroundLight
import com.example.appwatch.ui.theme.DividerColor
import com.example.appwatch.ui.theme.Indigo100
import com.example.appwatch.ui.theme.Indigo500
import com.example.appwatch.ui.theme.Indigo600
import com.example.appwatch.ui.theme.Red600
import com.example.appwatch.ui.theme.SurfaceWhite
import com.example.appwatch.ui.theme.TextDisabled
import com.example.appwatch.ui.theme.TextPrimary
import com.example.appwatch.ui.theme.TextSecondary

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
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    var selectedDayFilter by remember { mutableStateOf("30") }

    // Initial sync for default 90
    LaunchedEffect(Unit) {
        if (auditType == "UNUSED") {
            viewModel.updateUnusedFilter("90")
        }
    }

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when(auditType) {
                            "UNUSED" -> "Apps unused for 3+ months"
                            "STALE" -> "Sensitive permissions in unused apps"
                            "SPECIAL" -> "Apps with system level permissions"
                            else -> "Security Audit"
                        },
//                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (auditType == "SPECIAL") {
                        IconButton(onClick = { showBottomSheet = true }) {
                            Icon(Icons.Default.Info, contentDescription = "Help", tint = Indigo500)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {

            // --- Chips are BACK ---
            if (auditType == "UNUSED") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
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
                                selectedContainerColor = Indigo100,
                                selectedLabelColor = Indigo600
                            )
                        )
                    }
                }
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Indigo500)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(events, key = { "${it.packageName}_${it.eventType}" }) { event ->
                        AuditAppItem(
                            event = event,
                            auditType = auditType,
                            onAppClick = { navController.navigate("app_detail/${event.packageName}") }
                        )
                    }
                }
            }
        }
    }
    if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                containerColor = SurfaceWhite
            ) {
                SpecialPermissionsExplanations()
            }
        }
    }

@Composable
fun AuditAppItem(event: NeedsAttentionEntity, auditType: String, onAppClick: () -> Unit) {
    val context = LocalContext.current
    Card(
        onClick = onAppClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, DividerColor)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(try { context.packageManager.getApplicationIcon(event.packageName) } catch (e: Exception) { null })
                    .crossfade(true).build(),
                contentDescription = null,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = event.packageName.split(".").last().replaceFirstChar { it.uppercase() },
                    fontWeight = FontWeight.Bold, color = TextPrimary
                )
                val detail = when(auditType) {
                    "STALE" -> "Unused from ${event.daysUnused} days with ${event.permissionName} permission/s"
                    "SPECIAL" -> event.permissionName ?: "System Access"
                    else -> "Unused from ${event.daysUnused} days"
                }
                Text(text = detail, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }
    }
}

@Composable
fun SpecialPermissionsExplanations() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp)
    ) {
        Text("Highly Sensitive Permissions", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text("At AppWatch, we categorize these as 'Highly Sensitive' because they allow apps to go beyond normal permissions."+
                "While trusted tools use these for advanced features, they require extra caution because they have a 'bird’s-eye view' of your phone's activity.",
            style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(modifier = Modifier.height(16.dp))

        val perms = listOf(
            "ACCESSIBILITY" to "Acts as a 'Screen Helper'. Essential for password managers to auto-fill or screen readers for assistance, but it can see everything you type.",
            "NOTIFICATION_ACCESS" to "Acts as a 'Message Reader'. Needed for Smartwatches to show alerts on your wrist, but it can also read private OTPs from your notifications.",
            "OVERLAY (Display Over Apps)" to "Creates 'Floating Windows'. Used by apps like 'Where is my train' or Messenger bubbles to show info over other apps. Be careful as it can show fake screens.",
            "DEVICE_ADMIN" to "Gives 'Master Control'. Useful for 'Find My Device' to lock your phone if lost, but it's powerful enough to wipe your data or change locks.",
            "INSTALL_UNKNOWN_APPS" to "Acts as an 'External Installer'. Lets apps like Chrome download and install other apps. Dangerous if a random app installs something without asking.",
            "WRITE_SETTINGS" to "Acts as a 'System Editor'. Automation apps use this to change your brightness or DND mode automatically, but it can modify core system behaviors.",
            "VPN_SERVICE" to "Acts as an 'Internet Guard'. Secures your connection on public Wi-Fi, but it means the app can see and route all your internet traffic."
        )

        perms.forEach { (title, desc) ->
            Column(modifier = Modifier.padding(vertical = 10.dp)) {
                Text(title, fontWeight = FontWeight.ExtraBold, color = Red600, fontSize = 13.sp)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = TextPrimary, lineHeight = 18.sp)
            }
        }
    }
}