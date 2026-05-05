package com.example.appwatch.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.appwatch.R
import com.example.appwatch.ui.viewModels.StorageViewModel
import com.example.appwatch.system.AppStorageInfo
import com.example.appwatch.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllAppsStorageScreen(
    navController: NavController,
    viewModel: StorageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val apps = uiState.userApps
    val maxSize = apps.firstOrNull()?.totalSizeBytes ?: 1L

    val StoragePrimary = Teal600

    var localExpandedIndex by remember { mutableStateOf<Int?>(null) }
    val listState = rememberLazyListState()

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        rememberTopAppBarState()
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = BackgroundLight,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.storage_all_apps_title),
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.storage_apps_count_template, apps.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }
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
        }
    ) { padding ->
        if (apps.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = StoragePrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.storage_analyzing),
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Total Summary Header
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = StoragePrimary.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PieChart, null, tint = StoragePrimary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.storage_total_header),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = formatStorageSize(uiState.totalUserAppsBytes),
                                fontWeight = FontWeight.Black,
                                color = StoragePrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )
                        }
                    }
                }

                itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
                    val isExpanded = localExpandedIndex == index
                    val fraction = if (maxSize > 0) app.totalSizeBytes.toFloat() / maxSize else 0f

                    ExpandableAppStorageCard(
                        app = app,
                        isExpanded = isExpanded,
                        fraction = fraction,
                        accentColor = StoragePrimary,
                        onToggle = {
                            localExpandedIndex = if (isExpanded) null else index
                        },
                        onClick = {
                            navController.navigate("app_detail/${app.packageName}")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ExpandableAppStorageCard(
    app: AppStorageInfo,
    isExpanded: Boolean,
    fraction: Float,
    accentColor: Color,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val animatedFraction by animateFloatAsState(targetValue = fraction, animationSpec = tween(600), label = "bar")
    val arrowRotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, animationSpec = tween(300), label = "arrow")

    val appIcon = remember(app.packageName) {
        try { context.packageManager.getApplicationIcon(app.packageName) } catch (e: Exception) { null }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = appIcon,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }

                Text(
                    text = formatStorageSize(app.totalSizeBytes),
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )

                IconButton(onClick = onToggle, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        null,
                        tint = TextSecondary,
                        modifier = Modifier.rotate(arrowRotation).size(20.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { animatedFraction },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = accentColor,
                trackColor = SurfaceVariantSoft
            )

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    HorizontalDivider(color = DividerColor.copy(alpha = 0.5f), thickness = 0.5.dp)
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StorageBreakdownItem(label = stringResource(R.string.storage_label_app), bytes = app.appSizeBytes, color = StorageApp)
                        StorageBreakdownItem(label = stringResource(R.string.storage_label_data), bytes = app.dataSizeBytes, color = StorageData)
                        StorageBreakdownItem(label = stringResource(R.string.storage_label_cache), bytes = app.cacheSizeBytes, color = StorageCache)
                        StorageBreakdownItem(label = stringResource(R.string.storage_label_total_small), bytes = app.totalSizeBytes, color = StorageTotal)
                    }
                }
            }
        }
    }
}

@Composable
fun StorageBreakdownItem(label: String, bytes: Long, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(IntrinsicSize.Min)) {
        Text(
            text = formatStorageSize(bytes),
            fontWeight = FontWeight.Black,
            fontSize = 13.sp,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}