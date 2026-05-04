package com.example.appwatch.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.appwatch.R
import com.example.appwatch.data.local.entity.RecentEventEntity
import com.example.appwatch.presentation.viewmodel.DashboardViewModel
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.example.appwatch.data.local.entity.NeedsAttentionEntity
import com.example.appwatch.ui.theme.*

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

    LaunchedEffect(Unit) {
        if (auditType == "UNUSED") {
            viewModel.updateUnusedFilter("90")
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState()
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when(auditType) {
                            "UNUSED" -> stringResource(R.string.audit_title_unused)
                            "STALE" -> stringResource(R.string.audit_title_stale)
                            "SPECIAL" -> stringResource(R.string.audit_title_special)
                            else -> stringResource(R.string.audit_title_default)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    if (auditType == "SPECIAL") {
                        IconButton(onClick = { showBottomSheet = true }) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = stringResource(R.string.cd_help),
                                tint = Indigo500
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BackgroundLight,
                    scrolledContainerColor = SurfaceWhite,
                    titleContentColor = TextPrimary
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {

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
                            label = {
                                Text(
                                    text = stringResource(R.string.audit_filter_days, day),
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                            },
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
            } else if (events.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No items need attention right now.",
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.packageName.split(".").last().replaceFirstChar { it.uppercase() },
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val detail = when(auditType) {
                    "STALE" -> stringResource(R.string.audit_detail_stale, event.daysUnused ?: 0, event.permissionName ?: "")
                    "SPECIAL" -> event.permissionName ?: stringResource(R.string.audit_detail_special_default)
                    else -> stringResource(R.string.audit_detail_unused, event.daysUnused ?: 0)
                }
                Text(
                    text = detail,
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
fun SpecialPermissionsExplanations() {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = stringResource(R.string.special_perms_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.special_perms_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Spacer(modifier = Modifier.height(16.dp))

        val perms = listOf(
            R.string.perm_desc_accessibility_title to R.string.perm_desc_accessibility_body,
            R.string.perm_desc_notif_title to R.string.perm_desc_notif_body,
            R.string.perm_desc_overlay_title to R.string.perm_desc_overlay_body,
            R.string.perm_desc_admin_title to R.string.perm_desc_admin_body,
            R.string.perm_desc_unknown_apps_title to R.string.perm_desc_unknown_apps_body,
            R.string.perm_desc_write_settings_title to R.string.perm_desc_write_settings_body,
            R.string.perm_desc_vpn_title to R.string.perm_desc_vpn_body
        )

        perms.forEach { (titleRes, descRes) ->
            Column(modifier = Modifier.padding(vertical = 10.dp)) {
                Text(
                    text = stringResource(titleRes),
                    fontWeight = FontWeight.ExtraBold,
                    color = Red600,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
                Text(
                    text = stringResource(descRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary,
                    lineHeight = 18.sp,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}