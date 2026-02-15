package com.ynab.receiptscanner.data.di

import android.content.Context
import com.ynab.receiptscanner.data.local.YnabDatabase
import com.ynab.receiptscanner.data.local.dao.LineItemDao
import com.ynab.receiptscanner.data.local.dao.PendingTransactionDao
import com.ynab.receiptscanner.data.local.dao.ReceiptDao
import com.ynab.receiptscanner.data.security.EncryptionHelper
import com.ynab.receiptscanner.data.security.KeystoreManager
import com.ynab.receiptscanner.data.security.SecureStorage
import com.ynab.receiptscanner.data.security.SecureStorageImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing data layer dependencies
 * Includes database, DAOs, security, and preferences
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    
    /**
     * Provide KeystoreManager singleton
     */
    @Provides
    @Singleton
    fun provideKeystoreManager(): KeystoreManager {
        return KeystoreManager()
    }
    
    /**
     * Provide EncryptionHelper singleton
     */
    @Provides
    @Singleton
    fun provideEncryptionHelper(
        keystoreManager: KeystoreManager
    ): EncryptionHelper {
        return EncryptionHelper(keystoreManager)
    }
    
    /**
     * Provide SecureStorage singleton
     */
    @Provides
    @Singleton
    fun provideSecureStorage(
        @ApplicationContext context: Context
    ): SecureStorage {
        return SecureStorageImpl(context)
    }
    
    /**
     * Provide YnabDatabase singleton with encryption
     * Uses KeystoreManager to get or create database encryption key
     */
    @Provides
    @Singleton
    fun provideYnabDatabase(
        @ApplicationContext context: Context,
        keystoreManager: KeystoreManager
    ): YnabDatabase {
        // Get or create database encryption key from Android Keystore
        val key = keystoreManager.getOrCreateKey(KeystoreManager.DATABASE_KEY_ALIAS)
        
        // Convert SecretKey to passphrase for SQLCipher
        val passphrase = key.encoded.toString(Charsets.UTF_8).toCharArray()
        
        return YnabDatabase.getInstance(context, passphrase)
    }
    
    /**
     * Provide ReceiptDao
     */
    @Provides
    @Singleton
    fun provideReceiptDao(database: YnabDatabase): ReceiptDao {
        return database.receiptDao()
    }
    
    /**
     * Provide LineItemDao
     */
    @Provides
    @Singleton
    fun provideLineItemDao(database: YnabDatabase): LineItemDao {
        return database.lineItemDao()
    }
    
    /**
     * Provide PendingTransactionDao
     */
    @Provides
    @Singleton
    fun providePendingTransactionDao(database: YnabDatabase): PendingTransactionDao {
        return database.pendingTransactionDao()
    }
    
    /**
     * Provide BudgetDao
     */
    @Provides
    @Singleton
    fun provideBudgetDao(database: YnabDatabase): com.ynab.receiptscanner.data.local.dao.BudgetDao {
        return database.budgetDao()
    }
    
    /**
     * Provide AccountDao
     */
    @Provides
    @Singleton
    fun provideAccountDao(database: YnabDatabase): com.ynab.receiptscanner.data.local.dao.AccountDao {
        return database.accountDao()
    }
    
    /**
     * Provide CategoryDao
     */
    @Provides
    @Singleton
    fun provideCategoryDao(database: YnabDatabase): com.ynab.receiptscanner.data.local.dao.CategoryDao {
        return database.categoryDao()
    }
    
    /**
     * Provide YnabRepository implementation
     */
    @Provides
    @Singleton
    fun provideYnabRepository(
        api: com.ynab.receiptscanner.data.remote.YnabApi,
        budgetDao: com.ynab.receiptscanner.data.local.dao.BudgetDao,
        accountDao: com.ynab.receiptscanner.data.local.dao.AccountDao,
        categoryDao: com.ynab.receiptscanner.data.local.dao.CategoryDao,
        pendingTransactionDao: PendingTransactionDao,
        transactionSyncManager: com.ynab.receiptscanner.data.sync.TransactionSyncManager,
        ynabMapper: com.ynab.receiptscanner.data.mapper.YnabMapper
    ): com.ynab.receiptscanner.domain.repository.YnabRepository {
        return com.ynab.receiptscanner.data.repository.YnabRepositoryImpl(
            api,
            budgetDao,
            accountDao,
            categoryDao,
            pendingTransactionDao,
            transactionSyncManager,
            ynabMapper
        )
    }
    
    /**
     * Provide OcrEngine implementation
     */
    @Provides
    @Singleton
    fun provideOcrEngine(
        imagePreprocessor: com.ynab.receiptscanner.data.ocr.ImagePreprocessor
    ): com.ynab.receiptscanner.data.ocr.OcrEngine {
        return com.ynab.receiptscanner.data.ocr.MLKitOcrEngine(imagePreprocessor)
    }
    
    /**
     * Provide ReceiptRepository implementation
     */
    @Provides
    @Singleton
    fun provideReceiptRepository(
        receiptDao: ReceiptDao,
        imageStorageManager: com.ynab.receiptscanner.data.storage.ImageStorageManager
    ): com.ynab.receiptscanner.domain.repository.ReceiptRepository {
        return com.ynab.receiptscanner.data.repository.ReceiptRepositoryImpl(
            receiptDao,
            imageStorageManager
        )
    }
    
    /**
     * Provide ConnectivityMonitor singleton
     */
    @Provides
    @Singleton
    fun provideConnectivityMonitor(
        @ApplicationContext context: Context
    ): com.ynab.receiptscanner.data.network.ConnectivityMonitor {
        return com.ynab.receiptscanner.data.network.ConnectivityMonitor(context)
    }
    
    /**
     * Provide ConnectivityRepository implementation
     */
    @Provides
    @Singleton
    fun provideConnectivityRepository(
        connectivityMonitor: com.ynab.receiptscanner.data.network.ConnectivityMonitor
    ): com.ynab.receiptscanner.domain.repository.ConnectivityRepository {
        return connectivityMonitor
    }
    
    /**
     * Provide SyncStatusTracker singleton
     */
    @Provides
    @Singleton
    fun provideSyncStatusTracker(
        receiptDao: ReceiptDao
    ): com.ynab.receiptscanner.data.sync.SyncStatusTracker {
        return com.ynab.receiptscanner.data.sync.SyncStatusTracker(receiptDao)
    }
}
