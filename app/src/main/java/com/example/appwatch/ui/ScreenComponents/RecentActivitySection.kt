package com.example.appwatch.ui.ScreenComponents

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import com.example.appwatch.domain.model.RecentItem
import com.example.appwatch.ui.theme.* // Naye colors yahan se uthayega

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
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Recent Activity",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            if (isUpdating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = Indigo500
                )
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
                containerColor = SurfaceWhite
            ) {
                BottomSheetContent(item = selectedItem!!)
            }
        }
    }
}

@Composable
private fun ActivityRowCard(item: RecentItem, isUpdating: Boolean, onClick: () -> Unit) {
    // Semantic Color Mapping based on your file
    val (iconTint, bgColor) = when (item.eventType) {
        "SIDELOADED_APK" -> Red600 to Red50
        "DATA_HOG" -> Amber600 to Amber50
        "INSTALL" -> Green600 to Green50
        "UPDATE" -> Blue600 to Blue50
        "UNINSTALL" -> Red600 to Red50
        else -> Indigo600 to Indigo50
    }

    Card(
        onClick = if (isUpdating) ({}) else onClick,
        enabled = !item.isTimeline,
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, DividerColor)
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
                    .background(bgColor, CircleShape),
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
                    color = if (item.eventType == "SIDELOADED_APK") Red600 else TextPrimary
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            if (!item.isTimeline) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextDisabled,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun BottomSheetContent(item: RecentItem) {
    val (sheetTitle, titleColor) = when(item.eventType) {
        "INSTALL" -> "New Installations" to TextPrimary
        "UPDATE" -> "Recent Updates" to TextPrimary
        "SIDELOADED_APK" -> "Unknown Sources" to Red600
        "UNINSTALL" -> "Removed Apps" to TextPrimary
        else -> "Details" to TextPrimary
    }

    val descriptionText = when(item.eventType) {
        "SIDELOADED_APK" -> "These apps were not installed from an official store. They might pose a security risk and monitor your system behavior."
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
            color = titleColor,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = descriptionText,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun EmptyActivityState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Green500,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "All Caught Up",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "No recent events to show",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
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