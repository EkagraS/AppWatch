package com.ekagra.privascope.ui.ScreenComponents

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ekagra.privascope.data.local.entity.RecentEventEntity
import com.ekagra.privascope.domain.model.RecentItem
import com.ekagra.privascope.ui.viewModels.DashboardViewModel
import com.ekagra.privascope.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.ekagra.privascope.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentActivitySection(
    recentItems: List<RecentItem>,
    isUpdating: Boolean,
    viewModel: DashboardViewModel,
    onNavigateToEventScreen: (String) -> Unit
) {
    var selectedItem by remember { mutableStateOf<RecentItem?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.recent_header),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (isUpdating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = Indigo500
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (recentItems.isEmpty()) {
            EmptyActivityState()
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                recentItems.forEach { item ->
                    ActivityRowCard(
                        item = item,
                        isUpdating = isUpdating,
                        onClick = {
                            val count = item.description.filter { it.isDigit() }.toIntOrNull() ?: 0
                            if (count > 5) {
                                onNavigateToEventScreen(item.eventType)
                            } else {
                                selectedItem = item
                            }
                        }
                    )
                }
            }
        }

        if (selectedItem != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedItem = null },
                sheetState = sheetState,
                containerColor = SurfaceWhite,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                BottomSheetContent(
                    item = selectedItem!!,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun ActivityRowCard(item: RecentItem, isUpdating: Boolean, onClick: () -> Unit) {
    val (iconTint, bgColor) = when (item.eventType) {
        "SIDELOADED_APK" -> Red600 to Red50
        "DATA_HOG" -> Amber600 to Amber50
        "INSTALL" -> Green600 to Green50
        "UPDATE" -> Blue600 to Blue50
        "UNINSTALL" -> Red600 to Red50
        else -> Indigo600 to Indigo50
    }

    Card(
        onClick = if (isUpdating) ({}) else onClick,
        enabled = !item.isTimeline,
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, DividerColor)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(bgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconForEventType(item.eventType),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (item.eventType == "SIDELOADED_APK") Red600 else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }

            if (!item.isTimeline) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextDisabled,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun BottomSheetContent(item: RecentItem, viewModel: DashboardViewModel) {
    val eventList by viewModel.getEventsByType(item.eventType).collectAsStateWithLifecycle(initialValue = emptyList())

    val (sheetTitleRes, titleColor) = when(item.eventType) {
        "INSTALL" -> R.string.sheet_title_install to TextPrimary
        "UPDATE" -> R.string.sheet_title_update to TextPrimary
        "SIDELOADED_APK" -> R.string.sheet_title_sideloaded to Red600
        "UNINSTALL" -> R.string.sheet_title_uninstall to TextPrimary
        else -> R.string.sheet_title_details to TextPrimary
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 40.dp, top = 8.dp)
    ) {
        Text(
            text = stringResource(sheetTitleRes),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = titleColor,
            modifier = Modifier.padding(bottom = 8.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (item.eventType == "SIDELOADED_APK") {
            Text(
                text = stringResource(R.string.desc_sideloaded_warning),
                style = MaterialTheme.typography.bodyMedium,
                color = Red600,
                modifier = Modifier.padding(bottom = 16.dp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            eventList.forEach { event ->
                MiniEventItem(event = event)
            }
        }
    }
}

@Composable
fun MiniEventItem(event: RecentEventEntity) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    var appName by remember(event.packageName) { mutableStateOf(event.packageName) }
    var appIconBitmap by remember(event.packageName) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(event.packageName) {
        withContext(Dispatchers.IO) {
            try {
                val appInfo = packageManager.getApplicationInfo(event.packageName, 0)
                appName = packageManager.getApplicationLabel(appInfo).toString()
                appIconBitmap = packageManager.getApplicationIcon(appInfo).toBitmap().asImageBitmap()
            } catch (e: Exception) { /* Fallback handled */ }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (appIconBitmap != null) {
            Image(
                bitmap = appIconBitmap!!,
                contentDescription = null,
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
            )
        } else {
            Icon(Icons.Default.Android, null, tint = Indigo500, modifier = Modifier.size(36.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = appName,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = event.extraInfo ?: event.packageName,
                fontSize = 11.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
private fun EmptyActivityState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Green500,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.recent_empty_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = stringResource(R.string.recent_empty_desc),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

fun getIconForEventType(eventType: String): ImageVector {
    return when (eventType) {
        "SIDELOADED_APK" -> Icons.Default.GppBad
        "DATA_HOG" -> Icons.Default.DataUsage
        "UPDATE" -> Icons.Default.Update
        "INSTALL" -> Icons.Default.Download
        "UNINSTALL" -> Icons.Default.Delete
        else -> Icons.Default.Info
    }
}