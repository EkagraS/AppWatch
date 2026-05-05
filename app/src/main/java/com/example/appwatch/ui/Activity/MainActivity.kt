package com.example.appwatch.ui.Activity

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.appwatch.ui.screens.AllAppsStorageScreen
import com.example.appwatch.ui.screens.AllUsageScreen
import com.example.appwatch.ui.screens.AppDetailScreen
import com.example.appwatch.ui.screens.AppListScreen
import com.example.appwatch.ui.screens.AppsWithPermissionScreen
import com.example.appwatch.ui.screens.DashboardScreen
import com.example.appwatch.ui.screens.NeedsAttentionScreen
import com.example.appwatch.ui.screens.OnboardingScreen
import com.example.appwatch.ui.screens.PermissionScreen
import com.example.appwatch.ui.screens.RecentEventScreen
import com.example.appwatch.ui.screens.SettingsScreen
import com.example.appwatch.ui.screens.SplashScreen
import com.example.appwatch.ui.screens.StorageDetailScreen
import com.example.appwatch.ui.screens.UsageStatsScreen
import com.example.appwatch.ui.screens.today.AppDataUsageScreen
import com.example.appwatch.ui.screens.today.AppNotificationScreen
import com.example.appwatch.ui.theme.AppWatch2Theme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            delay(15000L) // 15 sec stability check
            val sharedPrefs = getSharedPreferences("app_watch_prefs", Context.MODE_PRIVATE)
            sharedPrefs.edit().putInt("continuous_crash_count", 0).apply()
        }
        setContent {
            AppWatch2Theme {
                val navController = rememberNavController()
                AppWatchNavigation(navController = navController)
            }
        }
    }
}

@Composable
fun AppWatchNavigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(onNavigateNext = { route ->
                navController.navigate(route) {
                    popUpTo("splash") { inclusive = true }
                }
            })
        }
        composable("onboarding") {
            OnboardingScreen(onFinish = {
                navController.navigate("dashboard") {
                    popUpTo("onboarding") { inclusive = true }
                }
            })
        }
        composable("dashboard") { DashboardScreen(navController) }
        composable("app_list") { AppListScreen(navController) }
        composable("usage_stats") { UsageStatsScreen(navController) }
        composable("app_detail/{packageName}") { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString("packageName")
            AppDetailScreen(navController, packageName)
        }
        composable("app_permissions/{packageName}") { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString("packageName")
            PermissionScreen(navController, packageName)
        }
        composable("apps_with_permission/{permissionType}") { backStackEntry ->
            val permissionType = backStackEntry.arguments?.getString("permissionType")
            AppsWithPermissionScreen(navController, permissionType)
        }
        composable("storage_detail") {
            StorageDetailScreen(navController)
        }
        composable("all_apps_storage") {
            AllAppsStorageScreen(navController)
        }
        composable("settings") {
            SettingsScreen(navController)
        }
        composable("all_usage/{dayIndex}") { backStackEntry ->
            val dayIndex = backStackEntry.arguments?.getString("dayIndex")?.toIntOrNull() ?: 6
            AllUsageScreen(navController, dayIndex)
        }
        composable("recentEventScreen/{eventType}"){ backStackEntry ->
            val type = backStackEntry.arguments?.getString("eventType") ?: "UNKNOWN"
            RecentEventScreen(eventType = type,
                navController = navController
            )
        }
        composable("needs_attention/{auditType}") {backStackEntry ->
            val type= backStackEntry.arguments?.getString("auditType") ?: "UNKNOWN"
            NeedsAttentionScreen(navController = navController, auditType = type)
        }
        composable(route = "notification_screen") {
            AppNotificationScreen(navController = navController)
        }
        composable(route = "data_usage"){
            AppDataUsageScreen(navController= navController)
        }
    }
}