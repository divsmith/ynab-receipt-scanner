package com.ynab.receiptscanner.evaluation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.ynab.receiptscanner.data.ocr.OcrEngine
import com.ynab.receiptscanner.data.parser.ReceiptParser
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import com.ynab.receiptscanner.core.util.Result

/**
 * OCR Accuracy Evaluation Test
 * Processes test receipts and calculates accuracy metrics
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AccuracyEvaluationTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var ocrEngine: OcrEngine
    
    @Inject
    lateinit var receiptParser: ReceiptParser
    
    private lateinit var context: Context
    private lateinit var reportGenerator: AccuracyReportGenerator
    
    @Before
    fun setup() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
        reportGenerator = AccuracyReportGenerator()
    }
    
    @Test
    fun evaluateOcrAccuracy_onTestReceipts() = runTest {
        // Load ground truth
        val groundTruth = loadGroundTruth()
        
        val results = mutableListOf<EvaluationResult>()
        
        // Process each test receipt
        groundTruth.forEach { testCase ->
            val result = processReceipt(testCase)
            results.add(result)
        }
        
        // Calculate metrics
        val metrics = calculateMetrics(results)
        
        // Generate report
        val report = reportGenerator.generateReport(results, metrics)
        saveReport(report)
        
        // Log results
        println("\n=== OCR ACCURACY EVALUATION ===")
        println("Total receipts: ${results.size}")
        println("Payee accuracy: ${String.format("%.2f%%", metrics.payeeAccuracy * 100)}")
        println("Amount accuracy: ${String.format("%.2f%%", metrics.amountAccuracy * 100)}")
        println("Date accuracy: ${String.format("%.2f%%", metrics.dateAccuracy * 100)}")
        println("Average confidence: ${String.format("%.2f", metrics.averageConfidence)}")
        println("Report saved to: ${getReportPath()}")
        
        // Assert minimum thresholds
        assertThat(metrics.payeeAccuracy).isAtLeast(0.70) // 70% minimum
        assertThat(metrics.amountAccuracy).isAtLeast(0.75) // 75% minimum
        assertThat(metrics.averageConfidence).isAtLeast(0.60) // 0.60 minimum
    }
    
    private suspend fun processReceipt(testCase: GroundTruth): EvaluationResult {
        // Load image
        val bitmap = loadReceiptImage(testCase.filename)
        
        if (bitmap == null) {
            return EvaluationResult(
                filename = testCase.filename,
                success = false,
                errorMessage = "Failed to load image",
                expected = testCase,
                extracted = null,
                payeeMatch = false,
                amountMatch = false,
                dateMatch = false,
                confidence = 0.0
            )
        }
        
        // Run OCR
        val ocrResult = ocrEngine.processImage(bitmap)
        
        if (ocrResult !is Result.Success) {
            return EvaluationResult(
                filename = testCase.filename,
                success = false,
                errorMessage = "OCR failed: ${(ocrResult as? Result.Error)?.message}",
                expected = testCase,
                extracted = null,
                payeeMatch = false,
                amountMatch = false,
                dateMatch = false,
                confidence = 0.0
            )
        }
        
        // Parse receipt
        val text = ocrResult.data.text
        val parsed = receiptParser.parse(text)
        
        // Extract values
        val extracted = ExtractedData(
            payee = parsed.payee,
            amount = parsed.amount,
            date = parsed.date
        )
        
        // Compare with ground truth
        val payeeMatch = comparePayee(extracted.payee, testCase.payee)
        val amountMatch = compareAmount(extracted.amount, testCase.amount)
        val dateMatch = compareData(extracted.date, testCase.date)
        
        return EvaluationResult(
            filename = testCase.filename,
            success = true,
            errorMessage = null,
            expected = testCase,
            extracted = extracted,
            payeeMatch = payeeMatch,
            amountMatch = amountMatch,
            dateMatch = dateMatch,
            confidence = parsed.confidence
        )
    }
    
    private fun loadReceiptImage(filename: String): Bitmap? {
        return try {
            val inputStream = context.assets.open("test_receipts/$filename")
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        }
    }
    
    private fun loadGroundTruth(): List<GroundTruth> {
        val json = context.assets.open("test_receipts/ground_truth.json")
            .bufferedReader()
            .use { it.readText() }
        
        val jsonArray = JSONArray(json)
        val results = mutableListOf<GroundTruth>()
        
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            results.add(GroundTruth(
                filename = obj.getString("filename"),
                payee = obj.getString("payee"),
                amount = obj.getDouble("amount"),
                date = obj.getString("date"),
                currency = obj.optString("currency", "USD"),
                tax = obj.optDouble("tax", 0.0)
            ))
        }
        
        return results
    }
    
    private fun comparePayee(extracted: String?, expected: String): Boolean {
        if (extracted == null) return false
        
        // Normalize for comparison
        val extractedNorm = extracted.trim().lowercase()
        val expectedNorm = expected.trim().lowercase()
        
        // Exact match
        if (extractedNorm == expectedNorm) return true
        
        // Contains match (store name might have extra words)
        if (extractedNorm.contains(expectedNorm) || expectedNorm.contains(extractedNorm)) {
            return true
        }
        
        // Fuzzy match (Levenshtein distance)
        return calculateSimilarity(extractedNorm, expectedNorm) > 0.8
    }
    
    private fun compareAmount(extracted: Double?, expected: Double): Boolean {
        if (extracted == null) return false
        
        // Allow small floating point differences (within $0.01)
        return Math.abs(extracted - expected) < 0.01
    }
    
    private fun compareDate(extracted: Date?, expected: String): Boolean {
        if (extracted == null) return false
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val extractedStr = dateFormat.format(extracted)
        
        return extractedStr == expected
    }
    
    private fun calculateSimilarity(s1: String, s2: String): Double {
        val longer = if (s1.length > s2.length) s1 else s2
        val shorter = if (s1.length > s2.length) s2 else s1
        
        val longerLength = longer.length
        if (longerLength == 0) return 1.0
        
        return (longerLength - editDistance(longer, shorter)) / longerLength.toDouble()
    }
    
    private fun editDistance(s1: String, s2: String): Int {
        val costs = IntArray(s2.length + 1)
        for (i in 0..s1.length) {
            var lastValue = i
            for (j in 0..s2.length) {
                if (i == 0) {
                    costs[j] = j
                } else if (j > 0) {
                    var newValue = costs[j - 1]
                    if (s1[i - 1] != s2[j - 1]) {
                        newValue = minOf(minOf(newValue, lastValue), costs[j]) + 1
                    }
                    costs[j - 1] = lastValue
                    lastValue = newValue
                }
            }
            if (i > 0) costs[s2.length] = lastValue
        }
        return costs[s2.length]
    }
    
    private fun calculateMetrics(results: List<EvaluationResult>): AccuracyMetrics {
        val successful = results.filter { it.success }
        
        return AccuracyMetrics(
            totalReceipts = results.size,
            successfulOcr = successful.size,
            payeeAccuracy = successful.count { it.payeeMatch }.toDouble() / successful.size,
            amountAccuracy = successful.count { it.amountMatch }.toDouble() / successful.size,
            dateAccuracy = successful.count { it.dateMatch }.toDouble() / successful.size,
            averageConfidence = successful.map { it.confidence }.average()
        )
    }
    
    private fun saveReport(report: String) {
        val file = File(getReportPath())
        file.parentFile?.mkdirs()
        file.writeText(report)
    }
    
    private fun getReportPath(): String {
        return "${context.getExternalFilesDir(null)}/ocr_accuracy_report.md"
    }
    
    data class GroundTruth(
        val filename: String,
        val payee: String,
        val amount: Double,
        val date: String,
        val currency: String,
        val tax: Double
    )
    
    data class ExtractedData(
        val payee: String?,
        val amount: Double?,
        val date: Date?
    )
    
    data class EvaluationResult(
        val filename: String,
        val success: Boolean,
        val errorMessage: String?,
        val expected: GroundTruth,
        val extracted: ExtractedData?,
        val payeeMatch: Boolean,
        val amountMatch: Boolean,
        val dateMatch: Boolean,
        val confidence: Double
    )
    
    data class AccuracyMetrics(
        val totalReceipts: Int,
        val successfulOcr: Int,
        val payeeAccuracy: Double,
        val amountAccuracy: Double,
        val dateAccuracy: Double,
        val averageConfidence: Double
    )
}
