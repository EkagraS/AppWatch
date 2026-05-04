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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.appwatch.R
import com.example.appwatch.presentation.viewmodel.AppDetailViewModel
import com.example.appwatch.presentation.viewmodel.RiskTier
import com.example.appwatch.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionScreen(
    navController: NavController,
    packageName: String?,
    viewModel: AppDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showInfoSheet by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("All") }

    LaunchedEffect(packageName) {
        if (packageName != null) {
            viewModel.loadAppDetails(packageName)
        }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        rememberTopAppBarState()
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = BackgroundLight,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.perm_screen_all_title),
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back), tint = TextPrimary)
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = BackgroundLight,
                        scrolledContainerColor = Teal50,
                        titleContentColor = TextPrimary,
                        navigationIconContentColor = TextPrimary,
                        actionIconContentColor = TextPrimary
                    )
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

        val sortedPermissions = filteredPermissions.sortedBy { it.riskTier }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 20.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = uiState.appInfo?.appName ?: packageName ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    IconButton(onClick = { showInfoSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Classification Info",
                            tint = Blue600
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryChip(
                        label = stringResource(R.string.perm_label_total),
                        value = "${permissions.size}",
                        color = Blue600,
                        bgColor = Blue50,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryChip(
                        label = stringResource(R.string.perm_filter_high),
                        value = "$highRiskCount",
                        color = Red600,
                        bgColor = Red50,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryChip(
                        label = stringResource(R.string.perm_filter_sensitive),
                        value = "$sensitiveCount",
                        color = Amber600,
                        bgColor = Amber50,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val filterOptions = listOf(
                        stringResource(R.string.perm_filter_all) to "All",
                        stringResource(R.string.perm_filter_high) to "High Risk",
                        stringResource(R.string.perm_filter_sensitive) to "Sensitive"
                    )

                    filterOptions.forEach { (label, filterKey) ->
                        FilterChip(
                            selected = selectedFilter == filterKey,
                            onClick = { selectedFilter = filterKey },
                            label = {
                                Text(
                                    text = label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Blue600.copy(alpha = 0.1f),
                                selectedLabelColor = Blue600,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedFilter == filterKey,
                                borderColor = DividerColor,
                                selectedBorderColor = Blue600.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }

            if (sortedPermissions.isEmpty() && !uiState.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.perm_no_found),
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                items(sortedPermissions) { perm ->
                    PermissionDetailRow(
                        name = perm.name,
                        tier = perm.riskTier,
                        lastAccess = perm.lastAccess
                    )
                }
            }
        }
    }

    if (showInfoSheet) {
        ModalBottomSheet(
            onDismissRequest = { showInfoSheet = false },
            containerColor = SurfaceWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Permission Categories",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(16.dp))

                val highRiskList = uiState.permissions
                    .filter { it.riskTier == RiskTier.HIGH }
                    .map { it.name.replace("android.permission.", "").replace("_", " ") }

                CategoryInfoRow(
                    title = "Highly Sensitive",
                    content = if (highRiskList.isNotEmpty()) highRiskList.joinToString(", ") else "None",
                    color = Red600
                )

                val sensitiveRaw = uiState.permissions.filter { it.riskTier == RiskTier.SENSITIVE }
                val hasLocation = sensitiveRaw.any { it.name.contains("LOCATION") }

                val sensitiveList = mutableListOf<String>()
                if (hasLocation) sensitiveList.add("Location")

                sensitiveList.addAll(
                    sensitiveRaw
                        .filter { !it.name.contains("LOCATION") }
                        .map { it.name.replace("android.permission.", "").replace("_", " ") }
                )

                CategoryInfoRow(
                    title = "Sensitive",
                    content = if (sensitiveList.isNotEmpty()) sensitiveList.joinToString(", ") else "None",
                    color = Amber600
                )

                CategoryInfoRow(
                    title = "Normal",
                    content = "Rest other permissions",
                    color = Blue600
                )
            }
        }
    }
}

@Composable
fun CategoryInfoRow(title: String, content: String, color: Color) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = "$title:",
            style = MaterialTheme.typography.labelLarge,
            color = color,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            lineHeight = 18.sp,
            maxLines = 5,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SummaryChip(label: String, value: String, color: Color, bgColor: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Clip
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
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (lastAccess.isNotBlank()) {
                    Text(
                        text = lastAccess,
                        style = MaterialTheme.typography.bodySmall,
                        color = Blue600,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }

            if (tier != RiskTier.STANDARD) {
                Surface(
                    color = iconColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (tier == RiskTier.HIGH) stringResource(R.string.tier_high_short) else stringResource(R.string.tier_sensitive_short),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = iconColor,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }
    }
}