package com.aetherion.noc.presentation.topology

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.aetherion.noc.core.security.SecurityManager
import com.aetherion.noc.domain.model.*
import com.aetherion.noc.domain.usecase.*
import com.aetherion.noc.presentation.common.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.sin

// ═══════════════════════════════════════════════════════════════════════════
// Aetherion Mobile NOC — Topology Viewer
// Developer: Mohammad Abdalftah Ibrahime
// Interactive graph with zoom/pan, node drill-down, lazy loading.
// ═══════════════════════════════════════════════════════════════════════════

@HiltViewModel
class TopologyViewModel @Inject constructor(
    private val getTopologyUseCase: GetTopologyUseCase,
    private val observeTopologyUseCase: ObserveTopologyUseCase,
    private val securityManager: SecurityManager
) : ViewModel() {

    private val tenantId: String get() = securityManager.getTenantId() ?: ""

    private val _topologyState = MutableStateFlow<TopologyUiState>(TopologyUiState.Loading)
    val topologyState: StateFlow<TopologyUiState> = _topologyState.asStateFlow()

    private val _selectedNode = MutableStateFlow<TopologyNode?>(null)
    val selectedNode: StateFlow<TopologyNode?> = _selectedNode.asStateFlow()

    init {
        loadTopology()
        observeUpdates()
    }

    private fun loadTopology() = viewModelScope.launch {
        _topologyState.value = TopologyUiState.Loading
        when (val result = getTopologyUseCase(tenantId)) {
            is Result.Success -> _topologyState.value = TopologyUiState.Success(result.data)
            is Result.Error   -> _topologyState.value = TopologyUiState.Error(result.message ?: "Failed to load topology")
            else -> {}
        }
    }

    private fun observeUpdates() {
        observeTopologyUseCase(tenantId)
            .onEach { graph -> _topologyState.value = TopologyUiState.Success(graph) }
            .launchIn(viewModelScope)
    }

    fun selectNode(node: TopologyNode?) { _selectedNode.value = node }
    fun refresh() = loadTopology()
}

sealed class TopologyUiState {
    data object Loading : TopologyUiState()
    data class Success(val graph: TopologyGraph) : TopologyUiState()
    data class Error(val message: String) : TopologyUiState()
}

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopologyScreen(
    onNodeClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: TopologyViewModel = hiltViewModel()
) {
    val uiState      by viewModel.topologyState.collectAsStateWithLifecycle()
    val selectedNode by viewModel.selectedNode.collectAsStateWithLifecycle()

    AetherionNOCTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Network Topology", color = AetherionColors.TextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Outlined.ArrowBack, null, tint = AetherionColors.TextPrimary)
                        }
                    },
                    actions = {
                        IconButton(onClick = viewModel::refresh) {
                            Icon(Icons.Outlined.Refresh, null, tint = AetherionColors.AetherBlue)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AetherionColors.SurfaceCard)
                )
            },
            containerColor = AetherionColors.SurfaceDark
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                when (val state = uiState) {
                    is TopologyUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AetherionColors.AetherBlue)
                            Spacer(Modifier.height(12.dp))
                            Text("Loading topology...", color = AetherionColors.TextSecondary,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    is TopologyUiState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.Hub, null, tint = AetherionColors.TextSecondary,
                                modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(state.message, color = AetherionColors.Critical,
                                style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = viewModel::refresh,
                                border = BorderStroke(1.dp, AetherionColors.AetherBlue)) {
                                Text("Retry", color = AetherionColors.AetherBlue)
                            }
                        }
                    }
                    is TopologyUiState.Success -> TopologyCanvas(
                        graph = state.graph,
                        selectedNode = selectedNode,
                        onNodeTap = { node -> viewModel.selectNode(node) },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Node detail bottom panel
                selectedNode?.let { node ->
                    NodeDetailPanel(
                        node = node,
                        onDismiss = { viewModel.selectNode(null) },
                        onNavigate = {
                            viewModel.selectNode(null)
                            onNodeClick(node.nodeId)
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }

                // Legend overlay
                TopologyLegend(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                )

                // Zoom hint
                if (uiState is TopologyUiState.Success) {
                    Text(
                        "Pinch to zoom • Tap node for details",
                        style = MaterialTheme.typography.bodySmall,
                        color = AetherionColors.TextMuted,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = if (selectedNode != null) 180.dp else 16.dp)
                    )
                }
            }
        }
    }
}

