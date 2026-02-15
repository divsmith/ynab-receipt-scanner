package com.ynab.receiptscanner.data.local.converter

import androidx.room.TypeConverter
import com.ynab.receiptscanner.domain.model.Currency
import com.ynab.receiptscanner.domain.model.SyncStatus
import java.util.Date

/**
 * Room type converters for complex data types
 * Handles conversion between complex types and primitive types for database storage
 */
class Converters {
    
    /**
     * Convert timestamp to Date
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    
    /**
     * Convert Date to timestamp
     */
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
    
    /**
     * Convert Currency to string (currency code)
     */
    @TypeConverter
    fun fromCurrency(currency: Currency?): String? {
        return currency?.code
    }
    
    /**
     * Convert string to Currency
     */
    @TypeConverter
    fun toCurrency(code: String?): Currency? {
        return code?.let { Currency.fromCode(it) }
    }
    
    /**
     * Convert SyncStatus to string
     */
    @TypeConverter
    fun fromSyncStatus(status: SyncStatus?): String? {
        return status?.name
    }
    
    /**
     * Convert string to SyncStatus
     */
    @TypeConverter
    fun toSyncStatus(status: String?): SyncStatus? {
        return status?.let { 
            try {
                SyncStatus.valueOf(it)
            } catch (e: IllegalArgumentException) {
                SyncStatus.PENDING
            }
        }
    }
    
    /**
     * Convert comma-separated string to list of strings
     */
    @TypeConverter
    fun fromStringList(value: String?): List<String>? {
        return value?.split(",")?.filter { it.isNotBlank() }
    }
    
    /**
     * Convert list of strings to comma-separated string
     */
    @TypeConverter
    fun toStringList(list: List<String>?): String? {
        return list?.joinToString(",")
    }
}
