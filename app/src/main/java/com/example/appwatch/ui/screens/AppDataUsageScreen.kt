package com.example.appwatch.ui.screens.today

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.appwatch.data.local.entity.AppDataUsageEntity
import com.example.appwatch.ui.viewmodels.AppDataUsageViewmodel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDataUsageScreen(
    navController: NavController,
    viewModel: AppDataUsageViewmodel=hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 1. Logic: Filter apps with > 1MB total usage
    val filteredList = remember(uiState.dataUsage) {
        uiState.dataUsage.filter {
            (it.mobileUsageBytes + it.wifiUsageBytes) > 1024 * 1024
        }.sortedByDescending { it.mobileUsageBytes + it.wifiUsageBytes }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Today's Data Usage") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (filteredList.isEmpty()) {
                Text(
                    text = "No apps used more than 1MB today.",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredList) { usage ->
                        DataUsageItem(usage, context)
                    }
                }
            }
        }
    }
}

@Composable
fun DataUsageItem(usage: AppDataUsageEntity, context: android.content.Context) {
    val pm = context.packageManager

    // App details fetch karna
    val appInfo = try {
        pm.getApplicationInfo(usage.packageName, 0)
    } catch (e: Exception) {
        null
    }

    val appName = appInfo?.loadLabel(pm)?.toString() ?: usage.packageName
    val appIcon = appInfo?.loadIcon(pm) // Asali Icon yahan se aayega

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. App Icon (Coil use karke Drawable load kar rahe hain)
            AsyncImage(
                model = appIcon,
                contentDescription = null,
                modifier = Modifier.size(44.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            // 2. Center Column: Name and Package Name
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1
                )
                // App Name ke niche Package Name
                Text(
                    text = usage.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1
                )
            }

            // 3. Right Side: Data Breakdown (Mobile/WiFi)
            Column(horizontalAlignment = Alignment.End) {
                if (usage.mobileUsageBytes > 0) {
                    Text(
                        text = "Mobile: ${formatBytes(usage.mobileUsageBytes)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFE91E63), // Pinkish for Mobile
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (usage.wifiUsageBytes > 0) {
                    Text(
                        text = "WiFi: ${formatBytes(usage.wifiUsageBytes)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF2196F3), // Blue for WiFi
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Total ki line (Optional, par breakdown ke niche choti si achi lagti hai)
                HorizontalDivider(
                    modifier = Modifier.width(40.dp).padding(vertical = 2.dp),
                    thickness = 0.5.dp,
                    color = Color.LightGray
                )

                Text(
                    text = formatBytes(usage.mobileUsageBytes + usage.wifiUsageBytes),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun UsageChip(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}

// Helper function to format bytes
fun formatBytes(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0

    return when {
        gb >= 1.0 -> String.format("%.2f GB", gb)
        mb >= 1.0 -> String.format("%.2f MB", mb)
        kb >= 1.0 -> String.format("%.2f KB", kb)
        else -> "$bytes B"
    }
}