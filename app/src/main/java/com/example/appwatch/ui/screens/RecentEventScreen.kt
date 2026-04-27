package com.example.appwatch.ui.screens

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowBack
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
import com.example.appwatch.data.local.entity.RecentEventEntity
import com.example.appwatch.presentation.viewmodel.DashboardViewModel
import com.example.appwatch.ui.ScreenComponents.getIconForEventType
import com.example.appwatch.ui.theme.* // Sabhi naye colors yahan se aayenge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentEventScreen(
    navController: NavController,
    eventType: String,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val eventList by viewModel.getEventsByType(eventType).collectAsState(initial = emptyList())

    // Balanced & Understandable Titles
    val screenTitle = when (eventType) {
        "INSTALL" -> "Newly Installed Apps"
        "UPDATE" -> "Recently Updated Apps"
        "SIDELOADED_APK" -> "Unknown Source Installs"
        "UNINSTALL" -> "Recently Removed Apps"
        "DATA_HOG" -> "High Data Consumption"
        else -> "Recent App Activity"
    }

    Scaffold(
        containerColor = BackgroundLight, // Naya light background
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = screenTitle,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {navController.popBackStack()}) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceWhite // Header clear dikhega
                )
            )
        }
    ) { paddingValues ->

        if (eventList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No activity recorded yet", color = TextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp), // Thoda better spacing
                contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
            ) {
                items(eventList) { event ->
                    EventListItemCard(event = event, onAppClick = { navController.navigate("app_detail/${event.packageName}") })
                }
            }
        }
    }
}

@Composable
fun EventListItemCard(event: RecentEventEntity, onAppClick: () -> Unit) {
    val context = LocalContext.current
    val packageManager = context.packageManager

    var appName by remember(event.packageName) { mutableStateOf(event.packageName) }
    var appIconBitmap by remember(event.packageName) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(event.packageName) {
        withContext(Dispatchers.IO) {
            try {
                val appInfo = packageManager.getApplicationInfo(event.packageName, PackageManager.GET_META_DATA)
                appName = packageManager.getApplicationLabel(appInfo).toString()
                val drawable: Drawable = packageManager.getApplicationIcon(event.packageName)
                appIconBitmap = drawable.toBitmap().asImageBitmap()
            } catch (e: PackageManager.NameNotFoundException) {
                // Keep fallback
            }
        }
    }

    Card(
        onClick = onAppClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceWhite // Clean white cards
        ),
        shape = RoundedCornerShape(16.dp), // Premium rounded corners
        border = BorderStroke(1.dp, DividerColor) // Subtle border as per new system
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (appIconBitmap != null) {
                Image(
                    bitmap = appIconBitmap!!,
                    contentDescription = appName,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Android,
                    contentDescription = null,
                    tint = Indigo500, // Primary brand color fallback
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = if (!event.extraInfo.isNullOrEmpty()) event.extraInfo else event.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            // Semantic Icon Tinting based on event type
            val iconTint = when(event.eventType) {
                "INSTALL" -> ActivityInstall
                "UNINSTALL" -> ActivityUninstall
                "UPDATE" -> ActivityUpdate
                else -> TextSecondary.copy(alpha = 0.6f)
            }

            Icon(
                imageVector = getIconForEventType(event.eventType),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}