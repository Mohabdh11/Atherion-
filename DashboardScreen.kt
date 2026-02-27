package com.aetherion.noc.presentation.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aetherion.noc.domain.model.*
import com.aetherion.noc.presentation.common.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ═══════════════════════════════════════════════════════════════════════════
// Aetherion Mobile NOC — Executive Dashboard Screen
// Developer: Mohammad Abdalftah Ibrahime
// ═══════════════════════════════════════════════════════════════════════════

private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToAlerts: () -> Unit,
    onNavigateToTopology: () -> Unit,
    onNavigateToDevice: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState     by viewModel.dashboardState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val lastRefresh  by viewModel.lastRefreshTime.collectAsStateWithLifecycle()

    val pullState = rememberPullToRefreshState()
    if (pullState.isRefreshing) { LaunchedEffect(true) { viewModel.refresh() } }
    LaunchedEffect(isRefreshing) { if (!isRefreshing) pullState.endRefresh() }

    AetherionNOCTheme {
        Scaffold(
            topBar = { DashboardTopBar(lastRefresh, onNavigateToAlerts) },
            containerColor = AetherionColors.SurfaceDark
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .nestedScroll(pullState.nestedScrollConnection)
            ) {
                when (val state = uiState) {
                    is DashboardUiState.Loading -> DashboardSkeleton()
                    is DashboardUiState.Error   -> DashboardError(state.message) { viewModel.refresh() }
                    is DashboardUiState.Success -> DashboardContent(
                        summary = state.data,
                        onNavigateToAlerts = onNavigateToAlerts,
                        onNavigateToTopology = onNavigateToTopology
                    )
                }

                PullToRefreshContainer(
                    state = pullState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = AetherionColors.SurfaceElevated,
                    contentColor = AetherionColors.AetherBlue
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(lastRefresh: Long, onAlertsClick: () -> Unit) {
    TopAppBar(
        title = {
            Column {
                Text(
                    "Network Dashboard",
                    style = MaterialTheme.typography.titleMedium,
                    color = AetherionColors.TextPrimary
                )
                Text(
                    "Last sync: ${TimeFormatter.format(Instant.ofEpochMilli(lastRefresh))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AetherionColors.TextSecondary
                )
            }
        },
        actions = {
            IconButton(onClick = onAlertsClick) {
                Icon(Icons.Outlined.Notifications, "Alerts", tint = AetherionColors.AetherBlue)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AetherionColors.SurfaceCard
        )
    )
}

@Composable
private fun DashboardContent(
    summary: DashboardSummary,
    onNavigateToAlerts: () -> Unit,
    onNavigateToTopology: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Network Health Score ───────────────────────────────
        item {
            HealthScoreCard(score = summary.healthScore, slaCompliance = summary.slaCompliance)
        }

        // ── Alert Summary Row ──────────────────────────────────
        item {
            AlertSummaryRow(
                critical = summary.criticalAlerts,
                major    = summary.majorAlerts,
                minor    = summary.minorAlerts,
                warning  = summary.warningAlerts,
                onClick  = onNavigateToAlerts
            )
        }

        // ── Device Status ──────────────────────────────────────
        item {
            DeviceStatusCard(
                online  = summary.devicesOnline,
                offline = summary.devicesOffline,
                total   = summary.devicesTotal,
                onClick = onNavigateToTopology
            )
        }

        // ── AI Insight Panel ───────────────────────────────────
        item {
            AiInsightPanel(summary.aiInsightSummary)
        }

        // ── Region Health Grid ─────────────────────────────────
        if (summary.networkRegions.isNotEmpty()) {
            item {
                SectionHeader("Regional Health")
            }
            items(summary.networkRegions, key = { it.regionId }) { region ->
                RegionHealthRow(region)
            }
        }

        item { Spacer(Modifier.height(80.dp)) } // bottom nav clearance
    }
}

// ─── Health Score Card ────────────────────────────────────────────────────────

@Composable
private fun HealthScoreCard(score: Float, slaCompliance: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AetherionColors.SurfaceCard),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular progress
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
                Canvas(modifier = Modifier.size(100.dp)) {
                    val strokeWidth = 10.dp.toPx()
                    drawArc(
                        color = AetherionColors.SurfaceBorder,
                        startAngle = -90f, sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = score.toHealthColor(),
                        startAngle = -90f, sweepAngle = (score / 100f) * 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${score.toInt()}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = score.toHealthColor()
                    )
                    Text("Score", style = MaterialTheme.typography.labelSmall, color = AetherionColors.TextSecondary)
                }
            }

            Spacer(Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Network Health",
                    style = MaterialTheme.typography.titleMedium,
                    color = AetherionColors.TextPrimary
                )
                Spacer(Modifier.height(8.dp))
                MetricRow("SLA Compliance", "${slaCompliance.toInt()}%", AetherionColors.AetherBlue)
                Spacer(Modifier.height(4.dp))
                MetricRow(
                    "Status",
                    when {
                        score >= 90 -> "OPTIMAL"
                        score >= 70 -> "DEGRADED"
                        else        -> "CRITICAL"
                    },
                    score.toHealthColor()
                )
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String, valueColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = AetherionColors.TextSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(value, style = MaterialTheme.typography.labelMedium, color = valueColor)
    }
}

