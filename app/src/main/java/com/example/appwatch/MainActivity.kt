package com.example.appwatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.appwatch.ui.screens.AppDetailScreen
import com.example.appwatch.ui.screens.AppListScreen
import com.example.appwatch.ui.screens.DashboardScreen
import com.example.appwatch.ui.screens.OnboardingScreen
import com.example.appwatch.ui.screens.PermissionAuditScreen
import com.example.appwatch.ui.screens.SplashScreen
import com.example.appwatch.ui.screens.UsageStatsScreen
import com.example.appwatch.ui.theme.AppWatch2Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppWatch2Theme { // Ensure theme is applied
                val navController = rememberNavController()
                AppWatchNavigation(navController = navController)
            }
        }
    }
}

@Composable
fun AppWatchNavigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "splash") { composable("splash") {SplashScreen(onNavigateNext = { route ->navController.navigate(route) {
        popUpTo("splash") { inclusive = true } } })
    }
    composable("onboarding") { OnboardingScreen(onFinish = {navController.navigate("dashboard") {
        popUpTo("onboarding") { inclusive = true } } })
    }
    composable("dashboard") { DashboardScreen(navController) }
    composable("app_list") { AppListScreen(navController) }
    composable("permission_audit") { PermissionAuditScreen(navController) }
    composable("usage_stats") { UsageStatsScreen(navController) }
    composable("app_detail/{packageName}") { backStackEntry ->
        val packageName = backStackEntry.arguments?.getString("packageName")
        AppDetailScreen(navController, packageName)
    }
    }
}