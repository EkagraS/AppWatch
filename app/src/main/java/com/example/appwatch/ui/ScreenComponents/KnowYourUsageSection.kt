package com.example.appwatch.ui.ScreenComponents

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.appwatch.domain.model.AppUsage
import com.example.appwatch.presentation.viewmodel.UsageStatsViewModel
import com.example.appwatch.ui.screens.AppIconSmall
import com.example.appwatch.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowYourUsageSection(viewModel: UsageStatsViewModel) {
    val activeRange by viewModel.todayActiveStreak.collectAsStateWithLifecycle()
    val inactiveRange by viewModel.todayInactiveStreak.collectAsStateWithLifecycle()
    val highNoiseApps by viewModel.highNoiseApps.collectAsStateWithLifecycle()

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text("Today's Summary", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(18.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StreakCard(
                title = "Longest Active usage",
                range = activeRange,
                color = Orange600,
                icon = Icons.Default.Timer,
                modifier = Modifier.weight(1f)
            )
            StreakCard(
                title = "Longest Inactive session",
                range = inactiveRange,
                color = Green600,
                icon = Icons.Default.PauseCircle,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(20.dp))

        Text("Apps with more noise", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Red50),
            border = BorderStroke(1.dp, Red100)
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                if (highNoiseApps.isEmpty()) {
                    Text(
                        "No apps detected this week.",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    highNoiseApps.forEach { app ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(app.appName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                "${app.notificationCount} alerts • ${app.appUnlocks} opens",
                                fontSize = 13.sp,
                                color = Red600,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        PremiumInsightsSection(viewModel)
    }
}

@Composable
fun StreakCard(title: String, range: String, color: Color, icon: ImageVector, modifier: Modifier) {
    Surface(
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.15f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, fontSize = 12.sp, color = Color.Gray)
            Text(range, fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color.Black)
        }
    }
}

@Composable
fun PremiumInsightsSection(viewModel: UsageStatsViewModel) {
    val weeklyTop by viewModel.top3AppsWeekly.collectAsStateWithLifecycle()
    val monthlyTop by viewModel.top3AppsMonthly.collectAsStateWithLifecycle()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Usage Insights", fontWeight = FontWeight.ExtraBold, color = TextPrimary, fontSize = 18.sp)

        // Blue ki jagah Teal use kiya hai
        InsightModernCard(
            title = "Most used app this week",
            apps = weeklyTop,
            backgroundColor = Teal50,
            accentColor = Teal600
        )

        InsightModernCard(
            title = "Most used app this month",
            apps = monthlyTop,
            backgroundColor = Purple50,
            accentColor = Purple600
        )
    }
}

@Composable
fun InsightModernCard(
    title: String,
    apps: List<AppUsage>,
    backgroundColor: Color,
    accentColor: Color
) {
    var isExpanded by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f)

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded } // Toggle on card click
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.Bold, color = accentColor, fontSize = 14.sp)
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand",
                    tint = accentColor,
                    modifier = Modifier.rotate(rotationState) // Rotation logic
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    if (apps.isEmpty()) {
                        Text("No data available yet", color = TextSecondary, fontSize = 12.sp)
                    } else {
                        apps.forEach { app ->
                            Row(
                                modifier = Modifier.padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppIconSmall(app.packageName)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    // FIXED: Ensuring app.appName is used
                                    text = app.appName,
                                    modifier = Modifier.weight(1f),
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = TextPrimary
                                )
                                Text(app.usageTimeString, fontWeight = FontWeight.Bold, color = accentColor)
                            }
                        }
                    }
                }
            }
        }
    }
}