package com.aetherion.noc.data.remote.dto

import com.aetherion.noc.domain.model.*
import java.time.Instant
import java.time.format.DateTimeParseException

// ═══════════════════════════════════════════════════════════════════════════
// Aetherion Mobile NOC — DTO → Domain Model Mappers
// Developer: Mohammad Abdalftah Ibrahime
// ═══════════════════════════════════════════════════════════════════════════

private fun String?.toInstantOrNow(): Instant = try {
    this?.let { Instant.parse(it) } ?: Instant.now()
} catch (e: DateTimeParseException) {
    Instant.now()
}

private fun String?.toInstantOrNull(): Instant? = try {
    this?.let { Instant.parse(it) }
} catch (e: DateTimeParseException) {
    null
}

// ─── Auth ─────────────────────────────────────────────────────────────────────

fun LoginResponseDto.toDomain(): AuthToken = AuthToken(
    accessToken = accessToken,
    refreshToken = refreshToken,
    expiresAt = System.currentTimeMillis() + (expiresIn * 1000),
    tokenType = tokenType
)

fun UserMeDto.toDomain(): UserProfile = UserProfile(
    userId = userId,
    username = username,
    email = email,
    tenantId = tenantId,
    roles = roles,
    permissions = permissions
)

// ─── Dashboard ────────────────────────────────────────────────────────────────

fun DashboardSummaryDto.toDomain(): DashboardSummary = DashboardSummary(
    tenantId = tenantId,
    healthScore = healthScore,
    devicesOnline = devicesOnline,
    devicesOffline = devicesOffline,
    devicesTotal = devicesTotal,
    criticalAlerts = criticalAlerts,
    majorAlerts = majorAlerts,
    minorAlerts = minorAlerts,
    warningAlerts = warningAlerts,
    slaCompliance = slaCompliance,
    aiInsightSummary = aiSummary,
    lastUpdatedAt = lastUpdated.toInstantOrNow(),
    networkRegions = regions.map { it.toDomain() }
)

fun RegionHealthDto.toDomain(): RegionHealth = RegionHealth(
    regionId = regionId,
    regionName = regionName,
    healthScore = healthScore,
    status = when (status.lowercase()) {
        "healthy"  -> NetworkStatus.HEALTHY
        "degraded" -> NetworkStatus.DEGRADED
        "critical" -> NetworkStatus.CRITICAL
        else       -> NetworkStatus.UNKNOWN
    }
)

// ─── Alerts ───────────────────────────────────────────────────────────────────

fun AlertDto.toDomain(): Alert = Alert(
    alertId = alertId,
    severity = when (severity.lowercase()) {
        "critical" -> AlertSeverity.CRITICAL
        "major"    -> AlertSeverity.MAJOR
        "minor"    -> AlertSeverity.MINOR
        "warning"  -> AlertSeverity.WARNING
        else       -> AlertSeverity.INFO
    },
    status = when (status.lowercase()) {
        "raised"       -> AlertStatus.RAISED
        "acknowledged" -> AlertStatus.ACKNOWLEDGED
        "escalated"    -> AlertStatus.ESCALATED
        "resolved"     -> AlertStatus.RESOLVED
        "cleared"      -> AlertStatus.CLEARED
        else           -> AlertStatus.RAISED
    },
    title = title,
    description = description,
    deviceId = deviceId,
    deviceName = deviceName,
    regionId = regionId,
    regionName = regionName,
    rootCause = rootCause,
    recommendedAction = recommendedAction,
    raisedAt = raisedAt.toInstantOrNow(),
    acknowledgedAt = acknowledgedAt.toInstantOrNull(),
    acknowledgedBy = acknowledgedBy,
    resolvedAt = resolvedAt.toInstantOrNull(),
    tenantId = tenantId
)

// ─── Devices ──────────────────────────────────────────────────────────────────

fun DeviceDto.toDomain(): Device = Device(
    deviceId = deviceId,
    hostname = hostname,
    ipAddress = ipAddress,
    vendor = vendor,
    model = model,
    platform = platform,
    status = when (status.lowercase()) {
        "up"          -> DeviceStatus.UP
        "down"        -> DeviceStatus.DOWN
        "degraded"    -> DeviceStatus.DEGRADED
        "maintenance" -> DeviceStatus.MAINTENANCE
        else          -> DeviceStatus.UNKNOWN
    },
    region = region,
    site = site,
    uptime = uptime,
    cpuUsage = cpuUsage,
    memoryUsage = memoryUsage,
    interfaces = interfaces.map { it.toDomain() },
    lastSeenAt = lastSeen.toInstantOrNow(),
    tenantId = tenantId
)

