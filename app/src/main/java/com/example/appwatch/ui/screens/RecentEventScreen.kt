package com.example.appwatch.ui.screens

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.appwatch.data.local.entity.RecentEventEntity
import com.example.appwatch.presentation.viewmodel.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// import com.example.appwatch.ui.viewmodel.DashboardViewModel // 🔴 Apna ViewModel import kar lena

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentEventScreen(
    eventType: String,
    onNavigateBack: () -> Unit,
    // 🔴 TERA ASLI VIEWMODEL JISE HUM DB SE CONNECT KARENGE
    viewModel: DashboardViewModel = hiltViewModel()
) {
    // DB se Live flow collect ho raha hai
    val eventList by viewModel.getEventsByType(eventType).collectAsState(initial = emptyList())

    val screenTitle = when (eventType) {
        "INSTALL" -> "New Installations"
        "UPDATE" -> "Recent Updates"
        "SIDELOADED_APK" -> "Unknown Sources"
        "UNINSTALL" -> "Removed Apps"
        "DATA_HOG" -> "Heavy Data Consumers"
        else -> "Recent Activity"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = screenTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->

        if (eventList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No data available", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 🔴 THE REAL LOOP: DB se aayi actual apps ki list chal rahi hai
                items(eventList) { event ->
                    EventListItemCard(event = event)
                }
            }
        }
    }
}

@Composable
fun EventListItemCard(event: RecentEventEntity) {
    val context = LocalContext.current
    val packageManager = context.packageManager

    // States for Real App Name and Icon
    var appName by remember(event.packageName) { mutableStateOf(event.packageName) } // Default fallback
    var appIconBitmap by remember(event.packageName) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    // 🔴 BACKGROUND THREAD MEIN FETCH KARENGE TAQKI SCROLLING MAKHAN RAHE
    LaunchedEffect(event.packageName) {
        withContext(Dispatchers.IO) {
            try {
                // Asli Naam Nikaalo
                val appInfo = packageManager.getApplicationInfo(event.packageName, PackageManager.GET_META_DATA)
                appName = packageManager.getApplicationLabel(appInfo).toString()

                // Asli Icon Nikaalo aur Compose format (ImageBitmap) mein convert karo
                val drawable: Drawable = packageManager.getApplicationIcon(event.packageName)
                // Convert drawable to bitmap safely (handles Adaptive Icons too)
                appIconBitmap = drawable.toBitmap().asImageBitmap()

            } catch (e: PackageManager.NameNotFoundException) {
                // 🚨 UNINSTALLED APPS KE LIYE:
                // Agar app delete ho chuki hai, toh OS naam nahi de payega.
                // Uss case mein fallback humara package name hi rahega.
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp) // Thoda premium look
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 🔴 REAL APP ICON YA DEFAULT ICON
            if (appIconBitmap != null) {
                Image(
                    bitmap = appIconBitmap!!,
                    contentDescription = appName,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp)) // Icon ko thoda curve denge
                )
            } else {
                // Agar load nahi hua ya app uninstall ho chuki hai, toh Android ka default icon dikhao
                Icon(
                    imageVector = Icons.Default.Android,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appName, // Asli naam print hoga yahan!
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Extra Info ya Package Name
                Text(
                    text = if (!event.extraInfo.isNullOrEmpty()) event.extraInfo else event.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Right side mein tera chota sa event type ka icon (optional premium touch)
            Icon(
                imageVector = getIconForEventType(event.eventType),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}