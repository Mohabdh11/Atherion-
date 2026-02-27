package com.aetherion.noc.domain.repository

import androidx.paging.PagingData
import com.aetherion.noc.domain.model.*
import kotlinx.coroutines.flow.Flow

// ═══════════════════════════════════════════════════════════════════════════
// Aetherion Mobile NOC — Domain Repository Interfaces
// Developer: Mohammad Abdalftah Ibrahime
// Clean Architecture: Domain defines contracts, Data implements them.
// ═══════════════════════════════════════════════════════════════════════════

interface AuthRepository {
    suspend fun login(credentials: AuthCredentials): Result<AuthToken>
    suspend fun refreshToken(): Result<AuthToken>
    suspend fun logout()
    suspend fun getStoredToken(): AuthToken?
    suspend fun getCurrentUser(): Result<UserProfile>
    fun isAuthenticated(): Boolean
    fun observeAuthState(): Flow<Boolean>
}

interface DashboardRepository {
    /** Emits cached + live dashboard summary for a tenant. */
    fun observeDashboard(tenantId: String): Flow<Result<DashboardSummary>>
    suspend fun refreshDashboard(tenantId: String): Result<DashboardSummary>
}

interface AlertRepository {
    /** Paginated alert stream with optional filters. */
    fun getAlertsPaged(
        tenantId: String,
        severity: List<AlertSeverity>?,
        regionId: String?,
        status: AlertStatus?
    ): Flow<PagingData<Alert>>

    /** Real-time alert stream via WebSocket. */
    fun observeLiveAlerts(tenantId: String): Flow<Alert>

    suspend fun acknowledgeAlert(alertId: String): Result<Unit>
    suspend fun escalateAlert(alertId: String, note: String): Result<Unit>
}

interface DeviceRepository {
    suspend fun getDevices(tenantId: String, regionId: String?): Result<List<Device>>
    suspend fun getDevice(deviceId: String): Result<Device>
    suspend fun getDeviceMetrics(
        deviceId: String,
        range: MetricRange
    ): Result<DeviceMetrics>
}

enum class MetricRange { H24, D7, D30 }

interface TopologyRepository {
    suspend fun getTopology(tenantId: String): Result<TopologyGraph>
    fun observeTopologyUpdates(tenantId: String): Flow<TopologyGraph>
}

interface AiInsightRepository {
    fun getInsightsPaged(tenantId: String): Flow<PagingData<AiInsight>>
    suspend fun getLatestInsights(tenantId: String, limit: Int = 10): Result<List<AiInsight>>
}

interface GeoRepository {
    suspend fun getGeoStatus(tenantId: String): Result<GeoStatus>
    fun observeGeoUpdates(tenantId: String): Flow<GeoStatus>
}
