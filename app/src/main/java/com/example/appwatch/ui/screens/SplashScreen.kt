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
import com.example.appwatch.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.content.edit

@Composable
fun SplashScreen(onNavigateNext: (String) -> Unit) {
    val context = LocalContext.current

    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
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
        delay(2000L)
        val sharedPref = context.getSharedPreferences("app_watch_prefs", Context.MODE_PRIVATE)
        val isFirstRun = sharedPref.getBoolean("is_first_run", true)
        if (isFirstRun) {
            sharedPref.edit { putBoolean("is_first_run", false) }
            onNavigateNext("onboarding")
        } else {
            onNavigateNext("dashboard")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Indigo500,  // top — slightly lighter
                        Indigo600   // bottom — deeper
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
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale.value)
                    .background(
                        Color.White.copy(alpha = 0.15f),
                        RoundedCornerShape(32.dp)
                    ),
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

            Text(
                text = "Know what your apps\nare doing to you.",
                modifier = Modifier
                    .padding(horizontal = 48.dp)
                    .alpha(alpha.value * 0.85f),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    lineHeight = 26.sp
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }

        // Version — minimal, bottom right only
        Text(
            text = "v1.0.0",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 36.dp)
                .alpha(alpha.value * 0.35f),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}