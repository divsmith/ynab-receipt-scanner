package com.ynab.receiptscanner.ui.common

import android.view.View
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Snackbar display queue to avoid overlapping messages
 * Ensures snackbars are shown sequentially and not duplicated
 */
@Singleton
class SnackbarManager @Inject constructor() {
    
    data class SnackbarMessage(
        val message: String,
        val duration: Int = Snackbar.LENGTH_SHORT,
        val actionText: String? = null,
        val action: (() -> Unit)? = null,
        val id: String = message.hashCode().toString()
    )
    
    private val messageQueue = ConcurrentLinkedQueue<SnackbarMessage>()
    private val _currentMessage = MutableStateFlow<SnackbarMessage?>(null)
    val currentMessage: StateFlow<SnackbarMessage?> = _currentMessage
    
    private var currentSnackbar: Snackbar? = null
    private val shownMessageIds = mutableSetOf<String>()
    
    /**
     * Show a snackbar message
     * @param message Message to display
     * @param duration Snackbar duration
     * @param actionText Optional action button text
     * @param action Optional action callback
     */
    fun showMessage(
        message: String,
        duration: Int = Snackbar.LENGTH_SHORT,
        actionText: String? = null,
        action: (() -> Unit)? = null
    ) {
        val snackbarMessage = SnackbarMessage(message, duration, actionText, action)
        
        // Avoid duplicate messages
        if (shownMessageIds.contains(snackbarMessage.id)) {
            return
        }
        
        messageQueue.offer(snackbarMessage)
        processQueue()
    }
    
    /**
     * Show error message with retry action
     */
    fun showError(
        message: String,
        onRetry: (() -> Unit)? = null
    ) {
        showMessage(
            message = message,
            duration = Snackbar.LENGTH_LONG,
            actionText = if (onRetry != null) "Retry" else null,
            action = onRetry
        )
    }
    
    /**
     * Show success message
     */
    fun showSuccess(message: String) {
        showMessage(message, Snackbar.LENGTH_SHORT)
    }
    
    /**
     * Show message with custom action
     */
    fun showWithAction(
        message: String,
        actionText: String,
        action: () -> Unit
    ) {
        showMessage(message, Snackbar.LENGTH_LONG, actionText, action)
    }
    
    /**
     * Process the message queue
     */
    private fun processQueue() {
        if (currentSnackbar != null) {
            // Already showing a snackbar
            return
        }
        
        val nextMessage = messageQueue.poll() ?: return
        _currentMessage.value = nextMessage
        shownMessageIds.add(nextMessage.id)
        
        // Clean up old IDs to prevent memory leak
        if (shownMessageIds.size > 100) {
            shownMessageIds.clear()
        }
    }
    
    /**
     * Create and show snackbar from current message
     */
    fun showSnackbar(view: View) {
        val message = _currentMessage.value ?: return
        
        val snackbar = Snackbar.make(view, message.message, message.duration)
        
        if (message.actionText != null && message.action != null) {
            snackbar.setAction(message.actionText) {
                message.action.invoke()
            }
        }
        
        snackbar.addCallback(object : Snackbar.Callback() {
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                currentSnackbar = null
                _currentMessage.value = null
                processQueue()
            }
        })
        
        currentSnackbar = snackbar
        snackbar.show()
    }
    
    /**
     * Dismiss current snackbar
     */
    fun dismiss() {
        currentSnackbar?.dismiss()
        currentSnackbar = null
        _currentMessage.value = null
    }
    
    /**
     * Clear all queued messages
     */
    fun clearQueue() {
        messageQueue.clear()
        dismiss()
    }
}
