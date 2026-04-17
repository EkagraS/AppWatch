package com.example.appwatch.ui.screens

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import coil.compose.AsyncImage
import com.example.appwatch.domain.model.AppUsage
import com.example.appwatch.presentation.viewmodel.UsageStatsViewModel
import com.example.appwatch.system.UsageStatsHelper
import java.text.SimpleDateFormat
import java.util.*

val TextMain = Color(0xFF111827)
val TextSecondary = Color(0xFF6B7280)
val SurfaceWhite = Color(0xFFFFFFFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageStatsScreen(
    navController: NavController,
    viewModel: UsageStatsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val pm = remember { context.packageManager }
    val helper = remember { UsageStatsHelper(context) }

    val AppThemeBlue = Color(0xFF2563EB)
    val SoftGray = Color(0xFFF9FAFB)

    val AppColors = listOf(
        Color(0xFFF43F5E),
        Color(0xFF8B5CF6),
        Color(0xFF10B981),
        Color(0xFFF59E0B),
        Color(0xFF06B6D4)
    )

    // Selected day index — 6 = today (rightmost bar)
    var selectedDayIndex by remember { mutableStateOf(6) }

    // All 7 days usage data
    var allDaysUsage by remember { mutableStateOf<Map<Int, List<AppUsage>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    val weeklyChartData by viewModel.weeklyChartData.collectAsStateWithLifecycle()

    // Day labels for last 7 days
    val dayLabels = remember {
        (6 downTo 0).map { i ->
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
            Triple(
                SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time),  // Mon
                SimpleDateFormat("d MMM", Locale.getDefault()).format(cal.time), // 14 Apr
                cal.timeInMillis
            )
        }
    }

    // Load usage for all 7 days
    LaunchedEffect(Unit) {
        isLoading = true
        val result = mutableMapOf<Int, List<AppUsage>>()

        for (i in 0..6) {
            val dayIndex = i
            val daysAgo = 6 - i

            val raw = if (daysAgo == 0) {
                // Today — live from system
                helper.getDailyAppUsage()
            } else {
                // Past days — from Room via DAO
                val cal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -daysAgo)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                viewModel.getUsageForDay(cal.timeInMillis)
            }

            val filtered = raw.filter { it.totalTimeInForeground >= 60000 }
            val total = filtered.sumOf { it.totalTimeInForeground }.toFloat().coerceAtLeast(1f)

            result[dayIndex] = filtered
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

        allDaysUsage = result
        isLoading = false
    }

    val selectedDayUsage = allDaysUsage[selectedDayIndex] ?: emptyList()
    val selectedDayLabel = dayLabels.getOrNull(selectedDayIndex)
    val isToday = selectedDayIndex == 6

    val selectedTotalTime = remember(selectedDayIndex, allDaysUsage) {
        val apps = allDaysUsage[selectedDayIndex] ?: emptyList()
        val totalMs = apps.sumOf { entity ->
            // Convert back from string — use raw from helper instead
            0L
        }
        if (isToday) {
            helper.formatDuration(helper.getTotalScreenTimeToday())
        } else {
            val totalFromApps = weeklyChartData.getOrNull(selectedDayIndex)?.let {
                it * 60 * 60 * 1000
            }?.toLong() ?: 0L
            helper.formatDuration(totalFromApps)
        }
    }

    val topApps = selectedDayUsage.take(3)
    val remainingCount = (selectedDayUsage.size - 3).coerceAtLeast(0)

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
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AppThemeBlue)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Loading usage data...",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
            ) {
                // 1. Hero Card — updates based on selected day
                item {
                    UsageHeroCard(
                        totalTime = selectedTotalTime,
                        dateLabel = if (isToday) "Today" else selectedDayLabel?.second ?: "",
                        accentColor = AppThemeBlue,
                        isToday = isToday
                    )
                }

                // 2. Interactive 7-day chart
                item {
                    Text(
                        "Weekly Activity",
                        fontWeight = FontWeight.ExtraBold,
                        color = TextMain
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    InteractiveWeeklyChart(
                        data = weeklyChartData,
                        dayLabels = dayLabels.map { it.first },
                        selectedIndex = selectedDayIndex,
                        accentColor = AppThemeBlue,
                        onDaySelected = { index -> selectedDayIndex = index }
                    )
                }

                // 3. Selected day header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (isToday) "Today's Focus" else "${selectedDayLabel?.second ?: ""} Focus",
                            fontWeight = FontWeight.ExtraBold,
                            color = TextMain
                        )
                        if (selectedDayUsage.isEmpty()) {
                            Text(
                                "No data",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        } else {
                            Text(
                                "${selectedDayUsage.size} apps",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                // 4. App list for selected day
                if (selectedDayUsage.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No usage recorded for this day",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    items(topApps) { app ->
                        val colorIndex = topApps.indexOf(app)
                        UsageAppItem(
                            app = app,
                            accentColor = AppColors[colorIndex % AppColors.size],
                            onClick = { navController.navigate("app_detail/${app.packageName}") }
                        )
                    }

                    if (remainingCount > 0) {
                        item {
                            OutlinedButton(
                                onClick = {
                                    navController.navigate("all_usage/${selectedDayIndex}")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color.LightGray)
                            ) {
                                Text(
                                    "View all $remainingCount more apps",
                                    color = TextMain,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 5. Analytics snapshot
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val mostUsed = selectedDayUsage.firstOrNull()
                        val leastUsed = selectedDayUsage.lastOrNull()

                        AnalyticsCard(
                            label = "Peak Usage",
                            appName = mostUsed?.appName ?: "None",
                            time = mostUsed?.usageTimeString ?: "0m",
                            icon = Icons.Default.TrendingUp,
                            color = Color(0xFF8B5CF6),
                            modifier = Modifier.weight(1f)
                        )
                        AnalyticsCard(
                            label = "Apps Used",
                            appName = "${selectedDayUsage.size} apps",
                            time = selectedTotalTime,
                            icon = Icons.Default.Apps,
                            color = Color(0xFFF59E0B),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

// ─── Interactive Chart ────────────────────────────────────────────────────────

@Composable
fun InteractiveWeeklyChart(
    data: List<Float>,
    dayLabels: List<String>,
    selectedIndex: Int,
    accentColor: Color,
    onDaySelected: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = SurfaceWhite,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier
                    .height(160.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                // Y-axis labels
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    listOf("8h", "6h", "4h", "2h", "0h").forEach {
                        Text(
                            it,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Bars
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    data.forEachIndexed { index, value ->
                        val isSelected = index == selectedIndex
                        val animatedFraction by animateFloatAsState(
                            targetValue = (value / 8f).coerceIn(0.05f, 1f),
                            animationSpec = tween(400),
                            label = "bar_$index"
                        )
                        val barColor by animateColorAsState(
                            targetValue = if (isSelected) accentColor else accentColor.copy(0.25f),
                            animationSpec = tween(300),
                            label = "bar_color_$index"
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier
                                .fillMaxHeight()
                                .clickable { onDaySelected(index) }
                                .padding(horizontal = 2.dp)
                        ) {
                            // Value label above bar when selected
                            if (isSelected && value > 0) {
                                Text(
                                    "${value.toInt()}h",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accentColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            } else {
                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            Box(
                                modifier = Modifier
                                    .width(22.dp)
                                    .fillMaxHeight(fraction = animatedFraction)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(barColor)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = dayLabels.getOrElse(index) { "" },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) accentColor else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 10.sp
                            )
                            // Selected indicator dot
                            if (isSelected) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(accentColor, CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Hero Card ────────────────────────────────────────────────────────────────

@Composable
fun UsageHeroCard(
    totalTime: String,
    dateLabel: String,
    accentColor: Color,
    isToday: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = accentColor)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                if (isToday) "Screen Time Today" else "Screen Time — $dateLabel",
                color = Color.White.copy(0.75f),
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                totalTime,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = Color.White.copy(0.15f),
                shape = CircleShape
            ) {
                Text(
                    if (isToday) "Live · Tap a bar to explore" else "Tap today's bar for live data",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

// ─── App Item with real icon ──────────────────────────────────────────────────

@Composable
fun UsageAppItem(app: AppUsage, accentColor: Color, onClick: () -> Unit) {
    val context = LocalContext.current
    val icon = remember(app.packageName) {
        try {
            context.packageManager.getApplicationIcon(app.packageName)
        } catch (e: Exception) { null }
    }

    Surface(
        onClick = onClick,
        color = SurfaceWhite,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Real app icon
            if (icon != null) {
                AsyncImage(
                    model = icon,
                    contentDescription = app.appName,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(accentColor.copy(0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Apps, null, tint = accentColor)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        app.appName,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        app.usageTimeString,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { app.usagePercentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = accentColor,
                    trackColor = accentColor.copy(0.1f)
                )
            }
        }
    }
}

// ─── Analytics Card ───────────────────────────────────────────────────────────

@Composable
fun AnalyticsCard(
    label: String,
    appName: String,
    time: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier
) {
    Surface(
        modifier = modifier,
        color = SurfaceWhite,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )
            Text(
                appName,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                time,
                color = color,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}