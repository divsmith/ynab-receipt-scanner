# Public API Reference

## Table of Contents
- [Overview](#overview)
- [Module Access](#module-access)
- [Domain Layer](#domain-layer)
- [Data Layer](#data-layer)
- [Use Cases](#use-cases)
- [Repositories](#repositories)
- [Models](#models)
- [Utilities](#utilities)
- [Extension Functions](#extension-functions)
- [Error Handling](#error-handling)

## Overview

This document describes the public API surface of the YNAB Receipt Scanner app modules. These interfaces can be consumed by other modules or for testing purposes.

### Architecture Layers

```
┌─────────────────────────┐
│      App Module         │  ← UI, ViewModels
└───────────┬─────────────┘
            │ depends on
┌───────────▼─────────────┐
│     Domain Module       │  ← Use Cases, Domain Models
└───────────┬─────────────┘
            │ depends on
┌───────────▼─────────────┐
│      Data Module        │  ← Repositories, Data Sources
└───────────┬─────────────┘
            │ depends on
┌───────────▼─────────────┐
│      Core Module        │  ← Utilities, Extensions
└─────────────────────────┘
```

## Module Access

### Dependency Injection

All public components are accessible through Hilt dependency injection:

```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val scanReceiptUseCase: ScanReceiptUseCase,
    private val receiptRepository: ReceiptRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {
    // Implementation
}
```

### Direct Imports

```kotlin
// Domain models
import com.ynab.receiptscanner.domain.model.Receipt
import com.ynab.receiptscanner.domain.model.Transaction

// Use cases
import com.ynab.receiptscanner.domain.usecase.ScanReceiptUseCase
import com.ynab.receiptscanner.domain.usecase.CreateTransactionUseCase

// Repository interfaces
import com.ynab.receiptscanner.domain.repository.ReceiptRepository
import com.ynab.receiptscanner.domain.repository.YnabRepository
```

## Domain Layer

### Use Cases

#### ScanReceiptUseCase

Scan a receipt image and extract transaction details.

```kotlin
class ScanReceiptUseCase @Inject constructor(
    private val ocrService: OcrService,
    private val receiptParser: ReceiptParser,
    private val receiptRepository: ReceiptRepository
)

suspend operator fun invoke(imageUri: Uri): Result<Receipt>
```

**Usage:**
```kotlin
val result = scanReceiptUseCase(imageUri)
when (result) {
    is Result.Success -> {
        val receipt = result.data
        println("Merchant: ${receipt.merchantName}")
        println("Amount: ${receipt.amount}")
    }
    is Result.Error -> {
        println("Error: ${result.exception.message}")
    }
}
```

#### CreateTransactionUseCase

Create a transaction in YNAB from receipt data.

```kotlin
class CreateTransactionUseCase @Inject constructor(
    private val ynabRepository: YnabRepository,
    private val receiptRepository: ReceiptRepository
)

suspend operator fun invoke(
    budgetId: String,
    receipt: Receipt,
    accountId: String,
    categoryId: String?
): Result<Transaction>
```

**Usage:**
```kotlin
val result = createTransactionUseCase(
    budgetId = "budget-123",
    receipt = receipt,
    accountId = "account-456",
    categoryId = "category-789"
)

when (result) {
    is Result.Success -> println("Transaction created: ${result.data.id}")
    is Result.Error -> println("Failed: ${result.exception.message}")
}
```

#### GetAccountsUseCase

Fetch YNAB accounts.

```kotlin
class GetAccountsUseCase @Inject constructor(
    private val ynabRepository: YnabRepository
)

suspend operator fun invoke(budgetId: String): Result<List<Account>>
```

**Usage:**
```kotlin
val result = getAccountsUseCase("budget-123")
when (result) {
    is Result.Success -> {
        val accounts = result.data
        accounts.forEach { account ->
            println("${account.name}: ${account.type}")
        }
    }
    is Result.Error -> println("Error loading accounts")
}
```

#### SyncPendingTransactionsUseCase

Sync pending transactions to YNAB.

```kotlin
class SyncPendingTransactionsUseCase @Inject constructor(
    private val receiptRepository: ReceiptRepository,
    private val ynabRepository: YnabRepository,
    private val syncStatusRepository: SyncStatusRepository
)

suspend operator fun invoke(): Result<SyncResult>

data class SyncResult(
    val successCount: Int,
    val failureCount: Int,
    val errors: List<SyncError>
)
```

**Usage:**
```kotlin
val result = syncPendingTransactionsUseCase()
when (result) {
    is Result.Success -> {
        val syncResult = result.data
        println("Synced: ${syncResult.successCount} successful, ${syncResult.failureCount} failed")
    }
    is Result.Error -> println("Sync failed: ${result.exception.message}")
}
```

### Domain Models

#### Receipt

```kotlin
data class Receipt(
    val id: String = UUID.randomUUID().toString(),
    val imageUri: String?,
    val merchantName: String?,
    val amount: Double?,
    val date: LocalDate?,
    val memo: String?,
    val confidence: Float = 0f,
    val accountId: String?,
    val categoryId: String?,
    val synced: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
```

**Factory methods:**
```kotlin
companion object {
    fun create(
        imageUri: String,
        merchantName: String? = null,
        amount: Double? = null,
        date: LocalDate? = null
    ): Receipt = Receipt(
        imageUri = imageUri,
        merchantName = merchantName,
        amount = amount,
        date = date
    )
}
```

#### Transaction

```kotlin
data class Transaction(
    val id: String,
    val budgetId: String,
    val accountId: String,
    val date: LocalDate,
    val amount: Long,  // In milliunits
    val payeeName: String?,
    val categoryId: String?,
    val memo: String?,
    val cleared: TransactionStatus = TransactionStatus.UNCLEARED,
    val approved: Boolean = false
)

enum class TransactionStatus {
    CLEARED,
    UNCLEARED,
    RECONCILED
}
```

#### Account

```kotlin
data class Account(
    val id: String,
    val budgetId: String,
    val name: String,
    val type: AccountType,
    val balance: Long,  // In milliunits
    val closed: Boolean = false,
    val deleted: Boolean = false
)

enum class AccountType {
    CHECKING,
    SAVINGS,
    CASH,
    CREDIT_CARD,
    LINE_OF_CREDIT,
    OTHER_ASSET,
    OTHER_LIABILITY,
    PAYPAL,
    MERCHANT_ACCOUNT,
    INVESTMENT_ACCOUNT,
    MORTGAGE
}
```

#### Category

```kotlin
data class Category(
    val id: String,
    val budgetId: String,
    val categoryGroupId: String,
    val name: String,
    val hidden: Boolean = false,
    val deleted: Boolean = false,
    val budgeted: Long = 0,  // In milliunits
    val activity: Long = 0,
    val balance: Long = 0
)
```

#### Budget

```kotlin
data class Budget(
    val id: String,
    val name: String,
    val lastModifiedOn: Instant,
    val currencyFormat: CurrencyFormat
)

data class CurrencyFormat(
    val isoCode: String,
    val exampleFormat: String,
    val decimalDigits: Int,
    val decimalSeparator: String,
    val symbolFirst: Boolean,
    val groupSeparator: String,
    val currencySymbol: String,
    val displaySymbol: Boolean
)
```

## Data Layer

### Repositories

#### ReceiptRepository

Interface for receipt data operations.

```kotlin
interface ReceiptRepository {
    fun getAllReceipts(): Flow<List<Receipt>>
    fun getReceiptById(id: String): Flow<Receipt?>
    fun getPendingReceipts(): Flow<List<Receipt>>
    suspend fun insertReceipt(receipt: Receipt): String
    suspend fun updateReceipt(receipt: Receipt)
    suspend fun deleteReceipt(id: String)
    suspend fun markAsSynced(id: String)
}
```

**Usage:**
```kotlin
class MyViewModel @Inject constructor(
    private val receiptRepository: ReceiptRepository
) : ViewModel() {
    val receipts = receiptRepository.getAllReceipts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
```

#### YnabRepository

Interface for YNAB API operations.

```kotlin
interface YnabRepository {
    suspend fun getCurrentUser(): Result<User>
    suspend fun getBudgets(): Result<List<Budget>>
    suspend fun getAccounts(budgetId: String): Result<List<Account>>
    suspend fun getCategories(budgetId: String): Result<List<Category>>
    suspend fun createTransaction(
        budgetId: String,
        transaction: CreateTransactionRequest
    ): Result<Transaction>
}
```

**Usage:**
```kotlin
val result = ynabRepository.getAccounts("budget-123")
when (result) {
    is Result.Success -> {
        val accounts = result.data
        // Process accounts
    }
    is Result.Error -> {
        // Handle error
    }
}
```

#### AuthRepository

Interface for authentication operations.

```kotlin
interface AuthRepository {
    suspend fun authenticate(): Result<String>
    suspend fun refreshToken(): Result<String>
    fun getAccessToken(): String?
    suspend fun logout()
    fun isAuthenticated(): Flow<Boolean>
}
```

**Usage:**
```kotlin
// Check authentication status
viewModelScope.launch {
    authRepository.isAuthenticated().collect { isAuthenticated ->
        if (!isAuthenticated) {
            // Navigate to login
        }
    }
}

// Authenticate
val result = authRepository.authenticate()
when (result) {
    is Result.Success -> println("Authenticated")
    is Result.Error -> println("Auth failed")
}
```

#### SyncStatusRepository

Interface for sync status tracking.

```kotlin
interface SyncStatusRepository {
    fun getSyncStatus(): Flow<SyncStatus>
    suspend fun updateSyncStatus(status: SyncStatus)
    suspend fun recordSyncAttempt(success: Boolean, error: String? = null)
}

data class SyncStatus(
    val lastSyncTime: Instant?,
    val isSyncing: Boolean = false,
    val pendingCount: Int = 0,
    val lastError: String? = null
)
```

**Usage:**
```kotlin
val syncStatus = syncStatusRepository.getSyncStatus()
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = SyncStatus(lastSyncTime = null)
    )
```

## Use Cases

### Core Use Cases

```kotlin
// Scanning & OCR
class ScanReceiptUseCase
class ValidateReceiptImageUseCase

// Transaction Management
class CreateTransactionUseCase
class UpdateReceiptUseCase
class DeleteReceiptUseCase

// YNAB Integration
class GetBudgetsUseCase
class GetAccountsUseCase
class GetCategoriesUseCase
class SyncPendingTransactionsUseCase

// Authentication
class AuthenticateUseCase
class LogoutUseCase
class RefreshTokenUseCase
```

### Use Case Pattern

All use cases follow this pattern:

```kotlin
class MyUseCase @Inject constructor(
    private val repository: Repository
) {
    suspend operator fun invoke(params: Params): Result<Output> {
        // Implementation
    }
}

// Usage
val result = myUseCase(params)
```

## Models

### Data Transfer Objects (DTOs)

#### CreateTransactionRequest

```kotlin
data class CreateTransactionRequest(
    val accountId: String,
    val date: String,  // Format: YYYY-MM-DD
    val amount: Long,  // In milliunits (1000 milliunits = $1.00)
    val payeeName: String?,
    val categoryId: String?,
    val memo: String?,
    val cleared: String = "uncleared",  // "cleared", "uncleared", "reconciled"
    val approved: Boolean = true
)
```

**Example:**
```kotlin
val request = CreateTransactionRequest(
    accountId = "account-123",
    date = "2024-01-15",
    amount = -5000,  // -$5.00
    payeeName = "Coffee Shop",
    categoryId = "category-456",
    memo = "Morning coffee",
    cleared = "uncleared",
    approved = true
)
```

#### OcrResult

```kotlin
data class OcrResult(
    val text: String,
    val confidence: Float,
    val boundingBox: Rect?
)
```

#### ParsedReceipt

```kotlin
data class ParsedReceipt(
    val merchantName: String?,
    val amount: Double?,
    val date: LocalDate?,
    val items: List<LineItem>,
    val confidence: Float
)

data class LineItem(
    val description: String,
    val amount: Double
)
```

## Utilities

### Result Type

Generic result wrapper for operations:

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }
    
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception
    }
    
    fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> Error(exception)
    }
}
```

**Usage:**
```kotlin
val result: Result<Receipt> = scanReceipt(imageUri)

// Pattern matching
when (result) {
    is Result.Success -> println("Got: ${result.data}")
    is Result.Error -> println("Error: ${result.exception.message}")
}

// Get or null
val receipt: Receipt? = result.getOrNull()

// Transform
val merchantName: Result<String> = result.map { it.merchantName ?: "Unknown" }
```

### Currency Utils

```kotlin
object CurrencyUtils {
    /**
     * Convert dollars to milliunits (YNAB format)
     * Example: $5.00 -> -5000
     */
    fun dollarsToMilliunits(dollars: Double): Long {
        return (dollars * -1000).roundToLong()
    }
    
    /**
     * Convert milliunits to dollars
     * Example: -5000 -> $5.00
     */
    fun milliunitsToDollars(milliunits: Long): Double {
        return milliunits / -1000.0
    }
    
    /**
     * Format currency for display
     * Example: 5.00 -> "$5.00"
     */
    fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance().format(amount)
    }
}
```

**Usage:**
```kotlin
val amount = 5.99
val milliunits = CurrencyUtils.dollarsToMilliunits(amount)  // -5990

val transaction = CreateTransactionRequest(
    amount = milliunits,
    // ... other fields
)
```

### Date Utils

```kotlin
object DateUtils {
    /**
     * Format LocalDate to YNAB API format (YYYY-MM-DD)
     */
    fun formatForApi(date: LocalDate): String {
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }
    
    /**
     * Parse YNAB API date string
     */
    fun parseFromApi(dateString: String): LocalDate {
        return LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
    }
    
    /**
     * Format for display
     */
    fun formatForDisplay(date: LocalDate): String {
        return date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    }
}
```

**Usage:**
```kotlin
val today = LocalDate.now()
val apiFormat = DateUtils.formatForApi(today)  // "2024-01-15"
val displayFormat = DateUtils.formatForDisplay(today)  // "Jan 15, 2024"
```

## Extension Functions

### Flow Extensions

```kotlin
/**
 * Convert Flow to StateFlow with initial value
 */
fun <T> Flow<T>.stateInViewModel(
    viewModelScope: CoroutineScope,
    initialValue: T
): StateFlow<T> {
    return stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = initialValue
    )
}
```

### String Extensions

```kotlin
/**
 * Validate email format
 */
fun String.isValidEmail(): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

/**
 * Truncate string with ellipsis
 */
fun String.truncate(maxLength: Int): String {
    return if (length <= maxLength) this
    else "${take(maxLength - 3)}..."
}
```

### Double Extensions

```kotlin
/**
 * Round to 2 decimal places
 */
fun Double.roundToTwoDecimals(): Double {
    return (this * 100).roundToInt() / 100.0
}
```

## Error Handling

### Exception Types

```kotlin
sealed class AppException(message: String) : Exception(message)

class NetworkException(message: String) : AppException(message)
class AuthenticationException(message: String) : AppException(message)
class ValidationException(message: String) : AppException(message)
class NotFoundException(message: String) : AppException(message)
class RateLimitException(message: String) : AppException(message)
```

### Error Handling Pattern

```kotlin
suspend fun safeApiCall<T>(
    call: suspend () -> T
): Result<T> {
    return try {
        Result.Success(call())
    } catch (e: Exception) {
        Result.Error(handleException(e))
    }
}

private fun handleException(e: Exception): Exception {
    return when (e) {
        is HttpException -> when (e.code()) {
            401 -> AuthenticationException("Please sign in again")
            404 -> NotFoundException("Resource not found")
            429 -> RateLimitException("Too many requests")
            else -> NetworkException("Network error")
        }
        is IOException -> NetworkException("Check your connection")
        else -> AppException("An error occurred")
    }
}
```

**Usage:**
```kotlin
val result = safeApiCall {
    api.createTransaction(request)
}

when (result) {
    is Result.Success -> {
        // Handle success
    }
    is Result.Error -> {
        val message = when (result.exception) {
            is AuthenticationException -> "Please sign in again"
            is NetworkException -> "Check your connection"
            else -> "An error occurred"
        }
        showError(message)
    }
}
```

## Testing Support

### Test Utilities

```kotlin
/**
 * Create test receipt
 */
fun createTestReceipt(
    id: String = "test-receipt",
    merchantName: String = "Test Merchant",
    amount: Double = 10.00
): Receipt = Receipt(
    id = id,
    imageUri = "test://image",
    merchantName = merchantName,
    amount = amount,
    date = LocalDate.now()
)

/**
 * Create test account
 */
fun createTestAccount(
    id: String = "test-account",
    name: String = "Test Account",
    type: AccountType = AccountType.CHECKING
): Account = Account(
    id = id,
    budgetId = "test-budget",
    name = name,
    type = type,
    balance = 100000  // $100.00
)
```

### Fake Repositories

```kotlin
class FakeReceiptRepository : ReceiptRepository {
    private val receipts = MutableStateFlow<List<Receipt>>(emptyList())
    
    override fun getAllReceipts(): Flow<List<Receipt>> = receipts
    
    override suspend fun insertReceipt(receipt: Receipt): String {
        receipts.value = receipts.value + receipt
        return receipt.id
    }
    
    // ... other implementations
}
```

**Usage in tests:**
```kotlin
@Test
fun `test scanning receipt`() = runTest {
    val repository = FakeReceiptRepository()
    val useCase = ScanReceiptUseCase(mockOcr, mockParser, repository)
    
    val result = useCase(testImageUri)
    
    assertTrue(result is Result.Success)
}
```

---

## Usage Examples

### Complete Flow Example

```kotlin
@HiltViewModel
class ReceiptViewModel @Inject constructor(
    private val scanReceiptUseCase: ScanReceiptUseCase,
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val receiptRepository: ReceiptRepository
) : ViewModel() {
    
    // Observe all receipts
    val receipts = receiptRepository.getAllReceipts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Scan receipt
    fun scanReceipt(imageUri: Uri) {
        viewModelScope.launch {
            val result = scanReceiptUseCase(imageUri)
            when (result) {
                is Result.Success -> {
                    _uiState.value = UiState.Success(result.data)
                }
                is Result.Error -> {
                    _uiState.value = UiState.Error(result.exception.message)
                }
            }
        }
    }
    
    // Create transaction
    fun createTransaction(receipt: Receipt, accountId: String) {
        viewModelScope.launch {
            val result = createTransactionUseCase(
                budgetId = currentBudgetId,
                receipt = receipt,
                accountId = accountId,
                categoryId = receipt.categoryId
            )
            when (result) {
                is Result.Success -> {
                    // Transaction created
                }
                is Result.Error -> {
                    // Handle error
                }
            }
        }
    }
}
```

---

## Related Documentation

- [Architecture](ARCHITECTURE.md) - System architecture
- [Module Structure](MODULE_STRUCTURE.md) - Module details
- [Developer Guide](DEVELOPER_GUIDE.md) - Getting started
- [Testing Guide](../TESTING_GUIDE.md) - Testing practices

