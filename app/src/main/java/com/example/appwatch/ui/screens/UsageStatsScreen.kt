package com.example.appwatch.ui.screens

import android.content.pm.PackageManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.appwatch.domain.model.AppUsage
import com.example.appwatch.presentation.viewmodel.UsageStatsViewModel
import com.example.appwatch.system.UsageStatsHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// --- CLEAN THEME PALETTE ---
val TextMain = Color(0xFF111827)      // Deep charcoal/black for titles
val TextSecondary = Color(0xFF6B7280) // Muted gray for axis labels
val AppBackground = Color(0xFFF9FAFB) // Light gray background
val SurfaceWhite = Color(0xFFFFFFFF)  // Pure white for cards

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageStatsScreen(
    navController: NavController,
    viewModel: UsageStatsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val pm = remember { context.packageManager }
    val helper = remember { UsageStatsHelper(context) }

    // --- UPDATED VIBRANT PALETTE ---
    val AppThemeBlue = Color(0xFF2563EB)
    val SoftGray = Color(0xFFF9FAFB)
    val TextMain = Color(0xFF111827)

    // Colors for variety (used for bars and icons)
    val AppColors = listOf(
        Color(0xFFF43F5E), // Rose
        Color(0xFF8B5CF6), // Violet
        Color(0xFF10B981), // Emerald
        Color(0xFFF59E0B), // Amber
        Color(0xFF06B6D4)  // Cyan
    )

    var dailyUsage by remember { mutableStateOf<List<AppUsage>>(emptyList()) }

    LaunchedEffect(Unit) {
        val raw = helper.getDailyAppUsage()
        val filtered = raw.filter { it.totalTimeInForeground >= 60000 }
        val total = filtered.sumOf { it.totalTimeInForeground }.toFloat().coerceAtLeast(1f)

        dailyUsage = filtered
            .sortedByDescending { it.totalTimeInForeground }
            .map { entity ->
                val realName = try {
                    val info = pm.getApplicationInfo(entity.packageName, 0)
                    pm.getApplicationLabel(info).toString()
                } catch (e: Exception) { entity.packageName }

                AppUsage(
                    packageName = entity.packageName,
                    appName = realName,
                    usageTimeString = helper.formatDuration(entity.totalTimeInForeground),
                    usagePercentage = entity.totalTimeInForeground / total,
                    lastUsedString = helper.getLastUsedString(entity.packageName)
                )
            }
    }

    val weeklyChartData by viewModel.weeklyChartData.collectAsStateWithLifecycle()

    // 1. SHOW ONLY 3 APPS
    val topApps = dailyUsage.take(3)
    val remainingCount = if (dailyUsage.size > 3) dailyUsage.size - 3 else 0

    Scaffold(
        containerColor = SoftGray,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Usage Statistics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = SoftGray)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
        ) {
            // Hero Card
            item {
                UsageHeroCard(
                    totalTime = helper.formatDuration(helper.getTotalScreenTimeToday()),
                    accentColor = AppThemeBlue
                )
            }

            // Chart
            item {
                Text("Weekly Activity", fontWeight = FontWeight.ExtraBold, color = TextMain)
                WeeklyBarChart(data = weeklyChartData, accentColor = AppThemeBlue)
            }

            // 2. NEW HEADER: "Daily Focus" instead of "Top Apps"
            item {
                Text("Daily Focus", fontWeight = FontWeight.ExtraBold, color = TextMain)
            }

            // 3. THE LIST (Limit 3)
            items(topApps.size) { index ->
                val app = topApps[index]
                UsageAppItem(
                    app = app,
                    accentColor = AppColors[index % AppColors.size], // Rotate colors
                    onClick = { navController.navigate("app_detail/${app.packageName}") }
                )
            }

            // 4. "SEE ALL" AT THE BOTTOM
            if (remainingCount > 0) {
                item {
                    OutlinedButton(
                        onClick = { navController.navigate("all_usage") },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Text("View all $remainingCount other apps", color = TextMain, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 5. ACTIVITY SNAPSHOT (Most used / Last Active)
            item {
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val mostUsed = dailyUsage.firstOrNull()

                    AnalyticsCard(
                        label = "Peak Usage",
                        appName = mostUsed?.appName ?: "None",
                        time = mostUsed?.usageTimeString ?: "0m",
                        icon = Icons.Default.TrendingUp,
                        color = Color(0xFF8B5CF6),
                        modifier = Modifier.weight(1f)
                    )
                    AnalyticsCard(
                        label = "Last Active",
                        appName = dailyUsage.firstOrNull()?.appName ?: "None",
                        time = dailyUsage.firstOrNull()?.lastUsedString ?: "N/A",
                        icon = Icons.Default.History,
                        color = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun UsageHeroCard(totalTime: String, accentColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = accentColor)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Screen Time Today", color = Color.White.copy(0.7f), style = MaterialTheme.typography.labelLarge)
            Text(totalTime, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = Color.White)
            Spacer(modifier = Modifier.height(12.dp))
            // 6. BETTER TEXT: "Live Usage Insights" instead of ">1min"
            Surface(color = Color.White.copy(0.15f), shape = CircleShape) {
                Text(
                    "Live Usage Insights",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun WeeklyBarChart(data: List<Float>, accentColor: Color) {
    // Generate 3-letter labels: Mon, Tue, Wed... based on rolling 7 days
    val labels = remember {
        val list = mutableListOf<String>()
        for (i in 6 downTo 0) {
            val d = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            list.add(SimpleDateFormat("EEE", Locale.getDefault()).format(d.time))
        }
        list
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = SurfaceWhite,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .height(180.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            // --- Y-AXIS (0h to 8h) ---
            Column(
                modifier = Modifier.fillMaxHeight().padding(end = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                // We show markings every 2 hours
                listOf("8h", "6h", "4h", "2h", "0h").forEach {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
                // Spacer to align with the X-axis labels below the bars
                Spacer(modifier = Modifier.height(20.dp))
            }

            // --- BARS & X-AXIS ---
            Row(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEachIndexed { index, value ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        // The Bar: Scaled against 8 hours max
                        // We use coerceIn(0.05f, 1f) so the bar is always visible even at 0h
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .fillMaxHeight(fraction = (value / 8f).coerceIn(0.05f, 1f))
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (index == data.lastIndex) accentColor
                                    else accentColor.copy(0.2f)
                                )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // X-AXIS LABEL (Mon, Tue, etc.)
                        Text(
                            text = labels[index],
                            style = MaterialTheme.typography.labelSmall,
                            color = if (index == data.lastIndex) TextMain else TextSecondary,
                            fontWeight = if (index == data.lastIndex) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsCard(label: String, appName: String, time: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Surface(
        modifier = modifier,
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(12.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
            Text(appName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(time, color = color, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun UsageAppItem(app: AppUsage, accentColor: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(accentColor.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Apps, null, tint = accentColor)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(app.appName, fontWeight = FontWeight.Bold, color = Color(0xFF1F2937))
                    Text(app.usageTimeString, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { app.usagePercentage },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = accentColor, // 7. INDIVIDUAL APP COLORS
                    trackColor = accentColor.copy(0.1f)
                )
            }
        }
    }
}