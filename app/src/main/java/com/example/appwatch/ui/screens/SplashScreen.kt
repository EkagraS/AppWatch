package com.example.appwatch.ui.screens

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onNavigateNext: (String) -> Unit) {
    val context = LocalContext.current

    // Animation states for the logo and text
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        // 1. Run animations in PARALLEL using launch
        launch {
            scale.animateTo(
                targetValue = 1.1f,
                animationSpec = tween(800, easing = FastOutSlowInEasing)
            )
            scale.animateTo(targetValue = 1f, animationSpec = tween(200))
        }

        launch {
            alpha.animateTo(1f, animationSpec = tween(800))
        }

        // 2. TOTAL wait time (Parallel to animations)
        // Reduce this. 2.5s total is better for UX.
        delay(2000L)

        // 3. Navigation Logic
        val sharedPref = context.getSharedPreferences("app_watch_prefs", Context.MODE_PRIVATE)
        val isFirstRun = sharedPref.getBoolean("is_first_run", true)

        onNavigateNext(if (isFirstRun) "onboarding" else "dashboard")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF6366F1), // Indigo Primary
                        Color(0xFF4F46E5)  // Deep Indigo
                    )
                )
            )
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Premium Logo Design
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale.value)
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "AppWatch Shield",
                    modifier = Modifier.size(64.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Bold Typography for App Name
            Text(
                text = "AppWatch",
                modifier = Modifier.alpha(alpha.value),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(10.dp))

            // The Punch-line
            Text(
                text = "Watch the Watchers.\nAudit your Privacy.",
                modifier = Modifier
                    .padding(horizontal = 48.dp)
                    .alpha(alpha.value * 0.8f),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    lineHeight = 24.sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }

        // Professional Footer
        Text(
            text = "SECURED BY ECE LOGIC • VERSION 1.0",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .alpha(alpha.value * 0.4f),
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.sp
            ),
            color = Color.White
        )
    }
}