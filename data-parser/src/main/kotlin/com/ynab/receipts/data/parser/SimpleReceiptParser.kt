package com.ynab.receipts.data.parser

import com.ynab.receipts.domain.model.OcrDocument
import com.ynab.receipts.domain.model.ParsedReceipt
import com.ynab.receipts.domain.service.ReceiptParser
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

class SimpleReceiptParser @Inject constructor() : ReceiptParser {
    override suspend fun parse(ocr: OcrDocument, locale: Locale): ParsedReceipt {
        val lines = ocr.lines.map { it.text.trim() }.filter { it.isNotBlank() }
        val payee = lines.firstOrNull()
        val amountMinor = extractAmountMinor(lines)
        val date = extractDate(lines)

        return ParsedReceipt(
            payee = payee,
            amountMinor = amountMinor,
            currency = if (locale.country == "US") "USD" else "USD",
            date = date,
            confidence = mapOf(
                "payee" to if (payee != null) 0.7f else 0.0f,
                "amount" to if (amountMinor != null) 0.7f else 0.0f,
                "date" to if (date != null) 0.7f else 0.0f
            ),
            sourceOcrText = ocr.rawText
        )
    }

    private fun extractAmountMinor(lines: List<String>): Long? {
        val amountRegex = "(\\d+[\\.,]\\d{2})".toRegex()
        return lines.asSequence()
            .flatMap { amountRegex.findAll(it).map { match -> match.value } }
            .mapNotNull { amountString ->
                amountString.replace(",", "").toDoubleOrNull()?.let { (it * 100).toLong() }
            }
            .maxOrNull()
    }

    private fun extractDate(lines: List<String>): LocalDate? {
        val patterns = listOf(
            "(\\d{4})-(\\d{2})-(\\d{2})".toRegex(),
            "(\\d{2})/(\\d{2})/(\\d{4})".toRegex()
        )

        for (line in lines) {
            for (pattern in patterns) {
                val match = pattern.find(line) ?: continue
                val values = match.groupValues.drop(1)
                val date = if (pattern.pattern.startsWith("(\\d{4})")) {
                    LocalDate.of(values[0].toInt(), values[1].toInt(), values[2].toInt())
                } else {
                    LocalDate.of(values[2].toInt(), values[0].toInt(), values[1].toInt())
                }
                return date
            }
        }
        return null
    }
}
