package com.aetherion.noc.core.di

import android.content.Context
import androidx.room.Room
import com.aetherion.noc.BuildConfig
import com.aetherion.noc.core.network.AetherionHttpClientFactory
import com.aetherion.noc.core.network.AuthInterceptor
import com.aetherion.noc.core.network.TokenRefreshInterceptor
import com.aetherion.noc.core.network.TokenRefreshProvider
import com.aetherion.noc.core.security.SecurityManager
import com.aetherion.noc.data.local.AetherionDatabase
import com.aetherion.noc.data.remote.api.*
import com.aetherion.noc.data.repository.*
import com.aetherion.noc.domain.repository.*
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import javax.inject.Singleton

// ═══════════════════════════════════════════════════════════════════════════
// Aetherion Mobile NOC — Hilt Dependency Injection Modules
// Developer: Mohammad Abdalftah Ibrahime
// ═══════════════════════════════════════════════════════════════════════════

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(factory: AetherionHttpClientFactory) = factory.create()

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: okhttp3.OkHttpClient,
        json: Json
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides @Singleton
    fun provideAuthApi(retrofit: Retrofit): AetherionAuthApi =
        retrofit.create(AetherionAuthApi::class.java)

    @Provides @Singleton
    fun provideDashboardApi(retrofit: Retrofit): AetherionDashboardApi =
        retrofit.create(AetherionDashboardApi::class.java)

    @Provides @Singleton
    fun provideAlertApi(retrofit: Retrofit): AetherionAlertApi =
        retrofit.create(AetherionAlertApi::class.java)

    @Provides @Singleton
    fun provideDeviceApi(retrofit: Retrofit): AetherionDeviceApi =
        retrofit.create(AetherionDeviceApi::class.java)

    @Provides @Singleton
    fun provideTopologyApi(retrofit: Retrofit): AetherionTopologyApi =
        retrofit.create(AetherionTopologyApi::class.java)

    @Provides @Singleton
    fun provideAiApi(retrofit: Retrofit): AetherionAiApi =
        retrofit.create(AetherionAiApi::class.java)

    @Provides @Singleton
    fun provideGeoApi(retrofit: Retrofit): AetherionGeoApi =
        retrofit.create(AetherionGeoApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AetherionDatabase =
        Room.databaseBuilder(
            context,
            AetherionDatabase::class.java,
            "aetherion_noc.db"
        )
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    fun provideAlertDao(db: AetherionDatabase) = db.alertDao()

    @Provides
    fun provideDeviceDao(db: AetherionDatabase) = db.deviceDao()

    @Provides
    fun provideDashboardDao(db: AetherionDatabase) = db.dashboardDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindDashboardRepository(impl: DashboardRepositoryImpl): DashboardRepository

    @Binds @Singleton
    abstract fun bindAlertRepository(impl: AlertRepositoryImpl): AlertRepository

    @Binds @Singleton
    abstract fun bindDeviceRepository(impl: DeviceRepositoryImpl): DeviceRepository

    @Binds @Singleton
    abstract fun bindTopologyRepository(impl: TopologyRepositoryImpl): TopologyRepository

    @Binds @Singleton
    abstract fun bindAiInsightRepository(impl: AiInsightRepositoryImpl): AiInsightRepository

    @Binds @Singleton
    abstract fun bindGeoRepository(impl: GeoRepositoryImpl): GeoRepository

    @Binds @Singleton
    abstract fun bindTokenRefreshProvider(impl: AuthRepositoryImpl): TokenRefreshProvider
}
