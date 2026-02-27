package com.aetherion.noc

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aetherion.noc.core.security.SecurityManager
import com.aetherion.noc.data.local.AetherionDatabase
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.*
import org.junit.runner.RunWith
import javax.inject.Inject

// ═══════════════════════════════════════════════════════════════════════════
// Aetherion Mobile NOC — Integration Tests (Android)
// Developer: Mohammad Abdalftah Ibrahime
// ═══════════════════════════════════════════════════════════════════════════

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SecurityManagerIntegrationTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var securityManager: SecurityManager

    @Before
    fun setUp() { hiltRule.inject() }

    @Test
    fun `save and retrieve token round-trips correctly`() {
        val token = com.aetherion.noc.domain.model.AuthToken(
            accessToken  = "test-access-token",
            refreshToken = "test-refresh-token",
            expiresAt    = System.currentTimeMillis() + 3_600_000L
        )

        securityManager.saveToken(token)

        val retrieved = securityManager.getToken()
        assert(retrieved != null)
        assert(retrieved!!.accessToken == "test-access-token")
        assert(retrieved.refreshToken == "test-refresh-token")
        assert(!retrieved.isExpired)
    }

    @Test
    fun `clear tokens removes all stored credentials`() {
        val token = com.aetherion.noc.domain.model.AuthToken(
            accessToken  = "access",
            refreshToken = "refresh",
            expiresAt    = System.currentTimeMillis() + 3_600_000L
        )
        securityManager.saveToken(token)
        securityManager.clearTokens()

        assert(securityManager.getToken() == null)
        assert(!securityManager.isAuthenticated())
    }

    @Test
    fun `session timeout is detected correctly`() {
        val token = com.aetherion.noc.domain.model.AuthToken(
            accessToken  = "access",
            refreshToken = "refresh",
            expiresAt    = System.currentTimeMillis() + 3_600_000L
        )
        securityManager.saveToken(token)
        // Configure extremely short timeout for testing
        securityManager.configure(sessionTimeoutMs = 1L)
        Thread.sleep(5)

        val timedOut = securityManager.checkSessionTimeout()
        assert(timedOut)
    }
}

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DatabaseIntegrationTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var database: AetherionDatabase

    @Before
    fun setUp() { hiltRule.inject() }

    @After
    fun tearDown() { database.close() }

    @Test
    fun `insert and query dashboard entity`() {
        val dao = database.dashboardDao()
        val entity = com.aetherion.noc.data.local.DashboardEntity(
            tenantId        = "test-tenant",
            healthScore     = 90f,
            devicesOnline   = 50,
            devicesOffline  = 5,
            devicesTotal    = 55,
            criticalAlerts  = 0,
            majorAlerts     = 2,
            minorAlerts     = 4,
            warningAlerts   = 6,
            slaCompliance   = 99f,
            aiInsightSummary = "All systems nominal.",
            lastUpdatedAt   = System.currentTimeMillis()
        )

        kotlinx.coroutines.runBlocking {
            dao.insertDashboard(entity)
            val retrieved = dao.getDashboard("test-tenant")
            assert(retrieved != null)
            assert(retrieved!!.healthScore == 90f)
            assert(retrieved.tenantId == "test-tenant")
        }
    }
}

/**
 * Custom Hilt test runner — replace default instrumentation runner.
 * Must be registered in build.gradle.kts testInstrumentationRunner.
 */
class HiltTestRunner : androidx.test.runner.AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: android.content.Context?
    ): android.app.Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}

@dagger.hilt.android.testing.CustomTestApplication(android.app.Application::class)
interface HiltTestApplication
