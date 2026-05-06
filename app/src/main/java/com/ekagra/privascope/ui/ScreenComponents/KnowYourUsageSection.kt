package com.ekagra.privascope.ui.ScreenComponents

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekagra.privascope.domain.model.AppUsage
import com.ekagra.privascope.ui.viewModels.UsageStatsViewModel
import com.ekagra.privascope.ui.screens.AppIconSmall
import com.ekagra.privascope.ui.theme.*
import com.ekagra.privascope.R

@Composable
fun KnowYourUsageSection(viewModel: UsageStatsViewModel) {
    val activeRange by viewModel.todayActiveStreak.collectAsStateWithLifecycle()
    val inactiveRange by viewModel.todayInactiveStreak.collectAsStateWithLifecycle()
    val highNoiseApps by viewModel.highNoiseApps.collectAsStateWithLifecycle()

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = "Today's Summary",
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 16.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // First Row: Active & Inactive
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryItemCard(
                title = stringResource(R.string.usage_streak_active),
                value = activeRange,
                color = Color(0xFFFFA000),
                icon = Icons.Default.Smartphone,
                modifier = Modifier.weight(1f)
            )
            SummaryItemCard(
                title = stringResource(R.string.usage_streak_inactive),
                value = inactiveRange,
                color = Green600,
                icon = Icons.Default.PauseCircle,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))

        // Second Row: Unlock Pace & Marathon
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryItemCard(
                title = stringResource(R.string.usage_label_unlock_pace),
                value = stringResource(R.string.usage_val_unlock_pace_desc, viewModel.unlockPace),
                subText = stringResource(R.string.usage_val_unlock_pace_time),
                color = StatUnlocks,
                icon = Icons.Default.LockOpen,
                modifier = Modifier.weight(1f)
            )
            SummaryItemCard(
                title = stringResource(R.string.usage_label_marathon),
                value = viewModel.marathonAppName,
                subText = viewModel.marathonTime,
                color = StatData,
                icon = Icons.Default.Timer,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(24.dp))

        // Noise Analysis Section
        Text(
            text = stringResource(R.string.usage_noise_header),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Red50),
            border = BorderStroke(1.dp, Red100)
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                if (highNoiseApps.isEmpty()) {
                    Text(
                        text = stringResource(R.string.usage_noise_empty),
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 12.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    highNoiseApps.forEach { app ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = app.appName,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(
                                    R.string.usage_noise_template,
                                    app.notificationCount,
                                    app.appUnlocks
                                ),
                                fontSize = 13.sp,
                                color = Red600,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        PremiumInsightsSection(viewModel)
    }
}

@Composable
fun SummaryItemCard(
    title: String,
    value: String,
    subText: String? = null,
    color: Color,
    icon: ImageVector,
    modifier: Modifier
) {
    Surface(
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.15f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
            Text(
                text = value,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subText != null) {
                Text(
                    text = subText,
                    fontSize = 12.sp,
                    color = color,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}

@Composable
fun PremiumInsightsSection(viewModel: UsageStatsViewModel) {
    val weeklyTop by viewModel.top3AppsWeekly.collectAsStateWithLifecycle()
    val monthlyTop by viewModel.top3AppsMonthly.collectAsStateWithLifecycle()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.usage_insights_header),
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            fontSize = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        InsightModernCard(
            title = stringResource(R.string.usage_insight_weekly),
            apps = weeklyTop,
            backgroundColor = Teal50,
            accentColor = Teal600
        )

        InsightModernCard(
            title = stringResource(R.string.usage_insight_monthly),
            apps = monthlyTop,
            backgroundColor = Purple50,
            accentColor = Purple600
        )
    }
}

@Composable
fun InsightModernCard(
    title: String,
    apps: List<AppUsage>,
    backgroundColor: Color,
    accentColor: Color
) {
    var isExpanded by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "rotation")

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.cd_expand),
                    tint = accentColor,
                    modifier = Modifier.rotate(rotationState)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    if (apps.isEmpty()) {
                        Text(
                            text = stringResource(R.string.usage_no_data),
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        apps.forEach { app ->
                            Row(
                                modifier = Modifier.padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppIconSmall(app.packageName)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = app.appName,
                                    modifier = Modifier.weight(1f),
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = TextPrimary
                                )
                                Text(
                                    text = app.usageTimeString,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}