package com.example.appwatch.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appwatch.domain.model.ActivityItem

@Composable
fun NeedsAttentionSection(
    attentionItems: List<ActivityItem>,
    isUpdating: Boolean,
    onItemClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp) // Match with RecentActivitySection padding
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Needs Attention",
                style = MaterialTheme.typography.titleLarge, // titleMedium se titleLarge kiya (Match Recent)
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 8.dp) // bottom 12 se vertical 8 kiya (Match Recent)
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

        // --- Attention Cards ---
        if (attentionItems.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF10B981).copy(alpha = 0.05f),
                border = BoxDefaults.borderStroke()
            ) {
                Text(
                    "All systems look secure. No immediate action required.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF10B981)
                )
            }
        } else {
            // Spacer hata kar Arrangement use kiya taaki padding Recent jaisi rahe
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp) // 10.dp se 12.dp kiya (Match Recent)
            ) {
                attentionItems.forEach { item ->
                    AttentionCard(
                        item = item,
                        isUpdating = isUpdating,
                        onClick = { onItemClick(item.iconType) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AttentionCard(
    item: ActivityItem, isUpdating: Boolean,
    onClick: () -> Unit
) {
    val (icon, color) = when (item.iconType) {
        "UNUSED" -> Icons.Default.HourglassEmpty to Color(0xFF6366F1)
        "STALE" -> Icons.Default.Block to Color(0xFFF59E0B)
        "SPECIAL" -> Icons.Default.Security to Color(0xFFEF4444)
        else -> Icons.Default.Security to Color(0xFF6366F1)
    }

    Card(
        onClick = if (isUpdating) ({}) else onClick,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Icon Box
            Box(
                modifier = Modifier
                    .size(40.dp) // 44.dp se 40.dp kiya (Match Recent)
                    .background(color.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp) // 22.dp se 20.dp kiya (Match Recent)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium, // bodyLarge se titleMedium kiya (Match Recent)
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }

            // Right Chevron Arrow
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View Details",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

object BoxDefaults {
    @Composable
    fun borderStroke() = androidx.compose.foundation.BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}