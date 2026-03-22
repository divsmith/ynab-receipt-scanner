package com.ynab.receipts.domain.usecase

import com.ynab.receipts.domain.model.OcrDocument
import com.ynab.receipts.domain.model.OcrLine
import com.ynab.receipts.domain.model.ParsedReceipt
import com.ynab.receipts.domain.service.OcrEngine
import com.ynab.receipts.domain.service.ReceiptImagePreprocessor
import com.ynab.receipts.domain.service.ReceiptParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class ExtractReceiptFieldsUseCaseTest {
    @Test
    fun `uses highest quality OCR candidate for parsing`() = runBlocking {
        val original = byteArrayOf(1)
        val enhanced = byteArrayOf(2)
        val rotated = byteArrayOf(3)

        val documentsByImage = mapOf(
            original.contentHashCode() to OcrDocument(rawText = "", lines = emptyList()),
            enhanced.contentHashCode() to OcrDocument(rawText = "Store", lines = listOf(OcrLine("Store"))),
            rotated.contentHashCode() to OcrDocument(
                rawText = "Store\nTotal 12.34\n02/20/2026",
                lines = listOf(OcrLine("Store"), OcrLine("Total 12.34"), OcrLine("02/20/2026"))
            )
        )

        val preprocessor = ReceiptImagePreprocessor { listOf(original, enhanced, rotated) }
        val ocrEngine = FakeOcrEngine(documentsByImage)
        val parser = CapturingParser()
        val useCase = ExtractReceiptFieldsUseCase(ocrEngine, preprocessor, parser)

        useCase.invoke(original, Locale.US)

        assertEquals("Store\nTotal 12.34\n02/20/2026", parser.lastOcr?.rawText)
    }

    @Test
    fun `falls back to original image when preprocessing fails`() = runBlocking {
        val original = byteArrayOf(9)
        val originalDocument = OcrDocument(rawText = "Only original", lines = listOf(OcrLine("Only original")))
        val ocrEngine = FakeOcrEngine(mapOf(original.contentHashCode() to originalDocument))
        val parser = CapturingParser()
        val preprocessor = ReceiptImagePreprocessor { throw IllegalStateException("preprocess failed") }
        val useCase = ExtractReceiptFieldsUseCase(ocrEngine, preprocessor, parser)

        useCase.invoke(original, Locale.US)

        assertEquals(1, ocrEngine.recognizedImages.size)
        assertArrayEquals(original, ocrEngine.recognizedImages.single())
        assertEquals("Only original", parser.lastOcr?.rawText)
    }

    private class FakeOcrEngine(
        private val documentsByHash: Map<Int, OcrDocument>
    ) : OcrEngine {
        val recognizedImages = mutableListOf<ByteArray>()

        override suspend fun recognize(imageBytes: ByteArray): OcrDocument {
            recognizedImages += imageBytes
            return documentsByHash[imageBytes.contentHashCode()]
                ?: OcrDocument(rawText = "", lines = emptyList())
        }
    }

    private class CapturingParser : ReceiptParser {
        var lastOcr: OcrDocument? = null

        override suspend fun parse(ocr: OcrDocument, locale: Locale): ParsedReceipt {
            lastOcr = ocr
            return ParsedReceipt(
                payee = null,
                amountMinor = null,
                currency = "USD",
                date = null,
                sourceOcrText = ocr.rawText
            )
        }
    }
}
