package com.ynab.receipts.data.ocr

import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.ynab.receipts.domain.model.OcrDocument
import com.ynab.receipts.domain.model.OcrLine
import com.ynab.receipts.domain.service.OcrEngine
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class MlKitOcrEngine @Inject constructor() : OcrEngine {
    override suspend fun recognize(imageBytes: ByteArray): OcrDocument {
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: return OcrDocument(rawText = "", lines = emptyList())

        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val result = recognizer.process(inputImage).await()

        val lines = result.textBlocks
            .flatMap { block -> block.lines }
            .map { line -> OcrLine(text = line.text, confidence = null) }

        val bestEffortText = result.text
        return OcrDocument(
            rawText = bestEffortText,
            lines = lines
        )
    }
}
