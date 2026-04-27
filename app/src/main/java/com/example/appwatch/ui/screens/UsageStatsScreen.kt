package com.example.appwatch.ui.screens

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.appwatch.domain.model.AppUsage
import com.example.appwatch.presentation.viewmodel.UsageStatsViewModel
import com.example.appwatch.ui.ScreenComponents.KnowYourUsageSection
import com.example.appwatch.ui.ScreenComponents.UsageStatsLoader
import com.example.appwatch.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// Permission Check Helper
private fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.noteOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageStatsScreen(
    navController: NavController,
    viewModel: UsageStatsViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // ─── Colors (Indigo Replaced with Blue/Purple) ──────────────────────────
    val AppThemePrimary = Blue600 // Tunhe Indigo mana kiya tha, toh Blue600 as Primary
    val AppBackground = BackgroundLight
    val AppColors = listOf(ChartBar1, ChartBar2, ChartBar3, ChartBar4, ChartBar5)

    // ─── Permission State ─────────────────────────────────────────────────────
    var isPermissionGranted by remember { mutableStateOf(hasUsageStatsPermission(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isPermissionGranted = hasUsageStatsPermission(context)
                if (isPermissionGranted) viewModel.refreshAllData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ─── Data States from ViewModel ───────────────────────────────────────────
    var selectedDayIndex by remember { mutableStateOf(6) }
    val allDaysUsage by viewModel.allDaysUsage.collectAsStateWithLifecycle()
    val weeklyChartData by viewModel.weeklyChartData.collectAsStateWithLifecycle()

    val dayLabels = remember {
        (0..6).map { i ->
            val daysAgo = 6 - i
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
            Triple(
                SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time),
                SimpleDateFormat("d MMM", Locale.getDefault()).format(cal.time),
                cal.timeInMillis
            )
        }
    }

    val selectedDayUsage = remember(selectedDayIndex, allDaysUsage) {
        allDaysUsage[selectedDayIndex] ?: emptyList()
    }

    val isToday = selectedDayIndex == 6
    val selectedDayLabel = dayLabels.getOrNull(selectedDayIndex)

    val selectedTotalTime = remember(selectedDayIndex, weeklyChartData) {
        val hours = weeklyChartData.getOrNull(selectedDayIndex) ?: 0f
        val millis = (hours * 3600000).toLong()
        val h = millis / (1000 * 60 * 60)
        val m = (millis / (1000 * 60)) % 60
        if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    // ─── UI ───────────────────────────────────────────────────────────────────
    if (!isPermissionGranted) {
        UsageStatsLoader(onGrantPermissionClick = {
            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        })
    } else{
        if (viewModel.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppThemePrimary) // Sirf ek hi loader!
            }
        }else {
            Scaffold(
                containerColor = AppBackground,
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                "Usage Statistics",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = TextPrimary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = AppBackground)
                    )
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
                ) {
                    // 1. Hero Card
                    item {
                        UsageHeroCard(
                            totalTime = selectedTotalTime,
                            dateLabel = if (isToday) "Today" else selectedDayLabel?.second ?: "",
                            accentColor = AppThemePrimary,
                            isToday = isToday
                        )
                    }

                    // 2. Weekly Chart
                    item {
                        Text(
                            "Weekly Activity",
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        InteractiveWeeklyChart(
                            data = weeklyChartData,
                            dayLabels = dayLabels.map { it.first },
                            selectedIndex = selectedDayIndex,
                            accentColor = AppThemePrimary,
                            onDaySelected = { selectedDayIndex = it }
                        )
                    }

                    // 3. Top Apps Section
                    item {
                        SectionHeader(
                            title = if (isToday) "Today's Usage" else "${selectedDayLabel?.second ?: ""} Focus",
                            subtitle = "${selectedDayUsage.size} apps"
                        )
                    }

                    if (selectedDayUsage.isEmpty()) {
                        item { EmptyStateMessage() }
                    } else {
                        items(selectedDayUsage.take(3)) { app ->
                            UsageAppItem(
                                app = app,
                                accentColor = AppColors[selectedDayUsage.indexOf(app) % AppColors.size],
                                onClick = { navController.navigate("app_detail/${app.packageName}") }
                            )
                        }
                        if (selectedDayUsage.size > 3) {
                            item {
                                ViewMoreButton(count = selectedDayUsage.size - 3) {
                                    navController.navigate("all_usage/$selectedDayIndex")
                                }
                            }
                        }
                    }

                    // 4. Analytics Section (Marathon & Unlock Pace)
                    item {
                        Text(
                            "This day",
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val mostUsed = selectedDayUsage.firstOrNull()
                            AnalyticsCard(
                                label = "Peak Usage",
                                appName = mostUsed?.appName ?: "None",
                                time = mostUsed?.usageTimeString ?: "0m",
                                icon = Icons.Default.TrendingUp,
                                color = StatScreen,
                                modifier = Modifier.weight(1f)
                            )
                            AnalyticsCard(
                                label = "Unlock Pace",
                                appName = "${String.format("%.1f", viewModel.unlockPace)} device unlocks",
                                time = " per hour usage",
                                icon = Icons.Default.LockOpen,
                                color = StatUnlocks,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AnalyticsCard(
                                label = "Continuous app usage",
                                appName = viewModel.marathonAppName,
                                time = viewModel.marathonTime,
                                icon = Icons.Default.Timer,
                                color = StatData,
                                modifier = Modifier.weight(1f)
                            )
                            AnalyticsCard(
                                label = "Total Apps used",
                                appName = "${selectedDayUsage.size} Apps",
                                time = "",
                                icon = Icons.Default.Apps,
                                color = StatApps,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // 5. Usage Insights (Weekly/Monthly Modern Cards)
                    item {
                        KnowYourUsageSection(viewModel)
                    }
                }
            }
        }
    }
}

// ─── Sub-Components (Clean & Modern) ──────────────────────────────────────────

@Composable
fun SectionHeader(title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontWeight = FontWeight.ExtraBold, color = TextPrimary, fontSize = 18.sp)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

@Composable
fun AppIconSmall(packageName: String) {
    val context = LocalContext.current
    val icon = remember(packageName) {
        try { context.packageManager.getApplicationIcon(packageName) }
        catch (e: Exception) { null }
    }
    if (icon != null) {
        AsyncImage(
            model = icon,
            contentDescription = null,
            modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp))
        )
    } else {
        Box(modifier = Modifier.size(28.dp).background(DividerColor, CircleShape))
    }
}

@Composable
fun EmptyStateMessage() {
    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
        Text("No usage recorded for this day", color = TextSecondary)
    }
}

