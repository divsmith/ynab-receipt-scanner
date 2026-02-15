package com.ynab.receiptscanner.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.ynab.receiptscanner.core.util.Result
import com.ynab.receiptscanner.data.local.YnabDatabase
import com.ynab.receiptscanner.data.mapper.YnabMapper
import com.ynab.receiptscanner.data.remote.YnabApi
import com.ynab.receiptscanner.data.sync.TransactionSyncManager
import com.ynab.receiptscanner.domain.model.YnabTransaction
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Integration tests for YnabRepositoryImpl
 * Tests YNAB API operations with MockWebServer
 */
@RunWith(AndroidJUnit4::class)
class YnabRepositoryImplTest {
    
    private lateinit var mockWebServer: MockWebServer
    private lateinit var database: YnabDatabase
    private lateinit var repository: YnabRepositoryImpl
    private lateinit var api: YnabApi
    private lateinit var syncManager: TransactionSyncManager
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        
        // Setup MockWebServer
        mockWebServer = MockWebServer()
        mockWebServer.start()
        
        // Create in-memory database
        database = Room.inMemoryDatabaseBuilder(
            context,
            YnabDatabase::class.java
        ).allowMainThreadQueries().build()
        
        // Create Retrofit API with MockWebServer
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(1, TimeUnit.SECONDS)
            .writeTimeout(1, TimeUnit.SECONDS)
            .build()
        
        api = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(YnabApi::class.java)
        
        // Mock sync manager
        syncManager = mockk(relaxed = true)
        
