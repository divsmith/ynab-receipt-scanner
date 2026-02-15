# OCR Parser Guide

## Table of Contents
- [Overview](#overview)
- [OCR Pipeline](#ocr-pipeline)
- [ML Kit Configuration](#ml-kit-configuration)
- [Receipt Parser Architecture](#receipt-parser-architecture)
- [Adding New Receipt Formats](#adding-new-receipt-formats)
- [Improving Accuracy](#improving-accuracy)
- [Adding New Languages](#adding-new-languages)
- [Custom Field Extractors](#custom-field-extractors)
- [Confidence Scoring](#confidence-scoring)
- [Testing OCR](#testing-ocr)

## Overview

The YNAB Receipt Scanner uses **Google ML Kit Text Recognition** for OCR (Optical Character Recognition). The OCR pipeline extracts text from receipt images and parses it into structured fields (merchant, date, amount, etc.).

### Key Components
- **ML Kit**: Google's on-device OCR engine
- **ReceiptParser**: Parses raw OCR text into structured fields
- **Field Extractors**: Specialized parsers for each field type
- **Confidence Scoring**: Quality assessment of extracted data

## OCR Pipeline

```
┌───────────────┐
│ Receipt Image │
└───────┬───────┘
        │
        ▼
┌──────────────────────┐
│  ML Kit Recognition  │  ← Extract raw text
│  (Latin Script)      │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│  Text Preprocessing  │  ← Clean & normalize
│  - Trim whitespace   │
│  - Fix common errors │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│  Receipt Parser      │  ← Parse structured fields
│  - Merchant          │
│  - Date              │
│  - Amount            │
│  - Items (optional)  │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│  Validation          │  ← Check field validity
│  - Amount > 0        │
│  - Valid date        │
│  - Confidence score  │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│  Structured Receipt  │
│     (Output)         │
└──────────────────────┘
```

## ML Kit Configuration

### Setup

Add to `app/build.gradle.kts`:
```kotlin
dependencies {
    implementation("com.google.mlkit:text-recognition:16.0.0")
}
```

### Basic Usage

```kotlin
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class MlKitOcrRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : OcrRepository {
    
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    override suspend fun extractText(imagePath: String): Result<OcrResult> = 
        withContext(ioDispatcher) {
            try {
                val image = InputImage.fromFilePath(context, Uri.parse(imagePath))
                
                val text = suspendCancellableCoroutine<Text> { continuation ->
                    recognizer.process(image)
                        .addOnSuccessListener { visionText ->
                            continuation.resume(visionText)
                        }
                        .addOnFailureListener { e ->
                            continuation.resumeWithException(e)
                        }
                }
                
                val extractedText = text.text
                val confidence = calculateConfidence(text)
                
                Result.Success(OcrResult(extractedText, confidence))
            } catch (e: Exception) {
                Result.Error(OcrException("OCR failed", e))
            }
        }
    
    private fun calculateConfidence(text: Text): Double {
        if (text.textBlocks.isEmpty()) return 0.0
        
        val avgConfidence = text.textBlocks
            .flatMap { it.lines }
            .flatMap { it.elements }
            .map { it.confidence ?: 0f }
            .average()
        
        return avgConfidence.coerceIn(0.0, 1.0)
    }
}
```

### Advanced Configuration

For better accuracy with receipts:
```kotlin
val options = TextRecognizerOptions.Builder()
    .setExecutor(Executors.newSingleThreadExecutor())
    .build()

val recognizer = TextRecognition.getClient(options)
```

### Image Preprocessing

Improve OCR accuracy by preprocessing images:
```kotlin
fun preprocessImage(bitmap: Bitmap): Bitmap {
    var processed = bitmap
    
    // 1. Convert to grayscale
    processed = toGrayscale(processed)
    
    // 2. Increase contrast
    processed = adjustContrast(processed, 1.5f)
    
    // 3. Remove noise
    processed = denoise(processed)
    
    // 4. Straighten (deskew)
    processed = deskew(processed)
    
    return processed
}

fun toGrayscale(bitmap: Bitmap): Bitmap {
    val colorMatrix = ColorMatrix().apply {
        setSaturation(0f)
    }
    val paint = Paint().apply {
        colorFilter = ColorMatrixColorFilter(colorMatrix)
    }
    val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    Canvas(result).drawBitmap(bitmap, 0f, 0f, paint)
    return result
}
```

## Receipt Parser Architecture

### Parser Interface

```kotlin
interface ReceiptParser {
    fun parse(ocrText: String): Receipt
}

class ReceiptParserImpl @Inject constructor(
    private val merchantExtractor: MerchantExtractor,
    private val dateExtractor: DateExtractor,
    private val amountExtractor: AmountExtractor,
    private val itemExtractor: ItemExtractor
) : ReceiptParser {
    
    override fun parse(ocrText: String): Receipt {
        val cleanedText = preprocessText(ocrText)
        val lines = cleanedText.lines()
        
        return Receipt(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            merchantName = merchantExtractor.extract(lines),
            amount = amountExtractor.extract(lines),
            date = dateExtractor.extract(lines),
            rawOcrText = ocrText,
            fields = listOf(
                ReceiptField.Merchant(merchantExtractor.extract(lines)),
                ReceiptField.Amount(amountExtractor.extract(lines)),
                ReceiptField.Date(dateExtractor.extract(lines))
            )
        )
    }
    
    private fun preprocessText(text: String): String {
        return text
            .trim()
            .replace("\\s+".toRegex(), " ")
            .replace("[|]".toRegex(), "I")  // Common OCR mistake
            .replace("[O0]".toRegex()) { 
                // Disambiguate O vs 0 based on context
                if (it.value.isDigitContext()) "0" else "O"
            }
    }
}
```

### Field Extractors

#### Merchant Extractor
```kotlin
class MerchantExtractor {
    private val knownMerchants = setOf(
        "walmart", "target", "starbucks", "amazon", 
        "costco", "safeway", "whole foods", "trader joe's"
    )
    
    fun extract(lines: List<String>): String? {
        // Strategy 1: Check first few lines for known merchants
        for (line in lines.take(5)) {
            val normalized = line.lowercase().trim()
            knownMerchants.forEach { merchant ->
                if (normalized.contains(merchant)) {
                    return merchant.capitalizeWords()
                }
            }
        }
        
        // Strategy 2: Use first non-empty line as merchant
        val firstLine = lines.firstOrNull { it.isNotBlank() }
        if (firstLine != null && firstLine.length < 50) {
            return firstLine.trim()
        }
        
        // Strategy 3: Look for "Store #" pattern
        val storePattern = Regex("(.+?)\\s+(?:store|#)\\d+", RegexOption.IGNORE_CASE)
        lines.forEach { line ->
            storePattern.find(line)?.let {
                return it.groupValues[1].trim()
            }
        }
        
        return null
    }
}
```

#### Date Extractor
```kotlin
class DateExtractor {
    private val datePatterns = listOf(
        // MM/DD/YYYY
        Regex("""(\d{1,2})/(\d{1,2})/(\d{4})"""),
        // MM-DD-YYYY
        Regex("""(\d{1,2})-(\d{1,2})-(\d{4})"""),
        // YYYY-MM-DD
        Regex("""(\d{4})-(\d{1,2})-(\d{1,2})"""),
        // DD MMM YYYY (e.g., 14 Feb 2026)
        Regex("""(\d{1,2})\s+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\s+(\d{4})""", RegexOption.IGNORE_CASE),
        // Month DD, YYYY (e.g., February 14, 2026)
        Regex("""(January|February|March|April|May|June|July|August|September|October|November|December)\s+(\d{1,2}),?\s+(\d{4})""", RegexOption.IGNORE_CASE)
    )
    
    fun extract(lines: List<String>): String? {
        for (line in lines) {
            for (pattern in datePatterns) {
                pattern.find(line)?.let { match ->
                    return normalizeDate(match.value)
                }
            }
        }
        return null
    }
    
    private fun normalizeDate(dateString: String): String {
        // Parse various formats and output YYYY-MM-DD
        val parsers = listOf(
            SimpleDateFormat("MM/dd/yyyy", Locale.US),
            SimpleDateFormat("MM-dd-yyyy", Locale.US),
            SimpleDateFormat("yyyy-MM-dd", Locale.US),
            SimpleDateFormat("dd MMM yyyy", Locale.US),
            SimpleDateFormat("MMMM dd, yyyy", Locale.US)
        )
        
        for (parser in parsers) {
            try {
                val date = parser.parse(dateString)
                return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date!!)
            } catch (e: Exception) {
                continue
            }
        }
        
        return dateString
    }
}
```

#### Amount Extractor
```kotlin
class AmountExtractor {
    private val amountPatterns = listOf(
        // $XX.XX or $XX,XXX.XX
        Regex("""\$\s*(\d{1,3}(?:,\d{3})*\.\d{2})"""),
        // Total: XX.XX
        Regex("""total:?\s*\$?\s*(\d{1,3}(?:,\d{3})*\.\d{2})""", RegexOption.IGNORE_CASE),
        // Plain XX.XX (fallback)
        Regex("""(?<!\d)(\d{1,3}(?:,\d{3})*\.\d{2})(?!\d)""")
    )
    
    private val totalKeywords = setOf(
        "total", "amount due", "balance", "grand total", 
        "total amount", "amount", "sum"
    )
    
    fun extract(lines: List<String>): Double? {
        // Strategy 1: Look for explicit "Total" line
        for (line in lines) {
            val normalized = line.lowercase()
            if (totalKeywords.any { normalized.contains(it) }) {
                extractAmountFromLine(line)?.let { return it }
            }
        }
        
        // Strategy 2: Find largest amount (likely the total)
        val amounts = lines.mapNotNull { extractAmountFromLine(it) }
        return amounts.maxOrNull()
    }
    
    private fun extractAmountFromLine(line: String): Double? {
        for (pattern in amountPatterns) {
            pattern.find(line)?.let { match ->
                val amountStr = match.groupValues[1].replace(",", "")
                return amountStr.toDoubleOrNull()
            }
        }
        return null
    }
}
```

## Adding New Receipt Formats

### Step 1: Create Format Detector

```kotlin
interface ReceiptFormatDetector {
    fun detect(lines: List<String>): ReceiptFormat?
}

enum class ReceiptFormat {
    WALMART,
    TARGET,
    STARBUCKS,
    GENERIC
}

class WalmartFormatDetector : ReceiptFormatDetector {
    override fun detect(lines: List<String>): ReceiptFormat? {
        val firstLines = lines.take(5).joinToString(" ").lowercase()
        return if (firstLines.contains("walmart") || firstLines.contains("wal-mart")) {
            ReceiptFormat.WALMART
        } else null
    }
}
```

### Step 2: Create Specialized Parser

```kotlin
class WalmartReceiptParser : ReceiptParser {
    override fun parse(ocrText: String): Receipt {
        val lines = ocrText.lines()
        
        // Walmart-specific parsing logic
        val storeNumber = extractStoreNumber(lines)
        val merchantName = "Walmart #$storeNumber"
        
        // Walmart puts date in format "MM/DD/YYYY HH:MM"
        val date = extractWalmartDate(lines)
        
        // Walmart always has "TOTAL" in caps
        val amount = lines.firstOrNull { it.contains("TOTAL", ignoreCase = false) }
            ?.let { extractAmountFromLine(it) }
        
        return Receipt(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            merchantName = merchantName,
            amount = amount,
            date = date,
            rawOcrText = ocrText
        )
    }
    
    private fun extractStoreNumber(lines: List<String>): String? {
        val pattern = Regex("""ST#\s*(\d+)""", RegexOption.IGNORE_CASE)
        lines.forEach { line ->
            pattern.find(line)?.let {
                return it.groupValues[1]
            }
        }
        return null
    }
}
```

### Step 3: Register Parser

```kotlin
class ReceiptParserFactory {
    private val formatDetectors = listOf(
        WalmartFormatDetector(),
        TargetFormatDetector(),
        StarbucksFormatDetector()
    )
    
    private val parsers = mapOf(
        ReceiptFormat.WALMART to WalmartReceiptParser(),
        ReceiptFormat.TARGET to TargetReceiptParser(),
        ReceiptFormat.STARBUCKS to StarbucksReceiptParser(),
        ReceiptFormat.GENERIC to GenericReceiptParser()
    )
    
    fun getParser(ocrText: String): ReceiptParser {
        val lines = ocrText.lines()
        
        for (detector in formatDetectors) {
            detector.detect(lines)?.let { format ->
                return parsers[format]!!
            }
        }
        
        return parsers[ReceiptFormat.GENERIC]!!
    }
}
```

## Improving Accuracy

### 1. Train Custom ML Kit Model

For better accuracy with receipts:
```kotlin
// Use on-device custom model
val localModel = CustomRemoteModel.Builder(
    FirebaseModelSource.Builder("receipt_text_model").build()
).build()

val options = TextRecognizerOptions.Builder()
    .setExecutor(Executors.newSingleThreadExecutor())
    .build()
```

### 2. Use Multiple OCR Passes

```kotlin
suspend fun extractTextWithRetry(imagePath: String): Result<OcrResult> {
    val attempts = mutableListOf<OcrResult>()
    
    // Pass 1: Original image
    attempts.add(extractText(imagePath))
    
    // Pass 2: Enhanced contrast
    val enhanced = enhanceContrast(loadImage(imagePath))
    attempts.add(extractText(enhanced))
    
    // Pass 3: Grayscale
    val grayscale = toGrayscale(loadImage(imagePath))
    attempts.add(extractText(grayscale))
    
    // Return result with highest confidence
    return attempts.maxByOrNull { it.confidence }
}
```

### 3. Human-in-the-Loop Corrections

Learn from user edits:
```kotlin
class ReceiptLearningService {
    fun recordCorrection(
        originalOcrText: String,
        correctedMerchant: String,
        correctedAmount: Double,
        correctedDate: String
    ) {
        // Store corrections in database
        // Use for improving future parses
        val correction = OcrCorrection(
            ocrText = originalOcrText,
            correctedFields = mapOf(
                "merchant" to correctedMerchant,
                "amount" to correctedAmount.toString(),
                "date" to correctedDate
            )
        )
        
        database.ocrCorrectionDao().insert(correction)
    }
    
    fun suggestCorrection(ocrText: String): Receipt? {
        // Check if similar receipt was corrected before
        val similar = database.ocrCorrectionDao()
            .findSimilar(ocrText, threshold = 0.8)
        
        return similar?.toCorrectedReceipt()
    }
}
```

### 4. Post-Processing Validation

```kotlin
fun validateAndCorrect(receipt: Receipt): Receipt {
    var corrected = receipt
    
    // Fix common OCR mistakes
    corrected = corrected.copy(
        merchantName = fixCommonMistakes(receipt.merchantName),
        amount = validateAmount(receipt.amount),
        date = validateDate(receipt.date)
    )
    
    return corrected
}

fun fixCommonMistakes(text: String?): String? {
    return text
        ?.replace("0", "O", ignoreCase = false)  // In merchant names
        ?.replace("|", "I")  // Vertical bar → I
        ?.replace("5", "S")  // In certain contexts
}
```

## Adding New Languages

ML Kit supports multiple languages out of the box.

### Supported Scripts
- Latin (English, French, German, Spanish, etc.)
- Chinese
- Devanagari (Hindi, Sanskrit)
- Japanese
- Korean

### Configuration

```kotlin
// For Chinese receipts
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions

val options = ChineseTextRecognizerOptions.Builder().build()
val recognizer = TextRecognition.getClient(options)

// For Japanese/Korean receipts
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions

val japaneseOptions = JapaneseTextRecognizerOptions.Builder().build()
val japaneseRecognizer = TextRecognition.getClient(japaneseOptions)
```

### Auto-Detect Language

```kotlin
class MultiLanguageOcrRepository {
    private val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val chineseRecognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    
    suspend fun extractText(imagePath: String): Result<OcrResult> {
        // Try Latin first (most common)
        val latinResult = latinRecognizer.process(image).await()
        
        // If confidence is low, try other languages
        if (latinResult.confidence < 0.7) {
            val chineseResult = chineseRecognizer.process(image).await()
            if (chineseResult.confidence > latinResult.confidence) {
                return Result.Success(chineseResult)
            }
        }
        
        return Result.Success(latinResult)
    }
}
```

## Custom Field Extractors

### Item List Extractor

Extract individual items from receipt:
```kotlin
class ItemExtractor {
    private val itemPattern = Regex(
        """([A-Za-z\s]+?)\s+(\d+(?:\.\d{2})?)\s*(?:@|x)?\s*(\d+(?:\.\d{2})?)"""
    )
    
    fun extract(lines: List<String>): List<ReceiptItem> {
        val items = mutableListOf<ReceiptItem>()
        
        lines.forEach { line ->
            itemPattern.find(line)?.let { match ->
                val name = match.groupValues[1].trim()
                val quantity = match.groupValues[2].toDoubleOrNull() ?: 1.0
                val price = match.groupValues[3].toDoubleOrNull()
                
                if (price != null) {
                    items.add(ReceiptItem(name, quantity, price))
                }
            }
        }
        
        return items
    }
}

data class ReceiptItem(
    val name: String,
    val quantity: Double,
    val price: Double
)
```

### Tax Extractor

```kotlin
class TaxExtractor {
    fun extract(lines: List<String>): Double? {
        val taxPattern = Regex("""tax:?\s*\$?\s*(\d+\.\d{2})""", RegexOption.IGNORE_CASE)
        
        lines.forEach { line ->
            taxPattern.find(line)?.let {
                return it.groupValues[1].toDoubleOrNull()
            }
        }
        
        return null
    }
}
```

## Confidence Scoring

### Calculate Overall Confidence

```kotlin
data class ConfidenceScore(
    val overall: Double,
    val merchant: Double,
    val amount: Double,
    val date: Double
) {
    val isReliable: Boolean
        get() = overall >= 0.75
}

class ConfidenceCalculator {
    fun calculate(receipt: Receipt, ocrResult: OcrResult): ConfidenceScore {
        val merchantConfidence = calculateMerchantConfidence(receipt.merchantName)
        val amountConfidence = calculateAmountConfidence(receipt.amount)
        val dateConfidence = calculateDateConfidence(receipt.date)
        
        val overall = (
            ocrResult.confidence * 0.3 +
            merchantConfidence * 0.3 +
            amountConfidence * 0.25 +
            dateConfidence * 0.15
        )
        
        return ConfidenceScore(
            overall = overall,
            merchant = merchantConfidence,
            amount = amountConfidence,
            date = dateConfidence
        )
    }
    
    private fun calculateMerchantConfidence(merchant: String?): Double {
        if (merchant.isNullOrBlank()) return 0.0
        if (merchant.length < 3) return 0.3
        
        // Higher confidence if in known merchant list
        val isKnown = KnownMerchants.contains(merchant.lowercase())
        return if (isKnown) 0.95 else 0.6
    }
    
    private fun calculateAmountConfidence(amount: Double?): Double {
        if (amount == null || amount <= 0) return 0.0
        if (amount > 10000) return 0.5  // Suspiciously high
        return 0.9
    }
    
    private fun calculateDateConfidence(date: String?): Double {
        if (date.isNullOrBlank()) return 0.0
        
        // Try to parse date
        val parsed = tryParseDate(date) ?: return 0.2
        
        // Date should be recent (within 90 days)
        val daysAgo = (System.currentTimeMillis() - parsed.time) / (1000 * 60 * 60 * 24)
        return when {
            daysAgo < 0 -> 0.3  // Future date
            daysAgo <= 30 -> 0.95
            daysAgo <= 90 -> 0.75
            else -> 0.5
        }
    }
}
```

## Testing OCR

### Unit Tests

```kotlin
class ReceiptParserTest {
    private val parser = ReceiptParserImpl(
        MerchantExtractor(),
        DateExtractor(),
        AmountExtractor()
    )
    
    @Test
    fun `parse typical Walmart receipt`() {
        val ocrText = """
            WAL-MART
            Store #1234
            02/14/2026 10:30AM
            
            Milk           $3.99
            Bread          $2.49
            Eggs           $4.29
            
            SUBTOTAL      $10.77
            TAX            $0.97
            TOTAL         $11.74
        """.trimIndent()
        
        val receipt = parser.parse(ocrText)
        
        assertEquals("Walmart", receipt.merchantName)
        assertEquals(11.74, receipt.amount, 0.01)
        assertEquals("2026-02-14", receipt.date)
    }
}
```

### Accuracy Evaluation

```kotlin
class OcrAccuracyEvaluator {
    data class EvaluationResult(
        val totalReceipts: Int,
        val merchantAccuracy: Double,
        val amountAccuracy: Double,
        val dateAccuracy: Double
    )
    
    fun evaluate(testSet: List<GroundTruthReceipt>): EvaluationResult {
        var merchantCorrect = 0
        var amountCorrect = 0
        var dateCorrect = 0
        
        testSet.forEach { groundTruth ->
            val parsed = parser.parse(groundTruth.ocrText)
            
            if (parsed.merchantName?.equals(groundTruth.merchant, ignoreCase = true) == true) {
                merchantCorrect++
            }
            
            if (parsed.amount?.equals(groundTruth.amount) == true) {
                amountCorrect++
            }
            
            if (parsed.date == groundTruth.date) {
                dateCorrect++
            }
        }
        
        val total = testSet.size.toDouble()
        return EvaluationResult(
            totalReceipts = testSet.size,
            merchantAccuracy = merchantCorrect / total,
            amountAccuracy = amountCorrect / total,
            dateAccuracy = dateCorrect / total
        )
    }
}
```

---

## Related Documentation
- [Architecture](ARCHITECTURE.md) - System architecture
- [API Integration](API_INTEGRATION.md) - YNAB API usage
- [Testing Guide](../TESTING_GUIDE.md) - Testing strategies
