package com.example.appwatch.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DashboardInitialLoader() {
    // 1. Pulse Animation Logic for the Shield
    val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "IconScale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "CircleAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // --- CENTRAL SCANNER VISUAL ---
        Box(contentAlignment = Alignment.Center) {
            // Pulsing background circle
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(scale)
                    .background(Color(0xFF10B981).copy(alpha = alpha), CircleShape)
            )

            // Static inner circle for depth
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color(0xFF10B981).copy(alpha = 0.05f), CircleShape)
            )

            // The main Security Icon
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Scanning",
                modifier = Modifier.size(64.dp),
                tint = Color(0xFF10B981)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // --- STATUS TEXT ---
        Text(
            text = "Initializing AppWatch",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Analyzing system permissions...",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(48.dp))

        // --- SUBTLE PROGRESS INDICATOR ---
        CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            strokeWidth = 3.dp,
            color = Color(0xFF10B981),
            trackColor = Color.LightGray.copy(alpha = 0.2f)
        )

        // --- FOOTER SECURITY TAG ---
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(bottom = 20.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                modifier = Modifier
                    .background(Color(0xFFF3F4F6), RoundedCornerShape(50.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color.DarkGray
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "PRIVATE & OFFLINE ANALYSIS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}