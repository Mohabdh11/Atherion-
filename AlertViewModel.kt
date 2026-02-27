package com.aetherion.noc.presentation.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.aetherion.noc.core.security.SecurityManager
import com.aetherion.noc.domain.model.*
import com.aetherion.noc.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

// ═══════════════════════════════════════════════════════════════════════════
// Aetherion Mobile NOC — Alert Center ViewModel
// Developer: Mohammad Abdalftah Ibrahime
// ═══════════════════════════════════════════════════════════════════════════

@HiltViewModel
class AlertViewModel @Inject constructor(
    private val getPagedAlertsUseCase: GetPagedAlertsUseCase,
    private val observeLiveAlertsUseCase: ObserveLiveAlertsUseCase,
    private val acknowledgeAlertUseCase: AcknowledgeAlertUseCase,
    private val escalateAlertUseCase: EscalateAlertUseCase,
    private val securityManager: SecurityManager
) : ViewModel() {

    private val tenantId: String get() = securityManager.getTenantId() ?: ""

    // ─── Filters ──────────────────────────────────────────────────

    private val _selectedSeverities = MutableStateFlow<List<AlertSeverity>?>(null)
    val selectedSeverities: StateFlow<List<AlertSeverity>?> = _selectedSeverities.asStateFlow()

    private val _selectedRegion = MutableStateFlow<String?>(null)
    val selectedRegion: StateFlow<String?> = _selectedRegion.asStateFlow()

    private val _selectedStatus = MutableStateFlow<AlertStatus?>(null)
    val selectedStatus: StateFlow<AlertStatus?> = _selectedStatus.asStateFlow()

    // ─── Paged Alert Flow ─────────────────────────────────────────

    val alerts: Flow<PagingData<Alert>> = combine(
        _selectedSeverities, _selectedRegion, _selectedStatus
    ) { severities, region, status ->
        Triple(severities, region, status)
    }.flatMapLatest { (severities, region, status) ->
        getPagedAlertsUseCase(tenantId, severities, region, status)
    }.cachedIn(viewModelScope)

    // ─── Live Alerts Stream ───────────────────────────────────────

    val liveAlerts: Flow<Alert> = observeLiveAlertsUseCase(tenantId)

    // ─── Actions ──────────────────────────────────────────────────

    private val _actionState = MutableStateFlow<AlertActionState>(AlertActionState.Idle)
    val actionState: StateFlow<AlertActionState> = _actionState.asStateFlow()

    fun acknowledgeAlert(alertId: String) {
        viewModelScope.launch {
            _actionState.value = AlertActionState.Loading
            when (val result = acknowledgeAlertUseCase(alertId)) {
                is Result.Success -> _actionState.value = AlertActionState.Success("Alert acknowledged")
                is Result.Error   -> {
                    _actionState.value = AlertActionState.Error(result.message ?: "Failed")
                    Timber.e(result.exception, "Acknowledge failed")
                }
                else -> {}
            }
        }
    }

    fun escalateAlert(alertId: String, note: String) {
        viewModelScope.launch {
            _actionState.value = AlertActionState.Loading
            when (val result = escalateAlertUseCase(alertId, note)) {
                is Result.Success -> _actionState.value = AlertActionState.Success("Alert escalated")
                is Result.Error   -> {
                    _actionState.value = AlertActionState.Error(result.message ?: "Failed")
                    Timber.e(result.exception, "Escalate failed")
                }
                else -> {}
            }
        }
    }

    fun clearActionState() { _actionState.value = AlertActionState.Idle }

    // ─── Filter Actions ───────────────────────────────────────────

    fun toggleSeverityFilter(severity: AlertSeverity) {
        val current = _selectedSeverities.value?.toMutableList() ?: mutableListOf()
        if (current.contains(severity)) current.remove(severity) else current.add(severity)
        _selectedSeverities.value = current.takeIf { it.isNotEmpty() }
    }

    fun setRegionFilter(regionId: String?) { _selectedRegion.value = regionId }
    fun setStatusFilter(status: AlertStatus?) { _selectedStatus.value = status }
    fun clearFilters() {
        _selectedSeverities.value = null
        _selectedRegion.value = null
        _selectedStatus.value = null
    }
}

sealed class AlertActionState {
    data object Idle    : AlertActionState()
    data object Loading : AlertActionState()
    data class Success(val message: String) : AlertActionState()
    data class Error(val message: String)   : AlertActionState()
}
