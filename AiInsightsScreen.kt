package com.aetherion.noc.presentation.ai

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.aetherion.noc.core.security.SecurityManager
import com.aetherion.noc.domain.model.*
import com.aetherion.noc.domain.usecase.*
import com.aetherion.noc.presentation.common.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ═══════════════════════════════════════════════════════════════════════════
// Aetherion Mobile NOC — AI Insights Module
// Developer: Mohammad Abdalftah Ibrahime
// ═══════════════════════════════════════════════════════════════════════════

@HiltViewModel
class AiInsightsViewModel @Inject constructor(
    private val getPagedInsightsUseCase: GetPagedInsightsUseCase,
    private val securityManager: SecurityManager
) : ViewModel() {

    private val tenantId: String get() = securityManager.getTenantId() ?: ""

    val insights: Flow<PagingData<AiInsight>> =
        getPagedInsightsUseCase(tenantId).cachedIn(viewModelScope)
}

// ─── Screen ───────────────────────────────────────────────────────────────────

private val DateFmt = DateTimeFormatter.ofPattern("MM/dd HH:mm").withZone(ZoneId.systemDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiInsightsScreen(
    onNavigateToDevice: (String) -> Unit,
    viewModel: AiInsightsViewModel = hiltViewModel()
) {
    val insights = viewModel.insights.collectAsLazyPagingItems()

    AetherionNOCTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Psychology,
                                null,
                                tint = AetherionColors.AetherBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("AI Insights", color = AetherionColors.TextPrimary)
                        }
                    },
                    actions = {
                        IconButton(onClick = { insights.refresh() }) {
                            Icon(Icons.Outlined.Refresh, null, tint = AetherionColors.AetherBlue)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AetherionColors.SurfaceCard)
                )
            },
            containerColor = AetherionColors.SurfaceDark
        ) { padding ->
            when {
                insights.loadState.refresh is LoadState.Loading -> Box(
                    Modifier.fillMaxSize().padding(padding), Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AetherionColors.AetherBlue)
                        Spacer(Modifier.height(12.dp))
                        Text("Running AI analysis...", color = AetherionColors.TextSecondary,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                insights.loadState.refresh is LoadState.Error -> Box(
                    Modifier.fillMaxSize().padding(padding), Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.ErrorOutline, null, tint = AetherionColors.Critical,
                            modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Failed to load insights", color = AetherionColors.TextPrimary)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { insights.refresh() }) { Text("Retry") }
                    }
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(insights.itemCount, key = insights.itemKey { it.insightId }) { index ->
                        val insight = insights[index] ?: return@items
                        AiInsightCard(insight = insight, onDeviceClick = {
                            insight.affectedDeviceId?.let { onNavigateToDevice(it) }
                        })
                    }

                    if (insights.loadState.append is LoadState.Loading) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = AetherionColors.AetherBlue,
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }

                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun AiInsightCard(insight: AiInsight, onDeviceClick: () -> Unit) {
    val typeColor = insight.type.toColor()
    val typeDim   = insight.type.toDimColor()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AetherionColors.SurfaceCard),
        border = BorderStroke(1.dp, typeColor.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(typeDim, MaterialTheme.shapes.small)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        insight.type.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = typeColor
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    DateFmt.format(insight.predictedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = AetherionColors.TextMuted
                )
            }

            Spacer(Modifier.height(8.dp))

            // Title
            Text(
                insight.title,
                style = MaterialTheme.typography.titleMedium,
                color = AetherionColors.TextPrimary
            )

            Spacer(Modifier.height(4.dp))

            // Description
            Text(
                insight.description,
                style = MaterialTheme.typography.bodySmall,
                color = AetherionColors.TextSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(10.dp))

            // Risk / Confidence row
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                RiskBadge("Risk", insight.riskScore, insight.riskScore.toRiskColor())
                RiskBadge("Confidence", insight.confidence, AetherionColors.AetherBlue)
            }

            // Affected device
            if (insight.affectedDeviceName != null) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Router, null, tint = AetherionColors.TextSecondary,
                        modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        insight.affectedDeviceName,
                        style = MaterialTheme.typography.bodySmall,
                        color = AetherionColors.AetherBlue,
                        modifier = Modifier.clickable(onClick = onDeviceClick)
                    )
                }
            }

            // Predicted failure time
            if (insight.predictedFailureAt != null) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Schedule, null, tint = AetherionColors.Major,
                        modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Predicted failure: ${DateFmt.format(insight.predictedFailureAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AetherionColors.Major
                    )
                }
            }

            // Recommendation
            Spacer(Modifier.height(10.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = AetherionColors.SurfaceElevated)
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.Lightbulb, null, tint = AetherionColors.Minor,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        insight.recommendation,
                        style = MaterialTheme.typography.bodySmall,
                        color = AetherionColors.TextPrimary,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun RiskBadge(label: String, value: Float, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = AetherionColors.TextSecondary)
        Spacer(Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { value },
            modifier = Modifier.width(64.dp).height(5.dp),
            color = color,
            trackColor = AetherionColors.SurfaceBorder
        )
        Spacer(Modifier.height(2.dp))
        Text("${(value * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall, color = color)
    }
}

private fun Float.toRiskColor(): Color = when {
    this >= 0.8f -> AetherionColors.Critical
    this >= 0.5f -> AetherionColors.Major
    this >= 0.3f -> AetherionColors.Minor
    else         -> AetherionColors.Info
}

private val InsightType.label get() = when (this) {
    InsightType.ANOMALY                 -> "ANOMALY"
    InsightType.PREDICTED_FAILURE       -> "PRED. FAILURE"
    InsightType.CAPACITY_WARNING        -> "CAPACITY"
    InsightType.PERFORMANCE_DEGRADATION -> "PERFORMANCE"
    InsightType.SECURITY_THREAT         -> "SECURITY"
}

private fun InsightType.toColor(): Color = when (this) {
    InsightType.ANOMALY                 -> AetherionColors.Warning
    InsightType.PREDICTED_FAILURE       -> AetherionColors.Critical
    InsightType.CAPACITY_WARNING        -> AetherionColors.Major
    InsightType.PERFORMANCE_DEGRADATION -> AetherionColors.Minor
    InsightType.SECURITY_THREAT         -> Color(0xFFFF4081)
}

private fun InsightType.toDimColor(): Color = when (this) {
    InsightType.ANOMALY                 -> AetherionColors.WarningDim
    InsightType.PREDICTED_FAILURE       -> AetherionColors.CriticalDim
    InsightType.CAPACITY_WARNING        -> AetherionColors.MajorDim
    InsightType.PERFORMANCE_DEGRADATION -> AetherionColors.MinorDim
    InsightType.SECURITY_THREAT         -> Color(0xFF4A0020)
}
