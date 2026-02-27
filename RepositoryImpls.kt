package com.aetherion.noc.data.repository

import androidx.paging.*
import com.aetherion.noc.data.local.*
import com.aetherion.noc.data.remote.api.*
import com.aetherion.noc.data.remote.dto.*
import com.aetherion.noc.domain.model.*
import com.aetherion.noc.domain.repository.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

// ═══════════════════════════════════════════════════════════════════════════
// Aetherion Mobile NOC — Data Repository Implementations
// Developer: Mohammad Abdalftah Ibrahime
// ═══════════════════════════════════════════════════════════════════════════

// ─── Dashboard Repository ────────────────────────────────────────────────────

@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val dashboardApi: AetherionDashboardApi,
    private val dashboardDao: DashboardDao
) : DashboardRepository {

    override fun observeDashboard(tenantId: String): Flow<Result<DashboardSummary>> =
        dashboardDao.observeDashboard(tenantId)
            .map { entity ->
                if (entity != null) Result.Success(entity.toDomain())
                else Result.Loading
            }
            .onStart {
                // Trigger network refresh
                try {
                    refreshDashboard(tenantId)
                } catch (e: Exception) {
                    Timber.e(e, "Background refresh failed")
                }
            }

    override suspend fun refreshDashboard(tenantId: String): Result<DashboardSummary> {
        return try {
            val response = dashboardApi.getDashboardSummary(tenantId)
            if (response.isSuccessful) {
                val dto = response.body() ?: return Result.Error(Exception("Empty dashboard"))
                val domain = dto.toDomain()
                dashboardDao.insertDashboard(dto.toEntity())
                Result.Success(domain)
            } else {
                Result.Error(Exception("Dashboard fetch failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Dashboard refresh exception")
            Result.Error(e)
        }
    }

    private fun DashboardSummaryDto.toEntity() = DashboardEntity(
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
        lastUpdatedAt = System.currentTimeMillis()
    )

    private fun DashboardEntity.toDomain() = DashboardSummary(
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
        aiInsightSummary = aiInsightSummary,
        lastUpdatedAt = java.time.Instant.ofEpochMilli(lastUpdatedAt),
        networkRegions = emptyList()
    )
}

// ─── Alert Repository ────────────────────────────────────────────────────────

private const val ALERTS_PAGE_SIZE = 50

@Singleton
class AlertRepositoryImpl @Inject constructor(
    private val alertApi: AetherionAlertApi,
    private val alertDao: AlertDao
) : AlertRepository {

    override fun getAlertsPaged(
        tenantId: String,
        severity: List<AlertSeverity>?,
        regionId: String?,
        status: AlertStatus?
    ): Flow<PagingData<Alert>> = Pager(
        config = PagingConfig(
            pageSize = ALERTS_PAGE_SIZE,
            enablePlaceholders = false,
            prefetchDistance = 10
        ),
        pagingSourceFactory = {
            AlertRemotePagingSource(alertApi, alertDao, tenantId, severity, regionId, status)
        }
    ).flow

    override fun observeLiveAlerts(tenantId: String): Flow<Alert> = flow {
        // Poll endpoint for new alerts; WebSocket can be substituted
        while (true) {
            try {
                val response = alertApi.getAlerts(
                    page = 1, pageSize = 20,
                    status = AlertStatus.RAISED.name
                )
                if (response.isSuccessful) {
                    response.body()?.items?.forEach { dto ->
                        emit(dto.toDomain())
                    }
                }
            } catch (e: Exception) {
                Timber.w("Live alert poll failed: ${e.message}")
            }
            delay(10_000L) // 10-second poll
        }
    }

    override suspend fun acknowledgeAlert(alertId: String): Result<Unit> {
        return try {
            val response = alertApi.acknowledgeAlert(alertId)
            if (response.isSuccessful) {
                alertDao.markAcknowledged(alertId)
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Acknowledge failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun escalateAlert(alertId: String, note: String): Result<Unit> {
        return try {
            val response = alertApi.escalateAlert(alertId, mapOf("note" to note))
            if (response.isSuccessful) Result.Success(Unit)
            else Result.Error(Exception("Escalate failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

// ─── Alert Paging Source ──────────────────────────────────────────────────────

class AlertRemotePagingSource(
    private val alertApi: AetherionAlertApi,
    private val alertDao: AlertDao,
    private val tenantId: String,
    private val severity: List<AlertSeverity>?,
    private val regionId: String?,
    private val status: AlertStatus?
) : PagingSource<String?, Alert>() {

    override fun getRefreshKey(state: PagingState<String?, Alert>): String? = null

    override suspend fun load(params: LoadParams<String?>): LoadResult<String?, Alert> {
        return try {
            val response = alertApi.getAlerts(
                page = 1,
                pageSize = params.loadSize,
                severity = severity?.map { it.name },
                regionId = regionId,
                status = status?.name,
                cursor = params.key
            )
            if (response.isSuccessful) {
                val body = response.body()!!
                val alerts = body.items.map { it.toDomain() }

                // Cache to local DB
                alertDao.insertAlerts(body.items.map { it.toEntity() })

                LoadResult.Page(
                    data = alerts,
                    prevKey = null,
                    nextKey = body.nextCursor
                )
            } else {
                LoadResult.Error(Exception("Alert load failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    private fun AlertDto.toEntity() = AlertEntity(
        alertId = alertId, severity = severity, status = status,
        title = title, description = description,
        deviceId = deviceId, deviceName = deviceName,
        regionId = regionId, regionName = regionName,
        rootCause = rootCause, recommendedAction = recommendedAction,
        raisedAt = raisedAt.toEpochMillis(),
        acknowledgedAt = acknowledgedAt?.toEpochMillis(),
        acknowledgedBy = acknowledgedBy,
        resolvedAt = resolvedAt?.toEpochMillis(),
        tenantId = tenantId
    )

    private fun String.toEpochMillis(): Long = try {
        java.time.Instant.parse(this).toEpochMilli()
    } catch (e: Exception) { System.currentTimeMillis() }
}

// ─── Device Repository ────────────────────────────────────────────────────────

@Singleton
class DeviceRepositoryImpl @Inject constructor(
    private val deviceApi: AetherionDeviceApi,
    private val deviceDao: DeviceDao
) : DeviceRepository {

    override suspend fun getDevices(tenantId: String, regionId: String?): Result<List<Device>> {
        return try {
            val response = deviceApi.getDevices(tenantId, regionId)
            if (response.isSuccessful) {
                val devices = response.body()?.devices?.map { it.toDomain() } ?: emptyList()
                deviceDao.insertDevices(response.body()!!.devices.map { it.toEntity() })
                Result.Success(devices)
            } else {
                // Fallback to cache
                val cached = deviceDao.getDevices(tenantId).map { it.toDomain() }
                if (cached.isNotEmpty()) Result.Success(cached)
                else Result.Error(Exception("Device fetch failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            val cached = deviceDao.getDevices(tenantId).map { it.toDomain() }
            if (cached.isNotEmpty()) Result.Success(cached) else Result.Error(e)
        }
    }

    override suspend fun getDevice(deviceId: String): Result<Device> {
        return try {
            val response = deviceApi.getDevice(deviceId)
            if (response.isSuccessful) {
                Result.Success(response.body()!!.toDomain())
            } else {
                val cached = deviceDao.getDevice(deviceId)?.toDomain()
                if (cached != null) Result.Success(cached)
                else Result.Error(Exception("Device not found"))
            }
        } catch (e: Exception) {
            val cached = deviceDao.getDevice(deviceId)?.toDomain()
            if (cached != null) Result.Success(cached) else Result.Error(e)
        }
    }

    override suspend fun getDeviceMetrics(deviceId: String, range: MetricRange): Result<DeviceMetrics> {
        return try {
            val rangeStr = when (range) {
                MetricRange.H24 -> "24h"
                MetricRange.D7  -> "7d"
                MetricRange.D30 -> "30d"
            }
            val response = deviceApi.getDeviceMetrics(deviceId, rangeStr)
            if (response.isSuccessful) Result.Success(response.body()!!.toDomain())
            else Result.Error(Exception("Metrics fetch failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private fun DeviceDto.toEntity() = DeviceEntity(
        deviceId = deviceId, hostname = hostname, ipAddress = ipAddress,
        vendor = vendor, model = model, platform = platform, status = status,
        region = region, site = site, uptime = uptime,
        cpuUsage = cpuUsage, memoryUsage = memoryUsage,
        lastSeenAt = System.currentTimeMillis(), tenantId = tenantId
    )

    private fun DeviceEntity.toDomain() = Device(
        deviceId = deviceId, hostname = hostname, ipAddress = ipAddress,
        vendor = vendor, model = model, platform = platform,
        status = DeviceStatus.valueOf(status.uppercase()),
        region = region, site = site, uptime = uptime,
        cpuUsage = cpuUsage, memoryUsage = memoryUsage,
        interfaces = emptyList(),
        lastSeenAt = java.time.Instant.ofEpochMilli(lastSeenAt),
        tenantId = tenantId
    )
}

// ─── Topology Repository ──────────────────────────────────────────────────────

@Singleton
class TopologyRepositoryImpl @Inject constructor(
    private val topologyApi: AetherionTopologyApi
) : TopologyRepository {

    override suspend fun getTopology(tenantId: String): Result<TopologyGraph> {
        return try {
            val response = topologyApi.getTopology(tenantId)
            if (response.isSuccessful) Result.Success(response.body()!!.toDomain())
            else Result.Error(Exception("Topology fetch failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun observeTopologyUpdates(tenantId: String): Flow<TopologyGraph> = flow {
        while (true) {
            try {
                val response = topologyApi.getTopology(tenantId)
                if (response.isSuccessful) {
                    emit(response.body()!!.toDomain())
                }
            } catch (e: Exception) {
                Timber.w("Topology update failed: ${e.message}")
            }
            delay(120_000L) // 2-minute refresh
        }
    }
}

// ─── AI Insight Repository ────────────────────────────────────────────────────

@Singleton
class AiInsightRepositoryImpl @Inject constructor(
    private val aiApi: AetherionAiApi
) : AiInsightRepository {

    override fun getInsightsPaged(tenantId: String): Flow<PagingData<AiInsight>> = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false),
        pagingSourceFactory = { InsightPagingSource(aiApi, tenantId) }
    ).flow

    override suspend fun getLatestInsights(tenantId: String, limit: Int): Result<List<AiInsight>> {
        return try {
            val response = aiApi.getInsights(tenantId, 1, limit)
            if (response.isSuccessful) Result.Success(response.body()!!.insights.map { it.toDomain() })
            else Result.Error(Exception("Insights fetch failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

private class InsightPagingSource(
    private val aiApi: AetherionAiApi,
    private val tenantId: String
) : PagingSource<Int, AiInsight>() {

    override fun getRefreshKey(state: PagingState<Int, AiInsight>): Int? =
        state.anchorPosition?.let { state.closestPageToPosition(it)?.prevKey?.plus(1) }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, AiInsight> {
        val page = params.key ?: 1
        return try {
            val response = aiApi.getInsights(tenantId, page, params.loadSize)
            if (response.isSuccessful) {
                val body = response.body()!!
                LoadResult.Page(
                    data = body.insights.map { it.toDomain() },
                    prevKey = if (page == 1) null else page - 1,
                    nextKey = if (body.insights.isEmpty()) null else page + 1
                )
            } else LoadResult.Error(Exception("Page load failed: ${response.code()}"))
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}

// ─── Geo Repository ───────────────────────────────────────────────────────────

@Singleton
class GeoRepositoryImpl @Inject constructor(
    private val geoApi: AetherionGeoApi
) : GeoRepository {

    override suspend fun getGeoStatus(tenantId: String): Result<GeoStatus> {
        return try {
            val response = geoApi.getGeoStatus(tenantId)
            if (response.isSuccessful) Result.Success(response.body()!!.toDomain())
            else Result.Error(Exception("Geo status fetch failed: ${response.code()}"))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun observeGeoUpdates(tenantId: String): Flow<GeoStatus> = flow {
        while (true) {
            try {
                val response = geoApi.getGeoStatus(tenantId)
                if (response.isSuccessful) emit(response.body()!!.toDomain())
            } catch (e: Exception) {
                Timber.w("Geo update failed: ${e.message}")
            }
            delay(30_000L)
        }
    }
}
