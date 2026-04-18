package com.example.appwatch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.appwatch.domain.model.AppUsage
import com.example.appwatch.presentation.viewmodel.UsageStatsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllUsageScreen(
    navController: NavController,
    dayIndex: Int,
    viewModel: UsageStatsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val pm = remember { context.packageManager }

    val AppColors = listOf(
        Color(0xFFF43F5E),
        Color(0xFF8B5CF6),
        Color(0xFF10B981),
        Color(0xFFF59E0B),
        Color(0xFF06B6D4)
    )

    var allApps by remember { mutableStateOf<List<AppUsage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val isToday = dayIndex == 6
    val dateLabel = remember {
        val daysAgo = 6 - dayIndex
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
        if (isToday) "Today"
        else SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(cal.time)
    }

    LaunchedEffect(dayIndex) {
        isLoading = true
        val daysAgo = 6 - dayIndex
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -daysAgo)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // Always from Room — no system calls
        val appsFromRoom = viewModel.getUsageForDay(cal.timeInMillis)
        allApps = appsFromRoom.map { app ->
            val realName = try {
                val info = pm.getApplicationInfo(app.packageName, 0)
                pm.getApplicationLabel(info).toString()
            } catch (e: Exception) { app.appName }
            app.copy(appName = realName)
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("All Apps", fontWeight = FontWeight.Bold)
                        Text(
                            dateLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF2563EB))
                }
            }
            allApps.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No usage data for $dateLabel", color = TextSecondary)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    item {
                        Text(
                            "${allApps.size} apps used",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    itemsIndexed(allApps) { index, app ->
                        UsageAppItem(
                            app = app,
                            accentColor = AppColors[index % AppColors.size],
                            onClick = { navController.navigate("app_detail/${app.packageName}") }
                        )
                    }
                }
            }
        }
    }
}