package com.ynab.receiptscanner.domain.model

/**
 * Represents the synchronization status of a receipt with YNAB backend
 */
enum class SyncStatus {
    /**
     * Receipt is waiting to be synced to YNAB
     */
    PENDING,
    
    /**
     * Receipt is currently being synced
     */
    SYNCING,
    
    /**
     * Receipt has been successfully synced to YNAB
     */
    SYNCED,
    
    /**
     * Sync failed, requires retry or user intervention
     */
    FAILED
}
