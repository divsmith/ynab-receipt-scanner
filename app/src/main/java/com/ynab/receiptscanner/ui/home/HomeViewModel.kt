package com.ynab.receiptscanner.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ynab.receiptscanner.domain.model.ConnectivityStatus
import com.ynab.receiptscanner.domain.model.Receipt
import com.ynab.receiptscanner.domain.model.SyncStatus
import com.ynab.receiptscanner.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for HomeFragment
 * Manages receipt list, filtering, refresh operations, and sync status
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getAllReceiptsUseCase: GetAllReceiptsUseCase,
    private val syncPendingTransactionsUseCase: SyncPendingTransactionsUseCase,
    private val observeConnectivityUseCase: ObserveConnectivityUseCase,
    private val retrySyncUseCase: RetrySyncUseCase,
    private val getSyncStatusUseCase: GetSyncStatusUseCase
) : ViewModel() {
    
    private val _receipts = MutableLiveData<List<Receipt>>()
    val receipts: LiveData<List<Receipt>> = _receipts
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    private val _filterStatus = MutableLiveData<SyncStatus?>(null)
    val filterStatus: LiveData<SyncStatus?> = _filterStatus
    
    // Connectivity status as StateFlow for better lifecycle handling
    private val _connectivityStatus = MutableStateFlow<ConnectivityStatus>(ConnectivityStatus.Unknown)
    val connectivityStatus: StateFlow<ConnectivityStatus> = _connectivityStatus.asStateFlow()
    
    // Pending receipts count
    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()
    
    // Current sync status
    private val _currentSyncStatus = MutableStateFlow(SyncStatus.SYNCED)
    val currentSyncStatus: StateFlow<SyncStatus> = _currentSyncStatus.asStateFlow()
    
    private var allReceipts: List<Receipt> = emptyList()
    
    init {
        observeConnectivity()
        observePendingReceipts()
    }
    
    private fun observeConnectivity() {
        viewModelScope.launch {
            observeConnectivityUseCase()
                .catch { throwable ->
                    _error.value = "Connectivity monitoring failed: ${throwable.message}"
                }
                .collect { status ->
                    _connectivityStatus.value = status
                }
        }
    }
    
    private fun observePendingReceipts() {
        viewModelScope.launch {
            getSyncStatusUseCase.getPendingReceipts()
                .catch { throwable ->
                    _error.value = "Failed to monitor pending receipts: ${throwable.message}"
                }
                .collect { pendingReceipts ->
                    _pendingCount.value = pendingReceipts.size
                    
                    // Update current sync status based on pending receipts
                    _currentSyncStatus.value = when {
                        pendingReceipts.isEmpty() -> SyncStatus.SYNCED
                        pendingReceipts.any { it.syncStatus == SyncStatus.SYNCING } -> SyncStatus.SYNCING
                        pendingReceipts.any { it.syncStatus == SyncStatus.FAILED } -> SyncStatus.FAILED
                        else -> SyncStatus.PENDING
                    }
                }
        }
    }
    
    fun loadReceipts() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            getAllReceiptsUseCase()
                .catch { throwable ->
                    _error.value = throwable.message ?: "Failed to load receipts"
                    _isLoading.value = false
                }
                .collect { receiptList ->
                    allReceipts = receiptList
                    applyFilter()
                    _isLoading.value = false
                }
        }
    }
    
    fun refreshReceipts() {
        viewModelScope.launch {
            try {
                // Attempt to sync pending transactions first
                syncPendingTransactionsUseCase()
                    .catch { } // Ignore sync errors during refresh
                    .collect { }
            } catch (e: Exception) {
                // Continue with loading even if sync fails
            }
            loadReceipts()
        }
    }
    
    fun retrySync() {
        viewModelScope.launch {
            try {
                _currentSyncStatus.value = SyncStatus.SYNCING
                retrySyncUseCase.retryAll()
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Sync failed: ${e.message}"
            }
        }
    }
    
    fun filterBy(status: SyncStatus?) {
        _filterStatus.value = status
        applyFilter()
    }
    
    private fun applyFilter() {
        val filtered = when (_filterStatus.value) {
            null -> allReceipts
            else -> allReceipts.filter { it.syncStatus == _filterStatus.value }
        }
        _receipts.value = filtered
    }
    
    fun deleteReceipt(receipt: Receipt) {
        // TODO: Implement delete functionality
        // This would require a DeleteReceiptUseCase
        viewModelScope.launch {
            try {
                // deleteReceiptUseCase(receipt.id)
                loadReceipts()
            } catch (e: Exception) {
                _error.value = "Failed to delete receipt: ${e.message}"
            }
        }
    }
}
