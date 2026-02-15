package com.ynab.receiptscanner.data.local.preference

import com.ynab.receiptscanner.data.security.SecureStorage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages application preferences and settings
 * Stores non-sensitive app configuration and user preferences
 */
@Singleton
class AppPreferences @Inject constructor(
    private val secureStorage: SecureStorage
) {
    
    /**
     * Set default account ID for new transactions
     */
    suspend fun setDefaultAccountId(accountId: String) {
        secureStorage.putString(KEY_DEFAULT_ACCOUNT_ID, accountId)
    }
    
    /**
     * Get default account ID
     */
    suspend fun getDefaultAccountId(): String? {
        return secureStorage.getString(KEY_DEFAULT_ACCOUNT_ID)
    }
    
    /**
     * Set default category ID for new transactions
     */
    suspend fun setDefaultCategoryId(categoryId: String) {
        secureStorage.putString(KEY_DEFAULT_CATEGORY_ID, categoryId)
    }
    
    /**
     * Get default category ID
     */
    suspend fun getDefaultCategoryId(): String? {
        return secureStorage.getString(KEY_DEFAULT_CATEGORY_ID)
    }
    
    /**
     * Set default currency code
     */
    suspend fun setDefaultCurrency(currencyCode: String) {
        secureStorage.putString(KEY_DEFAULT_CURRENCY, currencyCode)
    }
    
    /**
     * Get default currency code
     */
    suspend fun getDefaultCurrency(): String {
        return secureStorage.getString(KEY_DEFAULT_CURRENCY) ?: "USD"
    }
    
    /**
     * Enable/disable auto-sync for receipts
     */
    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        secureStorage.putBoolean(KEY_AUTO_SYNC_ENABLED, enabled)
    }
    
    /**
     * Check if auto-sync is enabled
     */
    suspend fun isAutoSyncEnabled(): Boolean {
        return secureStorage.getBoolean(KEY_AUTO_SYNC_ENABLED, true)
    }
    
    /**
     * Enable/disable auto-upload of receipt images
     */
    suspend fun setAutoUploadImages(enabled: Boolean) {
        secureStorage.putBoolean(KEY_AUTO_UPLOAD_IMAGES, enabled)
    }
    
    /**
     * Check if auto-upload images is enabled
     */
    suspend fun isAutoUploadImagesEnabled(): Boolean {
        return secureStorage.getBoolean(KEY_AUTO_UPLOAD_IMAGES, true)
    }
    
    /**
     * Enable/disable OCR confidence warnings
     */
    suspend fun setShowOcrWarnings(enabled: Boolean) {
        secureStorage.putBoolean(KEY_SHOW_OCR_WARNINGS, enabled)
    }
    
    /**
     * Check if OCR confidence warnings should be shown
     */
    suspend fun shouldShowOcrWarnings(): Boolean {
        return secureStorage.getBoolean(KEY_SHOW_OCR_WARNINGS, true)
    }
    
    /**
     * Set OCR confidence threshold (0.0 to 1.0)
     */
    suspend fun setOcrConfidenceThreshold(threshold: Float) {
        // Store as int (0-100) for SharedPreferences compatibility
        secureStorage.putInt(KEY_OCR_CONFIDENCE_THRESHOLD, (threshold * 100).toInt())
    }
    
    /**
     * Get OCR confidence threshold (0.0 to 1.0)
     */
    suspend fun getOcrConfidenceThreshold(): Float {
        val intValue = secureStorage.getInt(KEY_OCR_CONFIDENCE_THRESHOLD, 70)
        return intValue / 100f
    }
    
    /**
     * Enable/disable image encryption
     */
    suspend fun setImageEncryptionEnabled(enabled: Boolean) {
        secureStorage.putBoolean(KEY_IMAGE_ENCRYPTION_ENABLED, enabled)
    }
    
    /**
     * Check if image encryption is enabled
     */
    suspend fun isImageEncryptionEnabled(): Boolean {
        return secureStorage.getBoolean(KEY_IMAGE_ENCRYPTION_ENABLED, true)
    }
    
    /**
     * Set last sync timestamp
     */
    suspend fun setLastSyncTime(timestamp: Long) {
        secureStorage.putLong(KEY_LAST_SYNC_TIME, timestamp)
    }
    
    /**
     * Get last sync timestamp
     */
    suspend fun getLastSyncTime(): Long {
        return secureStorage.getLong(KEY_LAST_SYNC_TIME, 0L)
    }
    
    /**
     * Enable/disable dark mode
     */
    suspend fun setDarkModeEnabled(enabled: Boolean) {
        secureStorage.putBoolean(KEY_DARK_MODE_ENABLED, enabled)
    }
    
    /**
     * Check if dark mode is enabled
     */
    suspend fun isDarkModeEnabled(): Boolean {
        return secureStorage.getBoolean(KEY_DARK_MODE_ENABLED, false)
    }
    
    /**
     * Set whether onboarding has been completed
     */
    suspend fun setOnboardingCompleted(completed: Boolean) {
        secureStorage.putBoolean(KEY_ONBOARDING_COMPLETED, completed)
    }
    
    /**
     * Check if onboarding has been completed
     */
    suspend fun isOnboardingCompleted(): Boolean {
        return secureStorage.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }
    
    /**
     * Clear all app preferences
     */
    suspend fun clearAll() {
        secureStorage.remove(KEY_DEFAULT_ACCOUNT_ID)
        secureStorage.remove(KEY_DEFAULT_CATEGORY_ID)
        secureStorage.remove(KEY_DEFAULT_CURRENCY)
        secureStorage.remove(KEY_AUTO_SYNC_ENABLED)
        secureStorage.remove(KEY_AUTO_UPLOAD_IMAGES)
        secureStorage.remove(KEY_SHOW_OCR_WARNINGS)
        secureStorage.remove(KEY_OCR_CONFIDENCE_THRESHOLD)
        secureStorage.remove(KEY_IMAGE_ENCRYPTION_ENABLED)
        secureStorage.remove(KEY_LAST_SYNC_TIME)
        secureStorage.remove(KEY_DARK_MODE_ENABLED)
        secureStorage.remove(KEY_ONBOARDING_COMPLETED)
    }
    
    companion object {
        private const val KEY_DEFAULT_ACCOUNT_ID = "app_default_account_id"
        private const val KEY_DEFAULT_CATEGORY_ID = "app_default_category_id"
        private const val KEY_DEFAULT_CURRENCY = "app_default_currency"
        private const val KEY_AUTO_SYNC_ENABLED = "app_auto_sync_enabled"
        private const val KEY_AUTO_UPLOAD_IMAGES = "app_auto_upload_images"
        private const val KEY_SHOW_OCR_WARNINGS = "app_show_ocr_warnings"
        private const val KEY_OCR_CONFIDENCE_THRESHOLD = "app_ocr_confidence_threshold"
        private const val KEY_IMAGE_ENCRYPTION_ENABLED = "app_image_encryption_enabled"
        private const val KEY_LAST_SYNC_TIME = "app_last_sync_time"
        private const val KEY_DARK_MODE_ENABLED = "app_dark_mode_enabled"
        private const val KEY_ONBOARDING_COMPLETED = "app_onboarding_completed"
    }
}
