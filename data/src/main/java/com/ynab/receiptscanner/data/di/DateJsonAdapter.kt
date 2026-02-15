package com.ynab.receiptscanner.data.di

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Moshi adapter for Date serialization/deserialization
 * Handles YNAB API date format: ISO 8601 (yyyy-MM-dd or yyyy-MM-dd'T'HH:mm:ss'Z')
 */
class DateJsonAdapter {
    
    companion object {
        private val ISO_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        
        private val ISO_DATETIME_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        
        private val ISO_DATETIME_FORMAT_WITH_MILLIS = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
    
    @ToJson
    fun toJson(date: Date): String {
        return ISO_DATE_FORMAT.format(date)
    }
    
    @FromJson
    fun fromJson(dateString: String): Date {
        return try {
            when {
                dateString.contains('T') && dateString.contains('.') -> {
                    ISO_DATETIME_FORMAT_WITH_MILLIS.parse(dateString)
                }
                dateString.contains('T') -> {
                    ISO_DATETIME_FORMAT.parse(dateString)
                }
                else -> {
                    ISO_DATE_FORMAT.parse(dateString)
                }
            } ?: throw IllegalArgumentException("Invalid date format: $dateString")
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse date: $dateString", e)
        }
    }
}
