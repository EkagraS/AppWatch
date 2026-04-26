package com.example.appwatch.ui.ScreenComponents

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.appwatch.domain.model.AppUsage
import com.example.appwatch.presentation.viewmodel.UsageStatsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowYourUsageSection(viewModel: UsageStatsViewModel) {
    val activeRange by viewModel.todayActiveStreak.collectAsStateWithLifecycle()
    val inactiveRange by viewModel.todayInactiveStreak.collectAsStateWithLifecycle()
    val weeklyTop3 by viewModel.top3AppsWeekly.collectAsStateWithLifecycle()
    val monthlyTop3 by viewModel.top3AppsMonthly.collectAsStateWithLifecycle()

    val highNoiseApps by viewModel.highNoiseApps.collectAsStateWithLifecycle()

    var showSheet by remember { mutableStateOf(false) }
    var isMonthlySelected by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // Outer padding wapas standard ki hai taaki screen ke edges se na chipke
    Column(modifier = Modifier.padding(vertical = 8.dp)) {

        Text("Today's Summary", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StreakCard(
                title = "Longest Active usage",
                range = activeRange,
                color = Color(0xFFF97316),
                icon = Icons.Default.Timer,
                modifier = Modifier.weight(1f)
            )
            StreakCard(
                title = "Longest Inactive session",
                range = inactiveRange,
                color = Color(0xFF10B981),
                icon = Icons.Default.PauseCircle,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(20.dp))

        // --- HIGH ALERTS SECTION ---
        Text("Apps with more notifications and less usage", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
            border = BorderStroke(1.dp, Color(0xFFFECACA))
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                if (highNoiseApps.isEmpty()) {
                    Text(
                        "No apps detected this week.",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 12.dp) // Empty state box bada dikhega ab
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
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // --- USAGE INSIGHTS ---
        Text("Usage Insights", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(10.dp))

        InsightCard(
            title = "Most used app this week",
            onClick = {
                isMonthlySelected = false
                showSheet = true
            }
        )

        Spacer(Modifier.height(10.dp))

        InsightCard(
            title = "Most used app this month",
            onClick = {
                isMonthlySelected = true
                showSheet = true
            }
        )
    }

    if (showSheet && (monthlyTop3.isNotEmpty() || weeklyTop3.isNotEmpty())) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = if (isMonthlySelected) "Most used App this" else "Most used App this",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
                Spacer(Modifier.height(16.dp))

                val currentList = if (isMonthlySelected) monthlyTop3 else weeklyTop3
                currentList.forEach { app ->
                    TopSheetItem(app)
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun StreakCard(title: String, range: String, color: Color, icon: ImageVector, modifier: Modifier) {
    Surface(
        // HEIGHT BADHAYI HAI (85dp -> 110dp) taaki box lamba aur bhara hua lage
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.15f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(28.dp)) // Icon bhi bada kiya thoda
            Spacer(Modifier.height(8.dp))
            Text(title, fontSize = 12.sp, color = Color.Gray)
            Text(range, fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color.Black)
        }
    }
}

@Composable
fun InsightCard(title: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(14.dp), // Border radius slightly badhaya
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background(Color(0xFFF1F5F9), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.TrendingUp, null, tint = Color(0xFF64748B), modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, fontSize = 16.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun TopSheetItem(app: AppUsage) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(app.appName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(app.usageTimeString, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}