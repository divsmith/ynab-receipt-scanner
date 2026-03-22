package com.ynab.receipts.domain.service

import com.ynab.receipts.domain.model.OcrDocument
import com.ynab.receipts.domain.model.ParsedReceipt
import java.util.Locale

fun interface ReceiptParser {
    suspend fun parse(ocr: OcrDocument, locale: Locale): ParsedReceipt
}
