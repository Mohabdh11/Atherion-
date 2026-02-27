package com.aetherion.noc

import app.cash.turbine.test
import com.aetherion.noc.domain.model.*
import com.aetherion.noc.domain.repository.AuthRepository
import com.aetherion.noc.domain.repository.DashboardRepository
import com.aetherion.noc.domain.usecase.*
import com.aetherion.noc.presentation.dashboard.DashboardUiState
import com.aetherion.noc.presentation.dashboard.DashboardViewModel
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*

// ═══════════════════════════════════════════════════════════════════════════
// Aetherion Mobile NOC — Unit Tests
// Developer: Mohammad Abdalftah Ibrahime
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalCoroutinesApi::class)
class LoginUseCaseTest {

    @MockK lateinit var authRepository: AuthRepository

    private lateinit var loginUseCase: LoginUseCase
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        Dispatchers.setMain(testDispatcher)
        loginUseCase = LoginUseCase(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `login with valid credentials returns success`() = runTest {
        val credentials = AuthCredentials("operator@aetherion.io", "Secur3Pass!", "tenant-1")
        val token = AuthToken("access", "refresh", System.currentTimeMillis() + 3600_000, "Bearer")
        coEvery { authRepository.login(any()) } returns Result.Success(token)

        val result = loginUseCase(credentials)

        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data.accessToken).isEqualTo("access")
    }

    @Test
    fun `login with blank username returns error without network call`() = runTest {
        val credentials = AuthCredentials("", "password123!", "tenant-1")

        val result = loginUseCase(credentials)

        assertThat(result).isInstanceOf(Result.Error::class.java)
        coVerify(exactly = 0) { authRepository.login(any()) }
    }

    @Test
    fun `login with short password returns error without network call`() = runTest {
        val credentials = AuthCredentials("user", "pass", "tenant-1")

        val result = loginUseCase(credentials)

        assertThat(result).isInstanceOf(Result.Error::class.java)
        coVerify(exactly = 0) { authRepository.login(any()) }
    }

    @Test
    fun `login with blank tenantId returns error`() = runTest {
        val credentials = AuthCredentials("user", "securepass!", "")

        val result = loginUseCase(credentials)

        assertThat(result).isInstanceOf(Result.Error::class.java)
        coVerify(exactly = 0) { authRepository.login(any()) }
    }

    @Test
    fun `login network failure returns error`() = runTest {
        val credentials = AuthCredentials("user", "securepass!", "tenant-1")
        coEvery { authRepository.login(any()) } returns Result.Error(Exception("Network timeout"))

        val result = loginUseCase(credentials)

        assertThat(result).isInstanceOf(Result.Error::class.java)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveDashboardUseCaseTest {

    @MockK lateinit var dashboardRepository: DashboardRepository

    private lateinit var useCase: ObserveDashboardUseCase
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        Dispatchers.setMain(testDispatcher)
        useCase = ObserveDashboardUseCase(dashboardRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `observe dashboard emits success when repository provides data`() = runTest {
        val summary = buildFakeDashboard()
        every { dashboardRepository.observeDashboard(any()) } returns flowOf(Result.Success(summary))

        useCase("tenant-1").test {
            val item = awaitItem()
            assertThat(item).isInstanceOf(Result.Success::class.java)
            assertThat((item as Result.Success).data.healthScore).isEqualTo(95f)
            awaitComplete()
        }
    }

    @Test
    fun `observe dashboard emits error when repository fails`() = runTest {
        every { dashboardRepository.observeDashboard(any()) } returns
            flowOf(Result.Error(Exception("Server error"), "Server error"))

        useCase("tenant-1").test {
            val item = awaitItem()
            assertThat(item).isInstanceOf(Result.Error::class.java)
            awaitComplete()
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AcknowledgeAlertUseCaseTest {

    @MockK lateinit var alertRepository: com.aetherion.noc.domain.repository.AlertRepository

    private lateinit var useCase: AcknowledgeAlertUseCase
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        Dispatchers.setMain(testDispatcher)
        useCase = AcknowledgeAlertUseCase(alertRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `acknowledge alert calls repository with correct id`() = runTest {
        coEvery { alertRepository.acknowledgeAlert("alert-123") } returns Result.Success(Unit)

        val result = useCase("alert-123")

        assertThat(result).isInstanceOf(Result.Success::class.java)
        coVerify { alertRepository.acknowledgeAlert("alert-123") }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class EscalateAlertUseCaseTest {

    @MockK lateinit var alertRepository: com.aetherion.noc.domain.repository.AlertRepository

    private lateinit var useCase: EscalateAlertUseCase
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        Dispatchers.setMain(testDispatcher)
        useCase = EscalateAlertUseCase(alertRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `escalate with blank note returns error without repository call`() = runTest {
        val result = useCase("alert-123", "")

        assertThat(result).isInstanceOf(Result.Error::class.java)
        coVerify(exactly = 0) { alertRepository.escalateAlert(any(), any()) }
    }

    @Test
    fun `escalate with valid note calls repository`() = runTest {
        coEvery { alertRepository.escalateAlert(any(), any()) } returns Result.Success(Unit)

        val result = useCase("alert-123", "Link flapping on core router")

        assertThat(result).isInstanceOf(Result.Success::class.java)
        coVerify { alertRepository.escalateAlert("alert-123", "Link flapping on core router") }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun buildFakeDashboard() = DashboardSummary(
    tenantId         = "tenant-1",
    healthScore      = 95f,
    devicesOnline    = 980,
    devicesOffline   = 20,
    devicesTotal     = 1000,
    criticalAlerts   = 2,
    majorAlerts      = 5,
    minorAlerts      = 12,
    warningAlerts    = 20,
    slaCompliance    = 99.5f,
    aiInsightSummary = "Network performing within expected parameters.",
    lastUpdatedAt    = java.time.Instant.now(),
    networkRegions   = emptyList()
)
