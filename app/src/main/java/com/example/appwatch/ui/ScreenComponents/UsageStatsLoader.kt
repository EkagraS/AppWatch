package com.example.appwatch.ui.ScreenComponents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appwatch.R
import com.example.appwatch.ui.theme.*

@Composable
fun UsageStatsLoader(onGrantPermissionClick: () -> Unit) {
    var showExplainerDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .statusBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.loader_usage_title),
                color = Color.Black,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.loader_label_today_time),
                        color = TextSecondary,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                    Text(
                        text = stringResource(R.string.loader_val_zero_time),
                        color = TextDisabled,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }

            Text(
                text = stringResource(R.string.loader_header_weekly),
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Card(
                modifier = Modifier.fillMaxWidth().height(180.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = RoundedCornerShape(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        repeat(7) {
                            Box(
                                Modifier
                                    .width(22.dp)
                                    .fillMaxHeight(0.2f)
                                    .background(
                                        SurfaceVariantSoft,
                                        RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                    )
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Lock,
                            null,
                            tint = TextDisabled.copy(alpha = 0.5f),
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = stringResource(R.string.loader_status_no_data),
                            color = TextDisabled,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.loader_header_day_summary),
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryPlaceholderCard(
                    label = stringResource(R.string.loader_label_longest_active),
                    value = stringResource(R.string.loader_val_placeholder_time),
                    icon = Icons.Default.Timer,
                    color = Purple500,
                    modifier = Modifier.weight(1f)
                )
                SummaryPlaceholderCard(
                    label = stringResource(R.string.loader_label_longest_break),
                    value = stringResource(R.string.loader_val_placeholder_time),
                    icon = Icons.Default.TimerOff,
                    color = Cyan500,
                    modifier = Modifier.weight(1f)
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Indigo50),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Indigo100)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = Indigo600,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.loader_card_title_locked),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.loader_card_desc_locked),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    Button(
                        onClick = { showExplainerDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp) // Height standardized
                    ) {
                        Text(
                            text = stringResource(R.string.btn_enable_permission),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        if (showExplainerDialog) {
            AlertDialog(
                onDismissRequest = { showExplainerDialog = false },
                icon = { Icon(Icons.Default.Security, null, tint = Indigo600) },
                title = {
                    Text(
                        text = stringResource(R.string.dialog_privacy_title),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                text = {
                    Column {
                        Text(
                            text = stringResource(R.string.dialog_privacy_usage_access_msg),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.dialog_privacy_timer_msg),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showExplainerDialog = false
                            onGrantPermissionClick()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                    ) {
                        Text(stringResource(R.string.btn_proceed))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExplainerDialog = false }) {
                        Text(stringResource(R.string.btn_later), color = TextSecondary)
                    }
                },
                containerColor = SurfaceWhite,
                shape = RoundedCornerShape(28.dp)
            )
        }
    }
}

@Composable
fun SummaryPlaceholderCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
            Icon(icon, null, tint = color.copy(alpha = 0.5f), modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                color = TextDisabled,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}