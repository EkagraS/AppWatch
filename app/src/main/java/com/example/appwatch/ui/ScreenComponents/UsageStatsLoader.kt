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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appwatch.ui.theme.*

@Composable
fun UsageStatsLoader(onGrantPermissionClick: () -> Unit) {
    var showExplainerDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState() // 👈 1. Fixing the "Kata hua" part

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .statusBarsPadding()// 👈 Scrolling enabled
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Screen usage", color = Color.Black, modifier = Modifier.fillMaxWidth().padding(top=16.dp), fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
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
                    Text("Today's Screen Time", color = TextSecondary, style = MaterialTheme.typography.labelLarge)
                    Text("0h 0m", color = TextDisabled, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black)
                }
            }

            // --- 3. Dummy Graph (With Locked Feel) ---
            Text("Weekly Activity", fontWeight = FontWeight.ExtraBold, color = TextPrimary)
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
                            Box(Modifier.width(22.dp).fillMaxHeight(0.2f).background(SurfaceVariantSoft, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)))
                        }
                    }
                    // Overlay icon to make it look locked
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Lock, null, tint = TextDisabled.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
                        Text("No Activity Data", color = TextDisabled, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // --- 4. Active/Inactive Summary Section ---
            Text("Day Summary", fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Active Card
                SummaryPlaceholderCard(
                    label = "Longest Active",
                    value = "--:--",
                    icon = Icons.Default.Timer,
                    color = Purple500,
                    modifier = Modifier.weight(1f)
                )
                // Inactive Card
                SummaryPlaceholderCard(
                    label = "Longest Break",
                    value = "--:--",
                    icon = Icons.Default.TimerOff,
                    color = Cyan500,
                    modifier = Modifier.weight(1f)
                )
            }

            // --- 5. Explanation and Button ---
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
                    Icon(Icons.Default.Security, contentDescription = null, tint = Indigo600, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Detailed Insights Locked",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        "Enable permission to unlock detailed data usage, app Notifications and total Screen unlocks. We only track timestamps and never read, store or use any of your private data. You can always change from settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )

                    Button(
                        onClick = { showExplainerDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(35.dp)
                    ) {
                        Text("Enable Permission", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp)) // Padding at bottom for scroll
        }

        // --- Scary Timer Dialog ---
        if (showExplainerDialog) {
            AlertDialog(
                onDismissRequest = { showExplainerDialog = false },
                icon = { Icon(Icons.Default.Security, null, tint = Indigo600) },
                title = { Text("Privacy Disclosure", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("To show your usage habits, AppWatch needs 'Usage Access'.", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Important: You might see a 10-second safety timer on your device settings. This is standard for security focused tools. We never read your private content.\n You can always remove this from the settings.",
                            style = MaterialTheme.typography.bodySmall, color = TextSecondary
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
                        Text("Proceed")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExplainerDialog = false }) {
                        Text("Later", color = TextSecondary)
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
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(value, fontWeight = FontWeight.Bold, color = TextDisabled)
        }
    }
}