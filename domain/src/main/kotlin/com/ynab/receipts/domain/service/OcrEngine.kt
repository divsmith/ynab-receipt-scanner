package com.ynab.receipts.domain.service

import com.ynab.receipts.domain.model.OcrDocument

fun interface OcrEngine {
    suspend fun recognize(imageBytes: ByteArray): OcrDocument
}
