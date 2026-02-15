package com.ynab.receiptscanner.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

/**
 * Base fragment with ViewBinding support
 * Provides common functionality for all fragments
 */
abstract class BaseFragment<VB : ViewBinding> : Fragment() {
    
    private var _binding: VB? = null
    protected val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = createBinding(inflater, container)
        return binding.root
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    /**
     * Create the ViewBinding instance for this fragment
     */
    abstract fun createBinding(inflater: LayoutInflater, container: ViewGroup?): VB
}
