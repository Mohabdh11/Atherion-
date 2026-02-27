package com.aetherion.noc.presentation.device

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aetherion.noc.domain.model.*
import com.aetherion.noc.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ═══════════════════════════════════════════════════════════════════════════
// Aetherion Mobile NOC — Device Detail ViewModel + Screen
// Developer: Mohammad Abdalftah Ibrahime
// ═══════════════════════════════════════════════════════════════════════════

@HiltViewModel
class DeviceDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getDeviceDetailUseCase: GetDeviceDetailUseCase,
    private val getDeviceMetricsUseCase: GetDeviceMetricsUseCase
) : ViewModel() {

    private val deviceId: String = checkNotNull(savedStateHandle["deviceId"])

    private val _device = MutableStateFlow<Result<Device>>(Result.Loading)
    val device: StateFlow<Result<Device>> = _device.asStateFlow()

    private val _metrics = MutableStateFlow<Result<DeviceMetrics>?>(null)
    val metrics: StateFlow<Result<DeviceMetrics>?> = _metrics.asStateFlow()

    private val _selectedRange = MutableStateFlow(MetricRange.H24)
    val selectedRange: StateFlow<MetricRange> = _selectedRange.asStateFlow()

    init {
        loadDevice()
        loadMetrics()
    }

    private fun loadDevice() = viewModelScope.launch {
        _device.value = Result.Loading
        _device.value = getDeviceDetailUseCase(deviceId)
    }

    private fun loadMetrics() {
        viewModelScope.launch {
            _selectedRange.collect { range ->
                _metrics.value = null
                _metrics.value = getDeviceMetricsUseCase(deviceId, range)
            }
        }
    }

    fun selectRange(range: MetricRange) { _selectedRange.value = range }
    fun refresh() { loadDevice(); loadMetrics() }
}

// ─── Screen ───────────────────────────────────────────────────────────────────

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aetherion.noc.presentation.common.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(
    deviceId: String,
    onBack: () -> Unit,
    viewModel: DeviceDetailViewModel = hiltViewModel()
) {
    val deviceResult  by viewModel.device.collectAsStateWithLifecycle()
    val metricsResult by viewModel.metrics.collectAsStateWithLifecycle()
    val selectedRange by viewModel.selectedRange.collectAsStateWithLifecycle()

    AetherionNOCTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        val name = (deviceResult as? Result.Success)?.data?.hostname ?: "Device"
                        Text(name, color = AetherionColors.TextPrimary)
                    },
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
            when (val state = deviceResult) {
                is Result.Loading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                    CircularProgressIndicator(color = AetherionColors.AetherBlue)
                }
                is Result.Error -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                    Text(state.message ?: "Failed to load device", color = AetherionColors.Critical)
                }
                is Result.Success -> DeviceDetailContent(
                    device = state.data,
                    metricsResult = metricsResult,
                    selectedRange = selectedRange,
                    onRangeSelect = viewModel::selectRange,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun DeviceDetailContent(
    device: Device,
    metricsResult: Result<DeviceMetrics>?,
    selectedRange: MetricRange,
    onRangeSelect: (MetricRange) -> Unit,
    modifier: Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Status Header ──────────────────────────────────────────
        item { DeviceStatusHeader(device) }

        // ── Metadata Card ──────────────────────────────────────────
        item { DeviceMetadataCard(device) }

        // ── CPU / Memory Gauges ────────────────────────────────────
        item { DeviceResourceGauges(device) }

        // ── Interfaces ─────────────────────────────────────────────
        item {
            SectionCard("Interfaces") {
                device.interfaces.forEach { iface ->
                    InterfaceRow(iface)
                    if (device.interfaces.last() != iface) {
                        HorizontalDivider(color = AetherionColors.SurfaceBorder, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
                if (device.interfaces.isEmpty()) {
                    Text("No interface data", style = MaterialTheme.typography.bodySmall,
                         color = AetherionColors.TextSecondary)
                }
            }
        }

        // ── Metrics Range Selector ─────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricRange.values().forEach { range ->
                    FilterChip(
                        selected = selectedRange == range,
                        onClick = { onRangeSelect(range) },
                        label = { Text(range.label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AetherionColors.AetherBlueDark,
                            selectedLabelColor = AetherionColors.TextPrimary,
                            containerColor = AetherionColors.SurfaceCard,
                            labelColor = AetherionColors.TextSecondary
                        )
                    )
                }
            }
        }

        // ── Historical Metrics ─────────────────────────────────────
        item {
            when (metricsResult) {
                null                  -> Box(Modifier.fillMaxWidth().height(150.dp), Alignment.Center) {
                    CircularProgressIndicator(color = AetherionColors.AetherBlue, modifier = Modifier.size(24.dp))
                }
                is Result.Success     -> MetricsChartCard(metricsResult.data)
                is Result.Error       -> Text("Metrics unavailable", color = AetherionColors.TextMuted,
                                              style = MaterialTheme.typography.bodySmall)
                is Result.Loading     -> CircularProgressIndicator()
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun DeviceStatusHeader(device: Device) {
    val statusColor = device.status.toColor()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(device.hostname, style = MaterialTheme.typography.headlineSmall,
                 color = AetherionColors.TextPrimary)
            Text(device.ipAddress, style = MaterialTheme.typography.bodyMedium,
                 color = AetherionColors.TextSecondary)
        }
        Box(
            modifier = Modifier
                .background(statusColor.copy(alpha = 0.15f), MaterialTheme.shapes.medium)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(device.status.name, style = MaterialTheme.typography.labelMedium, color = statusColor)
        }
    }
}

@Composable
private fun DeviceMetadataCard(device: Device) {
    SectionCard("Device Information") {
        MetadataRow("Vendor", device.vendor)
        MetadataRow("Model", device.model)
        MetadataRow("Platform", device.platform)
        MetadataRow("Region", device.region)
        MetadataRow("Site", device.site)
        MetadataRow("Uptime", formatUptime(device.uptime))
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall,
             color = AetherionColors.TextSecondary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = AetherionColors.TextPrimary)
    }
}

@Composable
private fun DeviceResourceGauges(device: Device) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ResourceGauge("CPU", device.cpuUsage, AetherionColors.ChartCpu, Modifier.weight(1f))
        ResourceGauge("Memory", device.memoryUsage, AetherionColors.ChartMemory, Modifier.weight(1f))
    }
}