// ─── Interactive Canvas ───────────────────────────────────────────────────────

@Composable
private fun TopologyCanvas(
    graph: TopologyGraph,
    selectedNode: TopologyNode?,
    onNodeTap: (TopologyNode) -> Unit,
    modifier: Modifier
) {
    var scale  by remember { mutableFloatStateOf(0.8f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val textMeasurer = rememberTextMeasurer()

    // Build node position map — use server coords if non-zero, else auto-layout
    val nodePositions: Map<String, Offset> = remember(graph.nodes) {
        val hasCoords = graph.nodes.any { it.x != 0f || it.y != 0f }
        if (hasCoords) {
            graph.nodes.associate { it.nodeId to Offset(it.x, it.y) }
        } else {
            // Hierarchical concentric layout
            val total = graph.nodes.size
            graph.nodes.mapIndexed { i, node ->
                val layer  = (i / 8) + 1
                val index  = i % 8
                val count  = minOf(8, total - (layer - 1) * 8)
                val angle  = (index.toFloat() / count) * (2 * Math.PI.toFloat())
                val radius = 220f * layer
                node.nodeId to Offset(
                    600f + radius * cos(angle),
                    500f + radius * sin(angle)
                )
            }.toMap()
        }
    }

    // Edge color by link status
    fun linkColor(status: LinkStatus): Color = when (status) {
        LinkStatus.UP       -> AetherionColors.Online.copy(alpha = 0.5f)
        LinkStatus.DEGRADED -> AetherionColors.Degraded.copy(alpha = 0.5f)
        LinkStatus.DOWN     -> AetherionColors.Offline.copy(alpha = 0.4f)
    }

    // Node fill color by device status
    fun nodeColor(node: TopologyNode): Color = when (node.status) {
        DeviceStatus.UP          -> AetherionColors.Online
        DeviceStatus.DOWN        -> AetherionColors.Offline
        DeviceStatus.DEGRADED    -> AetherionColors.Degraded
        DeviceStatus.MAINTENANCE -> AetherionColors.Maintenance
        DeviceStatus.UNKNOWN     -> AetherionColors.Info
    }

    Canvas(
        modifier = modifier
            .clipToBounds()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0D1526),
                        AetherionColors.SurfaceDark
                    )
                )
            )
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.2f, 6f)
                    offset += pan
                }
            }
            .pointerInput(graph.nodes, scale, offset) {
                detectTapGestures { tapOffset ->
                    // Hit-test each node
                    graph.nodes.forEach { node ->
                        val pos = nodePositions[node.nodeId] ?: return@forEach
                        val nodeScreen = Offset(pos.x * scale + offset.x, pos.y * scale + offset.y)
                        if ((tapOffset - nodeScreen).getDistance() < 26f * scale) {
                            onNodeTap(node)
                            return@detectTapGestures
                        }
                    }
                }
            }
    ) {
        // ── Draw edges ─────────────────────────────────────────────
        graph.edges.forEach { edge ->
            val src = nodePositions[edge.sourceId] ?: return@forEach
            val dst = nodePositions[edge.targetId]  ?: return@forEach
            val p1  = Offset(src.x * scale + offset.x, src.y * scale + offset.y)
            val p2  = Offset(dst.x * scale + offset.x, dst.y * scale + offset.y)

            drawLine(
                color       = linkColor(edge.linkStatus),
                start       = p1,
                end         = p2,
                strokeWidth = (if (edge.linkStatus == LinkStatus.DOWN) 1.5f else 2.5f) * scale.coerceAtMost(2f),
                pathEffect  = if (edge.linkStatus == LinkStatus.DOWN)
                    PathEffect.dashPathEffect(floatArrayOf(10f, 10f)) else null
            )
        }

        // ── Draw nodes ─────────────────────────────────────────────
        graph.nodes.forEach { node ->
            val pos    = nodePositions[node.nodeId] ?: return@forEach
            val center = Offset(pos.x * scale + offset.x, pos.y * scale + offset.y)
            val radius = 16f * scale.coerceIn(0.5f, 2f)
            val color  = nodeColor(node)
            val isSelected = node.nodeId == selectedNode?.nodeId

            // Glow ring for selected / critical
            if (isSelected || node.status == DeviceStatus.DOWN) {
                drawCircle(
                    color  = color.copy(alpha = 0.25f),
                    radius = radius + 8f * scale.coerceIn(0.5f, 2f),
                    center = center
                )
            }

            // Node fill
            drawCircle(color = color.copy(alpha = 0.15f), radius = radius, center = center)

            // Node border
            drawCircle(
                color  = color,
                radius = radius,
                center = center,
                style  = Stroke(width = (if (isSelected) 3f else 1.5f) * scale.coerceIn(0.5f, 2f))
            )

            // Node label (only if zoomed in enough)
            if (scale > 0.5f) {
                val labelStyle = TextStyle(
                    color      = AetherionColors.TextSecondary,
                    fontSize   = (9 * scale.coerceIn(0.6f, 1.5f)).sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                val measured = textMeasurer.measure(
                    text  = node.label.take(16),
                    style = labelStyle
                )
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(
                        center.x - measured.size.width / 2f,
                        center.y + radius + 4f
                    )
                )
            }
        }
    }
}

