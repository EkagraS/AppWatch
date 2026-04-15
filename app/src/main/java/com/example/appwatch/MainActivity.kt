package com.example.appwatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.appwatch.ui.screens.AllAppsStorageScreen
import com.example.appwatch.ui.screens.AppDetailScreen
import com.example.appwatch.ui.screens.AppListScreen
import com.example.appwatch.ui.screens.AppsWithPermissionScreen
import com.example.appwatch.ui.screens.DashboardScreen
import com.example.appwatch.ui.screens.OnboardingScreen
import com.example.appwatch.ui.screens.PermissionAuditScreen
import com.example.appwatch.ui.screens.PermissionScreen
import com.example.appwatch.ui.screens.SplashScreen
import com.example.appwatch.ui.screens.StorageDetailScreen
import com.example.appwatch.ui.screens.UsageStatsScreen
import com.example.appwatch.ui.theme.AppWatch2Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
        composable("permission_audit") { PermissionAuditScreen(navController) }
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
        composable("storage_detail") {
            StorageDetailScreen(navController)
        }
        composable("all_apps_storage") {
            AllAppsStorageScreen(navController)
        }
    }
}