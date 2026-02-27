package com.aetherion.noc.domain.usecase

import androidx.paging.PagingData
import com.aetherion.noc.domain.model.*
import com.aetherion.noc.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

// ═══════════════════════════════════════════════════════════════════════════
// Aetherion Mobile NOC — Use Cases (Domain Business Logic)
// Developer: Mohammad Abdalftah Ibrahime
// One use case = one operation = one class.
// ═══════════════════════════════════════════════════════════════════════════

// ─── Auth Use Cases ───────────────────────────────────────────────────────────

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(credentials: AuthCredentials): Result<AuthToken> {
        // Validate credentials before hitting network
        if (credentials.username.isBlank()) return Result.Error(
            IllegalArgumentException("Username cannot be empty")
        )
        if (credentials.password.length < 8) return Result.Error(
            IllegalArgumentException("Invalid credentials")
        )
        if (credentials.tenantId.isBlank()) return Result.Error(
            IllegalArgumentException("Tenant ID is required")
        )
        return authRepository.login(credentials)
    }
}

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke() = authRepository.logout()
}

class GetCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<UserProfile> = authRepository.getCurrentUser()
}

class ObserveAuthStateUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<Boolean> = authRepository.observeAuthState()
}

// ─── Dashboard Use Cases ──────────────────────────────────────────────────────

class ObserveDashboardUseCase @Inject constructor(
    private val dashboardRepository: DashboardRepository
) {
    operator fun invoke(tenantId: String): Flow<Result<DashboardSummary>> =
        dashboardRepository.observeDashboard(tenantId)
}

class RefreshDashboardUseCase @Inject constructor(
    private val dashboardRepository: DashboardRepository
) {
    suspend operator fun invoke(tenantId: String): Result<DashboardSummary> =
        dashboardRepository.refreshDashboard(tenantId)
}

// ─── Alert Use Cases ──────────────────────────────────────────────────────────

class GetPagedAlertsUseCase @Inject constructor(
    private val alertRepository: AlertRepository
) {
    operator fun invoke(
        tenantId: String,
        severity: List<AlertSeverity>? = null,
        regionId: String? = null,
        status: AlertStatus? = null
    ): Flow<PagingData<Alert>> =
        alertRepository.getAlertsPaged(tenantId, severity, regionId, status)
}

class ObserveLiveAlertsUseCase @Inject constructor(
    private val alertRepository: AlertRepository
) {
    operator fun invoke(tenantId: String): Flow<Alert> =
        alertRepository.observeLiveAlerts(tenantId)
}

class AcknowledgeAlertUseCase @Inject constructor(
    private val alertRepository: AlertRepository
) {
    suspend operator fun invoke(alertId: String): Result<Unit> =
        alertRepository.acknowledgeAlert(alertId)
}

class EscalateAlertUseCase @Inject constructor(
    private val alertRepository: AlertRepository
) {
    suspend operator fun invoke(alertId: String, note: String): Result<Unit> {
        if (note.isBlank()) return Result.Error(IllegalArgumentException("Escalation note is required"))
        return alertRepository.escalateAlert(alertId, note)
    }
}

// ─── Device Use Cases ─────────────────────────────────────────────────────────

class GetDevicesUseCase @Inject constructor(
    private val deviceRepository: DeviceRepository
) {
    suspend operator fun invoke(tenantId: String, regionId: String? = null): Result<List<Device>> =
        deviceRepository.getDevices(tenantId, regionId)
}

class GetDeviceDetailUseCase @Inject constructor(
    private val deviceRepository: DeviceRepository
) {
    suspend operator fun invoke(deviceId: String): Result<Device> =
        deviceRepository.getDevice(deviceId)
}

class GetDeviceMetricsUseCase @Inject constructor(
    private val deviceRepository: DeviceRepository
) {
    suspend operator fun invoke(deviceId: String, range: MetricRange): Result<DeviceMetrics> =
        deviceRepository.getDeviceMetrics(deviceId, range)
}

// ─── Topology Use Cases ───────────────────────────────────────────────────────

class GetTopologyUseCase @Inject constructor(
    private val topologyRepository: TopologyRepository
) {
    suspend operator fun invoke(tenantId: String): Result<TopologyGraph> =
        topologyRepository.getTopology(tenantId)
}

class ObserveTopologyUseCase @Inject constructor(
    private val topologyRepository: TopologyRepository
) {
    operator fun invoke(tenantId: String): Flow<TopologyGraph> =
        topologyRepository.observeTopologyUpdates(tenantId)
}

// ─── AI Insights Use Cases ───────────────────────────────────────────────────

class GetPagedInsightsUseCase @Inject constructor(
    private val aiInsightRepository: AiInsightRepository
) {
    operator fun invoke(tenantId: String): Flow<PagingData<AiInsight>> =
        aiInsightRepository.getInsightsPaged(tenantId)
}

class GetLatestInsightsUseCase @Inject constructor(
    private val aiInsightRepository: AiInsightRepository
) {
    suspend operator fun invoke(tenantId: String, limit: Int = 10): Result<List<AiInsight>> =
        aiInsightRepository.getLatestInsights(tenantId, limit)
}

// ─── Geo Use Cases ────────────────────────────────────────────────────────────

class GetGeoStatusUseCase @Inject constructor(
    private val geoRepository: GeoRepository
) {
    suspend operator fun invoke(tenantId: String): Result<GeoStatus> =
        geoRepository.getGeoStatus(tenantId)
}

class ObserveGeoUpdatesUseCase @Inject constructor(
    private val geoRepository: GeoRepository
) {
    operator fun invoke(tenantId: String): Flow<GeoStatus> =
        geoRepository.observeGeoUpdates(tenantId)
}
