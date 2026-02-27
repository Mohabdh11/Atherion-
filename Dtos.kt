package com.aetherion.noc.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ═══════════════════════════════════════════════════════════════════════════
// Aetherion Mobile NOC — Remote Data Transfer Objects
// Developer: Mohammad Abdalftah Ibrahime
// Aligned with Aetherion v8 REST API response schemas.
// ═══════════════════════════════════════════════════════════════════════════

// ─── Auth ─────────────────────────────────────────────────────────────────────

@Serializable
data class LoginRequestDto(
    val username: String,
    val password: String,
    @SerialName("tenant_id") val tenantId: String,
    @SerialName("mfa_code") val mfaCode: String? = null
)

@Serializable
data class LoginResponseDto(
    @SerialName("access_token")  val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in")    val expiresIn: Long,      // seconds
    @SerialName("token_type")    val tokenType: String = "Bearer",
    val role: String? = null,
    @SerialName("tenant_id")     val tenantId: String? = null
)

@Serializable
data class UserMeDto(
    @SerialName("user_id")    val userId: String,
    val username: String,
    val email: String,
    @SerialName("tenant_id") val tenantId: String,
    val roles: List<String>,
    val permissions: List<String> = emptyList()
)

// ─── Dashboard ────────────────────────────────────────────────────────────────

@Serializable
data class DashboardSummaryDto(
    @SerialName("tenant_id")          val tenantId: String,
    @SerialName("health_score")       val healthScore: Float,
    @SerialName("devices_online")     val devicesOnline: Int,
    @SerialName("devices_offline")    val devicesOffline: Int,
    @SerialName("devices_total")      val devicesTotal: Int,
    @SerialName("critical_alerts")    val criticalAlerts: Int,
    @SerialName("major_alerts")       val majorAlerts: Int,
    @SerialName("minor_alerts")       val minorAlerts: Int,
    @SerialName("warning_alerts")     val warningAlerts: Int,
    @SerialName("sla_compliance")     val slaCompliance: Float,
    @SerialName("ai_summary")         val aiSummary: String,
    @SerialName("last_updated")       val lastUpdated: String,
    val regions: List<RegionHealthDto> = emptyList()
)

@Serializable
data class RegionHealthDto(
    @SerialName("region_id")   val regionId: String,
    @SerialName("region_name") val regionName: String,
    @SerialName("health_score") val healthScore: Float,
    val status: String
)

// ─── Alerts ───────────────────────────────────────────────────────────────────

@Serializable
data class AlertsResponseDto(
    val items: List<AlertDto>,
    val total: Int,
    val page: Int,
    @SerialName("page_size") val pageSize: Int,
    @SerialName("next_cursor") val nextCursor: String? = null
)

@Serializable
data class AlertDto(
    @SerialName("alarm_id")           val alertId: String,
    val severity: String,
    val status: String,
    val title: String,
    val description: String,
    @SerialName("device_id")          val deviceId: String,
    @SerialName("device_name")        val deviceName: String,
    @SerialName("region_id")          val regionId: String,
    @SerialName("region_name")        val regionName: String,
    @SerialName("root_cause")         val rootCause: String? = null,
    @SerialName("recommended_action") val recommendedAction: String? = null,
    @SerialName("raised_at")          val raisedAt: String,
    @SerialName("acknowledged_at")    val acknowledgedAt: String? = null,
    @SerialName("acknowledged_by")    val acknowledgedBy: String? = null,
    @SerialName("resolved_at")        val resolvedAt: String? = null,
    @SerialName("tenant_id")          val tenantId: String
)

// ─── Devices ──────────────────────────────────────────────────────────────────

@Serializable
data class DevicesResponseDto(
    val devices: List<DeviceDto>,
    val total: Int
)

@Serializable
data class DeviceDto(
    @SerialName("node_id")     val deviceId: String,
    val hostname: String,
    @SerialName("ip_address")  val ipAddress: String,
    val vendor: String,
    val model: String,
    val platform: String,
    val status: String,
    val region: String,
    val site: String,
    val uptime: Long = 0L,
    @SerialName("cpu_usage")    val cpuUsage: Float = 0f,
    @SerialName("memory_usage") val memoryUsage: Float = 0f,
    val interfaces: List<InterfaceDto> = emptyList(),
    @SerialName("last_seen")   val lastSeen: String,
    @SerialName("tenant_id")   val tenantId: String
)

