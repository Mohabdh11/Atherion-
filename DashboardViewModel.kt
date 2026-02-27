package com.aetherion.noc.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aetherion.noc.BuildConfig
import com.aetherion.noc.core.security.SecurityManager
import com.aetherion.noc.domain.model.DashboardSummary
import com.aetherion.noc.domain.model.Result
import com.aetherion.noc.domain.usecase.GetLatestInsightsUseCase
import com.aetherion.noc.domain.usecase.ObserveDashboardUseCase
import com.aetherion.noc.domain.usecase.RefreshDashboardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject

// ═══════════════════════════════════════════════════════════════════════════
// Aetherion Mobile NOC — Dashboard ViewModel
// Developer: Mohammad Abdalftah Ibrahime
// ═══════════════════════════════════════════════════════════════════════════

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val observeDashboardUseCase: ObserveDashboardUseCase,
    private val refreshDashboardUseCase: RefreshDashboardUseCase,
    private val getLatestInsightsUseCase: GetLatestInsightsUseCase,
    private val securityManager: SecurityManager
) : ViewModel() {

    private val tenantId: String get() = securityManager.getTenantId() ?: ""

    // ─── UI State ─────────────────────────────────────────────────

    private val _dashboardState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val dashboardState: StateFlow<DashboardUiState> = _dashboardState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _lastRefreshTime = MutableStateFlow(System.currentTimeMillis())
    val lastRefreshTime: StateFlow<Long> = _lastRefreshTime.asStateFlow()

    private var autoRefreshJob: Job? = null

    init {
        observeDashboard()
        startAutoRefresh()
    }

    // ─── Dashboard Observation ────────────────────────────────────

    private fun observeDashboard() {
        observeDashboardUseCase(tenantId)
            .onEach { result ->
                when (result) {
                    is Result.Success -> _dashboardState.value = DashboardUiState.Success(result.data)
                    is Result.Error   -> {
                        // Only show error if we have no cached data
                        if (_dashboardState.value is DashboardUiState.Loading) {
                            _dashboardState.value = DashboardUiState.Error(
                                result.message ?: result.exception.message ?: "Unknown error"
                            )
                        }
                        Timber.e(result.exception, "Dashboard observe error")
                    }
                    is Result.Loading -> {
                        if (_dashboardState.value !is DashboardUiState.Success) {
                            _dashboardState.value = DashboardUiState.Loading
                        }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    // ─── Manual Refresh ───────────────────────────────────────────

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                when (val result = refreshDashboardUseCase(tenantId)) {
                    is Result.Success -> {
                        _dashboardState.value = DashboardUiState.Success(result.data)
                        _lastRefreshTime.value = System.currentTimeMillis()
                    }
                    is Result.Error -> {
                        Timber.e(result.exception, "Manual refresh failed")
                        // Don't replace cached data with error
                    }
                    else -> {}
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    // ─── Auto Refresh ─────────────────────────────────────────────

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(BuildConfig.REFRESH_INTERVAL_SEC * 1000L)
                try {
                    refreshDashboardUseCase(tenantId)
                    _lastRefreshTime.value = System.currentTimeMillis()
                } catch (e: Exception) {
                    Timber.w("Auto-refresh failed: ${e.message}")
                }
            }
        }
    }

    fun configureRefreshInterval(intervalSeconds: Int) {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(intervalSeconds * 1000L)
                refreshDashboardUseCase(tenantId)
                _lastRefreshTime.value = System.currentTimeMillis()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoRefreshJob?.cancel()
    }
}

sealed class DashboardUiState {
    data object Loading : DashboardUiState()
    data class Success(val data: DashboardSummary) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}
