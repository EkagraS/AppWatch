package com.ekagra.privascope.ui.screens.today

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.ekagra.privascope.data.local.entity.AppDataUsageEntity
import com.ekagra.privascope.ui.theme.BackgroundLight
import com.ekagra.privascope.ui.theme.Teal50
import com.ekagra.privascope.ui.theme.TextPrimary
import com.ekagra.privascope.ui.viewModels.AppDataUsageViewmodel
import com.ekagra.privascope.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDataUsageScreen(
    navController: NavController,
    viewModel: AppDataUsageViewmodel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val filteredList = remember(uiState.dataUsage) {
        uiState.dataUsage.filter {
            (it.mobileUsageBytes + it.wifiUsageBytes) > 1024 * 1024
        }.sortedByDescending { it.mobileUsageBytes + it.wifiUsageBytes }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
        rememberTopAppBarState()
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.data_usage_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BackgroundLight,
                    scrolledContainerColor = Teal50,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary,
                    actionIconContentColor = TextPrimary
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (filteredList.isEmpty()) {
                Text(
                    text = stringResource(R.string.data_usage_empty_state),
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredList) { usage ->
                        DataUsageItem(usage, context)
                    }
                }
            }
        }
    }
}

@Composable
fun DataUsageItem(usage: AppDataUsageEntity, context: android.content.Context) {
    val pm = context.packageManager

    val appInfo = try {
        pm.getApplicationInfo(usage.packageName, 0)
    } catch (e: Exception) {
        null
    }

    val appName = appInfo?.loadLabel(pm)?.toString() ?: usage.packageName
    val appIcon = appInfo?.loadIcon(pm)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = appIcon,
                contentDescription = null,
                modifier = Modifier.size(44.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis // Name bada ho toh dots dikhayega
                )
                Text(
                    text = usage.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Clip // Package name bada ho toh dots nahi dikhayega, bas cut jayega
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                if (usage.mobileUsageBytes > 0) {
                    Text(
                        text = stringResource(R.string.label_mobile, formatBytes(usage.mobileUsageBytes)),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFE91E63),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
                if (usage.wifiUsageBytes > 0) {
                    Text(
                        text = stringResource(R.string.label_wifi, formatBytes(usage.wifiUsageBytes)),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF2196F3),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.width(40.dp).padding(vertical = 2.dp),
                    thickness = 0.5.dp,
                    color = Color.LightGray
                )

                Text(
                    text = formatBytes(usage.mobileUsageBytes + usage.wifiUsageBytes),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}

fun formatBytes(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0

    return when {
        gb >= 1.0 -> String.format("%.2f GB", gb)
        mb >= 1.0 -> String.format("%.2f MB", mb)
        kb >= 1.0 -> String.format("%.2f KB", kb)
        else -> "$bytes B"
    }
}