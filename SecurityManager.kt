package com.aetherion.noc.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.aetherion.noc.domain.model.AuthToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

// ═══════════════════════════════════════════════════════════════════════════
// Aetherion Mobile NOC — Security Manager
// Developer: Mohammad Abdalftah Ibrahime
//
// Responsibilities:
//   • Encrypted token storage (EncryptedSharedPreferences + AES256-GCM)
//   • Biometric authentication prompt
//   • Session timeout enforcement
//   • Zero-trust: every API call validates token freshness
// ═══════════════════════════════════════════════════════════════════════════

@Singleton
class SecurityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // ─── Encrypted Storage ────────────────────────────────────────────────

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .setUserAuthenticationRequired(false) // Token encryption key
            .build()
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // ─── Session State ────────────────────────────────────────────────────

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Unauthenticated)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private var lastActivityTimestamp: Long = System.currentTimeMillis()
    private var sessionTimeoutMs: Long = 900_000L  // 15 minutes default

    // ─── Token Operations ─────────────────────────────────────────────────

    fun saveToken(token: AuthToken) {
        encryptedPrefs.edit().apply {
            putString(KEY_ACCESS_TOKEN,  token.accessToken)
            putString(KEY_REFRESH_TOKEN, token.refreshToken)
            putLong(KEY_EXPIRES_AT,      token.expiresAt)
            putString(KEY_TOKEN_TYPE,    token.tokenType)
            apply()
        }
        _sessionState.value = SessionState.Authenticated
        refreshActivity()
        Timber.d("Token saved, expires in ${token.expiresIn / 1000}s")
    }

    fun getToken(): AuthToken? {
        val access  = encryptedPrefs.getString(KEY_ACCESS_TOKEN,  null) ?: return null
        val refresh = encryptedPrefs.getString(KEY_REFRESH_TOKEN, null) ?: return null
        val expires = encryptedPrefs.getLong(KEY_EXPIRES_AT, 0L)
        val type    = encryptedPrefs.getString(KEY_TOKEN_TYPE, "Bearer") ?: "Bearer"
        return AuthToken(access, refresh, expires, type)
    }

    fun getAccessToken(): String? = encryptedPrefs.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = encryptedPrefs.getString(KEY_REFRESH_TOKEN, null)

    fun clearTokens() {
        encryptedPrefs.edit().apply {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_EXPIRES_AT)
            remove(KEY_TOKEN_TYPE)
            apply()
        }
        _sessionState.value = SessionState.Unauthenticated
        Timber.d("Tokens cleared")
    }

    fun saveTenantId(tenantId: String) {
        encryptedPrefs.edit().putString(KEY_TENANT_ID, tenantId).apply()
    }

    fun getTenantId(): String? = encryptedPrefs.getString(KEY_TENANT_ID, null)

    // ─── Session Timeout ──────────────────────────────────────────────────

    fun configure(sessionTimeoutMs: Long) {
        this.sessionTimeoutMs = sessionTimeoutMs
    }

    fun refreshActivity() {
        lastActivityTimestamp = System.currentTimeMillis()
    }

    fun checkSessionTimeout(): Boolean {
        val elapsed = System.currentTimeMillis() - lastActivityTimestamp
        if (elapsed > sessionTimeoutMs && _sessionState.value == SessionState.Authenticated) {
            Timber.w("Session timed out after ${elapsed / 1000}s")
            clearTokens()
            _sessionState.value = SessionState.TimedOut
            return true
        }
        return false
    }

    fun isAuthenticated(): Boolean {
        val token = getToken() ?: return false
        return !token.isExpired && _sessionState.value == SessionState.Authenticated
    }

    // ─── Biometric Authentication ─────────────────────────────────────────

    fun isBiometricAvailable(): Boolean {
        val bm = BiometricManager.from(context)
        return bm.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "Aetherion NOC",
        subtitle: String = "Authenticate to access NOC",
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                refreshActivity()
                onSuccess()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Don't call onFailure here — user may retry
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onFailure(errString.toString())
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    companion object {
        private const val PREFS_NAME        = "aetherion_secure_prefs"
        private const val KEY_ACCESS_TOKEN  = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT    = "expires_at"
        private const val KEY_TOKEN_TYPE    = "token_type"
        private const val KEY_TENANT_ID     = "tenant_id"
    }
}

sealed class SessionState {
    data object Authenticated   : SessionState()
    data object Unauthenticated : SessionState()
    data object TimedOut        : SessionState()
    data object BiometricPending: SessionState()
}
