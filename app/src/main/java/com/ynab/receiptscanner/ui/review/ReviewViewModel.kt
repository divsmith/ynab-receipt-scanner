package com.ynab.receiptscanner.ui.review

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ynab.receiptscanner.domain.model.Receipt
import com.ynab.receiptscanner.domain.model.YnabAccount
import com.ynab.receiptscanner.domain.model.YnabCategory
import com.ynab.receiptscanner.domain.model.YnabTransaction
import com.ynab.receiptscanner.usecase.CreateTransactionUseCase
import com.ynab.receiptscanner.usecase.GetAccountsUseCase
import com.ynab.receiptscanner.usecase.GetAllReceiptsUseCase
import com.ynab.receiptscanner.usecase.GetCategoriesUseCase
import com.ynab.receiptscanner.usecase.SaveReceiptUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

/**
 * ViewModel for ReviewFragment
 * Manages receipt editing, validation, and submission to YNAB
 */
@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getAllReceiptsUseCase: GetAllReceiptsUseCase,
    private val saveReceiptUseCase: SaveReceiptUseCase,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val createTransactionUseCase: CreateTransactionUseCase
) : ViewModel() {
    
    private val receiptId: String = savedStateHandle.get<String>("receiptId")
        ?: throw IllegalArgumentException("Receipt ID is required")
    
    private val _state = MutableLiveData<ReviewState>(ReviewState.Loading)
    val state: LiveData<ReviewState> = _state
    
    private val _receipt = MutableLiveData<Receipt>()
    val receipt: LiveData<Receipt> = _receipt
    
    private val _accounts = MutableLiveData<List<YnabAccount>>()
    val accounts: LiveData<List<YnabAccount>> = _accounts
    
    private val _categories = MutableLiveData<List<YnabCategory>>()
    val categories: LiveData<List<YnabCategory>> = _categories
    
    private val _selectedAccount = MutableLiveData<YnabAccount?>()
    val selectedAccount: LiveData<YnabAccount?> = _selectedAccount
    
    private val _selectedCategory = MutableLiveData<YnabCategory?>()
    val selectedCategory: LiveData<YnabCategory?> = _selectedCategory
    
    private val _validationErrors = MutableLiveData<Map<String, String>>()
    val validationErrors: LiveData<Map<String, String>> = _validationErrors
    
    init {
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            _state.value = ReviewState.Loading
            
            try {
                // Load receipt
                val receipts = getAllReceiptsUseCase().first()
                val receipt = receipts.find { it.id == receiptId }
                    ?: throw IllegalStateException("Receipt not found")
                _receipt.value = receipt
                
                // TODO: Get budgetId from preferences or auth manager
                val budgetId = "default" // Placeholder
                
                // Load accounts
                when (val result = getAccountsUseCase(budgetId)) {
                    is com.ynab.receiptscanner.core.util.Result.Success -> {
                        _accounts.value = result.data
                    }
                    is com.ynab.receiptscanner.core.util.Result.Error -> {
                        // Handle error silently for now
                    }
                    is com.ynab.receiptscanner.core.util.Result.Loading -> {
                        // Keep current state
                    }
                }
                
                // Load categories
                when (val result = getCategoriesUseCase(budgetId)) {
                    is com.ynab.receiptscanner.core.util.Result.Success -> {
                        _categories.value = result.data
                    }
                    is com.ynab.receiptscanner.core.util.Result.Error -> {
                        // Handle error silently for now
                    }
                    is com.ynab.receiptscanner.core.util.Result.Loading -> {
                        // Keep current state
                    }
                }
                
                _state.value = ReviewState.Ready()
            } catch (e: Exception) {
                _state.value = ReviewState.Error(e.message ?: "Failed to load data")
            }
        }
    }
    
    fun updatePayee(payee: String) {
        _receipt.value = _receipt.value?.copy(payee = payee)
        _state.value = ReviewState.Ready(hasUnsavedChanges = true)
    }
    
    fun updateAmount(amount: Double?) {
        _receipt.value = _receipt.value?.copy(amount = amount)
        _state.value = ReviewState.Ready(hasUnsavedChanges = true)
    }
    
    fun updateDate(date: Date) {
        _receipt.value = _receipt.value?.copy(date = date)
        _state.value = ReviewState.Ready(hasUnsavedChanges = true)
    }
    
    fun selectAccount(account: YnabAccount) {
        _selectedAccount.value = account
        _state.value = ReviewState.Ready(hasUnsavedChanges = true)
    }
    
    fun selectCategory(category: YnabCategory) {
        _selectedCategory.value = category
        _state.value = ReviewState.Ready(hasUnsavedChanges = true)
    }
    
    fun submitTransaction() {
        if (!validate()) {
            return
        }
        
        viewModelScope.launch {
            _state.value = ReviewState.Submitting
            
            try {
                val receipt = _receipt.value ?: throw IllegalStateException("No receipt data")
                val account = _selectedAccount.value ?: throw IllegalStateException("No account selected")
                val category = _selectedCategory.value
                
                // Save receipt changes
                saveReceiptUseCase(receipt)
                
                // TODO: Get budgetId from preferences or auth manager
                val budgetId = "default" // Placeholder
                
                // Create transaction
                val transaction = YnabTransaction(
                    id = "", // Will be generated by YNAB
                    accountId = account.id,
                    date = receipt.date ?: Date(),
                    amount = ((receipt.amount ?: 0.0) * -1000).toLong(), // Convert to milliunits and negate for expense
                    payee = receipt.payee,
                    categoryId = category?.id,
                    memo = "Scanned receipt",
                    cleared = "cleared"
                )
                
                when (val result = createTransactionUseCase(budgetId, transaction)) {
                    is com.ynab.receiptscanner.core.util.Result.Success -> {
                        _state.value = ReviewState.Success
                    }
                    is com.ynab.receiptscanner.core.util.Result.Error -> {
                        _state.value = ReviewState.Error(result.exception.message ?: "Failed to create transaction")
                    }
                    is com.ynab.receiptscanner.core.util.Result.Loading -> {
                        // Keep submitting state
                    }
                }
            } catch (e: Exception) {
                _state.value = ReviewState.Error(e.message ?: "Failed to submit transaction")
            }
        }
    }
    
    private fun validate(): Boolean {
        val errors = mutableMapOf<String, String>()
        val receipt = _receipt.value
        
        if (receipt?.payee.isNullOrBlank()) {
            errors["payee"] = "Payee is required"
        }
        
        if (receipt?.amount == null || receipt.amount!! <= 0) {
            errors["amount"] = "Valid amount is required"
        }
        
        if (receipt?.date == null) {
            errors["date"] = "Date is required"
        }
        
        if (_selectedAccount.value == null) {
            errors["account"] = "Account is required"
        }
        
        _validationErrors.value = errors
        return errors.isEmpty()
    }
}
