package com.ynab.receipts.data.local.db

import androidx.room.TypeConverter
import com.ynab.receipts.domain.model.ReceiptStatus
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class RoomConverters {
    @TypeConverter
    fun fromUuid(value: UUID?): String? = value?.toString()

    @TypeConverter
    fun toUuid(value: String?): UUID? = value?.let(UUID::fromString)

    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun fromReceiptStatus(value: ReceiptStatus?): String? = value?.name

    @TypeConverter
    fun toReceiptStatus(value: String?): ReceiptStatus? = value?.let(ReceiptStatus::valueOf)
}
