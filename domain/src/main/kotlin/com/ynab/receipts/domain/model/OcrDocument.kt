package com.ynab.receipts.domain.model

data class OcrLine(
    val text: String,
    val confidence: Float? = null
)

data class OcrDocument(
    val rawText: String,
    val lines: List<OcrLine>
)
