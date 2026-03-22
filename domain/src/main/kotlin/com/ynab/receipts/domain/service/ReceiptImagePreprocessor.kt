package com.ynab.receipts.domain.service

fun interface ReceiptImagePreprocessor {
    fun buildCandidates(imageBytes: ByteArray): List<ByteArray>
}
