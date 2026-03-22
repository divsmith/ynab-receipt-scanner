package com.ynab.receipts.di

import android.content.Context
import androidx.room.Room
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.ynab.receipts.data.local.LocalReceiptRepository
import com.ynab.receipts.data.local.db.AppDatabase
import com.ynab.receipts.data.local.db.QueueDao
import com.ynab.receipts.data.local.db.ReceiptDao
import com.ynab.receipts.data.ocr.DefaultReceiptImagePreprocessor
import com.ynab.receipts.data.ocr.MlKitOcrEngine
import com.ynab.receipts.data.parser.SimpleReceiptParser
import com.ynab.receipts.data.ynab.RetrofitYnabRepository
import com.ynab.receipts.data.ynab.YnabApi
import com.ynab.receipts.domain.repository.ReceiptRepository
import com.ynab.receipts.domain.repository.YnabRepository
import com.ynab.receipts.domain.service.OcrEngine
import com.ynab.receipts.domain.service.ReceiptImagePreprocessor
import com.ynab.receipts.domain.service.ReceiptParser
import com.ynab.receipts.domain.service.TransactionSyncService
import com.ynab.receipts.sync.DefaultTransactionSyncService
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingModule {
    @Binds
    abstract fun bindOcrEngine(impl: MlKitOcrEngine): OcrEngine

    @Binds
    abstract fun bindReceiptImagePreprocessor(impl: DefaultReceiptImagePreprocessor): ReceiptImagePreprocessor

    @Binds
    abstract fun bindReceiptParser(impl: SimpleReceiptParser): ReceiptParser

    @Binds
    abstract fun bindReceiptRepository(impl: LocalReceiptRepository): ReceiptRepository

    @Binds
    abstract fun bindYnabRepository(impl: RetrofitYnabRepository): YnabRepository

    @Binds
    abstract fun bindTransactionSyncService(impl: DefaultTransactionSyncService): TransactionSyncService
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "receipt_scanner.db"
        ).build()
    }

    @Provides
    fun provideReceiptDao(database: AppDatabase): ReceiptDao = database.receiptDao()

    @Provides
    fun provideQueueDao(database: AppDatabase): QueueDao = database.queueDao()

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.ynab.com/v1/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideYnabApi(retrofit: Retrofit): YnabApi = retrofit.create(YnabApi::class.java)
}
