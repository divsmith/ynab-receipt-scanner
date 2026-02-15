# YNAB API Integration - Quick Reference

## 🚀 Quick Start (3 Steps)

1. **Register app**: https://app.youneedabudget.com/settings/developer
   - Redirect URI: `ynabreceipt://oauth/callback`
   - Copy Client ID

2. **Update Constants.kt**:
   ```kotlin
   const val OAUTH_CLIENT_ID = "paste_your_client_id_here"
   ```

3. **Build & Run**:
   ```bash
   ./gradlew installDebug
   ```

---

## 🔐 Authentication

### OAuth Sign-In
```kotlin
// In AuthActivity
binding.btnSignInOauth.setOnClickListener {
    viewModel.startOAuthFlow()
}

// Observe state
viewModel.authState.collect { state ->
    when (state) {
        is AuthState.SignedIn -> navigateToMainScreen()
        is AuthState.Error -> showError(state.message)
        else -> {}
    }
}
```

### Check Auth Status
```kotlin
val getAuthStatus: GetAuthStatusUseCase by inject()

when (val result = getAuthStatus()) {
    is Result.Success -> {
        val status = result.data
        if (status.isAuthenticated) {
            // User is signed in
        }
    }
}
```

### Sign Out
```kotlin
val signOut: SignOutUseCase by inject()
signOut() // Revokes tokens and clears local data
```

---

## 📊 Fetch Data

### Get Budgets
```kotlin
val getBudgets: GetBudgetsUseCase by inject()

when (val result = getBudgets(forceRefresh = false)) {
    is Result.Success -> {
        val budgets: List<YnabBudget> = result.data
        // Display budgets
    }
    is Result.Error -> handleError(result.exception)
}
```

### Get Accounts
```kotlin
val getAccounts: GetAccountsUseCase by inject()

when (val result = getAccounts(budgetId, forceRefresh = false)) {
    is Result.Success -> {
        val accounts: List<YnabAccount> = result.data
        val activeAccounts = accounts.filter { !it.closed }
    }
}
```

### Get Categories
```kotlin
val getCategories: GetCategoriesUseCase by inject()

when (val result = getCategories(budgetId)) {
    is Result.Success -> {
        val categories: List<YnabCategory> = result.data
    }
}
```

---

## 💰 Transactions

### Create Transaction
```kotlin
val createTransaction: CreateTransactionUseCase by inject()

val transaction = YnabTransaction(
    id = UUID.randomUUID().toString(),
    accountId = "account-id",
    categoryId = "category-id",
    date = Date(),
    amount = -12340, // -$12.34 (negative for expense, milliunits)
    payee = "Store Name",
    memo = "Receipt notes",
    cleared = "cleared",
    approved = false,
    importId = null // Auto-generated with YNABRECEIPT: prefix
)

when (val result = createTransaction(budgetId, transaction)) {
    is Result.Success -> {
        val transactionId = result.data
        // Transaction created or queued for offline sync
    }
}
```

### Check for Duplicates
```kotlin
val checkDuplicate: CheckDuplicateTransactionUseCase by inject()

when (val result = checkDuplicate(budgetId, transaction)) {
    is Result.Success -> {
        val matches: List<DuplicateMatch> = result.data
        
        if (matches.isNotEmpty()) {
            val bestMatch = matches.first()
            val confidence = bestMatch.confidence // 0.0 - 1.0
            
            if (confidence > 0.8f) {
                // High confidence duplicate - warn user
                showDuplicateWarning(bestMatch.transaction)
            }
        }
    }
}
```

### Convert Amount to Milliunits
```kotlin
// YNAB uses milliunits: $1.00 = 1000 milliunits
val dollars = 12.34
val milliunits = (dollars * 1000).toLong() // 12340

// Negative for expenses, positive for income
val expense = -milliunits // -12340
val income = milliunits    // 12340
```

---

## 🔄 Sync & Offline Support

### Sync Pending Transactions
```kotlin
val syncTransactions: SyncPendingTransactionsUseCase by inject()

// Manual sync
when (val result = syncTransactions()) {
    is Result.Success -> {
        val count = result.data // Number synced
        Log.d(TAG, "Synced $count transactions")
    }
}

// Get sync status
val status = syncTransactions.getStatus()
Log.d(TAG, """
    Pending: ${status.pending}
    Failed: ${status.failed}
    Completed: ${status.completed}
""")
```

### Background Sync Worker
```kotlin
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sync: SyncPendingTransactionsUseCase
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return when (sync()) {
            is com.ynab.receiptscanner.core.util.Result.Success -> Result.success()
            is com.ynab.receiptscanner.core.util.Result.Error -> Result.retry()
        }
    }
}

// Schedule periodic sync
val workRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
    .setConstraints(
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    )
    .build()

WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    "transaction_sync",
    ExistingPeriodicWorkPolicy.KEEP,
    workRequest
)
```

---

## 🔍 Reactive Data with Flow

### Observe Budgets
```kotlin
repository.observeBudgets().collect { budgets ->
    // Updates automatically when cache changes
    updateUI(budgets)
}
```

### Observe Accounts
```kotlin
repository.observeAccounts(budgetId).collect { accounts ->
    updateAccountList(accounts)
}
```

### Observe Categories
```kotlin
repository.observeCategories(budgetId).collect { categories ->
    updateCategorySpinner(categories)
}
```

---

## 🧪 Testing Mode

### Use Personal Access Token
```kotlin
// For testing without full OAuth flow
val getAuthStatus: GetAuthStatusUseCase by inject()

getAuthStatus.setPersonalAccessToken("your_pat_here")

// Get PAT from: https://app.youneedabudget.com/settings/developer
```

