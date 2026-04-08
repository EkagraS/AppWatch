package com.example.appwatch.ui.screens

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Data for the slides
    val slides = listOf(
        OnboardingData(
            title = "Audit Your Privacy",
            description = "Gain deep insights into which apps are accessing your camera, mic, and location in real-time.",
            icon = Icons.Default.Security,
            color = Color(0xFF818CF8) // Lighter Indigo for icons on dark background
        ),
        OnboardingData(
            title = "Usage Intelligence",
            description = "Monitor screen time and identify apps you haven't used in 30 days that still hold risky permissions.",
            icon = Icons.Default.History,
            color = Color(0xFF34D399) // Lighter Emerald
        ),
        OnboardingData(
            title = "Watch the Watchers",
            description = "Receive instant alerts for high-risk background activities and take control of your digital footprint.",
            icon = Icons.Default.Lock,
            color = Color(0xFFA78BFA) // Lighter Purple
        )
    )

    val pagerState = rememberPagerState(pageCount = { slides.size })

    // Using a gradient background to match the Splash/Dashboard theme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E1B4B), // Deepest Indigo (Night)
                        Color(0xFF312E81)  // Deep Indigo
                    )
                )
            )
            .systemBarsPadding() // FIX: Ensures UI doesn't overlap with status or navigation bars
    ) {
        // Skip Button (only if not on the last page)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            if (pagerState.currentPage < slides.size - 1) {
                TextButton(onClick = {
                    completeOnboarding(context, onFinish)
                }) {
                    Text(
                        "Skip",
                        color = Color.White.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                // Empty box to maintain spacing
                Spacer(modifier = Modifier.height(48.dp))
            }
        }

        // Pager Content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            OnboardingSlide(data = slides[page])
        }

        // Bottom Section: Indicators and Buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 32.dp, end = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page Indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                repeat(slides.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (isSelected) 24.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color(0xFF6366F1)
                                else Color.White.copy(alpha = 0.2f)
                            )
                    )
                }
            }

            // Navigation Button
            Button(
                onClick = {
                    if (pagerState.currentPage < slides.size - 1) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        completeOnboarding(context, onFinish)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6366F1),
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = if (pagerState.currentPage == slides.size - 1) "Get Started" else "Continue",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun OnboardingSlide(data: OnboardingData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // High-End Icon Container
        Box(
            modifier = Modifier
                .size(180.dp)
                .background(data.color.copy(alpha = 0.15f), RoundedCornerShape(56.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = data.icon,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = data.color
            )
        }

        Spacer(modifier = Modifier.height(60.dp))

        Text(
            text = data.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = data.description,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 26.sp
        )
    }
}

private fun completeOnboarding(context: Context, onFinish: () -> Unit) {
    val sharedPref = context.getSharedPreferences("app_watch_prefs", Context.MODE_PRIVATE)
    sharedPref.edit().putBoolean("is_first_run", false).apply()
    onFinish()
}

data class OnboardingData(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)