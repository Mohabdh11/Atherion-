package com.aetherion.noc.data.repository

import com.aetherion.noc.core.network.TokenRefreshProvider
import com.aetherion.noc.core.security.SecurityManager
import com.aetherion.noc.data.remote.api.AetherionAuthApi
import com.aetherion.noc.data.remote.dto.LoginRequestDto
import com.aetherion.noc.data.remote.dto.toDomain
import com.aetherion.noc.domain.model.AuthCredentials
import com.aetherion.noc.domain.model.AuthToken
import com.aetherion.noc.domain.model.Result
import com.aetherion.noc.domain.model.UserProfile
import com.aetherion.noc.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

// ═══════════════════════════════════════════════════════════════════════════
// Aetherion Mobile NOC — Auth Repository Implementation
// Developer: Mohammad Abdalftah Ibrahime
// ═══════════════════════════════════════════════════════════════════════════

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AetherionAuthApi,
    private val securityManager: SecurityManager
) : AuthRepository, TokenRefreshProvider {

    override suspend fun login(credentials: AuthCredentials): Result<AuthToken> {
        return try {
            val response = authApi.login(
                LoginRequestDto(
                    username = credentials.username,
                    password = credentials.password,
                    tenantId = credentials.tenantId,
                    mfaCode  = credentials.mfaCode
                )
            )
            if (response.isSuccessful) {
                val body = response.body() ?: return Result.Error(Exception("Empty response"))
                val token = body.toDomain()
                securityManager.saveToken(token)
                securityManager.saveTenantId(credentials.tenantId)
                Result.Success(token)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Login failed (${response.code()})"
                Timber.w("Login failed: ${response.code()}")
                Result.Error(Exception(errorMsg), errorMsg)
            }
        } catch (e: Exception) {
            Timber.e(e, "Login exception")
            Result.Error(e, "Network error: ${e.message}")
        }
    }

    override suspend fun refreshToken(): Result<AuthToken> {
        return try {
            val refreshToken = securityManager.getRefreshToken()
                ?: return Result.Error(Exception("No refresh token available"))
            val response = authApi.refreshToken("Bearer $refreshToken")
            if (response.isSuccessful) {
                val body = response.body() ?: return Result.Error(Exception("Empty response"))
                val token = body.toDomain()
                securityManager.saveToken(token)
                Result.Success(token)
            } else {
                securityManager.clearTokens()
                Result.Error(Exception("Token refresh failed"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Token refresh exception")
            Result.Error(e)
        }
    }

    override suspend fun logout() {
        try {
            authApi.logout()
        } catch (e: Exception) {
            Timber.w("Logout API call failed (${e.message}), clearing local tokens anyway")
        } finally {
            securityManager.clearTokens()
        }
    }

    override suspend fun getStoredToken(): AuthToken? = securityManager.getToken()

    override suspend fun getCurrentUser(): Result<UserProfile> {
        return try {
            val response = authApi.getCurrentUser()
            if (response.isSuccessful) {
                val body = response.body() ?: return Result.Error(Exception("Empty user response"))
                Result.Success(body.toDomain())
            } else {
                Result.Error(Exception("Failed to fetch user (${response.code()})"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Get current user exception")
            Result.Error(e)
        }
    }

    override fun isAuthenticated(): Boolean = securityManager.isAuthenticated()

    override fun observeAuthState(): Flow<Boolean> =
        securityManager.sessionState.map { state ->
            state is com.aetherion.noc.core.security.SessionState.Authenticated
        }

    // TokenRefreshProvider implementation
    override suspend fun refreshToken(): Boolean {
        return when (refreshToken()) {
            is Result.Success -> true
            else -> false
        }
    }
}
