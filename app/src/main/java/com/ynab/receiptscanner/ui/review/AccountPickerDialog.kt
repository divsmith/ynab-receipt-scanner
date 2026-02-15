package com.ynab.receiptscanner.ui.review

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ynab.receiptscanner.databinding.DialogAccountPickerBinding
import com.ynab.receiptscanner.domain.model.YnabAccount

/**
 * Bottom sheet dialog for selecting YNAB account
 * Features searchable list of accounts
 */
class AccountPickerDialog(
    private val accounts: List<YnabAccount>,
    private val onAccountSelected: (YnabAccount) -> Unit
) : BottomSheetDialogFragment() {
    
    private var _binding: DialogAccountPickerBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var accountAdapter: AccountAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAccountPickerBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupSearch()
    }
    
    private fun setupRecyclerView() {
        accountAdapter = AccountAdapter { account ->
            onAccountSelected(account)
            dismiss()
        }
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = accountAdapter
        }
        
        // Show only active accounts
        val activeAccounts = accounts.filter { it.isActive() }
        accountAdapter.submitList(activeAccounts)
    }
    
    private fun setupSearch() {
        binding.searchView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase()
                val filtered = accounts.filter {
                    it.isActive() && it.name.lowercase().contains(query)
                }
                accountAdapter.submitList(filtered)
            }
        })
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
