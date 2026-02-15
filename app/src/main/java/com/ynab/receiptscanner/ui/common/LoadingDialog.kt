package com.ynab.receiptscanner.ui.common

import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.ynab.receiptscanner.R

/**
 * Material loading dialog
 * Shows indeterminate progress indicator
 */
class LoadingDialog : DialogFragment() {
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_loading, null)
        builder.setView(view)
        
        isCancelable = false
        
        return builder.create()
    }
    
    companion object {
        private const val TAG = "LoadingDialog"
        
        fun show(fragmentManager: FragmentManager): LoadingDialog {
            val dialog = LoadingDialog()
            dialog.show(fragmentManager, TAG)
            return dialog
        }
    }
}
