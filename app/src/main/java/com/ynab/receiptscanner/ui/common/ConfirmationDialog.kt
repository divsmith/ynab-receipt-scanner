package com.ynab.receiptscanner.ui.common

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Reusable confirmation dialog
 */
class ConfirmationDialog private constructor() : DialogFragment() {
    
    private var title: String? = null
    private var message: String? = null
    private var positiveButtonText: String? = null
    private var negativeButtonText: String? = null
    private var onConfirm: (() -> Unit)? = null
    private var onCancel: (() -> Unit)? = null
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveButtonText) { _, _ ->
                onConfirm?.invoke()
            }
            .setNegativeButton(negativeButtonText) { _, _ ->
                onCancel?.invoke()
            }
            .create()
    }
    
    companion object {
        fun create(
            title: String,
            message: String,
            positiveButtonText: String,
            negativeButtonText: String,
            onConfirm: () -> Unit,
            onCancel: (() -> Unit)? = null
        ): ConfirmationDialog {
            return ConfirmationDialog().apply {
                this.title = title
                this.message = message
                this.positiveButtonText = positiveButtonText
                this.negativeButtonText = negativeButtonText
                this.onConfirm = onConfirm
                this.onCancel = onCancel
            }
        }
    }
}
