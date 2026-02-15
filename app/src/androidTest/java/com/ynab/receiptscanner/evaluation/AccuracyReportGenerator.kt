package com.ynab.receiptscanner.evaluation

import java.text.SimpleDateFormat
import java.util.*

/**
 * Generates markdown report for OCR accuracy evaluation
 */
class AccuracyReportGenerator {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    
    fun generateReport(
        results: List<AccuracyEvaluationTest.EvaluationResult>,
        metrics: AccuracyEvaluationTest.AccuracyMetrics
    ): String {
        return buildString {
            appendHeader()
            appendSummary(metrics)
            appendDetailedResults(results)
            appendRecommendations(metrics)
            appendFooter()
        }
    }
    
    private fun StringBuilder.appendHeader() {
        appendLine("# OCR Accuracy Evaluation Report")
        appendLine()
        appendLine("**Generated:** ${dateFormat.format(Date())}")
        appendLine()
        appendLine("---")
        appendLine()
    }
    
    private fun StringBuilder.appendSummary(metrics: AccuracyEvaluationTest.AccuracyMetrics) {
        appendLine("## Summary")
        appendLine()
        appendLine("| Metric | Value | Status |")
        appendLine("|--------|-------|--------|")
        
        appendLine("| Total Receipts | ${metrics.totalReceipts} | - |")
        appendLine("| Successful OCR | ${metrics.successfulOcr} | ${getStatus(metrics.successfulOcr.toDouble() / metrics.totalReceipts, 0.95)} |")
        appendLine("| Payee Accuracy | ${formatPercentage(metrics.payeeAccuracy)} | ${getStatus(metrics.payeeAccuracy, 0.85)} |")
        appendLine("| Amount Accuracy | ${formatPercentage(metrics.amountAccuracy)} | ${getStatus(metrics.amountAccuracy, 0.90)} |")
        appendLine("| Date Accuracy | ${formatPercentage(metrics.dateAccuracy)} | ${getStatus(metrics.dateAccuracy, 0.85)} |")
        appendLine("| Average Confidence | ${String.format("%.2f", metrics.averageConfidence)} | ${getStatus(metrics.averageConfidence, 0.80)} |")
        
        appendLine()
    }
    
    private fun StringBuilder.appendDetailedResults(results: List<AccuracyEvaluationTest.EvaluationResult>) {
        appendLine("## Detailed Results")
        appendLine()
        appendLine("| Receipt | Payee | Amount | Date | Confidence | Status |")
        appendLine("|---------|-------|--------|------|------------|--------|")
        
        results.forEach { result ->
            val payeeIcon = if (result.payeeMatch) "✅" else "❌"
            val amountIcon = if (result.amountMatch) "✅" else "❌"
            val dateIcon = if (result.dateMatch) "✅" else "❌"
            val status = if (result.success) "✅ Success" else "❌ Failed"
            
            appendLine("| ${result.filename} | $payeeIcon | $amountIcon | $dateIcon | ${String.format("%.2f", result.confidence)} | $status |")
        }
        
        appendLine()
    }
    
    private fun StringBuilder.appendRecommendations(metrics: AccuracyEvaluationTest.AccuracyMetrics) {
        appendLine("## Recommendations")
        appendLine()
        
        if (metrics.payeeAccuracy < 0.85) {
            appendLine("### ⚠️ Low Payee Accuracy")
            appendLine("- Current: ${formatPercentage(metrics.payeeAccuracy)}")
            appendLine("- Target: 85%+")
            appendLine("- **Action**: Improve payee extraction algorithm, add more merchant name patterns")
            appendLine()
        }
        
        if (metrics.amountAccuracy < 0.90) {
            appendLine("### ⚠️ Low Amount Accuracy")
            appendLine("- Current: ${formatPercentage(metrics.amountAccuracy)}")
            appendLine("- Target: 90%+")
            appendLine("- **Action**: Enhance amount parsing regex, handle more currency formats")
            appendLine()
        }
        
        if (metrics.dateAccuracy < 0.85) {
            appendLine("### ⚠️ Low Date Accuracy")
            appendLine("- Current: ${formatPercentage(metrics.dateAccuracy)}")
            appendLine("- Target: 85%+")
            appendLine("- **Action**: Add more date format patterns, improve date extraction logic")
            appendLine()
        }
        
        if (metrics.averageConfidence < 0.80) {
            appendLine("### ⚠️ Low Confidence Score")
            appendLine("- Current: ${String.format("%.2f", metrics.averageConfidence)}")
            appendLine("- Target: 0.80+")
            appendLine("- **Action**: Improve image preprocessing, use higher quality test images")
            appendLine()
        }
        
        if (metrics.payeeAccuracy >= 0.85 && 
            metrics.amountAccuracy >= 0.90 && 
            metrics.dateAccuracy >= 0.85 && 
            metrics.averageConfidence >= 0.80) {
            appendLine("### ✅ All Metrics Meet Targets")
            appendLine("The OCR system is performing well across all measured metrics.")
            appendLine()
        }
    }
    
    private fun StringBuilder.appendFooter() {
        appendLine("---")
        appendLine()
        appendLine("## Legend")
        appendLine()
        appendLine("- ✅ **Pass**: Meets or exceeds target threshold")
        appendLine("- ❌ **Fail**: Below target threshold")
        appendLine()
        appendLine("## Targets")
        appendLine()
        appendLine("- **Payee Accuracy**: 85%+")
        appendLine("- **Amount Accuracy**: 90%+")
        appendLine("- **Date Accuracy**: 85%+")
        appendLine("- **Confidence**: 0.80+")
        appendLine()
    }
    
    private fun formatPercentage(value: Double): String {
        return String.format("%.1f%%", value * 100)
    }
    
    private fun getStatus(value: Double, threshold: Double): String {
        return if (value >= threshold) "✅ Pass" else "❌ Fail"
    }
}
