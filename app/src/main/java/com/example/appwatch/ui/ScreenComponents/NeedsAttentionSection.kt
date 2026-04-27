package com.example.appwatch.ui.ScreenComponents

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appwatch.domain.model.ActivityItem
import com.example.appwatch.ui.theme.Amber50
import com.example.appwatch.ui.theme.Amber600
import com.example.appwatch.ui.theme.DividerColor
import com.example.appwatch.ui.theme.Green100
import com.example.appwatch.ui.theme.Green50
import com.example.appwatch.ui.theme.Green600
import com.example.appwatch.ui.theme.Indigo50
import com.example.appwatch.ui.theme.Indigo500
import com.example.appwatch.ui.theme.Red50
import com.example.appwatch.ui.theme.Red600
import com.example.appwatch.ui.theme.SurfaceWhite
import com.example.appwatch.ui.theme.TextDisabled
import com.example.appwatch.ui.theme.TextPrimary
import com.example.appwatch.ui.theme.TextSecondary

@Composable
fun NeedsAttentionSection(
    attentionItems: List<ActivityItem>,
    isUpdating: Boolean,
    onItemClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Needs Attention",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            if (isUpdating && attentionItems.isNotEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = Indigo500
                )
            }
        }

        if (attentionItems.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = RoundedCornerShape(16.dp),
                color = Green50,
                border = BorderStroke(1.dp, Green100)
            ) {
                Text(
                    "All systems look secure. No immediate action required.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Green600,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
private fun AttentionCard(item: ActivityItem, isUpdating: Boolean, onClick: () -> Unit) {
    val (icon, color, bgColor) = when (item.iconType) {
        "UNUSED" -> Triple(Icons.Default.HourglassEmpty, Indigo500, Indigo50)
        "STALE" -> Triple(Icons.Default.Block, Amber600, Amber50)
        "SPECIAL" -> Triple(Icons.Default.Security, Red600, Red50)
        else -> Triple(Icons.Default.Security, Indigo500, Indigo50)
    }

    Card(
        onClick = if (isUpdating) ({}) else onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, DividerColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(bgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }

            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = TextDisabled, modifier = Modifier.size(20.dp))
        }
    }
}