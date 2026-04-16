package com.example.appwatch.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// --- UNIQUE TYPES FOR EVERY SHEET ---
enum class SheetType {
    USAGE_INFO,
    PERMISSION_INFO,
    STORAGE_INFO,
    PRIVACY,
    CONTACT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    var activeSheet by remember { mutableStateOf<SheetType?>(null) }
    var showRevokeDialog by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // 1. APP DESCRIPTION
            item {
                Text(
                    text = "AppWatch is a high-performance privacy and system utility. It analyzes app behaviors, monitors sensor activity, and provides deep-dive storage analytics. Your data never leaves your device.",
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    lineHeight = 20.sp
                )
            }

            // 2. SYSTEM ACCESS & CONTROL (Lightest Amber)
            item { SectionHeader("System Access & Control", Color(0xFFD97706)) }
            item {
                PermissionRow("App Usage Access", "To track screen time and unused apps.", Color(0xFFFFF9C4), true) {
                    showRevokeDialog = Settings.ACTION_USAGE_ACCESS_SETTINGS
                }
            }
            item {
                PermissionRow("Storage Statistics", "To calculate exact app and cache sizes.", Color(0xFFFFF9C4), true) {
                    showRevokeDialog = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                }
            }
            item { InfoRow("All Apps Visibility", "To identify all installed applications.", Color(0xFFFFF9C4)) }
            item { InfoRow("Auto-Start Access", "To resume monitoring after phone restarts.", Color(0xFFFFF9C4)) }

            // 3. TECHNICAL ANALYSIS (Lightest Mint)
            item { SectionHeader("Technical Analysis Details", Color(0xFF059669)) }

            item { SubsectionHeader("Usage Statistics") }
            item { AnalysisItem("Usage & Activity", "How we measure app usage and inactivity.", Color(0xFFE8F5E9)) { activeSheet = SheetType.USAGE_INFO } }

            item { SubsectionHeader("Permission Audit") }
            item { AnalysisItem("Security Monitoring", "How we catch background sensor and data access.", Color(0xFFE8F5E9)) { activeSheet = SheetType.PERMISSION_INFO } }

            item { SubsectionHeader("Storage Analytics") }
            item { AnalysisItem("Storage & Junk", "How we separate data and find temporary files.", Color(0xFFE8F5E9)) { activeSheet = SheetType.STORAGE_INFO } }

            // 4. FOOTER
            item { Spacer(modifier = Modifier.height(24.dp)) }
            item { PlainActionRow("Privacy Policy", Icons.Default.Shield) { activeSheet = SheetType.PRIVACY } }
            item { PlainActionRow("Contact Developer", Icons.Default.Person) { activeSheet = SheetType.CONTACT } }
            item {
                Text(
                    text = "Version 1.0.0",
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    textAlign = TextAlign.Center,
                    color = Color.LightGray,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }

    // --- DIALOGS ---
    if (showRevokeDialog != null) {
        AlertDialog(
            onDismissRequest = { showRevokeDialog = null },
            title = { Text("Revoke Permission?") },
            text = { Text("Turning this off will stop AppWatch from analyzing your system properly. Go to settings?") },
            confirmButton = {
                TextButton(onClick = {
                    val action = showRevokeDialog!!
                    showRevokeDialog = null
                    context.startActivity(Intent(action).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }) { Text("YES", color = Color(0xFFEF4444)) }
            },
            dismissButton = {
                TextButton(onClick = { showRevokeDialog = null }) { Text("CANCEL") }
            }
        )
    }

    // --- DYNAMIC BOTTOM SHEETS ---
    if (activeSheet != null) {
        ModalBottomSheet(onDismissRequest = { activeSheet = null }, containerColor = Color.White) {
            when (activeSheet) {
                SheetType.USAGE_INFO -> UsageDetailSheet()
                SheetType.PERMISSION_INFO -> PermissionDetailSheet()
                SheetType.STORAGE_INFO -> StorageDetailSheet()
                SheetType.PRIVACY -> PrivacyPolicySheet()
                SheetType.CONTACT -> ContactSheet(context)
                else -> {}
            }
        }
    }
}

// --- SUB-COMPONENTS ---

@Composable
fun UsageDetailSheet() {
    Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
        Text("Usage Tracking", fontSize = 20.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(16.dp))
        DetailPoint("Daily Screen Time", "We calculate exactly how many minutes you spend on each app every day. This helps you understand your digital habits.")
        DetailPoint("Unused App Audit", "We look for apps that haven't been opened in 14 to 30 days. These apps often take up space and retain unnecessary permissions.")
        DetailPoint("Recent Installs", "We keep track of every app installed in the last 7 days so you can review new additions to your phone.")
    }
}

@Composable
fun PermissionDetailSheet() {
    Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
        Text("Permission Audit", fontSize = 20.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(16.dp))
        DetailPoint("Sensor Detection", "We detect if an app uses your Camera or Microphone while it is in the background. If a sensor is active without your knowledge, we alert you.")
        DetailPoint("Sensitive Access", "We identify which apps have high-risk access to your Location, SMS, and Contacts, ensuring you know exactly who can see your data.")
        DetailPoint("Background Activity", "We scan for apps that stay active in the background, consuming battery and resources even when you aren't using them.")
    }
}