@Composable
fun ViewMoreButton(count: Int, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderSoft)
    ) {
        Text("View all $count more apps", color = TextPrimary, fontWeight = FontWeight.Bold)
    }
}

// ─── Rest of the existing components (Updated Colors) ───────────────────────

@Composable
fun AnalyticsCard(label: String, appName: String, time: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Surface(modifier = modifier, color = SurfaceWhite, shape = RoundedCornerShape(20.dp), shadowElevation = 1.dp) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(12.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontWeight = FontWeight.Bold)
            Text(appName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = TextPrimary)
            Text(time, color = color, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun UsageHeroCard(totalTime: String, dateLabel: String, accentColor: Color, isToday: Boolean) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = accentColor)) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(if (isToday) "Screen Time Today" else "Screen Time — $dateLabel", color = TextOnDark.copy(0.75f), style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(totalTime, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = TextOnDark)
        }
    }
}

@Composable
fun UsageAppItem(app: AppUsage, accentColor: Color, onClick: () -> Unit) {
    val context = LocalContext.current
    val icon = remember(app.packageName) {
        try { context.packageManager.getApplicationIcon(app.packageName) }
        catch (e: Exception) { null }
    }
    Surface(onClick = onClick, color = SurfaceWhite, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), shadowElevation = 1.dp) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                AsyncImage(model = icon, contentDescription = null, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(app.appName, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f), maxLines = 1)
                    Text(app.usageTimeString, fontWeight = FontWeight.Bold, color = accentColor)
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { app.usagePercentage }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = accentColor, trackColor = accentColor.copy(0.1f))
            }
        }
    }
}

@Composable
fun InteractiveWeeklyChart(data: List<Float>, dayLabels: List<String>, selectedIndex: Int, accentColor: Color, onDaySelected: (Int) -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = SurfaceWhite, shadowElevation = 2.dp) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.height(160.dp).fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Row(modifier = Modifier.weight(1f).fillMaxHeight(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    data.forEachIndexed { index, value ->
                        val isSelected = index == selectedIndex
                        val animatedFraction by animateFloatAsState(targetValue = (value / 8f).coerceIn(0.05f, 1f), label = "")
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onDaySelected(index) }.padding(horizontal = 2.dp)) {
                            Box(modifier = Modifier.width(24.dp).fillMaxHeight(fraction = animatedFraction).clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)).background(if (isSelected) accentColor else accentColor.copy(0.2f)))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(dayLabels[index], fontSize = 10.sp, color = if (isSelected) accentColor else TextSecondary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }
}