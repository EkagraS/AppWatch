package com.example.appwatch.ui.screens.today

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.appwatch.R
import com.example.appwatch.data.local.entity.AppNotificationEntity
import com.example.appwatch.ui.ScreenComponents.NotificationLoader
import com.example.appwatch.ui.theme.*
import com.example.appwatch.ui.viewmodel.AppNotificationViewmodel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun isNotificationServiceEnabled(context: Context): Boolean {
    val packageNames = NotificationManagerCompat.getEnabledListenerPackages(context)
    return packageNames.contains(context.packageName)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNotificationScreen(
    navController: NavController,
    viewModel: AppNotificationViewmodel = hiltViewModel()
) {
    val context = LocalContext.current

    var isPermissionGranted by remember { mutableStateOf(isNotificationServiceEnabled(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isPermissionGranted = isNotificationServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val uiState by viewModel.uiState.collectAsState()
    val totalNotifications = if (isPermissionGranted) uiState.notificationList.sumOf { it.postedCount } else 0

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState()
    )

    if (!isPermissionGranted) {
        NotificationLoader(onGrantPermissionClick = {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        })
    } else {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = BackgroundLight,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.notif_title_today),
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back),
                                tint = TextPrimary
                            )
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    NotificationSummaryCard(totalNotifications)
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.notif_header_breakdown),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (uiState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxHeight(0.7f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Indigo600)
                        }
                    }
                } else if (uiState.notificationList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxHeight(0.7f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyNotificationsState()
                        }
                    }
                } else {
                    items(uiState.notificationList) { item ->
                        NotificationAppItem(item)
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationSummaryCard(total: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = StatNotifs),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.notif_label_total),
                    color = TextOnDark.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
                Text(
                    text = "$total",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = TextOnDark,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = TextOnDark
            )
        }
    }
}

@Composable
fun NotificationAppItem(item: AppNotificationEntity) {
    val context = LocalContext.current
    val loadingText = stringResource(R.string.notif_status_loading)
    var appName by remember { mutableStateOf(loadingText) }
    var appIcon by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(item.packageName) {
        withContext(Dispatchers.IO) {
            try {
                val pm = context.packageManager
                val appInfo = pm.getApplicationInfo(item.packageName, 0)
                appName = pm.getApplicationLabel(appInfo).toString()
                appIcon = pm.getApplicationIcon(appInfo).toBitmap().asImageBitmap()
            } catch (e: Exception) {
                appName = item.packageName.split(".").last()
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (appIcon != null) {
                    Image(
                        bitmap = appIcon!!,
                        contentDescription = null,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Icon(
                        Icons.Default.Android,
                        contentDescription = null,
                        modifier = Modifier.size(42.dp),
                        tint = Indigo300
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appName,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }

                Surface(
                    color = StatNotifs.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${item.postedCount}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = StatNotifs,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = BackgroundLight, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatDetailItem(
                    label = "Opened",
                    value = item.openedCount,
                    color = Indigo600
                )
                StatDetailItem(
                    label = "Swiped",
                    value = item.dismissedCount,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun StatDetailItem(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$value",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
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

@Composable
fun EmptyNotificationsState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.NotificationsActive, null, tint = TextDisabled, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.notif_empty_state),
            color = TextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}