@Composable
fun StorageDetailSheet() {
    Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
        Text("Storage Analytics", fontSize = 20.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(16.dp))
        DetailPoint("Data Breakdown", "We separate the actual app file (APK) from your personal saved data. This shows you if an app is growing too large because of your files.")
        DetailPoint("Junk Finder", "We locate temporary cache files that apps leave behind. Clearing these can free up significant space without losing any of your data.")
        DetailPoint("Percentage Use", "We calculate which apps consume the largest share of your phone's total memory, helping you prioritize what to delete.")
    }
}

// --- UI HELPERS ---

@Composable
fun DetailPoint(title: String, desc: String) {
    Column(modifier = Modifier.padding(vertical = 10.dp)) {
        Text(title, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
        Text(desc, fontSize = 14.sp, color = Color.Gray, lineHeight = 20.sp)
    }
}

@Composable
fun PermissionRow(title: String, desc: String, tint: Color, isGranted: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(desc, fontSize = 12.sp, color = Color.Gray)
        }
        Surface(
            onClick = onClick,
            color = tint,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.wrapContentWidth()
        ) {
            Text(
                text = if (isGranted) "Revoke" else "Enable",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun InfoRow(title: String, desc: String, tint: Color) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(desc, fontSize = 12.sp, color = Color.Gray)
        }
        Box(modifier = Modifier.size(8.dp).background(tint, CircleShape))
    }
}

@Composable
fun AnalysisItem(title: String, sub: String, tint: Color, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(32.dp).background(tint, CircleShape))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(sub, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun SectionHeader(title: String, color: Color) {
    Text(text = title, modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp), color = color, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelLarge)
}

@Composable
fun SubsectionHeader(title: String) {
    Text(text = title, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
}

@Composable
fun PlainActionRow(title: String, icon: ImageVector, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color.LightGray)
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PrivacyPolicySheet() {
    Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
        Text("Privacy Policy", fontSize = 20.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(16.dp))
        Text("AppWatch is built with a local-first philosophy. Your data is never uploaded, shared, or exported. Every scan and analysis happens directly on your device. We maintain no servers and use no third-party trackers. Your privacy is guaranteed by design.")
    }
}

@Composable
fun ContactSheet(context: Context) {
    Column(modifier = Modifier.padding(24.dp).padding(bottom = 40.dp)) {
        Text("Contact Developer", fontSize = 20.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Ekagra Shandilya", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        Text("LinkedIn: linkedin.com/in/ekagrashandilya", fontSize = 14.sp)
        Text("GitHub: github.com/shandilya-ekagra", fontSize = 14.sp)
        Text("Email: ekagra@lnmiit.ac.in", fontSize = 14.sp)
    }
}