@Composable
private fun ResourceGauge(label: String, value: Float, color: Color, modifier: Modifier) {
    val gaugeColor = when {
        value > 90f -> AetherionColors.Critical
        value > 75f -> AetherionColors.Major
        else        -> color
    }
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = AetherionColors.SurfaceCard)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = AetherionColors.TextSecondary)
            Spacer(Modifier.height(8.dp))
            Text("${value.toInt()}%", style = MaterialTheme.typography.headlineMedium, color = gaugeColor)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { value / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = gaugeColor,
                trackColor = AetherionColors.SurfaceBorder
            )
        }
    }
}

@Composable
private fun InterfaceRow(iface: DeviceInterface) {
    val color = when (iface.status) {
        InterfaceStatus.UP         -> AetherionColors.Online
        InterfaceStatus.DOWN       -> AetherionColors.Offline
        InterfaceStatus.ADMIN_DOWN -> AetherionColors.Info
        InterfaceStatus.TESTING    -> AetherionColors.Warning
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, MaterialTheme.shapes.extraSmall))
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(iface.name, style = MaterialTheme.typography.bodySmall, color = AetherionColors.TextPrimary)
            Text("${formatBps(iface.inBps)} ↓ / ${formatBps(iface.outBps)} ↑",
                 style = MaterialTheme.typography.bodySmall, color = AetherionColors.TextSecondary)
        }
        Text(iface.status.name, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
private fun MetricsChartCard(metrics: DeviceMetrics) {
    // Simple bar chart using LinearProgressIndicator as sparkline substitute
    // Production: integrate with a charting library (e.g. Vico, MPAndroidChart)
    SectionCard("Historical Metrics") {
        if (metrics.cpuHistory.isNotEmpty()) {
            Text("CPU (%)", style = MaterialTheme.typography.labelSmall, color = AetherionColors.ChartCpu)
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                metrics.cpuHistory.takeLast(24).forEach { point ->
                    LinearProgressIndicator(
                        progress = { (point.value / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.weight(1f).height(40.dp),
                        color = AetherionColors.ChartCpu,
                        trackColor = AetherionColors.SurfaceBorder
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        if (metrics.memoryHistory.isNotEmpty()) {
            Text("Memory (%)", style = MaterialTheme.typography.labelSmall, color = AetherionColors.ChartMemory)
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                metrics.memoryHistory.takeLast(24).forEach { point ->
                    LinearProgressIndicator(
                        progress = { (point.value / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.weight(1f).height(40.dp),
                        color = AetherionColors.ChartMemory,
                        trackColor = AetherionColors.SurfaceBorder
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AetherionColors.SurfaceCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelMedium,
                 color = AetherionColors.TextSecondary)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

private fun formatUptime(seconds: Long): String {
    val d = seconds / 86400
    val h = (seconds % 86400) / 3600
    val m = (seconds % 3600) / 60
    return "${d}d ${h}h ${m}m"
}

private fun formatBps(bps: Long): String = when {
    bps >= 1_000_000_000L -> "${"%.1f".format(bps / 1e9)}G"
    bps >= 1_000_000L     -> "${"%.1f".format(bps / 1e6)}M"
    bps >= 1_000L         -> "${"%.1f".format(bps / 1e3)}K"
    else                  -> "${bps}bps"
}

private val MetricRange.label get() = when (this) {
    MetricRange.H24 -> "24H"
    MetricRange.D7  -> "7D"
    MetricRange.D30 -> "30D"
}
