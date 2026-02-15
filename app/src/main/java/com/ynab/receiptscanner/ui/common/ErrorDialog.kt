package com.ynab.receiptscanner.ui.common

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ynab.receiptscanner.BuildConfig
import com.ynab.receiptscanner.R

/**
 * Material dialog for displaying critical errors
 * Shows detailed error information in debug mode
 * Provides options to contact support or report bug
 */
class ErrorDialog : DialogFragment() {
    
    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_MESSAGE = "message"
        private const val ARG_ERROR_DETAILS = "error_details"
        private const val ARG_SHOW_REPORT = "show_report"
        
        const val SUPPORT_EMAIL = "support@example.com"
        const val BUG_REPORT_URL = "https://github.com/yourusername/ynab-receipt-scanner/issues/new"
        
        /**
         * Create error dialog instance
         */
        fun newInstance(
            title: String = "Error",
            message: String,
            errorDetails: String? = null,
            showReportOptions: Boolean = true
        ): ErrorDialog {
            return ErrorDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_MESSAGE, message)
                    putString(ARG_ERROR_DETAILS, errorDetails)
                    putBoolean(ARG_SHOW_REPORT, showReportOptions)
                }
            }
        }
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val title = arguments?.getString(ARG_TITLE) ?: "Error"
        val message = arguments?.getString(ARG_MESSAGE) ?: "An error occurred"
        val errorDetails = arguments?.getString(ARG_ERROR_DETAILS)
        val showReportOptions = arguments?.getBoolean(ARG_SHOW_REPORT) ?: true
        
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(buildMessage(message, errorDetails))
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
        
        if (showReportOptions) {
            builder.setNegativeButton("Report Bug") { _, _ ->
                reportBug(message, errorDetails)
            }
            
            if (errorDetails != null && BuildConfig.DEBUG) {
                builder.setNeutralButton("Copy Details") { _, _ ->
                    copyErrorDetails(errorDetails)
                }
            }
        }
        
        return builder.create()
    }
    
    private fun buildMessage(message: String, errorDetails: String?): String {
        return if (BuildConfig.DEBUG && errorDetails != null) {
            "$message\n\nDebug Info:\n$errorDetails"
        } else {
            message
        }
    }
    
    private fun reportBug(message: String, errorDetails: String?) {
        val context = requireContext()
        
        // Try to open GitHub issues page
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(BUG_REPORT_URL)
        }
        
        if (intent.resolveActivity(context.packageManager) != null) {
            startActivity(intent)
        } else {
            // Fallback to email
            sendEmailReport(message, errorDetails)
        }
    }
    
    private fun sendEmailReport(message: String, errorDetails: String?) {
        val context = requireContext()
        val emailBody = buildEmailBody(message, errorDetails)
        
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, "YNAB Receipt Scanner - Error Report")
            putExtra(Intent.EXTRA_TEXT, emailBody)
        }
        
        try {
            startActivity(Intent.createChooser(emailIntent, "Send error report"))
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to send email", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun buildEmailBody(message: String, errorDetails: String?): String {
        return buildString {
            appendLine("Error Report")
            appendLine("============")
            appendLine()
            appendLine("Message: $message")
            appendLine()
            if (errorDetails != null) {
                appendLine("Details:")
                appendLine(errorDetails)
                appendLine()
            }
            appendLine("App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Android Version: ${android.os.Build.VERSION.RELEASE}")
            appendLine("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            appendLine()
            appendLine("Please describe what you were doing when this error occurred:")
            appendLine()
        }
    }
    
    private fun copyErrorDetails(errorDetails: String) {
        val context = requireContext()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Error Details", errorDetails)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Error details copied to clipboard", Toast.LENGTH_SHORT).show()
    }
}
