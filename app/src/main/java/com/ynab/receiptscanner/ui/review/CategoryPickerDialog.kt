package com.ynab.receiptscanner.ui.review

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ynab.receiptscanner.databinding.DialogCategoryPickerBinding
import com.ynab.receiptscanner.domain.model.YnabCategory

/**
 * Bottom sheet dialog for selecting YNAB category
 * Features searchable hierarchical category tree
 */
class CategoryPickerDialog(
    private val categories: List<YnabCategory>,
    private val onCategorySelected: (YnabCategory) -> Unit
) : BottomSheetDialogFragment() {
    
    private var _binding: DialogCategoryPickerBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var categoryAdapter: CategoryAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCategoryPickerBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupSearch()
    }
    
    private fun setupRecyclerView() {
        categoryAdapter = CategoryAdapter { category ->
            onCategorySelected(category)
            dismiss()
        }
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryAdapter
        }
        
        categoryAdapter.submitList(categories)
    }
    
    private fun setupSearch() {
        binding.searchView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase()
                val filtered = if (query.isEmpty()) {
                    categories
                } else {
                    categories.filter { it.name.lowercase().contains(query) }
                }
                categoryAdapter.submitList(filtered)
            }
        })
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
