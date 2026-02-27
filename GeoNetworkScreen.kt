package com.aetherion.noc.presentation.geo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.aetherion.noc.core.security.SecurityManager
import com.aetherion.noc.domain.model.*
import com.aetherion.noc.domain.usecase.*
import com.aetherion.noc.presentation.common.theme.*
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ═══════════════════════════════════════════════════════════════════════════
// Aetherion Mobile NOC — Geo Network View
// Developer: Mohammad Abdalftah Ibrahime
// Map-based outage display, heatmap congestion, regional health.
// ═══════════════════════════════════════════════════════════════════════════

@HiltViewModel
class GeoViewModel @Inject constructor(
    private val getGeoStatusUseCase: GetGeoStatusUseCase,
    private val observeGeoUpdatesUseCase: ObserveGeoUpdatesUseCase,
    private val securityManager: SecurityManager
) : ViewModel() {

    private val tenantId: String get() = securityManager.getTenantId() ?: ""

    private val _geoState = MutableStateFlow<GeoUiState>(GeoUiState.Loading)
    val geoState: StateFlow<GeoUiState> = _geoState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadGeoStatus()
        observeUpdates()
    }

    private fun loadGeoStatus() = viewModelScope.launch {
        _geoState.value = GeoUiState.Loading
        when (val r = getGeoStatusUseCase(tenantId)) {
            is Result.Success -> _geoState.value = GeoUiState.Success(r.data)
            is Result.Error   -> _geoState.value = GeoUiState.Error(r.message ?: "Failed")
            else -> {}
        }
    }

    private fun observeUpdates() {
        observeGeoUpdatesUseCase(tenantId)
            .onEach { geo -> _geoState.value = GeoUiState.Success(geo) }
            .launchIn(viewModelScope)
    }

    fun refresh() = viewModelScope.launch {
        _isRefreshing.value = true
        try { loadGeoStatus() } finally { _isRefreshing.value = false }
    }
}

sealed class GeoUiState {
    data object Loading : GeoUiState()
    data class Success(val data: GeoStatus) : GeoUiState()
    data class Error(val message: String) : GeoUiState()
}

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeoNetworkScreen(
    viewModel: GeoViewModel = hiltViewModel()
) {
    val uiState      by viewModel.geoState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    // Map camera — start centered on a neutral position
    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(20.0, 30.0), 2f)
    }

    // Tab state: Map | Regional Summary
    var selectedTab by remember { mutableIntStateOf(0) }

    AetherionNOCTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Geo Network View", color = AetherionColors.TextPrimary) },
                    actions = {
                        IconButton(onClick = viewModel::refresh, enabled = !isRefreshing) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = AetherionColors.AetherBlue,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Outlined.Refresh, null, tint = AetherionColors.AetherBlue)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AetherionColors.SurfaceCard)
                )
            },
            containerColor = AetherionColors.SurfaceDark
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // Tab row
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = AetherionColors.SurfaceCard,
                    contentColor = AetherionColors.AetherBlue
                ) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                        Row(modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Map, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Map View", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                        Row(modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.List, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Regional", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                when (val state = uiState) {
                    is GeoUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(color = AetherionColors.AetherBlue)
                    }
                    is GeoUiState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.MapOff, null, tint = AetherionColors.Critical,
                                modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text(state.message, color = AetherionColors.TextPrimary)
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = viewModel::refresh) { Text("Retry") }
                        }
                    }
                    is GeoUiState.Success -> {
                        when (selectedTab) {
                            0 -> GeoMapView(
                                geoStatus = state.data,
                                cameraState = cameraState
                            )
                            1 -> RegionalSummaryList(state.data.regionSummaries)
                        }
                    }
                }
            }
        }
    }
}

// ─── Map View ────────────────────────────────────────────────────────────────

