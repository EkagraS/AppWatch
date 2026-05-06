package com.ekagra.privascope.ui.screens

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import com.ekagra.privascope.domain.model.AppUsage
import com.ekagra.privascope.ui.viewModels.UsageStatsViewModel
import com.ekagra.privascope.ui.ScreenComponents.KnowYourUsageSection
import com.ekagra.privascope.ui.ScreenComponents.UsageStatsLoader
import com.ekagra.privascope.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import com.ekagra.privascope.R

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

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        rememberTopAppBarState()
    )

    val AppThemePrimary = Blue600
    val AppBackground = BackgroundLight
    val AppColors = listOf(ChartBar1, ChartBar2, ChartBar3, ChartBar4, ChartBar5)

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

    if (!isPermissionGranted) {
        UsageStatsLoader(onGrantPermissionClick = {
            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        })
    } else {
        if (viewModel.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppThemePrimary)
            }
        } else {
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                containerColor = AppBackground,
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                stringResource(R.string.usage_stats_title),
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.cd_back),
                                    tint = TextPrimary
                                )
                            }
                        },
                        scrollBehavior = scrollBehavior,
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = BackgroundLight,
                            scrolledContainerColor = Teal50,
                            titleContentColor = TextPrimary,
                            navigationIconContentColor = TextPrimary,
                            actionIconContentColor = TextPrimary
                        )
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
                    // Section 1: Screen Time Today
                    item {
                        UsageHeroCard(
                            totalTime = selectedTotalTime,
                            dateLabel = if (isToday) stringResource(R.string.usage_today) else selectedDayLabel?.second ?: "",
                            accentColor = AppThemePrimary,
                            isToday = isToday
                        )
                    }

                    // Section 2: Weekly Activity (Graph)
                    item {
                        Text(
                            stringResource(R.string.usage_header_weekly),
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

                    // Section 3: Today/Selected Graph Usage (App List + Peak + Total Apps)
                    item {
                        val headerTitle = if (isToday) {
                            stringResource(R.string.usage_header_today)
                        } else {
                            stringResource(R.string.usage_header_focus_suffix, selectedDayLabel?.second ?: "")
                        }
                        SectionHeader(
                            title = headerTitle,
                            subtitle = stringResource(R.string.usage_val_apps_used_count, selectedDayUsage.size)
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

                    // Reactive Analytics (Jo graph ke sath badlenge)
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val mostUsed = selectedDayUsage.firstOrNull()
                            AnalyticsCard(
                                label = stringResource(R.string.usage_label_peak),
                                appName = mostUsed?.appName ?: stringResource(R.string.usage_val_none),
                                time = mostUsed?.usageTimeString ?: stringResource(R.string.usage_val_zero_m),
                                icon = Icons.Default.TrendingUp,
                                color = StatScreen,
                                modifier = Modifier.weight(1f)
                            )
                            AnalyticsCard(
                                label = stringResource(R.string.usage_label_total_apps),
                                appName = stringResource(R.string.usage_val_apps_caps_count, selectedDayUsage.size),
                                time = "", // List size se reactive hai
                                icon = Icons.Default.Apps,
                                color = StatApps,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        // Includes active/inactive sessions
                        KnowYourUsageSection(viewModel)
                    }
                }
            }
        }
    }
}

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
fun EmptyStateMessage() {
    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.usage_empty_recorded), color = TextSecondary)
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
        Text(stringResource(R.string.btn_view_more_apps, count), color = TextPrimary, fontWeight = FontWeight.Bold)
    }
}

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
            val label = if (isToday) stringResource(R.string.usage_hero_today) else stringResource(R.string.usage_hero_date_prefix, dateLabel)
            Text(label, color = TextOnDark.copy(0.75f), style = MaterialTheme.typography.labelLarge)
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