### Check if using PAT
```kotlin
when (val result = getAuthStatus()) {
    is Result.Success -> {
        if (result.data.isUsingPersonalAccessToken) {
            // Using PAT (test mode)
        }
    }
}
```

---

## ⚙️ Configuration

### Update OAuth Client ID
[core/Constants.kt](../core/src/main/java/com/ynab/receiptscanner/core/Constants.kt):
```kotlin
const val OAUTH_CLIENT_ID = "your_actual_client_id"
```

### Adjust Sync Settings
[core/Constants.kt](../core/src/main/java/com/ynab/receiptscanner/core/Constants.kt):
```kotlin
const val SYNC_MAX_RETRIES = 5              // Max retry attempts
const val SYNC_RETRY_INITIAL_DELAY_MS = 1000L  // Initial delay
const val SYNC_RETRY_MAX_DELAY_MS = 32000L     // Max delay
const val SYNC_RETRY_BACKOFF_MULTIPLIER = 2.0  // Exponential backoff
```

### Adjust Duplicate Detection
[core/Constants.kt](../core/src/main/java/com/ynab/receiptscanner/core/Constants.kt):
```kotlin
const val DUPLICATE_DATE_TOLERANCE_DAYS = 2         // ±2 days
const val DUPLICATE_PAYEE_SIMILARITY_THRESHOLD = 0.8f  // 80% similarity
```

---

## 🛠️ Common Patterns

### ViewModel with Use Case
```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val getBudgets: GetBudgetsUseCase,
    private val createTransaction: CreateTransactionUseCase
) : ViewModel() {
    
    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()
    
    fun loadData() = viewModelScope.launch {
        _state.value = UiState.Loading
        
        when (val result = getBudgets()) {
            is Result.Success -> {
                _state.value = UiState.Success(result.data)
            }
            is Result.Error -> {
                _state.value = UiState.Error(result.exception.message)
            }
        }
    }
}
```

### Error Handling
```kotlin
when (val result = someUseCase()) {
    is Result.Success -> {
        // Handle success
        val data = result.data
    }
    is Result.Error -> {
        // Handle error
        val exception = result.exception
        val message = exception.message ?: "Unknown error"
        
        when (exception) {
            is IOException -> showNetworkError()
            is HttpException -> handleApiError(exception.code())
            else -> showGenericError(message)
        }
    }
}
```

---

## 📝 Common Tasks

### Get Budget by Name
```kotlin
val budgets = getBudgets().data
val myBudget = budgets.firstOrNull { it.name == "My Budget" }
```

### Filter Active Accounts
```kotlin
val accounts = getAccounts(budgetId).data
val activeAccounts = accounts.filter { !it.closed }
val checkingAccounts = activeAccounts.filter { it.type == "checking" }
```

### Convert Currency
```kotlin
// From milliunits to dollars
val milliunits = -12340L
val dollars = milliunits / 1000.0 // -12.34

// From dollars to milliunits
val dollars = 12.34
val milliunits = (dollars * 1000).toLong() // 12340
```

### Format Transaction Display
```kotlin
val transaction: YnabTransaction = ...

val displayAmount = String.format("$%.2f", transaction.getAmountInCurrency())
val displayDate = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(transaction.date)
val displayPayee = transaction.payee ?: "Unknown"

"$displayAmount - $displayPayee on $displayDate"
// "$12.34 - Starbucks on Jan 15, 2024"
```

---

## 🔧 Debugging

### Enable Network Logging
Already enabled via `HttpLoggingInterceptor` in NetworkModule.

### Check Token Status
```kotlin
val authPrefs: AuthPreferences by inject()

val token = authPrefs.getAccessToken()
val isExpired = authPrefs.isTokenExpired()
val expiresAt = Date(authPrefs.getTokenExpirationTime())

Log.d(TAG, """
    Token: ${token?.take(8)}...
    Expired: $isExpired
    Expires at: $expiresAt
""")
```

### Check Database
```kotlin
val budgetDao: BudgetDao by inject()
val accountDao: AccountDao by inject()

val budgetCount = budgetDao.getAllBudgets().first().size
val accountCount = accountDao.getAccountsForBudget(budgetId).first().size

Log.d(TAG, "Cached: $budgetCount budgets, $accountCount accounts")
```

---

## 📚 Key Classes Quick Reference

| Class | Purpose | Location |
|-------|---------|----------|
| `YnabAuthManager` | OAuth flow | data/auth/ |
| `YnabApi` | API interface | data/remote/ |
| `YnabRepositoryImpl` | Data access | data/repository/ |
| `TransactionSyncManager` | Background sync | data/sync/ |
| `DuplicateDetector` | Find duplicates | data/repository/ |
| `AuthPreferences` | Token storage | data/local/preference/ |
| `GetBudgetsUseCase` | Fetch budgets | domain/usecase/ |
| `CreateTransactionUseCase` | Create transaction | domain/usecase/ |

---

## 🆘 Troubleshooting

### OAuth Redirect Not Working
- Verify `OAUTH_CLIENT_ID` in Constants.kt
- Check redirect URI: `ynabreceipt://oauth/callback`
- Verify AndroidManifest intent-filter

### Token Expired
- Tokens auto-refresh via `TokenRefreshAuthenticator`
- Force refresh: `authManager.refreshToken()`

### Network Errors
- Check internet permission
- Verify `network_security_config.xml`
- Check YNAB API status: https://status.youneedabudget.com/

### Duplicate Transactions
- Adjust similarity threshold in Constants.kt
- Check `import_id` generation
- Use `CheckDuplicateTransactionUseCase` before creating

---

## 📖 Full Documentation

See [YNAB API Integration Guide](./ynab-api-integration.md) for comprehensive documentation.

---

**Last Updated**: February 2026  
**Version**: 1.0.0