@Composable
private fun GeoMapView(
    geoStatus: GeoStatus,
    cameraState: CameraPositionState
) {
    val mapProperties = remember {
        MapProperties(
            mapType = MapType.NORMAL,
            isMyLocationEnabled = false,
            mapStyleOptions = MapStyleOptions(DARK_MAP_STYLE)
        )
    }
    val mapUiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = true,
            compassEnabled = true,
            myLocationButtonEnabled = false
        )
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraState,
        properties = mapProperties,
        uiSettings = mapUiSettings
    ) {
        // Fault markers
        geoStatus.faults.forEach { fault ->
            val markerColor = when (fault.severity) {
                AlertSeverity.CRITICAL -> BitmapDescriptorFactory.HUE_RED
                AlertSeverity.MAJOR    -> BitmapDescriptorFactory.HUE_ORANGE
                AlertSeverity.MINOR    -> BitmapDescriptorFactory.HUE_YELLOW
                else                   -> BitmapDescriptorFactory.HUE_CYAN
            }
            Marker(
                state = MarkerState(position = LatLng(fault.latitude, fault.longitude)),
                title = fault.severity.name,
                snippet = fault.description,
                icon = BitmapDescriptorFactory.defaultMarker(markerColor)
            )
        }

        // Congestion circles
        geoStatus.congestionZones.forEach { zone ->
            val alpha = (zone.utilizationPercent / 100f).coerceIn(0.2f, 0.7f)
            Circle(
                center = LatLng(zone.centerLat, zone.centerLng),
                radius = zone.radius * 1000.0,  // km → meters
                fillColor = Color(0xFFFF7A00).copy(alpha = alpha),
                strokeColor = AetherionColors.Major,
                strokeWidth = 2f
            )
        }

        // Region health markers
        geoStatus.regionSummaries.forEach { region ->
            val hue = when {
                region.healthScore >= 90f -> BitmapDescriptorFactory.HUE_GREEN
                region.healthScore >= 70f -> BitmapDescriptorFactory.HUE_YELLOW
                else                      -> BitmapDescriptorFactory.HUE_RED
            }
            Marker(
                state = MarkerState(position = LatLng(region.lat, region.lng)),
                title = region.name,
                snippet = "Health: ${region.healthScore.toInt()}% · Alerts: ${region.alertCount}",
                icon = BitmapDescriptorFactory.defaultMarker(hue),
                alpha = 0.85f
            )
        }
    }
}

// ─── Regional Summary List ────────────────────────────────────────────────────

@Composable
private fun RegionalSummaryList(regions: List<GeoRegionSummary>) {
    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(regions, key = { it.regionId }) { region ->
            RegionSummaryCard(region)
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun RegionSummaryCard(region: GeoRegionSummary) {
    val healthColor = region.healthScore.toHealthColor()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AetherionColors.SurfaceCard)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(region.name, style = MaterialTheme.typography.titleMedium,
                    color = AetherionColors.TextPrimary)
                Text(
                    "Lat: ${"%.2f".format(region.lat)}, Lng: ${"%.2f".format(region.lng)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AetherionColors.TextSecondary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${region.healthScore.toInt()}%",
                    style = MaterialTheme.typography.headlineSmall,
                    color = healthColor
                )
                if (region.alertCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Warning, null,
                            tint = AetherionColors.Major, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("${region.alertCount} alerts",
                            style = MaterialTheme.typography.labelSmall,
                            color = AetherionColors.Major)
                    }
                }
            }
        }
    }
}

// Dark map style JSON for Google Maps
private const val DARK_MAP_STYLE = """
[{"elementType":"geometry","stylers":[{"color":"#0A0F1E"}]},
{"elementType":"labels.icon","stylers":[{"visibility":"off"}]},
{"elementType":"labels.text.fill","stylers":[{"color":"#94A3B8"}]},
{"elementType":"labels.text.stroke","stylers":[{"color":"#0A0F1E"}]},
{"featureType":"administrative","elementType":"geometry","stylers":[{"color":"#1F2D40"}]},
{"featureType":"poi","stylers":[{"visibility":"off"}]},
{"featureType":"road","elementType":"geometry","stylers":[{"color":"#1A2332"}]},
{"featureType":"road","elementType":"geometry.stroke","stylers":[{"color":"#111827"}]},
{"featureType":"road.highway","elementType":"geometry","stylers":[{"color":"#1F2D40"}]},
{"featureType":"transit","stylers":[{"visibility":"off"}]},
{"featureType":"water","elementType":"geometry","stylers":[{"color":"#050D1A"}]},
{"featureType":"water","elementType":"labels.text.fill","stylers":[{"color":"#475569"}]}]
"""
