package com.aetherion.noc.core.network

import com.aetherion.noc.BuildConfig
import com.aetherion.noc.core.security.SecurityManager
import kotlinx.coroutines.runBlocking
import okhttp3.CertificatePinner
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

// ═══════════════════════════════════════════════════════════════════════════
// Aetherion Mobile NOC — OkHttp Network Configuration
// Developer: Mohammad Abdalftah Ibrahime
//
// Implements:
//   • Certificate Pinning (SHA-256)
//   • Auth header injection
//   • Automatic token refresh on 401
//   • Retry logic with exponential backoff
//   • Rate-limit handling (429 → Retry-After)
//   • Tenant ID injection
//   • No sensitive logging in production
// ═══════════════════════════════════════════════════════════════════════════

// ─── Auth Interceptor ─────────────────────────────────────────────────────────

class AuthInterceptor @Inject constructor(
    private val securityManager: SecurityManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        securityManager.refreshActivity()

        val token = securityManager.getAccessToken()
        val tenantId = securityManager.getTenantId()

        val requestBuilder = chain.request().newBuilder()

        if (token != null) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        if (tenantId != null) {
            requestBuilder.addHeader(BuildConfig.TENANT_HEADER, tenantId)
        }
        // Zero-trust: always identify client
        requestBuilder.addHeader("X-Client-App", "AetherionNOC-Android")
        requestBuilder.addHeader("X-Client-Version", BuildConfig.VERSION_NAME)

        return chain.proceed(requestBuilder.build())
    }
}

// ─── Token Refresh Interceptor ────────────────────────────────────────────────

class TokenRefreshInterceptor @Inject constructor(
    private val securityManager: SecurityManager,
    private val tokenRefreshProvider: TokenRefreshProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        if (response.code == 401) {
            response.close()
            Timber.d("Token expired, attempting refresh")

            return runBlocking {
                val refreshed = tokenRefreshProvider.refreshToken()
                if (refreshed) {
                    val newToken = securityManager.getAccessToken()
                    val tenantId = securityManager.getTenantId()
                    val newRequest = chain.request().newBuilder().apply {
                        if (newToken != null) header("Authorization", "Bearer $newToken")
                        if (tenantId != null) header(BuildConfig.TENANT_HEADER, tenantId)
                    }.build()
                    chain.proceed(newRequest)
                } else {
                    Timber.w("Token refresh failed — logging out")
                    securityManager.clearTokens()
                    response
                }
            }
        }
        return response
    }
}

// ─── Retry Interceptor ────────────────────────────────────────────────────────

class RetryInterceptor(
    private val maxRetries: Int = 3
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response: Response? = null
        var attempt = 0
        var lastException: Exception? = null

        while (attempt <= maxRetries) {
            try {
                response?.close()
                response = chain.proceed(request)

                // Retry on 5xx server errors (not 4xx client errors)
                if (response.code in 500..599 && attempt < maxRetries) {
                    val backoffMs = (BACKOFF_BASE_MS * (1 shl attempt)).coerceAtMost(MAX_BACKOFF_MS)
                    Timber.w("Server error ${response.code}, retry ${attempt + 1}/$maxRetries in ${backoffMs}ms")
                    Thread.sleep(backoffMs)
                    attempt++
                    continue
                }
                return response
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries) {
                    val backoffMs = (BACKOFF_BASE_MS * (1 shl attempt)).coerceAtMost(MAX_BACKOFF_MS)
                    Timber.w("Request failed (${e.message}), retry ${attempt + 1}/$maxRetries in ${backoffMs}ms")
                    Thread.sleep(backoffMs)
                    attempt++
                } else {
                    break
                }
            }
        }

        throw lastException ?: Exception("Max retries ($maxRetries) exceeded")
    }

    companion object {
        private const val BACKOFF_BASE_MS = 1000L
        private const val MAX_BACKOFF_MS  = 16_000L
    }
}

// ─── Rate Limit Interceptor ───────────────────────────────────────────────────

class RateLimitInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())

        if (response.code == 429) {
            val retryAfter = response.header("Retry-After")?.toLongOrNull() ?: DEFAULT_WAIT_SEC
            Timber.w("Rate limited by server, waiting ${retryAfter}s")
            response.close()
            Thread.sleep(retryAfter * 1000)
            return chain.proceed(chain.request())
        }
        return response
    }

    companion object {
        private const val DEFAULT_WAIT_SEC = 5L
    }
}

// ─── Token Refresh Provider ───────────────────────────────────────────────────

interface TokenRefreshProvider {
    suspend fun refreshToken(): Boolean
}

// ─── OkHttp Client Builder ────────────────────────────────────────────────────

@Singleton
class AetherionHttpClientFactory @Inject constructor(
    private val authInterceptor: AuthInterceptor,
    private val tokenRefreshInterceptor: TokenRefreshInterceptor
) {
    fun create(): OkHttpClient {
        val certificatePinner = CertificatePinner.Builder()
            .add("*.aetherion.internal", BuildConfig.CERT_PIN_1)
            .add("*.aetherion.internal", BuildConfig.CERT_PIN_2)
            .build()

        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            // Certificate pinning (production)
            .apply {
                if (BuildConfig.STRICT_SSL) {
                    certificatePinner(certificatePinner)
                }
            }
            // Interceptors: order matters
            .addInterceptor(authInterceptor)
            .addInterceptor(tokenRefreshInterceptor)
            .addInterceptor(RateLimitInterceptor())
            .addInterceptor(RetryInterceptor(maxRetries = 3))
            // Logging (only in debug builds — never in production)
            .apply {
                if (BuildConfig.ENABLE_LOGGING) {
                    val logging = HttpLoggingInterceptor { message ->
                        // Strip auth headers from logs
                        if (!message.contains("Authorization") && !message.contains("Bearer")) {
                            Timber.tag("OkHttp").d(message)
                        }
                    }.apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }
                    addInterceptor(logging)
                }
            }
            .build()
    }
}
