package com.aetherion.noc.presentation.alerts

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.aetherion.noc.domain.model.*
import com.aetherion.noc.presentation.common.theme.*
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ═══════════════════════════════════════════════════════════════════════════
// Aetherion Mobile NOC — Alert Center Screen
// Developer: Mohammad Abdalftah Ibrahime
// ═══════════════════════════════════════════════════════════════════════════

private val DateFmt = DateTimeFormatter.ofPattern("MM/dd HH:mm").withZone(ZoneId.systemDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertCenterScreen(
    onNavigateToDevice: (String) -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: AlertViewModel = hiltViewModel()
) {
    val alerts          = viewModel.alerts.collectAsLazyPagingItems()
    val selectedSev     by viewModel.selectedSeverities.collectAsStateWithLifecycle()
    val actionState     by viewModel.actionState.collectAsStateWithLifecycle()

    var escalateTarget  by remember { mutableStateOf<String?>(null) }
    var escalateNote    by remember { mutableStateOf("") }
    val snackbarState   = remember { SnackbarHostState() }

    // Handle action results
    LaunchedEffect(actionState) {
        when (actionState) {
            is AlertActionState.Success -> {
                snackbarState.showSnackbar((actionState as AlertActionState.Success).message)
                viewModel.clearActionState()
                alerts.refresh()
            }
            is AlertActionState.Error -> {
                snackbarState.showSnackbar((actionState as AlertActionState.Error).message)
                viewModel.clearActionState()
            }
            else -> {}
        }
    }

    AetherionNOCTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text("Alert Center", style = MaterialTheme.typography.titleMedium,
                             color = AetherionColors.TextPrimary)
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Outlined.ArrowBack, "Back", tint = AetherionColors.TextPrimary)
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.clearFilters() }) {
                            Icon(Icons.Outlined.FilterAltOff, "Clear filters",
                                 tint = if (selectedSev != null) AetherionColors.AetherBlue
                                        else AetherionColors.TextSecondary)
                        }
                        IconButton(onClick = { alerts.refresh() }) {
                            Icon(Icons.Outlined.Refresh, "Refresh", tint = AetherionColors.AetherBlue)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AetherionColors.SurfaceCard
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarState) },
            containerColor = AetherionColors.SurfaceDark
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // ── Severity Filter Chips ──────────────────────
                SeverityFilterRow(
                    selectedSeverities = selectedSev ?: emptyList(),
                    onToggle = viewModel::toggleSeverityFilter
                )

                HorizontalDivider(color = AetherionColors.SurfaceBorder)

                // ── Alert List ─────────────────────────────────
                when {
                    alerts.loadState.refresh is LoadState.Loading -> AlertListSkeleton()
                    alerts.loadState.refresh is LoadState.Error   -> AlertListError { alerts.refresh() }
                    else -> AlertList(
                        alerts = alerts,
                        onAcknowledge = viewModel::acknowledgeAlert,
                        onEscalate    = { id -> escalateTarget = id },
                        onDeviceClick = onNavigateToDevice
                    )
                }
            }
        }
    }

    // Escalate dialog
    if (escalateTarget != null) {
        AlertDialog(
            onDismissRequest = { escalateTarget = null; escalateNote = "" },
            title = { Text("Escalate Alert") },
            text = {
                Column {
                    Text(
                        "Provide escalation note:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AetherionColors.TextSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = escalateNote,
                        onValueChange = { escalateNote = it },
                        placeholder = { Text("Escalation reason...") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.escalateAlert(escalateTarget!!, escalateNote)
                        escalateTarget = null
                        escalateNote = ""
                    },
                    enabled = escalateNote.isNotBlank()
                ) {
                    Text("Escalate", color = AetherionColors.Major)
                }
            },
            dismissButton = {
                TextButton(onClick = { escalateTarget = null; escalateNote = "" }) {
                    Text("Cancel")
                }
            },
            containerColor = AetherionColors.SurfaceElevated
        )
    }
}

@Composable
private fun SeverityFilterRow(
    selectedSeverities: List<AlertSeverity>,
    onToggle: (AlertSeverity) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(AetherionColors.SurfaceCard)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(AlertSeverity.values()) { severity ->
            val selected = selectedSeverities.contains(severity)
            FilterChip(
                selected = selected,
                onClick = { onToggle(severity) },
                label = { Text(severity.name, style = MaterialTheme.typography.labelSmall) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = severity.toDimColor(),
                    selectedLabelColor = severity.toColor(),
                    containerColor = AetherionColors.SurfaceElevated,
                    labelColor = AetherionColors.TextSecondary
                )
            )
        }
    }
}

@Composable
private fun AlertList(
    alerts: LazyPagingItems<Alert>,
    onAcknowledge: (String) -> Unit,
    onEscalate: (String) -> Unit,
    onDeviceClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(alerts.itemCount, key = alerts.itemKey { it.alertId }) { index ->
            val alert = alerts[index]
            if (alert != null) {
                AlertListItem(
                    alert = alert,
                    onAcknowledge = { onAcknowledge(alert.alertId) },
                    onEscalate    = { onEscalate(alert.alertId) },
                    onDeviceClick = { onDeviceClick(alert.deviceId) }
                )
            }
        }

        // Append loading state
        if (alerts.loadState.append is LoadState.Loading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = AetherionColors.AetherBlue,
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertListItem(
    alert: Alert,
    onAcknowledge: () -> Unit,
    onEscalate: () -> Unit,
    onDeviceClick: () -> Unit
) {
    val severityColor = alert.severity.toColor()
    val severityDim   = alert.severity.toDimColor()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = AetherionColors.SurfaceCard),
        border = BorderStroke(width = 1.dp, color = severityColor.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Severity badge
                Box(
                    modifier = Modifier
                        .background(severityDim, MaterialTheme.shapes.small)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        alert.severity.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = severityColor
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    alert.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AetherionColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    DateFmt.format(alert.raisedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = AetherionColors.TextMuted
                )
            }

            Spacer(Modifier.height(6.dp))

            // Device / region
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Router, null, tint = AetherionColors.TextSecondary,
                     modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    alert.deviceName,
                    style = MaterialTheme.typography.bodySmall,
                    color = AetherionColors.AetherBlue,
                    modifier = Modifier.clickable(onClick = onDeviceClick)
                )
                Text(" · ${alert.regionName}", style = MaterialTheme.typography.bodySmall,
                     color = AetherionColors.TextSecondary)
            }

            // Root cause
            if (!alert.rootCause.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Root cause: ${alert.rootCause}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AetherionColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Action buttons (only for non-resolved/acknowledged)
            if (alert.status == AlertStatus.RAISED) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onAcknowledge) {
                        Text("Acknowledge", style = MaterialTheme.typography.labelSmall,
                             color = AetherionColors.AetherBlue)
                    }
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = onEscalate) {
                        Text("Escalate", style = MaterialTheme.typography.labelSmall,
                             color = AetherionColors.Major)
                    }
                }
            } else {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CheckCircleOutline, null,
                         tint = AetherionColors.Online, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(alert.status.name, style = MaterialTheme.typography.labelSmall,
                         color = AetherionColors.TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun AlertListSkeleton() {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp),
           verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(8) {
            Card(modifier = Modifier.fillMaxWidth().height(90.dp),
                 colors = CardDefaults.cardColors(containerColor = AetherionColors.SurfaceCard)) {}
        }
    }
}

@Composable
private fun AlertListError(onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.ErrorOutline, null, tint = AetherionColors.Critical,
                 modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text("Failed to load alerts", color = AetherionColors.TextPrimary)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onRetry) { Text("Retry") }
        }
    }
}
