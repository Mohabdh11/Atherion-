package com.aetherion.noc.data.local

import androidx.paging.PagingSource
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ═══════════════════════════════════════════════════════════════════════════
// Aetherion Mobile NOC — Room Database
// Developer: Mohammad Abdalftah Ibrahime
// Local cache for offline support and last-known-state persistence.
// ═══════════════════════════════════════════════════════════════════════════

// ─── Entities ─────────────────────────────────────────────────────────────────

@Entity(tableName = "cached_alerts", indices = [Index("severity"), Index("tenant_id"), Index("region_id")])
data class AlertEntity(
    @PrimaryKey val alertId: String,
    val severity: String,
    val status: String,
    val title: String,
    val description: String,
    val deviceId: String,
    val deviceName: String,
    val regionId: String,
    val regionName: String,
    val rootCause: String?,
    val recommendedAction: String?,
    val raisedAt: Long,          // epoch millis
    val acknowledgedAt: Long?,
    val acknowledgedBy: String?,
    val resolvedAt: Long?,
    val tenantId: String,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_devices", indices = [Index("tenant_id"), Index("region")])
data class DeviceEntity(
    @PrimaryKey val deviceId: String,
    val hostname: String,
    val ipAddress: String,
    val vendor: String,
    val model: String,
    val platform: String,
    val status: String,
    val region: String,
    val site: String,
    val uptime: Long,
    val cpuUsage: Float,
    val memoryUsage: Float,
    val lastSeenAt: Long,
    val tenantId: String,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_dashboard")
data class DashboardEntity(
    @PrimaryKey val tenantId: String,
    val healthScore: Float,
    val devicesOnline: Int,
    val devicesOffline: Int,
    val devicesTotal: Int,
    val criticalAlerts: Int,
    val majorAlerts: Int,
    val minorAlerts: Int,
    val warningAlerts: Int,
    val slaCompliance: Float,
    val aiInsightSummary: String,
    val lastUpdatedAt: Long,
    val cachedAt: Long = System.currentTimeMillis()
)

// ─── DAOs ──────────────────────────────────────────────────────────────────────

@Dao
interface AlertDao {

    @Query("""
        SELECT * FROM cached_alerts
        WHERE tenant_id = :tenantId
        AND (:severity IS NULL OR severity IN (:severityList))
        AND (:regionId IS NULL OR region_id = :regionId)
        AND (:status IS NULL OR status = :status)
        ORDER BY raisedAt DESC
    """)
    fun getAlertsPaged(
        tenantId: String,
        severity: String?,
        severityList: List<String>,
        regionId: String?,
        status: String?
    ): PagingSource<Int, AlertEntity>

    @Query("SELECT * FROM cached_alerts WHERE alertId = :alertId")
    suspend fun getAlert(alertId: String): AlertEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlerts(alerts: List<AlertEntity>)

    @Query("DELETE FROM cached_alerts WHERE tenantId = :tenantId AND cachedAt < :threshold")
    suspend fun deleteOldAlerts(tenantId: String, threshold: Long)

    @Query("UPDATE cached_alerts SET status = 'ACKNOWLEDGED' WHERE alertId = :alertId")
    suspend fun markAcknowledged(alertId: String)

    @Query("SELECT COUNT(*) FROM cached_alerts WHERE tenantId = :tenantId AND severity = 'CRITICAL' AND status != 'RESOLVED'")
    fun observeCriticalCount(tenantId: String): Flow<Int>
}

@Dao
interface DeviceDao {

    @Query("SELECT * FROM cached_devices WHERE tenantId = :tenantId ORDER BY hostname ASC")
    suspend fun getDevices(tenantId: String): List<DeviceEntity>

    @Query("SELECT * FROM cached_devices WHERE deviceId = :deviceId")
    suspend fun getDevice(deviceId: String): DeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevices(devices: List<DeviceEntity>)

    @Query("SELECT COUNT(*) FROM cached_devices WHERE tenantId = :tenantId AND status = 'UP'")
    fun observeOnlineCount(tenantId: String): Flow<Int>

    @Query("DELETE FROM cached_devices WHERE tenantId = :tenantId AND cachedAt < :threshold")
    suspend fun deleteOldDevices(tenantId: String, threshold: Long)
}

@Dao
interface DashboardDao {

    @Query("SELECT * FROM cached_dashboard WHERE tenantId = :tenantId")
    fun observeDashboard(tenantId: String): Flow<DashboardEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDashboard(dashboard: DashboardEntity)

    @Query("SELECT * FROM cached_dashboard WHERE tenantId = :tenantId")
    suspend fun getDashboard(tenantId: String): DashboardEntity?
}

// ─── Database ─────────────────────────────────────────────────────────────────

@Database(
    entities = [AlertEntity::class, DeviceEntity::class, DashboardEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AetherionDatabase : RoomDatabase() {
    abstract fun alertDao(): AlertDao
    abstract fun deviceDao(): DeviceDao
    abstract fun dashboardDao(): DashboardDao
}
