package com.example.appwatch.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- SHARED BRUSH HELPER ---
@Composable
private fun rememberShimmerBrush(): Brush {
    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.3f),
        Color.LightGray.copy(alpha = 0.1f),
        Color.LightGray.copy(alpha = 0.3f),
    )
    val transition = rememberInfiniteTransition(label = "Shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "ShimmerAnim"
    )
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )
}

// --- 1. DASHBOARD SCREEN SHIMMER ---
@Composable
fun DashboardShimmer() {
    val brush = rememberShimmerBrush()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Overview (3 Boxes)
        Spacer(modifier = Modifier.height(80.dp))
        Text(
            "AppWatch Dashboard",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(30.dp))
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(40.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(3) { Box(modifier = Modifier.weight(1f).height(110.dp).clip(RoundedCornerShape(16.dp)).background(brush)) }
        }
        Spacer(modifier = Modifier.height(32.dp))
        // Recent Activity (Text + 3 Rectangles)
        Box(modifier = Modifier.width(140.dp).height(20.dp).background(brush))
        Spacer(modifier = Modifier.height(16.dp))
        repeat(3) {
            Box(modifier = Modifier.fillMaxWidth().height(65.dp).clip(RoundedCornerShape(12.dp)).background(brush))
            Spacer(modifier = Modifier.height(12.dp))
        }
        Spacer(modifier = Modifier.height(32.dp))
        // Privacy Insights (4 Boxes)
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                repeat(2) { Box(modifier = Modifier.weight(1f).height(100.dp).clip(RoundedCornerShape(16.dp)).background(brush)) }
            }
        }
    }
}

// --- 2. STORAGE SCREEN SHIMMER ---
@Composable
fun StorageShimmer() {
    val brush = rememberShimmerBrush()
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        // Large Chart Placeholder
        Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(24.dp)).background(brush))
        Spacer(modifier = Modifier.height(32.dp))
        // List of Storage Categories
        repeat(5) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(brush))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Box(modifier = Modifier.width(120.dp).height(16.dp).background(brush))
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.width(200.dp).height(12.dp).background(brush))
                }
            }
        }
    }
}

// --- 3. APP LIST SHIMMER ---
@Composable
fun AppListShimmer() {
    val brush = rememberShimmerBrush()
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        repeat(10) {
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)).background(brush))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Box(modifier = Modifier.width(150.dp).height(18.dp).background(brush))
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.width(100.dp).height(14.dp).background(brush))
                    }
                }
            }
        }
    }
}

// --- 4. SCREEN USAGE SHIMMER ---
@Composable
fun UsageShimmer() {
    val brush = rememberShimmerBrush()
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        // Big Header (Total Screen Time)
        Box(modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(20.dp)).background(brush))
        Spacer(modifier = Modifier.height(32.dp))
        // Usage Bars List
        repeat(6) {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.width(80.dp).height(14.dp).background(brush))
                    Box(modifier = Modifier.width(40.dp).height(14.dp).background(brush))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(brush))
            }
        }
    }
}