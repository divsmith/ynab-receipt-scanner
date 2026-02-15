package com.ynab.receiptscanner.ui.review

/**
 * Sealed class representing different states of the review screen
 */
sealed class ReviewState {
    object Loading : ReviewState()
    data class Ready(
        val hasUnsavedChanges: Boolean = false
    ) : ReviewState()
    object Submitting : ReviewState()
    object Success : ReviewState()
    data class Error(val message: String) : ReviewState()
}
