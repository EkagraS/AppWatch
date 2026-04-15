package com.example.appwatch.ui.screens

import android.content.pm.PackageManager
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

// --- CLEAN & TRUSTWORTHY PALETTE ---
val AppBackground = Color(0xFFF9FAFB) // Very light gray/white
val SurfaceWhite = Color(0xFFFFFFFF)
val PrimaryBlue = Color(0xFF2563EB)   // Trustworthy Blue
val SuccessMint = Color(0xFF10B981)   // Soft Green
val TextMain = Color(0xFF111827)      // Deep charcoal
val TextSecondary = Color(0xFF6B7280) // Muted gray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageStatsScreen(
    navController: NavController,
    viewModel: UsageStatsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val pm = remember { context.packageManager }
    val helper = remember { UsageStatsHelper(context) }

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
    val topApps = dailyUsage.take(5)
    val remainingCount = if (dailyUsage.size > 5) dailyUsage.size - 5 else 0

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = { Text("Usage Statistics", fontWeight = FontWeight.Bold, color = TextMain) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextMain)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
        ) {
            // 1. CLEAN HERO CARD
            item {
                val totalTime = dailyUsage.sumOf {
                    // Manual parsing of your duration string for the UI total
                    0L // This should ideally come from a calculated field in your helper
                }
                // For now, let's use the helper's direct total for accuracy
                val actualTotal = helper.formatDuration(helper.getTotalScreenTimeToday())

                UsageHeroCard(totalTime = actualTotal)
            }

            // 2. CHART SECTION
            item {
                SectionHeader("Weekly Activity")
                WeeklyBarChart(data = weeklyChartData)
            }

            // 3. TOP USAGE LIST
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader("Top Apps Today")
                    if (remainingCount > 0) {
                        TextButton(onClick = { navController.navigate("all_usage") }) {
                            Text("See all ($remainingCount+)", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            items(topApps) { app ->
                AppUsageRow(app = app)
            }

            // 4. ANALYTICS GRID
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SmallStatCard(
                        label = "Most Used",
                        value = dailyUsage.firstOrNull()?.appName ?: "N/A",
                        icon = Icons.Default.TrendingUp,
                        modifier = Modifier.weight(1f)
                    )
                    SmallStatCard(
                        label = "Last Active",
                        value = dailyUsage.firstOrNull()?.lastUsedString ?: "N/A",
                        icon = Icons.Default.Update,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        color = TextMain
    )
}

@Composable
fun UsageHeroCard(totalTime: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryBlue),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Screen Time Today", color = Color.White.copy(0.8f), style = MaterialTheme.typography.labelLarge)
            Text(totalTime, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = Color.White)
            Spacer(Modifier.height(12.dp))
            Surface(
                color = Color.White.copy(0.2f),
                shape = CircleShape
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Tracking only apps > 1m", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun AppUsageRow(app: AppUsage) {
    Surface(
        color = SurfaceWhite,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(PrimaryBlue.copy(0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Apps, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(app.appName, color = TextMain, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(app.usageTimeString, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { app.usagePercentage },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = PrimaryBlue,
                    trackColor = PrimaryBlue.copy(0.1f)
                )
            }
        }
    }
}

@Composable
fun SmallStatCard(label: String, value: String, icon: ImageVector, modifier: Modifier) {
    Surface(
        modifier = modifier,
        color = SurfaceWhite,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = SuccessMint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(value, color = TextMain, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun WeeklyBarChart(data: List<Float>) {

    val max = data.maxOrNull()?.takeIf { it > 0 } ?: 1f

    Surface(
        color = SurfaceWhite,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEachIndexed { index, value ->
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .fillMaxHeight((value / max).coerceIn(0.1f, 1f)) // ✅ FIXED
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (index == data.size - 1)
                                PrimaryBlue
                            else
                                PrimaryBlue.copy(0.2f)
                        )
                )
            }
        }
    }
}