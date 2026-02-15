package com.ynab.receiptscanner.data.di

import android.content.Context
import com.ynab.receiptscanner.data.analytics.AnalyticsManager
import com.ynab.receiptscanner.data.analytics.FirebaseAnalyticsImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for analytics dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {
    
    /**
     * Provide AnalyticsManager implementation
     * Uses Firebase implementation (stub until Firebase is configured)
     */
    @Provides
    @Singleton
    fun provideAnalyticsManager(
        @ApplicationContext context: Context
    ): AnalyticsManager {
        return FirebaseAnalyticsImpl(context)
    }
}
