package com.example.appwatch.ui.ScreenComponents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Security
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
fun NotificationLoader(onGrantPermissionClick: () -> Unit) {
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
                .padding(16.dp)
        ) {
            Text("Notification details", color = Color.Black, modifier = Modifier.fillMaxWidth().padding(top=16.dp), fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(20.dp))
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
                    Text("Today's Notifications", color = TextSecondary, style = MaterialTheme.typography.labelLarge)
                    Text("0", color = TextDisabled, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Placeholder List (Ghost Items) ---
            Text("Recent Alerts", style = MaterialTheme.typography.titleSmall, color = TextPrimary, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))

            repeat(3) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    color = SurfaceWhite.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(32.dp).background(SurfaceVariantSoft, RoundedCornerShape(8.dp)))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Box(Modifier.size(width = 120.dp, height = 10.dp).background(SurfaceVariantSoft, RoundedCornerShape(2.dp)))
                            Spacer(Modifier.height(6.dp))
                            Box(Modifier.size(width = 80.dp, height = 8.dp).background(SurfaceVariantSoft, RoundedCornerShape(2.dp)))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- CTA Card (Orange Theme for Notifications) ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Orange50),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Orange100)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.NotificationsOff, contentDescription = null, tint = Orange600, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Audit Notifications",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        "See which apps send the most alerts and audit sensitive message access. We never read, store or use any of your private data. You can always change from settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )

                    Button(
                        onClick = { showExplainerDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Orange600),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Grant Access", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- Notification Access Explainer ---
        if (showExplainerDialog) {
            AlertDialog(
                onDismissRequest = { showExplainerDialog = false },
                icon = { Icon(Icons.Default.Security, contentDescription = null, tint = Orange600) },
                title = { Text("Privacy Disclosure", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            "To audit which apps are reading your notifications (like potential spyware), AppWatch needs Notification Access.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Important: You might see a 10-second safety timer on your device settings. This is standard for security focused tools. We never read your private content and we only use it to count alerts.\n You can always remove this from the settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showExplainerDialog = false
                            onGrantPermissionClick()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Orange600)
                    ) {
                        Text("Open Settings")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExplainerDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = SurfaceWhite,
                shape = RoundedCornerShape(28.dp)
            )
        }
    }
}