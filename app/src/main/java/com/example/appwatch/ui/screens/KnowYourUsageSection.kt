package com.example.appwatch.ui.screens

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

    var showSheet by remember { mutableStateOf(false) }
    // Naya state taaki sheet ko pata chale Weekly dikhana hai ya Monthly
    var isMonthlySelected by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        // --- STREAK CARDS ---
        Text("Today's Summary", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StreakCard(
                title = "Longest Session",
                range = activeRange,
                color = Color(0xFFFF9800),
                icon = Icons.Default.Whatshot,
                modifier = Modifier.weight(1f)
            )
            StreakCard(
                title = "Longest Break",
                range = inactiveRange,
                color = Color(0xFF3F51B5),
                icon = Icons.Default.NightlightRound,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(20.dp))

        Text("Usage Insights", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))

        // Weekly Card
        InsightCard(
            title = "Weekly Top Performer",
            topApp = weeklyTop3.firstOrNull()?.appName ?: "No Data",
            onClick = {
                isMonthlySelected = false
                showSheet = true
            }
        )

        Spacer(Modifier.height(10.dp))

        // Monthly Card
        InsightCard(
            title = "Monthly Top Performer",
            topApp = monthlyTop3.firstOrNull()?.appName ?: "No Data",
            onClick = {
                isMonthlySelected = true
                showSheet = true
            }
        )
    }

    // --- BOTTOM SHEET ---
    if (showSheet) {
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
                // Title fix: Selected mode ke hisab se header
                Text(
                    text = if (isMonthlySelected) "Top 3 This Month" else "Top 3 This Week",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp
                )
                Spacer(Modifier.height(16.dp))

                // List filter logic
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
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.15f))
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(6.dp))
            Text(title, fontSize = 12.sp, color = Color.Gray)
            Text(range, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color.Black)
        }
    }
}

@Composable
fun InsightCard(title: String, topApp: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Star hata kar clean circle placeholder banaya hai (Yahan App Icon aayega)
            Box(Modifier.size(36.dp).background(Color(0xFFF1F5F9), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.TrendingUp, null, tint = Color(0xFF64748B), modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontSize = 11.sp, color = Color.Gray)
                Text(topApp, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun TopSheetItem(app: AppUsage) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(app.appName, fontWeight = FontWeight.Bold)
        Text(app.usageTimeString, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold)
    }
}