package com.receiptscanner.di

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.time.Instant

/**
 * Moshi adapter for java.time.Instant serialization
 */
class InstantAdapter {
    @ToJson
    fun toJson(instant: Instant): Long {
        return instant.epochSecond
    }

    @FromJson
    fun fromJson(epochSecond: Long): Instant {
        return Instant.ofEpochSecond(epochSecond)
    }
}