fun InterfaceDto.toDomain(): DeviceInterface = DeviceInterface(
    name = name,
    status = when (status.lowercase()) {
        "up"         -> InterfaceStatus.UP
        "down"       -> InterfaceStatus.DOWN
        "admin_down" -> InterfaceStatus.ADMIN_DOWN
        else         -> InterfaceStatus.TESTING
    },
    speed = speed,
    inBps = inBps,
    outBps = outBps,
    errorRate = errorRate
)

fun MetricPointDto.toDomain(): MetricPoint = MetricPoint(
    timestamp = ts.toInstantOrNow(),
    value = value
)

fun MetricSeriesDto.toDomain(): DeviceMetrics = DeviceMetrics(
    deviceId = deviceId,
    cpuHistory = cpu.map { it.toDomain() },
    memoryHistory = memory.map { it.toDomain() },
    trafficInHistory = trafficIn.map { it.toDomain() },
    trafficOutHistory = trafficOut.map { it.toDomain() }
)

// ─── Topology ─────────────────────────────────────────────────────────────────

fun TopologyResponseDto.toDomain(): TopologyGraph = TopologyGraph(
    nodes = nodes.map { it.toDomain() },
    edges = edges.map { it.toDomain() },
    version = version,
    generatedAt = generatedAt.toInstantOrNow()
)

fun TopologyNodeDto.toDomain(): TopologyNode = TopologyNode(
    nodeId = nodeId,
    label = label,
    type = type,
    status = when (status.lowercase()) {
        "up"          -> DeviceStatus.UP
        "down"        -> DeviceStatus.DOWN
        "degraded"    -> DeviceStatus.DEGRADED
        "maintenance" -> DeviceStatus.MAINTENANCE
        else          -> DeviceStatus.UNKNOWN
    },
    healthScore = healthScore,
    latitude = lat,
    longitude = lng,
    x = x,
    y = y,
    metadata = metadata
)

fun TopologyEdgeDto.toDomain(): TopologyEdge = TopologyEdge(
    edgeId = edgeId,
    sourceId = source,
    targetId = target,
    linkStatus = when (linkStatus.lowercase()) {
        "up"       -> LinkStatus.UP
        "degraded" -> LinkStatus.DEGRADED
        else       -> LinkStatus.DOWN
    },
    bandwidth = bandwidth,
    utilization = utilization
)

// ─── AI Insights ──────────────────────────────────────────────────────────────

fun AiInsightDto.toDomain(): AiInsight = AiInsight(
    insightId = insightId,
    type = when (type.lowercase()) {
        "anomaly"                -> InsightType.ANOMALY
        "predicted_failure"      -> InsightType.PREDICTED_FAILURE
        "capacity_warning"       -> InsightType.CAPACITY_WARNING
        "performance_degradation"-> InsightType.PERFORMANCE_DEGRADATION
        "security_threat"        -> InsightType.SECURITY_THREAT
        else                     -> InsightType.ANOMALY
    },
    title = title,
    description = description,
    affectedDeviceId = affectedDeviceId,
    affectedDeviceName = affectedDeviceName,
    riskScore = riskScore,
    confidence = confidence,
    recommendation = recommendation,
    predictedAt = predictedAt.toInstantOrNow(),
    predictedFailureAt = predictedFailureAt.toInstantOrNull(),
    tenantId = tenantId
)

// ─── Geo ──────────────────────────────────────────────────────────────────────

fun GeoStatusDto.toDomain(): GeoStatus = GeoStatus(
    faults = faults.map { it.toDomain() },
    congestionZones = congestionZones.map { it.toDomain() },
    regionSummaries = regions.map { it.toDomain() }
)

fun GeoFaultDto.toDomain(): GeoFault = GeoFault(
    faultId = faultId,
    latitude = lat,
    longitude = lng,
    severity = when (severity.lowercase()) {
        "critical" -> AlertSeverity.CRITICAL
        "major"    -> AlertSeverity.MAJOR
        "minor"    -> AlertSeverity.MINOR
        else       -> AlertSeverity.WARNING
    },
    description = description,
    affectedUsers = affectedUsers,
    detectedAt = detectedAt.toInstantOrNow()
)

fun CongestionZoneDto.toDomain(): CongestionZone = CongestionZone(
    zoneId = zoneId,
    centerLat = centerLat,
    centerLng = centerLng,
    radius = radius,
    utilizationPercent = utilization
)

fun GeoRegionDto.toDomain(): GeoRegionSummary = GeoRegionSummary(
    regionId = regionId,
    name = name,
    lat = lat,
    lng = lng,
    healthScore = healthScore,
    alertCount = alertCount
)
