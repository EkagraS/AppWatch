package com.example.appwatch.ui.screens.today

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.appwatch.data.local.entity.AppNotificationEntity
import com.example.appwatch.ui.viewmodel.AppNotificationViewmodel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNotificationScreen(
    navController: NavController,
    viewModel: AppNotificationViewmodel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val totalNotifications = uiState.notificationList.sumOf { it.count }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Today's Notifications", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
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
            NotificationSummaryCard(totalNotifications)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Notification Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.notificationList.isEmpty()) {
                EmptyNotificationsState()
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Total Notifications", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "$total",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black
                )
            }
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun NotificationAppItem(item: AppNotificationEntity) {
    val context = LocalContext.current
    var appName by remember { mutableStateOf("Loading...") }
    var appIcon by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    // 🔴 ASYNC FETCHING: Scroll ke time lag nahi aayega
    LaunchedEffect(item.packageName) {
        withContext(Dispatchers.IO) {
            try {
                val pm = context.packageManager
                val appInfo = pm.getApplicationInfo(item.packageName, 0)
                appName = pm.getApplicationLabel(appInfo).toString()
                appIcon = pm.getApplicationIcon(appInfo).toBitmap().asImageBitmap()
            } catch (e: Exception) {
                appName = item.packageName
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (appIcon != null) {
                Image(
                    bitmap = appIcon!!,
                    contentDescription = null,
                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(8.dp))
                )
            } else {
                Icon(Icons.Default.Android, contentDescription = null, modifier = Modifier.size(42.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = appName, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(text = item.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Text(
                text = "${item.count}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
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
        Text("No notifications tracked yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}