// ─── Alert Summary Row ────────────────────────────────────────────────────────

@Composable
private fun AlertSummaryRow(
    critical: Int, major: Int, minor: Int, warning: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AlertChip(
            count = critical, label = "CRIT",
            color = AetherionColors.Critical, dim = AetherionColors.CriticalDim,
            modifier = Modifier.weight(1f), onClick = onClick
        )
        AlertChip(
            count = major, label = "MAJ",
            color = AetherionColors.Major, dim = AetherionColors.MajorDim,
            modifier = Modifier.weight(1f), onClick = onClick
        )
        AlertChip(
            count = minor, label = "MIN",
            color = AetherionColors.Minor, dim = AetherionColors.MinorDim,
            modifier = Modifier.weight(1f), onClick = onClick
        )
        AlertChip(
            count = warning, label = "WARN",
            color = AetherionColors.Warning, dim = AetherionColors.WarningDim,
            modifier = Modifier.weight(1f), onClick = onClick
        )
    }
}

@Composable
private fun AlertChip(
    count: Int, label: String,
    color: Color, dim: Color,
    modifier: Modifier, onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = dim),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineSmall,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.8f)
            )
        }
    }
}

// ─── Device Status Card ───────────────────────────────────────────────────────

@Composable
private fun DeviceStatusCard(online: Int, offline: Int, total: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = AetherionColors.SurfaceCard),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Device Inventory",
                    style = MaterialTheme.typography.titleMedium,
                    color = AetherionColors.TextPrimary
                )
                Icon(
                    Icons.Outlined.DevicesOther, null,
                    tint = AetherionColors.AetherBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { if (total > 0) online.toFloat() / total else 0f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.small),
                color = AetherionColors.Online,
                trackColor = AetherionColors.SurfaceBorder
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                StatusBadge("Online", online, AetherionColors.Online, Modifier.weight(1f))
                StatusBadge("Offline", offline, AetherionColors.Offline, Modifier.weight(1f))
                StatusBadge("Total", total, AetherionColors.TextSecondary, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatusBadge(label: String, count: Int, color: Color, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count.toString(), style = MaterialTheme.typography.titleLarge, color = color)
        Text(label, style = MaterialTheme.typography.bodySmall, color = AetherionColors.TextSecondary)
    }
}

// ─── AI Insight Panel ─────────────────────────────────────────────────────────

@Composable
private fun AiInsightPanel(summary: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AetherionColors.SurfaceCard
        ),
        border = BorderStroke(1.dp, AetherionColors.AetherBlue.copy(alpha = 0.3f)),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Psychology, null,
                    tint = AetherionColors.AetherBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "AI Insight",
                    style = MaterialTheme.typography.titleMedium,
                    color = AetherionColors.AetherBlue
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = AetherionColors.TextPrimary,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─── Region Health ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = AetherionColors.TextSecondary,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun RegionHealthRow(region: RegionHealth) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AetherionColors.SurfaceCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(region.status.toColor(), shape = MaterialTheme.shapes.small)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                region.regionName,
                style = MaterialTheme.typography.bodyMedium,
                color = AetherionColors.TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${region.healthScore.toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                color = region.healthScore.toHealthColor()
            )
        }
    }
}

private fun NetworkStatus.toColor() = when (this) {
    NetworkStatus.HEALTHY  -> AetherionColors.Online
    NetworkStatus.DEGRADED -> AetherionColors.Degraded
    NetworkStatus.CRITICAL -> AetherionColors.Critical
    NetworkStatus.UNKNOWN  -> AetherionColors.Info
}

// ─── Loading / Error States ───────────────────────────────────────────────────

@Composable
private fun DashboardSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(4) {
            Card(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                colors = CardDefaults.cardColors(containerColor = AetherionColors.SurfaceCard)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().shimmer()
                )
            }
        }
    }
}

@Composable
private fun DashboardError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.CloudOff, null,
            tint = AetherionColors.TextSecondary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text("Connection Failed", style = MaterialTheme.typography.titleMedium, color = AetherionColors.TextPrimary)
        Text(message, style = MaterialTheme.typography.bodySmall, color = AetherionColors.TextSecondary)
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onRetry, border = BorderStroke(1.dp, AetherionColors.AetherBlue)) {
            Text("Retry", color = AetherionColors.AetherBlue)
        }
    }
}

// Shimmer placeholder (simple implementation)
private fun Modifier.shimmer() = this.background(
    Brush.horizontalGradient(
        colors = listOf(
            AetherionColors.SurfaceBorder,
            AetherionColors.SurfaceElevated,
            AetherionColors.SurfaceBorder
        )
    )
)