// ─── Node Detail Panel ────────────────────────────────────────────────────────

@Composable
private fun NodeDetailPanel(
    node: TopologyNode,
    onDismiss: () -> Unit,
    onNavigate: () -> Unit,
    modifier: Modifier
) {
    val statusColor = node.status.toColor()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = AetherionColors.SurfaceElevated),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(statusColor, shape = MaterialTheme.shapes.small)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        node.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = AetherionColors.TextPrimary
                    )
                    Text(
                        "${node.type} · ${node.status.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AetherionColors.TextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, null, tint = AetherionColors.TextSecondary)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Health score bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Health",
                    style = MaterialTheme.typography.bodySmall,
                    color = AetherionColors.TextSecondary,
                    modifier = Modifier.width(56.dp)
                )
                LinearProgressIndicator(
                    progress = { node.healthScore / 100f },
                    modifier = Modifier.weight(1f).height(6.dp),
                    color = node.healthScore.toHealthColor(),
                    trackColor = AetherionColors.SurfaceBorder
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${node.healthScore.toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = node.healthScore.toHealthColor()
                )
            }

            // Metadata rows
            if (node.metadata.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                node.metadata.entries.take(3).forEach { (k, v) ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(k, style = MaterialTheme.typography.bodySmall,
                            color = AetherionColors.TextSecondary, modifier = Modifier.weight(1f))
                        Text(v, style = MaterialTheme.typography.bodySmall,
                            color = AetherionColors.TextPrimary)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onNavigate,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AetherionColors.AetherBlue)
            ) {
                Icon(Icons.Outlined.OpenInNew, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("View Device Detail", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// ─── Legend ───────────────────────────────────────────────────────────────────

@Composable
private fun TopologyLegend(modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = AetherionColors.SurfaceCard.copy(alpha = 0.9f)
        )
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LegendItem("Up",          AetherionColors.Online)
            LegendItem("Degraded",    AetherionColors.Degraded)
            LegendItem("Down",        AetherionColors.Offline)
            LegendItem("Maintenance", AetherionColors.Maintenance)
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, MaterialTheme.shapes.extraSmall))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = AetherionColors.TextSecondary)
    }
}

private fun DeviceStatus.toColor(): Color = when (this) {
    DeviceStatus.UP          -> AetherionColors.Online
    DeviceStatus.DOWN        -> AetherionColors.Offline
    DeviceStatus.DEGRADED    -> AetherionColors.Degraded
    DeviceStatus.MAINTENANCE -> AetherionColors.Maintenance
    DeviceStatus.UNKNOWN     -> AetherionColors.Info
}