@Serializable
data class InterfaceDto(
    val name: String,
    val status: String,
    @SerialName("speed_bps") val speed: Long = 0L,
    @SerialName("in_bps")    val inBps: Long = 0L,
    @SerialName("out_bps")   val outBps: Long = 0L,
    @SerialName("error_rate") val errorRate: Float = 0f
)

@Serializable
data class MetricSeriesDto(
    @SerialName("device_id") val deviceId: String,
    val cpu: List<MetricPointDto> = emptyList(),
    val memory: List<MetricPointDto> = emptyList(),
    @SerialName("traffic_in") val trafficIn: List<MetricPointDto> = emptyList(),
    @SerialName("traffic_out") val trafficOut: List<MetricPointDto> = emptyList()
)

@Serializable
data class MetricPointDto(
    val ts: String,
    val value: Float
)

// ─── Topology ─────────────────────────────────────────────────────────────────

@Serializable
data class TopologyResponseDto(
    val nodes: List<TopologyNodeDto>,
    val edges: List<TopologyEdgeDto>,
    val version: Long,
    @SerialName("generated_at") val generatedAt: String
)

@Serializable
data class TopologyNodeDto(
    @SerialName("node_id")     val nodeId: String,
    val label: String,
    val type: String,
    val status: String,
    @SerialName("health_score") val healthScore: Float = 100f,
    val lat: Double? = null,
    val lng: Double? = null,
    val x: Float = 0f,
    val y: Float = 0f,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
data class TopologyEdgeDto(
    @SerialName("edge_id")    val edgeId: String,
    val source: String,
    val target: String,
    @SerialName("link_status") val linkStatus: String,
    val bandwidth: Long = 0L,
    val utilization: Float = 0f
)

// ─── AI Insights ──────────────────────────────────────────────────────────────

@Serializable
data class InsightsResponseDto(
    val insights: List<AiInsightDto>,
    val total: Int
)

@Serializable
data class AiInsightDto(
    @SerialName("insight_id")           val insightId: String,
    val type: String,
    val title: String,
    val description: String,
    @SerialName("affected_device_id")   val affectedDeviceId: String? = null,
    @SerialName("affected_device_name") val affectedDeviceName: String? = null,
    @SerialName("risk_score")           val riskScore: Float,
    val confidence: Float,
    val recommendation: String,
    @SerialName("predicted_at")         val predictedAt: String,
    @SerialName("predicted_failure_at") val predictedFailureAt: String? = null,
    @SerialName("tenant_id")            val tenantId: String
)

// ─── Geo ──────────────────────────────────────────────────────────────────────

@Serializable
data class GeoStatusDto(
    val faults: List<GeoFaultDto>,
    @SerialName("congestion_zones") val congestionZones: List<CongestionZoneDto>,
    val regions: List<GeoRegionDto>
)

@Serializable
data class GeoFaultDto(
    @SerialName("fault_id")       val faultId: String,
    val lat: Double,
    val lng: Double,
    val severity: String,
    val description: String,
    @SerialName("affected_users") val affectedUsers: Int = 0,
    @SerialName("detected_at")    val detectedAt: String
)

@Serializable
data class CongestionZoneDto(
    @SerialName("zone_id")      val zoneId: String,
    @SerialName("center_lat")   val centerLat: Double,
    @SerialName("center_lng")   val centerLng: Double,
    val radius: Float,
    val utilization: Float
)

@Serializable
data class GeoRegionDto(
    @SerialName("region_id")    val regionId: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    @SerialName("health_score") val healthScore: Float,
    @SerialName("alert_count")  val alertCount: Int
)

// ─── Common ───────────────────────────────────────────────────────────────────

@Serializable
data class ApiErrorDto(
    val detail: String,
    val code: String? = null,
    val violations: List<String> = emptyList()
)
