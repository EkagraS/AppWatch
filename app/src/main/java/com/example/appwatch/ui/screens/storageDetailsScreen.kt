package com.example.appwatch.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.appwatch.presentation.viewmodel.StorageViewModel
import com.example.appwatch.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageDetailScreen(
    navController: NavController,
    viewModel: StorageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            viewModel.loadMediaStorage()
            viewModel.checkMediaPermission()
        }
    }

    fun requestMediaPermission() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissions)
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        rememberTopAppBarState()
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.storage_analysis_title),
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
                actions = {
                    if (uiState.isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp).padding(end = 16.dp),
                            strokeWidth = 2.dp,
                            color = Indigo600
                        )
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {

            if (uiState.isRefreshing) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Indigo600.copy(alpha = 0.08f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Indigo600)
                            Text(
                                text = stringResource(R.string.storage_updating_msg),
                                style = MaterialTheme.typography.bodySmall,
                                color = Indigo600,
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )
                        }
                    }
                }
            }

            item {
                DeviceStorageCard(
                    totalBytes = uiState.deviceStorage?.totalBytes ?: 0L,
                    usedBytes = uiState.deviceStorage?.usedBytes ?: 0L,
                    freeBytes = uiState.deviceStorage?.freeBytes ?: 0L
                )
            }

            item {
                Text(
                    text = stringResource(R.string.storage_apps_header),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { navController.navigate("all_apps_storage") },
                        colors = CardDefaults.cardColors(containerColor = Indigo50)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).background(Indigo100, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Apps, null, tint = Indigo600, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.storage_apps_downloaded),
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(R.string.storage_apps_count_template, uiState.userApps.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                            }
                            Text(
                                text = formatStorageSize(uiState.totalUserAppsBytes),
                                fontWeight = FontWeight.Bold,
                                color = Indigo600,
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )
                            Icon(Icons.Default.ChevronRight, null, tint = TextSecondary)
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Purple50)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).background(Purple100, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Settings, null, tint = Purple600, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.storage_apps_system),
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(R.string.storage_apps_count_template, uiState.systemApps.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                            }
                            Text(
                                text = formatStorageSize(uiState.totalSystemAppsBytes),
                                fontWeight = FontWeight.Bold,
                                color = Purple600,
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.storage_largest_header),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val top3 = uiState.userApps.take(3)
            val maxSize = top3.firstOrNull()?.totalSizeBytes ?: 1L

            if (uiState.isLoadingFromRoom && top3.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Indigo600, modifier = Modifier.size(32.dp))
                    }
                }
            } else {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        top3.forEach { app ->
                            TopAppStorageRow(
                                app = app,
                                maxBytes = maxSize,
                                onClick = { navController.navigate("app_detail/${app.packageName}") }
                            )
                        }
                        if (uiState.userApps.size > 3) {
                            TextButton(
                                onClick = { navController.navigate("all_apps_storage") },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text(
                                    text = stringResource(R.string.storage_more_apps_template, uiState.userApps.size - 3),
                                    color = Indigo600,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.storage_media_header),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            item {
                MediaStorageSection(
                    mediaStorage = uiState.mediaStorage,
                    onEnablePermission = { requestMediaPermission() }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun TopAppStorageRow(
    app: com.example.appwatch.system.AppStorageInfo,
    maxBytes: Long,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val fraction = if (maxBytes > 0) app.totalSizeBytes.toFloat() / maxBytes else 0f
    val animatedFraction by animateFloatAsState(targetValue = fraction, animationSpec = tween(800), label = "prog")

    val appIcon = remember(app.packageName) {
        try { context.packageManager.getApplicationIcon(app.packageName) } catch (e: Exception) { null }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = appIcon,
                    contentDescription = null,
                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = app.appName,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatStorageSize(app.totalSizeBytes),
                    fontWeight = FontWeight.Bold,
                    color = Indigo600,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
            LinearProgressIndicator(
                progress = { animatedFraction },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = Indigo600,
                trackColor = SurfaceVariantSoft
            )
        }
    }
}

@Composable
fun MediaStorageSection(
    mediaStorage: com.example.appwatch.presentation.viewmodel.MediaStorageInfo,
    onEnablePermission: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!mediaStorage.hasPermission) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.storage_media_locked_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                listOf(
                    Triple(Icons.Default.Photo, stringResource(R.string.storage_media_photos), Red500),
                    Triple(Icons.Default.VideoLibrary, stringResource(R.string.storage_media_videos), Purple500),
                    Triple(Icons.Default.MusicNote, stringResource(R.string.storage_media_music), Cyan500),
                    Triple(Icons.Default.Download, stringResource(R.string.storage_media_downloads), Green500)
                ).forEach { (icon, label, color) ->
                    MediaLockedRow(icon = icon, label = label, color = color)
                }
                Button(
                    onClick = onEnablePermission,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.btn_enable_storage))
                }
            } else {
                listOf(
                    Triple(Icons.Default.Photo, stringResource(R.string.storage_media_photos), mediaStorage.photosBytes to Red500),
                    Triple(Icons.Default.VideoLibrary, stringResource(R.string.storage_media_videos), mediaStorage.videosBytes to Purple500),
                    Triple(Icons.Default.MusicNote, stringResource(R.string.storage_media_music), mediaStorage.musicBytes to Cyan500),
                    Triple(Icons.Default.Download, stringResource(R.string.storage_media_downloads), mediaStorage.downloadsBytes to Green500)
                ).forEach { (icon, label, pair) ->
                    MediaUnlockedRow(icon = icon, label = label, bytes = pair.first, color = pair.second)
                }
            }
        }
    }
}

@Composable
fun MediaLockedRow(icon: ImageVector, label: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(36.dp).background(color.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(Icons.Default.Lock, null, tint = TextDisabled, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun MediaUnlockedRow(icon: ImageVector, label: String, bytes: Long, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(36.dp).background(color.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = formatStorageSize(bytes),
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}

@Composable
fun DeviceStorageCard(totalBytes: Long, usedBytes: Long, freeBytes: Long) {
    val usedFraction = if (totalBytes > 0) usedBytes.toFloat() / totalBytes else 0f
    val animatedProgress by animateFloatAsState(targetValue = usedFraction, animationSpec = tween(800), label = "prog")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = stringResource(R.string.storage_device_header),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape),
                    color = if (usedFraction > 0.9f) Red500 else Indigo600,
                    trackColor = SurfaceVariantSoft,
                    strokeCap = StrokeCap.Round
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = stringResource(R.string.storage_status_used, formatStorageSize(usedBytes)),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                    Text(
                        text = stringResource(R.string.storage_status_free, formatStorageSize(freeBytes)),
                        style = MaterialTheme.typography.bodySmall,
                        color = Green600,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StorageStatItem(stringResource(R.string.storage_label_total), formatStorageSize(totalBytes), TextPrimary)
                StorageStatItem(stringResource(R.string.storage_label_used), formatStorageSize(usedBytes), Indigo600)
                StorageStatItem(stringResource(R.string.storage_label_free), formatStorageSize(freeBytes), Green600)
            }
        }
    }
}

@Composable
fun StorageStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}

fun formatStorageSize(bytes: Long): String {
    return when {
        bytes >= 1024L * 1024 * 1024 -> "%.1f GB".format(bytes / (1024.0 * 1024 * 1024))
        bytes >= 1024L * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
        bytes >= 1024L -> "%.1f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
}