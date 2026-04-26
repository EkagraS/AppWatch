package com.example.appwatch.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appwatch.ui.theme.*

@Composable
fun DashboardInitialLoader() {
    val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "IconScale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CircleAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Scanner visual
            Box(contentAlignment = Alignment.Center) {
                // Outer pulsing ring
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(scale)
                        .background(Indigo500.copy(alpha = alpha), CircleShape)
                )
                // Inner static ring
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(Indigo500.copy(alpha = 0.06f), CircleShape)
                )
                // Shield icon
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Scanning",
                    modifier = Modifier.size(56.dp),
                    tint = Indigo500
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Setting up AppWatch",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Scanning your apps.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "This only happens once.",
                style = MaterialTheme.typography.bodySmall,
                color = TextDisabled
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 3.dp,
                color = Indigo500,
                trackColor = Indigo100
            )
        }

        // Bottom privacy badge
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .background(SurfaceVariantSoft, RoundedCornerShape(50.dp))
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Shield,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = TextSecondary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "PRIVATE & OFFLINE ANALYSIS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.5.sp
            )
        }
    }
}