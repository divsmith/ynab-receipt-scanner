package com.ynab.receiptscanner.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ynab.receiptscanner.R
import com.ynab.receiptscanner.databinding.DialogReceiptFilterBinding
import com.ynab.receiptscanner.domain.model.SyncStatus

/**
 * Bottom sheet dialog for filtering receipts by sync status
 */
class ReceiptFilterDialog(
    private val onFilterSelected: (SyncStatus?) -> Unit
) : BottomSheetDialogFragment() {
    
    private var _binding: DialogReceiptFilterBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogReceiptFilterBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.filterAll.setOnClickListener {
            onFilterSelected(null)
            dismiss()
        }
        
        binding.filterPending.setOnClickListener {
            onFilterSelected(SyncStatus.PENDING)
            dismiss()
        }
        
        binding.filterSyncing.setOnClickListener {
            onFilterSelected(SyncStatus.SYNCING)
            dismiss()
        }
        
        binding.filterSynced.setOnClickListener {
            onFilterSelected(SyncStatus.SYNCED)
            dismiss()
        }
        
        binding.filterError.setOnClickListener {
            onFilterSelected(SyncStatus.FAILED)
            dismiss()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
