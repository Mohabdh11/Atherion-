package com.aetherion.noc.domain.model

import java.time.Instant

// ═══════════════════════════════════════════════════════════════════════════
// Aetherion Mobile NOC — Domain Models
// Developer: Mohammad Abdalftah Ibrahime
// Pure domain layer — no framework dependencies.
// ═══════════════════════════════════════════════════════════════════════════

// ─── Authentication ──────────────────────────────────────────────────────────

data class AuthCredentials(
    val username: String,
    val password: String,
    val tenantId: String,
    val mfaCode: String? = null
)

data class AuthToken(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,   // epoch millis
    val tokenType: String = "Bearer"
) {
    val isExpired: Boolean get() = System.currentTimeMillis() >= expiresAt
    val expiresIn: Long get() = expiresAt - System.currentTimeMillis()
}

data class UserProfile(
    val userId: String,
    val username: String,
    val email: String,
    val tenantId: String,
    val roles: List<String>,
    val permissions: List<String>
) {
    fun hasRole(role: String): Boolean = roles.contains(role)
    fun hasPermission(permission: String): Boolean = permissions.contains(permission)
    fun isAdmin(): Boolean = hasRole("admin") || hasRole("super_admin")
    fun isOperator(): Boolean = hasRole("operator") || isAdmin()
    fun isViewer(): Boolean = hasRole("viewer") || isOperator()
}

// ─── Dashboard ────────────────────────────────────────────────────────────────

data class DashboardSummary(
    val tenantId: String,
    val healthScore: Float,            // 0.0 – 100.0
    val devicesOnline: Int,
    val devicesOffline: Int,
    val devicesTotal: Int,
    val criticalAlerts: Int,
    val majorAlerts: Int,
    val minorAlerts: Int,
    val warningAlerts: Int,
    val slaCompliance: Float,          // percentage
    val aiInsightSummary: String,
    val lastUpdatedAt: Instant,
    val networkRegions: List<RegionHealth>
) {
    val totalAlerts: Int get() = criticalAlerts + majorAlerts + minorAlerts + warningAlerts
    val devicesOnlinePercent: Float
        get() = if (devicesTotal > 0) devicesOnline.toFloat() / devicesTotal * 100f else 0f
}

data class RegionHealth(
    val regionId: String,
    val regionName: String,
    val healthScore: Float,
    val status: NetworkStatus
)

enum class NetworkStatus { HEALTHY, DEGRADED, CRITICAL, UNKNOWN }

// ─── Alerts ──────────────────────────────────────────────────────────────────

data class Alert(
    val alertId: String,
    val severity: AlertSeverity,
    val status: AlertStatus,
    val title: String,
    val description: String,
    val deviceId: String,
    val deviceName: String,
    val regionId: String,
    val regionName: String,
    val rootCause: String?,
    val recommendedAction: String?,
    val raisedAt: Instant,
    val acknowledgedAt: Instant?,
    val acknowledgedBy: String?,
    val resolvedAt: Instant?,
    val tenantId: String
)

enum class AlertSeverity(val priority: Int) {
    CRITICAL(1), MAJOR(2), MINOR(3), WARNING(4), INFO(5)
}

enum class AlertStatus { RAISED, ACKNOWLEDGED, ESCALATED, RESOLVED, CLEARED }

// ─── Devices ─────────────────────────────────────────────────────────────────

data class Device(
    val deviceId: String,
    val hostname: String,
    val ipAddress: String,
    val vendor: String,
    val model: String,
    val platform: String,
    val status: DeviceStatus,
    val region: String,
    val site: String,
    val uptime: Long,              // seconds
    val cpuUsage: Float,           // 0.0 – 100.0
    val memoryUsage: Float,        // 0.0 – 100.0
    val interfaces: List<DeviceInterface>,
    val lastSeenAt: Instant,
    val tenantId: String
)

data class DeviceInterface(
    val name: String,
    val status: InterfaceStatus,
    val speed: Long,               // bps
    val inBps: Long,
    val outBps: Long,
    val errorRate: Float
)

enum class DeviceStatus { UP, DOWN, DEGRADED, UNKNOWN, MAINTENANCE }
enum class InterfaceStatus { UP, DOWN, ADMIN_DOWN, TESTING }

data class MetricPoint(
    val timestamp: Instant,
    val value: Float
)

data class DeviceMetrics(
    val deviceId: String,
    val cpuHistory: List<MetricPoint>,
    val memoryHistory: List<MetricPoint>,
    val trafficInHistory: List<MetricPoint>,
    val trafficOutHistory: List<MetricPoint>
)

// ─── Topology ────────────────────────────────────────────────────────────────

data class TopologyGraph(
    val nodes: List<TopologyNode>,
    val edges: List<TopologyEdge>,
    val version: Long,
    val generatedAt: Instant
)

data class TopologyNode(
    val nodeId: String,
    val label: String,
    val type: String,
    val status: DeviceStatus,
    val healthScore: Float,
    val latitude: Double?,
    val longitude: Double?,
    val x: Float,
    val y: Float,
    val metadata: Map<String, String>
)

data class TopologyEdge(
    val edgeId: String,
    val sourceId: String,
    val targetId: String,
    val linkStatus: LinkStatus,
    val bandwidth: Long,
    val utilization: Float
)

enum class LinkStatus { UP, DOWN, DEGRADED }

// ─── AI Insights ─────────────────────────────────────────────────────────────

data class AiInsight(
    val insightId: String,
    val type: InsightType,
    val title: String,
    val description: String,
    val affectedDeviceId: String?,
    val affectedDeviceName: String?,
    val riskScore: Float,          // 0.0 – 1.0
    val confidence: Float,         // 0.0 – 1.0
    val recommendation: String,
    val predictedAt: Instant,
    val predictedFailureAt: Instant?,
    val tenantId: String
)

enum class InsightType { ANOMALY, PREDICTED_FAILURE, CAPACITY_WARNING, PERFORMANCE_DEGRADATION, SECURITY_THREAT }

// ─── Geo ─────────────────────────────────────────────────────────────────────

data class GeoStatus(
    val faults: List<GeoFault>,
    val congestionZones: List<CongestionZone>,
    val regionSummaries: List<GeoRegionSummary>
)

data class GeoFault(
    val faultId: String,
    val latitude: Double,
    val longitude: Double,
    val severity: AlertSeverity,
    val description: String,
    val affectedUsers: Int,
    val detectedAt: Instant
)

data class CongestionZone(
    val zoneId: String,
    val centerLat: Double,
    val centerLng: Double,
    val radius: Float,             // km
    val utilizationPercent: Float
)

data class GeoRegionSummary(
    val regionId: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val healthScore: Float,
    val alertCount: Int
)

// ─── Common Result Wrapper ───────────────────────────────────────────────────

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable, val message: String? = null) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}

fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

fun <T> Result<T>.onError(action: (Throwable, String?) -> Unit): Result<T> {
    if (this is Result.Error) action(exception, message)
    return this
}
