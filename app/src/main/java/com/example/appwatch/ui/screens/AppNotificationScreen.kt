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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.appwatch.data.local.entity.AppNotificationEntity
import com.example.appwatch.ui.ScreenComponents.NotificationLoader // 👈 Tera component
import com.example.appwatch.ui.theme.* // 👈 Naye colors
import com.example.appwatch.ui.viewmodel.AppNotificationViewmodel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Permission Check Helper for Notification Listener
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

    // ─── Permission State & Observer ──────────────────────────────────────────
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

    // ─── Data Flow ────────────────────────────────────────────────────────────
    val uiState by viewModel.uiState.collectAsState()
    val totalNotifications = if (isPermissionGranted) uiState.notificationList.sumOf { it.count } else 0

    // ─── Conditional Rendering ────────────────────────────────────────────────
    if (!isPermissionGranted) {
        NotificationLoader(onGrantPermissionClick = {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        })
    } else {
        Scaffold(
            containerColor = BackgroundLight, // 👈 New Theme Color
            topBar = {
                TopAppBar(
                    title = { Text("Today's Notifications", fontWeight = FontWeight.Bold, color = TextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundLight)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                NotificationSummaryCard(totalNotifications)

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Notification Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Indigo600)
                    }
                } else if (uiState.notificationList.isEmpty()) {
                    EmptyNotificationsState()
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        items(uiState.notificationList) { item ->
                            NotificationAppItem(item)
                        }
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
        colors = CardDefaults.cardColors(containerColor = StatNotifs), // 👈 Orange500 from theme
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Total Notifications", color = TextOnDark.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "$total",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = TextOnDark
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
    var appName by remember { mutableStateOf("Loading...") }
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
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite), // 👈 Clean look
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                Text(text = appName, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1)
                Text(
                    text = item.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1
                )
            }

            Text(
                text = "${item.count}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = StatNotifs // 👈 Orange accent for count
            )
        }
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
        Text("No notifications tracked yet", color = TextSecondary)
    }
}