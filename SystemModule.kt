package com.aetherion.noc.core.di

import android.app.NotificationManager
import android.content.Context
import com.aetherion.noc.core.logging.AetherionTree
import com.aetherion.noc.core.notifications.AetherionFirebaseService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// ═══════════════════════════════════════════════════════════════════════════
// Aetherion Mobile NOC — System Services DI Module
// Developer: Mohammad Abdalftah Ibrahime
// ═══════════════════════════════════════════════════════════════════════════

@Module
@InstallIn(SingletonComponent::class)
object SystemModule {

    @Provides
    @Singleton
    fun provideNotificationManager(
        @ApplicationContext context: Context
    ): NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
}
