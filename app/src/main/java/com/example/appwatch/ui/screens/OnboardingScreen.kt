package com.example.appwatch.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appwatch.R
import com.example.appwatch.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 4 })

    var showTimerWarningDialog by remember { mutableStateOf<TimerWarningType?>(null) }
    var showSkippedDialog by remember { mutableStateOf<Int?>(null) } // Changed to Int for string resource ID

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
    }

    val pageColors = listOf(
        OnboardingPage1,
        OnboardingPage2,
        OnboardingPage3,
        OnboardingPage4
    )

    val currentColor by animateColorAsState(
        targetValue = pageColors[pagerState.currentPage],
        animationSpec = tween(400),
        label = "page_color"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        currentColor.copy(alpha = 0.12f),
                        BackgroundLight
                    )
                )
            )
    ) {
        Column(modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()) {

            // Skip button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (pagerState.currentPage < 3) {
                    TextButton(onClick = onFinish) {
                        Text(
                            stringResource(R.string.btn_skip),
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = false
            ) { page ->
                when (page) {
                    0 -> WelcomePage(accentColor = currentColor)
                    1 -> UsageAccessPage(accentColor = currentColor)
                    2 -> NotificationAccessPage(accentColor = currentColor)
                    3 -> MediaStoragePage(
                        accentColor = currentColor,
                        onGrant = {
                            val permissions = if (android.os.Build.VERSION.SDK_INT >=
                                android.os.Build.VERSION_CODES.TIRAMISU) {
                                arrayOf(
                                    android.Manifest.permission.READ_MEDIA_IMAGES,
                                    android.Manifest.permission.READ_MEDIA_VIDEO,
                                    android.Manifest.permission.READ_MEDIA_AUDIO
                                )
                            } else {
                                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                            mediaPermissionLauncher.launch(permissions)
                        },
                        onSkip = onFinish
                    )
                }
            }

            // Page indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { index ->
                    val isSelected = index == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isSelected) 24.dp else 8.dp, 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isSelected) currentColor
                                else currentColor.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            // Bottom button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                if (pagerState.currentPage < 3) {
                    val isPermissionPage = pagerState.currentPage == 1 ||
                            pagerState.currentPage == 2

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (isPermissionPage) {
                            Button(
                                onClick = {
                                    val type = if (pagerState.currentPage == 1)
                                        TimerWarningType.USAGE
                                    else
                                        TimerWarningType.NOTIFICATION
                                    showTimerWarningDialog = type
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = currentColor
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    stringResource(R.string.btn_grant_access),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    val stringResId = if (pagerState.currentPage == 1)
                                        R.string.perm_usage_access
                                    else
                                        R.string.perm_notification_access
                                    showSkippedDialog = stringResId
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = currentColor
                                )
                            ) {
                                Text(
                                    stringResource(R.string.btn_skip_for_now),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                            }
                        } else {
                            Button(
                                onClick = {
                                    scope.launch {
                                        pagerState.animateScrollToPage(
                                            pagerState.currentPage + 1
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = currentColor
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    stringResource(R.string.btn_next),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Timer warning dialog
    if (showTimerWarningDialog != null) {
        val isUsage = showTimerWarningDialog == TimerWarningType.USAGE
        AlertDialog(
            onDismissRequest = { showTimerWarningDialog = null },
            icon = {
                Icon(
                    Icons.Default.Info,
                    null,
                    tint = if (isUsage) OnboardingPage2 else OnboardingPage3
                )
            },
            title = {
                Text(stringResource(R.string.dialog_timer_title), fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    stringResource(R.string.dialog_timer_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showTimerWarningDialog = null
                        val intent = if (isUsage) {
                            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        } else {
                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        }
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isUsage) OnboardingPage2 else OnboardingPage3
                    )
                ) {
                    Text(stringResource(R.string.btn_got_it), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimerWarningDialog = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    // Skipped dialog
    if (showSkippedDialog != null) {
        AlertDialog(
            onDismissRequest = { showSkippedDialog = null },
            icon = {
                Icon(Icons.Default.CheckCircle, null, tint = Green500)
            },
            title = {
                Text(stringResource(R.string.dialog_skip_title), fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    stringResource(R.string.dialog_skip_desc, stringResource(showSkippedDialog!!)),
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSkippedDialog = null
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Green500)
                ) {
                    Text(stringResource(R.string.btn_okay), fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

enum class TimerWarningType { USAGE, NOTIFICATION }

// ─── Page 1 — Welcome ─────────────────────────────────────────────────────────

@Composable
fun WelcomePage(accentColor: Color) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Shield,
                null,
                tint = accentColor,
                modifier = Modifier.size(52.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            stringResource(R.string.app_name),
            fontSize = 36.sp,
            fontWeight = FontWeight.Black,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            stringResource(R.string.app_punch_line),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = accentColor,
            textAlign = TextAlign.Center,
            lineHeight = 26.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            stringResource(R.string.onboarding_page1),
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            OnboardingFeatureChip(Icons.Default.Lock, stringResource(R.string.feature_local), accentColor)
            OnboardingFeatureChip(Icons.Default.WifiOff, stringResource(R.string.feature_no_internet), accentColor)
            OnboardingFeatureChip(Icons.Default.Visibility, stringResource(R.string.feature_transparent), accentColor)
        }
    }
}

// ─── Page 2 — Usage Access ────────────────────────────────────────────────────

@Composable
fun UsageAccessPage(accentColor: Color) {
    OnboardingPermissionPage(
        accentColor = accentColor,
        icon = Icons.Default.QueryStats,
        title = stringResource(R.string.onboarding2_title),
        description = stringResource(R.string.onboarding2_description),
        whatWeUseItFor = listOf(
            stringResource(R.string.usage_feature_screen_time) to Icons.Default.BarChart,
            stringResource(R.string.usage_feature_unused) to Icons.Default.History,
            stringResource(R.string.usage_feature_launch_count) to Icons.Default.TrendingUp,
            stringResource(R.string.usage_feature_storage) to Icons.Default.Storage
        ),
        warningNote = stringResource(R.string.onboarding_error)
    )
}

// ─── Page 3 — Notification Access ────────────────────────────────────────────

@Composable
fun NotificationAccessPage(accentColor: Color) {
    OnboardingPermissionPage(
        accentColor = accentColor,
        icon = Icons.Default.Notifications,
        title = stringResource(R.string.onboarding3_title),
        description = stringResource(R.string.onboarding3_description),
        whatWeUseItFor = listOf(
            stringResource(R.string.notif_feature_total) to Icons.Default.NotificationsActive,
            stringResource(R.string.notif_feature_most) to Icons.Default.BarChart
        ),
        warningNote = stringResource(R.string.onboarding_error)
    )
}

// ─── Page 4 — Media Storage ───────────────────────────────────────────────────

@Composable
fun MediaStoragePage(
    accentColor: Color,
    onGrant: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(accentColor.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.PermMedia,
                null,
                tint = accentColor,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            color = accentColor.copy(alpha = 0.1f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                stringResource(R.string.label_optional),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                color = accentColor,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            stringResource(R.string.onboarding4_title),
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            stringResource(R.string.onboarding4_description),
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        listOf(
            Triple(Icons.Default.Photo, stringResource(R.string.onboarding_media_photos_videos), ColorCamera),
            Triple(Icons.Default.MusicNote, stringResource(R.string.onboarding_media_music), ColorMic),
            Triple(Icons.Default.Download, stringResource(R.string.onboarding_media_downloads), ColorLocation)
        ).forEach { (icon, label, color) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(color.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onGrant,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(stringResource(R.string.btn_grant_storage), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                stringResource(R.string.btn_skip_storage),
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ─── Reusable Permission Page ─────────────────────────────────────────────────

@Composable
fun OnboardingPermissionPage(
    accentColor: Color,
    icon: ImageVector,
    title: String,
    description: String,
    whatWeUseItFor: List<Pair<String, ImageVector>>,
    warningNote: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(accentColor.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                null,
                tint = accentColor,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            title,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            lineHeight = 32.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            description,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // What we use it for
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = accentColor.copy(alpha = 0.06f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.label_usage_purpose),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = accentColor
                )
                Spacer(modifier = Modifier.height(12.dp))
                whatWeUseItFor.forEach { (label, itemIcon) ->
                    Row(
                        modifier = Modifier.padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            itemIcon,
                            null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            label,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Warning note
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Amber50, RoundedCornerShape(10.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.Warning,
                null,
                tint = Amber500,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                warningNote,
                style = MaterialTheme.typography.bodySmall,
                color = Amber600,
                lineHeight = 18.sp
            )
        }
    }
}

// ─── Feature Chip ─────────────────────────────────────────────────────────────

@Composable
fun OnboardingFeatureChip(icon: ImageVector, label: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(color.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}