package com.ynab.receiptscanner.ui.review

import android.app.DatePickerDialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.ynab.receiptscanner.R
import com.ynab.receiptscanner.databinding.FragmentReviewBinding
import com.ynab.receiptscanner.ui.base.BaseFragment
import com.ynab.receiptscanner.ui.common.LoadingDialog
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Fragment for reviewing and editing scanned receipt data
 * Allows editing fields, selecting account/category, and submitting to YNAB
 */
@AndroidEntryPoint
class ReviewFragment : BaseFragment<FragmentReviewBinding>() {
    
    private val viewModel: ReviewViewModel by viewModels()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
    private var loadingDialog: LoadingDialog? = null
    
    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentReviewBinding {
        return FragmentReviewBinding.inflate(inflater, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupListeners()
        observeViewModel()
    }
    
    private fun setupListeners() {
        // Payee field
        binding.payeeEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val payee = binding.payeeEditText.text.toString()
                viewModel.updatePayee(payee)
            }
        }
        
        // Amount field
        binding.amountEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val amount = binding.amountEditText.text.toString().toDoubleOrNull()
                viewModel.updateAmount(amount)
            }
        }
        
        // Date picker
        binding.dateButton.setOnClickListener {
            showDatePicker()
        }
        
        // Account picker
        binding.accountButton.setOnClickListener {
            showAccountPicker()
        }
        
        // Category picker
        binding.categoryButton.setOnClickListener {
            showCategoryPicker()
        }
        
        // Submit button
        binding.submitButton.setOnClickListener {
            viewModel.submitTransaction()
        }
    }
    
    private fun observeViewModel() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ReviewState.Loading -> {
                    showLoading(true)
                }
                is ReviewState.Ready -> {
                    showLoading(false)
                    binding.submitButton.isEnabled = true
                }
                is ReviewState.Submitting -> {
                    showLoading(true)
                    binding.submitButton.isEnabled = false
                }
                is ReviewState.Success -> {
                    showLoading(false)
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.transaction_submitted),
                        Toast.LENGTH_SHORT
                    ).show()
                    findNavController().navigate(R.id.action_review_to_home)
                }
                is ReviewState.Error -> {
                    showLoading(false)
                    binding.submitButton.isEnabled = true
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
        
        viewModel.receipt.observe(viewLifecycleOwner) { receipt ->
            // Load receipt image
            receipt.imagePath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(path)
                    binding.receiptImage.setImageBitmap(bitmap)
                }
            }
            
            // Update fields
            binding.payeeEditText.setText(receipt.payee ?: "")
            binding.amountEditText.setText(receipt.amount?.toString() ?: "")
            receipt.date?.let {
                binding.dateButton.text = dateFormat.format(it)
            }
        }
        
        viewModel.selectedAccount.observe(viewLifecycleOwner) { account ->
            if (account != null) {
                binding.accountButton.text = account.name
            } else {
                binding.accountButton.text = getString(R.string.select_account)
            }
        }
        
        viewModel.selectedCategory.observe(viewLifecycleOwner) { category ->
            if (category != null) {
                binding.categoryButton.text = category.name
            } else {
                binding.categoryButton.text = getString(R.string.select_category)
            }
        }
        
        viewModel.validationErrors.observe(viewLifecycleOwner) { errors ->
            // Show validation errors
            binding.payeeLayout.error = errors["payee"]
            binding.amountLayout.error = errors["amount"]
            
            if (errors.containsKey("date")) {
                Snackbar.make(binding.root, errors["date"] ?: "", Snackbar.LENGTH_SHORT).show()
            }
            if (errors.containsKey("account")) {
                Snackbar.make(binding.root, errors["account"] ?: "", Snackbar.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        viewModel.receipt.value?.date?.let {
            calendar.time = it
        }
        
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                viewModel.updateDate(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    private fun showAccountPicker() {
        val accounts = viewModel.accounts.value ?: return
        val dialog = AccountPickerDialog(accounts) { account ->
            viewModel.selectAccount(account)
        }
        dialog.show(childFragmentManager, "account_picker")
    }
    
    private fun showCategoryPicker() {
        val categories = viewModel.categories.value ?: return
        val dialog = CategoryPickerDialog(categories) { category ->
            viewModel.selectCategory(category)
        }
        dialog.show(childFragmentManager, "category_picker")
    }
    
    private fun showLoading(show: Boolean) {
        if (show) {
            loadingDialog = LoadingDialog.show(childFragmentManager)
        } else {
            loadingDialog?.dismiss()
            loadingDialog = null
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        loadingDialog?.dismiss()
    }
}
