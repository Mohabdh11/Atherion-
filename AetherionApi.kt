package com.aetherion.noc.data.remote.api

import com.aetherion.noc.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

// ═══════════════════════════════════════════════════════════════════════════
// Aetherion Mobile NOC — Retrofit API Interfaces
// Developer: Mohammad Abdalftah Ibrahime
// Aligned with Aetherion v8 backend endpoints.
// ═══════════════════════════════════════════════════════════════════════════

interface AetherionAuthApi {

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequestDto
    ): Response<LoginResponseDto>

    @POST("auth/refresh")
    suspend fun refreshToken(
        @Header("Authorization") refreshToken: String
    ): Response<LoginResponseDto>

    @GET("auth/me")
    suspend fun getCurrentUser(): Response<UserMeDto>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>
}

interface AetherionDashboardApi {

    @GET("tenants/{tenantId}/dashboard")
    suspend fun getDashboardSummary(
        @Path("tenantId") tenantId: String
    ): Response<DashboardSummaryDto>
}

interface AetherionAlertApi {

    @GET("alarms")
    suspend fun getAlerts(
        @Query("page")      page: Int,
        @Query("page_size") pageSize: Int,
        @Query("severity")  severity: List<String>? = null,
        @Query("region_id") regionId: String? = null,
        @Query("status")    status: String? = null,
        @Query("cursor")    cursor: String? = null
    ): Response<AlertsResponseDto>

    @POST("alarms/{alertId}/acknowledge")
    suspend fun acknowledgeAlert(
        @Path("alertId") alertId: String
    ): Response<Unit>

    @POST("alarms/{alertId}/escalate")
    suspend fun escalateAlert(
        @Path("alertId") alertId: String,
        @Body body: Map<String, String>
    ): Response<Unit>
}

interface AetherionDeviceApi {

    @GET("nodes")
    suspend fun getDevices(
        @Query("tenant_id") tenantId: String,
        @Query("region")    regionId: String? = null,
        @Query("limit")     limit: Int = 200
    ): Response<DevicesResponseDto>

    @GET("nodes/{deviceId}")
    suspend fun getDevice(
        @Path("deviceId") deviceId: String
    ): Response<DeviceDto>

    @GET("nodes/{deviceId}/metrics")
    suspend fun getDeviceMetrics(
        @Path("deviceId") deviceId: String,
        @Query("range")   range: String   // "24h" | "7d" | "30d"
    ): Response<MetricSeriesDto>
}

interface AetherionTopologyApi {

    @GET("topology")
    suspend fun getTopology(
        @Query("tenant_id")  tenantId: String,
        @Query("limit")      limit: Int = 1000
    ): Response<TopologyResponseDto>

    @GET("topology/version")
    suspend fun getTopologyVersion(): Response<Map<String, Long>>

    @GET("topology/critical-nodes")
    suspend fun getCriticalNodes(): Response<List<TopologyNodeDto>>
}

interface AetherionAiApi {

    @GET("ai/insights")
    suspend fun getInsights(
        @Query("tenant_id") tenantId: String,
        @Query("page")      page: Int,
        @Query("page_size") pageSize: Int
    ): Response<InsightsResponseDto>
}

interface AetherionGeoApi {

    @GET("geo/status")
    suspend fun getGeoStatus(
        @Query("tenant_id") tenantId: String
    ): Response<GeoStatusDto>
}
