package com.ynab.receiptscanner.data.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.ynab.receiptscanner.core.Constants
import com.ynab.receiptscanner.data.auth.TokenRefreshAuthenticator
import com.ynab.receiptscanner.data.remote.YnabApi
import com.ynab.receiptscanner.data.remote.interceptor.AuthInterceptor
import com.ynab.receiptscanner.data.remote.interceptor.ErrorInterceptor
import com.ynab.receiptscanner.data.remote.interceptor.RetryInterceptor
import com.ynab.receiptscanner.data.remote.interceptor.CacheInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Dagger Hilt module for network dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    /**
     * Provide Moshi JSON parser
     */
    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .add(DateJsonAdapter())
            .build()
    }
    
    /**
     * Provide OkHttp logging interceptor for debugging
     */
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }
    
    /**
     * Provide base OkHttpClient (without auth interceptor)
     * Used for auth-related requests that don't need authentication header
     */
    @Provides
    @Singleton
    @BaseOkHttpClient
    fun provideBaseOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(Constants.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(Constants.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(Constants.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()
    }
    
    /**
     * Provide OkHttpClient with authentication and error handling
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        @BaseOkHttpClient baseClient: OkHttpClient,
        authInterceptor: AuthInterceptor,
        errorInterceptor: ErrorInterceptor,
        tokenRefreshAuthenticator: TokenRefreshAuthenticator,
        retryInterceptor: RetryInterceptor,
        cacheInterceptor: CacheInterceptor
    ): OkHttpClient {
        return baseClient.newBuilder()
            .addInterceptor(cacheInterceptor)     // Add cache first
            .addInterceptor(authInterceptor)
            .addInterceptor(errorInterceptor)
            .addInterceptor(retryInterceptor)     // Add retry interceptor
            .authenticator(tokenRefreshAuthenticator)
            .build()
    }
    
    /**
     * Provide Retrofit instance
     */
    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.YNAB_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }
    
    /**
     * Provide YNAB API service
     */
    @Provides
    @Singleton
    fun provideYnabApi(retrofit: Retrofit): YnabApi {
        return retrofit.create(YnabApi::class.java)
    }
}

/**
 * Qualifier for base OkHttpClient (without auth interceptors)
 */
@Retention(AnnotationRetention.BINARY)
@javax.inject.Qualifier
annotation class BaseOkHttpClient
