package com.example.appwatch.ui.ScreenComponents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.appwatch.domain.model.RecentItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentActivitySection(
    recentItems: List<RecentItem>,
    isUpdating: Boolean,
    onNavigateToEventScreen: (String) -> Unit
) {
    var selectedItem by remember { mutableStateOf<RecentItem?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
//                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Recent Activity",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (isUpdating) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Updating",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.width(8.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (recentItems.isEmpty()) {
            EmptyActivityState()
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                recentItems.forEach { item ->
                    ActivityRowCard(
                        item = item,
                        isUpdating = isUpdating,
                        onClick = {
                            val count = item.description.filter { it.isDigit() }.toIntOrNull() ?: 1
                            if (count > 5) {
                                onNavigateToEventScreen(item.eventType)
                            } else {
                                selectedItem = item
                            }
                        }
                    )
                }
            }
        }

        if (selectedItem != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedItem = null },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                BottomSheetContent(item = selectedItem!!)
            }
        }
    }
}

@Composable
private fun ActivityRowCard(item: RecentItem, isUpdating: Boolean, onClick: () -> Unit) {
    val (iconTint, bgAlpha) = when (item.eventType) {
        "SIDELOADED_APK" -> Pair(Color(0xFFE53935), 0.1f)
        "DATA_HOG" -> Pair(Color(0xFFFB8C00), 0.1f)
        else -> Pair(MaterialTheme.colorScheme.primary, 0.1f)
    }

    Card(
        onClick = if (isUpdating) ({}) else onClick,
        enabled = !item.isTimeline, // Data Hog pe click disable
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = bgAlpha)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconForEventType(item.eventType),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (item.eventType == "SIDELOADED_APK") Color(0xFFE53935) else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!item.isTimeline) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun BottomSheetContent(item: RecentItem) {
    val sheetTitle = when(item.eventType) {
        "INSTALL" -> "New Installations"
        "UPDATE" -> "Recent Updates"
        "SIDELOADED_APK" -> "Unknown Sources"
        "UNINSTALL" -> "Removed Apps"
        else -> "Details"
    }

    val descriptionText = when(item.eventType) {
        "SIDELOADED_APK" -> "These apps were not installed from an official store. They might pose a security risk."
        else -> item.description
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, bottom = 40.dp, top = 8.dp)
    ) {
        Text(
            text = sheetTitle,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (item.eventType == "SIDELOADED_APK") Color(0xFFE53935) else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = descriptionText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun EmptyActivityState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "All Caught Up",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "No recent events to show",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

fun getIconForEventType(eventType: String): ImageVector {
    return when (eventType) {
        "SIDELOADED_APK" -> Icons.Default.GppBad
        "DATA_HOG" -> Icons.Default.DataUsage
        "UPDATE" -> Icons.Default.Update
        "INSTALL" -> Icons.Default.Download
        "UNINSTALL" -> Icons.Default.Delete
        else -> Icons.Default.Info
    }
}