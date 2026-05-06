package com.ekagra.privascope.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ekagra.privascope.ui.Activity.MainActivity
import com.ekagra.privascope.ui.theme.*
import kotlinx.coroutines.delay

class CrashRecoveryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPrefs = getSharedPreferences("priva_scope_prefs", Context.MODE_PRIVATE)
        val isCritical = sharedPrefs.getBoolean("is_critical_loop", false)

        setContent {
            PrivaScopeTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (isCritical) {
                        CriticalRecoveryScreen(
                            onResetData = { resetAndRestart() }
                        )
                    } else {
                        CrashRecoveryScreen(
                            onRestart = { restartApp() },
                        )
                    }
                }
            }
        }
    }

    private fun restartApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun resetAndRestart() {
        val sharedPrefs = getSharedPreferences("priva_scope_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().clear().commit()
        val securePrefs = getSharedPreferences("secure_prefs", Context.MODE_PRIVATE)
        securePrefs.edit().clear().commit()
        deleteDatabase("priva_scope_database")
        restartApp()
    }
}

@Composable
fun CrashRecoveryScreen(
    onRestart: () -> Unit
) {
    var timeLeft by remember { mutableIntStateOf(20) }
    val progress by animateFloatAsState(targetValue = timeLeft / 20f, label = "timer")

    // Current Data for Display
    val currentTime = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
    val deviceName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
    val osVersion = "Android ${android.os.Build.VERSION.RELEASE}"

    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(1000L)
            timeLeft--
        }
        onRestart()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = Indigo600,
            modifier = Modifier.size(56.dp)
        )

        Spacer(Modifier.height(16.dp))

        Text("PrivaScope crashed", fontSize = 22.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(8.dp))

        Text(
            "App crashed due to some unexpected error and needs a restart. Don't worry, your data is safe. Restarting automatically in $timeLeft seconds.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )

        Spacer(Modifier.height(32.dp))

        // Visual Timer
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(70.dp),
                color = Indigo600,
                strokeWidth = 5.dp,
                trackColor = Indigo600.copy(alpha = 0.1f)
            )
            Text("$timeLeft", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        Spacer(Modifier.height(32.dp))

        // --- NEW DETAILS SECTION ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Summary",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Indigo600
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

                DetailRow(label = "App Name", value = "PrivaScope")
                DetailRow(label = "Occurrence Time", value = currentTime)
                DetailRow(label = "Device", value = deviceName)
                DetailRow(label = "System", value = osVersion)
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
        ) {
            Text("Restart Now", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Text(text = value, fontWeight = FontWeight.Medium, fontSize = 13.sp)
    }
}

@Composable
fun CriticalRecoveryScreen(
    onResetData: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Friendly Maintenance Icon
        Icon(
            imageVector = Icons.Default.Build,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(64.dp)
        )

        Spacer(Modifier.height(24.dp))

        Text("PrivaScope crashed", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(12.dp))

        Text(
            "The app is crashing more than usual. Clearing the app's cache and data may fix this.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(40.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(Modifier.width(12.dp))
                Text(
                    "This will reset your settings and usage history.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(Modifier.height(40.dp))

        Button(
            onClick = onResetData,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("Clear Data & Restart")
        }
    }
}