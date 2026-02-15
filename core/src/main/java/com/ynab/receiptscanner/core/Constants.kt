package com.ynab.receiptscanner.core

/**
 * Application-wide constants
 */
object Constants {
    
    // API Configuration
    const val YNAB_BASE_URL = "https://api.youneedabudget.com/v1/"
    const val API_TIMEOUT_SECONDS = 30L
    
    // OAuth Configuration
    const val OAUTH_AUTHORIZE_URL = "https://app.youneedabudget.com/oauth/authorize"
    const val OAUTH_TOKEN_URL = "https://api.youneedabudget.com/v1/oauth/token"
    const val OAUTH_REVOKE_URL = "https://api.youneedabudget.com/v1/oauth/token/revoke"
    const val OAUTH_CLIENT_ID = "YOUR_CLIENT_ID_HERE" // Replace with your registered app client ID
    const val OAUTH_REDIRECT_URI = "ynabreceipt://oauth/callback"
    const val OAUTH_TOKEN_REFRESH_BUFFER_SECONDS = 300L // Refresh 5 minutes before expiry
    
    // Sync Configuration
    const val SYNC_MAX_RETRIES = 5
    const val SYNC_RETRY_INITIAL_DELAY_MS = 1000L
    const val SYNC_RETRY_MAX_DELAY_MS = 32000L
    const val SYNC_RETRY_BACKOFF_MULTIPLIER = 2.0
    
    // Duplicate Detection
    const val DUPLICATE_DATE_TOLERANCE_DAYS = 2
    const val DUPLICATE_PAYEE_SIMILARITY_THRESHOLD = 0.8f
    
    // Database
    const val DATABASE_NAME = "ynab_receipt_scanner_db"
    const val DATABASE_VERSION = 1
    
    // Shared Preferences
    const val PREF_NAME = "ynab_receipt_scanner_prefs"
    const val PREF_API_KEY = "api_key"
    const val PREF_SELECTED_BUDGET_ID = "selected_budget_id"
    const val PREF_DEFAULT_ACCOUNT_ID = "default_account_id"
    
    // OCR Configuration
    const val OCR_CONFIDENCE_THRESHOLD = 0.7f
    const val OCR_MAX_RETRIES = 3
    
    // Receipt Image
    const val MAX_IMAGE_SIZE_MB = 5
    const val IMAGE_COMPRESSION_QUALITY = 80
    const val IMAGE_MAX_WIDTH = 1920
    const val IMAGE_MAX_HEIGHT = 1920
    
    // Date Formats
    const val DATE_FORMAT_DISPLAY = "MMM dd, yyyy"
    const val DATE_FORMAT_API = "yyyy-MM-dd"
    const val DATE_FORMAT_FULL = "yyyy-MM-dd'T'HH:mm:ss'Z'"
    
    // Transaction Defaults
    const val DEFAULT_CURRENCY = "USD"
    const val DEFAULT_CLEARED_STATUS = "cleared"
    const val DEFAULT_APPROVED_STATUS = false
    
    // WorkManager
    const val SYNC_WORK_NAME = "sync_receipts_work"
    const val OCR_WORK_NAME = "ocr_processing_work"
    
    // Camera
    const val CAMERA_PREVIEW_ASPECT_RATIO = 4.0 / 3.0
    
    // Validation
    const val MIN_AMOUNT = 0.01
    const val MAX_AMOUNT = 999999.99
    const val MAX_PAYEE_LENGTH = 50
    const val MAX_MEMO_LENGTH = 200
    
    // Error Messages
    const val ERROR_NETWORK = "Network error. Please check your connection."
    const val ERROR_API_KEY_INVALID = "Invalid API key. Please check your settings."
    const val ERROR_OCR_FAILED = "Failed to read receipt. Please try again."
    const val ERROR_CAMERA_PERMISSION = "Camera permission is required to scan receipts."
    const val ERROR_STORAGE_PERMISSION = "Storage permission is required to save receipts."
    
    // Feature Flags
    const val FEATURE_AUTO_SYNC = true
    const val FEATURE_OFFLINE_MODE = true
    const val FEATURE_ADVANCED_OCR = true
}
