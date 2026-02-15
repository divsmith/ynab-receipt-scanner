package com.ynab.receiptscanner.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ynab.receiptscanner.data.local.converter.Converters
import com.ynab.receiptscanner.data.local.dao.*
import com.ynab.receiptscanner.data.local.entity.*
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

/**
 * Main Room database for YNAB Receipt Scanner
 * Encrypted using SQLCipher for secure storage of sensitive receipt data
 * 
 * @version 1 - Initial database schema
 */
@Database(
    entities = [
        ReceiptEntity::class,
        LineItemEntity::class,
        PendingTransactionEntity::class,
        BudgetEntity::class,
        AccountEntity::class,
        CategoryEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class YnabDatabase : RoomDatabase() {
    
    /**
     * Access to Receipt data operations
     */
    abstract fun receiptDao(): ReceiptDao
    
    /**
     * Access to LineItem data operations
     */
    abstract fun lineItemDao(): LineItemDao
    
    /**
     * Access to PendingTransaction data operations
     */
    abstract fun pendingTransactionDao(): PendingTransactionDao
    
    /**
     * Access to Budget data operations
     */
    abstract fun budgetDao(): BudgetDao
    
    /**
     * Access to Account data operations
     */
    abstract fun accountDao(): AccountDao
    
    /**
     * Access to Category data operations
     */
    abstract fun categoryDao(): CategoryDao
    
    companion object {
        private const val DATABASE_NAME = "ynab_receipt_scanner.db"
        
        @Volatile
        private var INSTANCE: YnabDatabase? = null
        
        /**
         * Get database instance with encryption
         * @param context Application context
         * @param passphrase Database encryption passphrase
         * @return Encrypted database instance
         */
        fun getInstance(context: Context, passphrase: CharArray): YnabDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context, passphrase).also { INSTANCE = it }
            }
        }
        
        /**
         * Build encrypted database instance
         */
        private fun buildDatabase(context: Context, passphrase: CharArray): YnabDatabase {
            val factory = SupportFactory(SQLiteDatabase.getBytes(passphrase))
            
            return Room.databaseBuilder(
                context.applicationContext,
                YnabDatabase::class.java,
                DATABASE_NAME
            )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration() // For development; remove in production
                .build()
        }
        
        /**
         * Close and clear database instance
         * Used for testing or when passphrase changes
         */
        fun clearInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
