package com.ynab.receiptscanner.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.ynab.receiptscanner.R
import com.ynab.receiptscanner.databinding.FragmentHomeBinding
import com.ynab.receiptscanner.domain.model.ConnectivityStatus
import com.ynab.receiptscanner.domain.model.SyncStatus
import com.ynab.receiptscanner.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Home screen displaying list of scanned receipts
 * Features: List view, filter, refresh, navigation to review/camera, sync status
 */
@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>() {
    
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var receiptAdapter: ReceiptAdapter
    
    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupListeners()
        observeViewModel()
        
        viewModel.loadReceipts()
    }
    
    private fun setupRecyclerView() {
        receiptAdapter = ReceiptAdapter(
            onItemClick = { receipt ->
                val bundle = androidx.core.os.bundleOf("receiptId" to receipt.id)
                findNavController().navigate(R.id.action_home_to_review, bundle)
            },
            onDeleteClick = { receipt ->
                viewModel.deleteReceipt(receipt)
            }
        )
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = receiptAdapter
            setHasFixedSize(true)
        }
    }
    
    private fun setupListeners() {
        // FAB to navigate to camera
        binding.fabScan.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_camera)
        }
        
        // Swipe to refresh
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshReceipts()
        }
        
        // Filter button
        binding.filterButton.setOnClickListener {
            showFilterDialog()
        }
        
        // Sync status view - retry on click
        binding.syncStatusView.setOnRetryClickListener {
            viewModel.retrySync()
        }
    }
    
    private fun observeViewModel() {
        viewModel.receipts.observe(viewLifecycleOwner) { receipts ->
            receiptAdapter.submitList(receipts)
            updateEmptyState(receipts.isEmpty())
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
        }
        
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG)
                    .setAction(R.string.retry) {
                        viewModel.loadReceipts()
                    }
                    .show()
            }
        }
        
        viewModel.filterStatus.observe(viewLifecycleOwner) { status ->
            updateFilterButtonAppearance(status)
        }
        
        // Observe connectivity status
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.connectivityStatus.collect { status ->
                binding.syncStatusView.setConnectivityStatus(status)
                
                // Show snackbar when connectivity changes
                when (status) {
                    is ConnectivityStatus.Connected -> {
                        if (status.isMetered) {
                            showConnectivitySnackbar("Connected via cellular data")
                        } else {
                            showConnectivitySnackbar("Connected to WiFi")
                        }
                    }
                    is ConnectivityStatus.Disconnected -> {
                        showConnectivitySnackbar("No internet connection")
                    }
                    is ConnectivityStatus.Unknown -> {}
                }
            }
        }
        
        // Observe sync status
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentSyncStatus.collect { status ->
                binding.syncStatusView.setSyncStatus(status)
            }
        }
        
        // Observe pending count
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.pendingCount.collect { count ->
                binding.syncStatusView.setPendingCount(count)
            }
        }
    }
    
    private fun showConnectivitySnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyStateView.root.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.emptyStateView.root.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }
    }
    
    private fun updateFilterButtonAppearance(status: SyncStatus?) {
        val text = when (status) {
            null -> getString(R.string.filter_all)
            SyncStatus.PENDING -> getString(R.string.filter_pending)
            SyncStatus.SYNCING -> getString(R.string.filter_syncing)
            SyncStatus.SYNCED -> getString(R.string.filter_synced)
            SyncStatus.FAILED -> getString(R.string.filter_error)
        }
        binding.filterButton.text = text
    }
    
    private fun showFilterDialog() {
        val dialog = ReceiptFilterDialog { selectedStatus ->
            viewModel.filterBy(selectedStatus)
        }
        dialog.show(childFragmentManager, "filter_dialog")
    }
}
