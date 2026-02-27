package com.aetherion.noc.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aetherion.noc.BuildConfig
import com.aetherion.noc.core.security.SecurityManager
import com.aetherion.noc.domain.model.AuthCredentials
import com.aetherion.noc.domain.model.Result
import com.aetherion.noc.domain.model.UserProfile
import com.aetherion.noc.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ═══════════════════════════════════════════════════════════════════════════
// Aetherion Mobile NOC — Auth ViewModel
// Developer: Mohammad Abdalftah Ibrahime
// ═══════════════════════════════════════════════════════════════════════════

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val securityManager: SecurityManager
) : ViewModel() {

    // ─── UI State ─────────────────────────────────────────────────────────

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    val isAuthenticated: StateFlow<Boolean> = observeAuthStateUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, securityManager.isAuthenticated())

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    // Form state
    private val _username  = MutableStateFlow("")
    private val _password  = MutableStateFlow("")
    private val _tenantId  = MutableStateFlow("")
    private val _mfaCode   = MutableStateFlow("")
    private val _mfaRequired = MutableStateFlow(false)

    val username: StateFlow<String>  = _username.asStateFlow()
    val password: StateFlow<String>  = _password.asStateFlow()
    val tenantId: StateFlow<String>  = _tenantId.asStateFlow()
    val mfaCode: StateFlow<String>   = _mfaCode.asStateFlow()
    val mfaRequired: StateFlow<Boolean> = _mfaRequired.asStateFlow()

    init {
        // Session timeout check
        viewModelScope.launch {
            securityManager.sessionState.collect { state ->
                when (state) {
                    is com.aetherion.noc.core.security.SessionState.TimedOut -> {
                        _uiState.value = LoginUiState.SessionExpired
                    }
                    is com.aetherion.noc.core.security.SessionState.Authenticated -> {
                        loadCurrentUser()
                    }
                    else -> {}
                }
            }
        }

        // Configure session timeout from build config
        securityManager.configure(BuildConfig.SESSION_TIMEOUT_MS)
    }

    // ─── Login Flow ───────────────────────────────────────────────────────

    fun onUsernameChange(value: String) { _username.value = value }
    fun onPasswordChange(value: String) { _password.value = value }
    fun onTenantIdChange(value: String) { _tenantId.value = value }
    fun onMfaCodeChange(value: String) { _mfaCode.value = value }

    fun login() {
        if (_uiState.value is LoginUiState.Loading) return

        val credentials = AuthCredentials(
            username = _username.value.trim(),
            password = _password.value,
            tenantId = _tenantId.value.trim(),
            mfaCode  = _mfaCode.value.takeIf { it.isNotBlank() }
        )

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            when (val result = loginUseCase(credentials)) {
                is Result.Success -> {
                    loadCurrentUser()
                    _uiState.value = LoginUiState.Success
                }
                is Result.Error -> {
                    val msg = result.message ?: result.exception.message ?: "Login failed"
                    // Check if MFA required
                    if (msg.contains("mfa", ignoreCase = true) || msg.contains("otp", ignoreCase = true)) {
                        _mfaRequired.value = true
                        _uiState.value = LoginUiState.MfaRequired
                    } else {
                        _uiState.value = LoginUiState.Error(msg)
                    }
                }
                else -> {}
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            _currentUser.value = null
            _uiState.value = LoginUiState.Idle
        }
    }

    fun clearError() {
        if (_uiState.value is LoginUiState.Error) {
            _uiState.value = LoginUiState.Idle
        }
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            when (val result = getCurrentUserUseCase()) {
                is Result.Success -> _currentUser.value = result.data
                else -> {}
            }
        }
    }
}

sealed class LoginUiState {
    data object Idle          : LoginUiState()
    data object Loading       : LoginUiState()
    data object Success       : LoginUiState()
    data object MfaRequired   : LoginUiState()
    data object SessionExpired: LoginUiState()
    data class Error(val message: String) : LoginUiState()
}
