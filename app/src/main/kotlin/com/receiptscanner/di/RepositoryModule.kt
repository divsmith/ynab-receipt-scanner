package com.receiptscanner.di

import com.receiptscanner.data.repository.YnabAuthRepositoryImpl
import com.receiptscanner.domain.repository.YnabAuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing repository implementations
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindYnabAuthRepository(
        impl: YnabAuthRepositoryImpl
    ): YnabAuthRepository
}