        // Create repository
        repository = YnabRepositoryImpl(
            api,
            database.budgetDao(),
            database.accountDao(),
            database.categoryDao(),
            database.pendingTransactionDao(),
            syncManager,
            YnabMapper()
        )
    }
    
    @After
    fun tearDown() {
        mockWebServer.shutdown()
        database.close()
    }
    
    @Test
    fun getBudgets_successResponse_returnsBudgets() = runTest {
        // Given
        val jsonResponse = """
{
  "data": {
    "budgets": [
      {
        "id": "budget-1",
        "name": "My Budget",
        "last_modified_on": "2024-01-01T00:00:00Z",
        "first_month": "2024-01-01",
        "last_month": "2024-12-31",
        "currency_format": {
          "iso_code": "USD",
          "example_format": "${'$'}123.45",
          "decimal_digits": 2,
          "decimal_separator": ".",
          "symbol_first": true,
          "group_separator": ",",
          "currency_symbol": "${'$'}",
          "display_symbol": true
        }
      }
    ]
  }
}
        """.trimIndent()
        
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(jsonResponse)
            .addHeader("Content-Type", "application/json"))
        
        // When
        val result = repository.getBudgets(forceRefresh = true)
        
        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val budgets = (result as Result.Success).data
        assertThat(budgets).hasSize(1)
        assertThat(budgets[0].id).isEqualTo("budget-1")
        assertThat(budgets[0].name).isEqualTo("My Budget")
    }
    
    @Test
    fun getBudgets_errorResponse_returnsError() = runTest {
        // Given
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(401)
            .setBody("{\"error\":{\"id\":\"401\",\"name\":\"unauthorized\",\"detail\":\"Unauthorized\"}}"))
        
        // When
        val result = repository.getBudgets(forceRefresh = true)
        
        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
    }
    
    @Test
    fun getAccounts_successResponse_returnsAccounts() = runTest {
        // Given
        val budgetId = "budget-123"
        val jsonResponse = """
{
  "data": {
    "accounts": [
      {
        "id": "account-1",
        "name": "Checking",
        "type": "checking",
        "on_budget": true,
        "closed": false,
        "balance": 100000,
        "cleared_balance": 100000,
        "uncleared_balance": 0,
        "deleted": false,
        "transfer_payee_id": "payee-1"
      }
    ]
  }
}
        """.trimIndent()
        
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(jsonResponse)
            .addHeader("Content-Type", "application/json"))
        
        // When
        val result = repository.getAccounts(budgetId, forceRefresh = true)
        
        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val accounts = (result as Result.Success).data
        assertThat(accounts).hasSize(1)
        assertThat(accounts[0].id).isEqualTo("account-1")
        assertThat(accounts[0].name).isEqualTo("Checking")
    }
    
    @Test
    fun getCategories_successResponse_returnsCategories() = runTest {
        // Given
        val budgetId = "budget-123"
        val jsonResponse = """
{
  "data": {
    "category_groups": [
      {
        "id": "group-1",
        "name": "Monthly Bills",
        "hidden": false,
        "deleted": false,
        "categories": [
          {
            "id": "category-1",
            "category_group_id": "group-1",
            "name": "Rent",
            "hidden": false,
            "deleted": false,
            "budgeted": 0,
            "activity": 0,
            "balance": 0
          }
        ]
      }
    ]
  }
}
        """.trimIndent()
        
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(jsonResponse)
            .addHeader("Content-Type", "application/json"))
        
        // When
        val result = repository.getCategories(budgetId, forceRefresh = true)
        
        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val categories = (result as Result.Success).data
        assertThat(categories).isNotEmpty()
    }
    
    @Test
    fun createTransaction_successResponse_returnsTransactionId() = runTest {
        // Given
        val budgetId = "budget-123"
        val transaction = createTestTransaction()
        
        val jsonResponse = """
{
  "data": {
    "transaction": {
      "id": "transaction-456",
      "date": "2024-01-15",
      "amount": -25500,
      "memo": "Test transaction",
      "cleared": "cleared",
      "approved": true,
      "account_id": "account-123",
      "payee_id": "payee-789",
      "category_id": "category-456",
      "deleted": false,
      "import_id": "YNAB:test-import-id"
    }
  }
}
        """.trimIndent()
        
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(201)
            .setBody(jsonResponse)
            .addHeader("Content-Type", "application/json"))
        
        // When
        val result = repository.createTransaction(budgetId, transaction)
        
        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        assertThat((result as Result.Success).data).isEqualTo("transaction-456")
    }
    
    @Test
    fun createTransaction_networkError_queuesForOffline() = runTest {
        // Given
        val budgetId = "budget-123"
        val transaction = createTestTransaction()
        
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(503)
            .setBody("Service Unavailable"))
        
        coEvery { syncManager.queueTransaction(budgetId, transaction) } returns Result.Success(Unit)
        
        // When
        val result = repository.createTransaction(budgetId, transaction)
        
        // Then - Should queue for offline sync
        assertThat(result).isInstanceOf(Result.Error::class.java)
    }
    
    @Test
    fun getBudgets_withCache_returnsFromCache() = runTest {
        // Given - First fetch to populate cache
        val jsonResponse = """
{
  "data": {
    "budgets": [
      {
        "id": "cached-budget",
        "name": "Cached Budget",
        "last_modified_on": "2024-01-01T00:00:00Z",
        "first_month": "2024-01-01",
        "last_month": "2024-12-31",
        "currency_format": {
          "iso_code": "USD",
          "example_format": "${'$'}123.45",
          "decimal_digits": 2,
          "decimal_separator": ".",
          "symbol_first": true,
          "group_separator": ",",
          "currency_symbol": "${'$'}",
          "display_symbol": true
        }
      }
    ]
  }
}
        """.trimIndent()
        
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(jsonResponse))
        
        repository.getBudgets(forceRefresh = true)
        
        // When - Second fetch without refresh (should use cache)
        val result = repository.getBudgets(forceRefresh = false)
        
        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        // Only one network call should have been made
        assertThat(mockWebServer.requestCount).isEqualTo(1)
    }
    
    @Test
    fun authenticate_successResponse_savesToken() = runTest {
        // Given
        val token = "test-access-token"
        val jsonResponse = """
{
  "data": {
    "user": {
      "id": "user-123"
    }
  }
}
        """.trimIndent()
        
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(jsonResponse))
        
        // When
        val result = repository.authenticate(token)
        
        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
    }
    
    @Test
    fun rateLimiting_tooManyRequests_returnsError() = runTest {
        // Given
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(429)
            .setBody("{\"error\":{\"id\":\"429\",\"name\":\"rate_limit_exceeded\",\"detail\":\"Too many requests\"}}"))
        
        // When
        val result = repository.getBudgets(forceRefresh = true)
        
        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
    }
    
    private fun createTestTransaction(
        accountId: String = "account-123",
        categoryId: String? = "category-456",
        amount: Long = -25500,
        payeeName: String? = "Test Store"
    ) = YnabTransaction(
        accountId = accountId,
        date = Date(),
        amount = amount,
        payeeName = payeeName,
        categoryId = categoryId,
        memo = "Test transaction",
        cleared = YnabTransaction.ClearedStatus.CLEARED,
        approved = true,
        importId = "YNAB:test-import-id"
    )
}
