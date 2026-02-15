package com.ynab.receiptscanner.data.mapper

import com.ynab.receiptscanner.data.local.entity.LineItemEntity
import com.ynab.receiptscanner.data.local.entity.PendingTransactionEntity
import com.ynab.receiptscanner.data.local.entity.ReceiptEntity
import com.ynab.receiptscanner.domain.model.LineItem
import com.ynab.receiptscanner.domain.model.Receipt
import java.util.Date

/**
 * Mapper functions for converting between domain models and database entities
 * Provides bidirectional mapping for data layer operations
 */

/**
 * Convert Receipt domain model to ReceiptEntity
 */
fun Receipt.toEntity(): ReceiptEntity {
    return ReceiptEntity(
        id = id,
        payee = payee,
        amount = amount,
        date = date,
        currency = currency,
        tax = tax,
        imagePath = imagePath,
        ocrText = ocrText,
        syncStatus = syncStatus,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

/**
 * Convert ReceiptEntity to Receipt domain model
 */
fun ReceiptEntity.toDomain(): Receipt {
    return Receipt(
        id = id,
        payee = payee,
        amount = amount,
        date = date,
        currency = currency,
        tax = tax,
        imagePath = imagePath,
        ocrText = ocrText,
        syncStatus = syncStatus,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

/**
 * Convert list of ReceiptEntity to list of Receipt domain models
 */
fun List<ReceiptEntity>.toDomain(): List<Receipt> {
    return map { it.toDomain() }
}

/**
 * Convert list of Receipt domain models to list of ReceiptEntity
 */
fun List<Receipt>.toEntity(): List<ReceiptEntity> {
    return map { it.toEntity() }
}

/**
 * Convert LineItem domain model to LineItemEntity
 */
fun LineItem.toEntity(): LineItemEntity {
    return LineItemEntity(
        id = id,
        receiptId = receiptId,
        description = description,
        quantity = quantity,
        unitPrice = unitPrice,
        totalPrice = totalPrice
    )
}

/**
 * Convert LineItemEntity to LineItem domain model
 */
fun LineItemEntity.toDomain(): LineItem {
    return LineItem(
        id = id,
        receiptId = receiptId,
        description = description,
        quantity = quantity,
        unitPrice = unitPrice,
        totalPrice = totalPrice
    )
}

/**
 * Convert list of LineItemEntity to list of LineItem domain models
 */
fun List<LineItemEntity>.toLineItemDomain(): List<LineItem> {
    return map { it.toDomain() }
}

/**
 * Convert list of LineItem domain models to list of LineItemEntity
 */
fun List<LineItem>.toLineItemEntity(): List<LineItemEntity> {
    return map { it.toEntity() }
}

/**
 * Data class representing a receipt with its line items
 */
data class ReceiptWithLineItems(
    val receipt: Receipt,
    val lineItems: List<LineItem>
)

/**
 * Data class representing entity versions
 */
data class ReceiptEntityWithLineItems(
    val receipt: ReceiptEntity,
    val lineItems: List<LineItemEntity>
)

/**
 * Convert ReceiptEntityWithLineItems to ReceiptWithLineItems domain model
 */
fun ReceiptEntityWithLineItems.toDomain(): ReceiptWithLineItems {
    return ReceiptWithLineItems(
        receipt = receipt.toDomain(),
        lineItems = lineItems.toLineItemDomain()
    )
}

/**
 * Convert ReceiptWithLineItems to ReceiptEntityWithLineItems
 */
fun ReceiptWithLineItems.toEntity(): ReceiptEntityWithLineItems {
    return ReceiptEntityWithLineItems(
        receipt = receipt.toEntity(),
        lineItems = lineItems.toLineItemEntity()
    )
}

/**
 * Create PendingTransactionEntity from Receipt
 * Used for queueing receipts for sync to YNAB
 * @param receipt Source receipt
 * @param accountId YNAB account ID for the transaction
 * @param categoryId Optional YNAB category ID
 * @return PendingTransactionEntity or null if receipt doesn't have required fields
 */
fun Receipt.toPendingTransaction(
    accountId: String,
    categoryId: String? = null
): PendingTransactionEntity? {
    // Validate required fields
    if (payee == null || amount == null || date == null) {
        return null
    }
    
    // Convert amount to YNAB milliunits (amount * 1000)
    val amountInMilliunits = (amount * 1000).toLong()
    
    return PendingTransactionEntity(
        id = java.util.UUID.randomUUID().toString(),
        receiptId = id,
        accountId = accountId,
        categoryId = categoryId,
        date = date,
        amount = amountInMilliunits,
        payee = payee,
        memo = buildMemo(),
        cleared = "uncleared",
        approved = false,
        importId = "YNAB:receipt:$id",
        status = "PENDING",
        retryCount = 0,
        lastError = null,
        createdAt = Date(),
        updatedAt = Date()
    )
}

/**
 * Build memo string for transaction from receipt data
 */
private fun Receipt.buildMemo(): String {
    val parts = mutableListOf<String>()
    
    parts.add("Receipt Scanner")
    
    tax?.let { parts.add("Tax: ${currency.symbol}${String.format("%.2f", it)}") }
    imagePath?.let { parts.add("Image: Yes") }
    
    return parts.joinToString(" • ")
}

/**
 * Update Receipt with sync status from PendingTransactionEntity
 */
fun Receipt.updateFromPendingTransaction(pending: PendingTransactionEntity): Receipt {
    val newSyncStatus = when (pending.status) {
        "PENDING" -> com.ynab.receiptscanner.domain.model.SyncStatus.PENDING
        "PROCESSING" -> com.ynab.receiptscanner.domain.model.SyncStatus.SYNCING
        "COMPLETED" -> com.ynab.receiptscanner.domain.model.SyncStatus.SYNCED
        "FAILED" -> com.ynab.receiptscanner.domain.model.SyncStatus.FAILED
        else -> com.ynab.receiptscanner.domain.model.SyncStatus.PENDING
    }
    
    return copy(
        syncStatus = newSyncStatus,
        updatedAt = Date()
    )
}

// YNAB Entity Mappings

/**
 * Convert YnabBudget to BudgetEntity
 */
fun com.ynab.receiptscanner.domain.model.YnabBudget.toEntity(): com.ynab.receiptscanner.data.local.entity.BudgetEntity {
    return com.ynab.receiptscanner.data.local.entity.BudgetEntity(
        id = id,
        name = name,
        lastModifiedOn = lastModifiedOn
    )
}

/**
 * Convert BudgetEntity to YnabBudget
 */
fun com.ynab.receiptscanner.data.local.entity.BudgetEntity.toDomain(): com.ynab.receiptscanner.domain.model.YnabBudget {
    return com.ynab.receiptscanner.domain.model.YnabBudget(
        id = id,
        name = name,
        lastModifiedOn = lastModifiedOn,
        accounts = emptyList(),
        categories = emptyList()
    )
}

/**
 * Convert YnabAccount to AccountEntity
 */
fun com.ynab.receiptscanner.domain.model.YnabAccount.toEntity(): com.ynab.receiptscanner.data.local.entity.AccountEntity {
    return com.ynab.receiptscanner.data.local.entity.AccountEntity(
        id = id,
        budgetId = budgetId,
        name = name,
        type = type,
        balance = balance,
        closed = closed
    )
}

/**
 * Convert AccountEntity to YnabAccount
 */
fun com.ynab.receiptscanner.data.local.entity.AccountEntity.toDomain(): com.ynab.receiptscanner.domain.model.YnabAccount {
    return com.ynab.receiptscanner.domain.model.YnabAccount(
        id = id,
        budgetId = budgetId,
        name = name,
        type = type,
        balance = balance,
        closed = closed
    )
}

/**
 * Convert YnabCategory to CategoryEntity
 */
fun com.ynab.receiptscanner.domain.model.YnabCategory.toEntity(budgetId: String): com.ynab.receiptscanner.data.local.entity.CategoryEntity {
    return com.ynab.receiptscanner.data.local.entity.CategoryEntity(
        id = id,
        budgetId = budgetId,
        categoryGroupId = categoryGroupId,
        name = name,
        budgeted = budgeted,
        activity = activity,
        balance = balance
    )
}

/**
 * Convert CategoryEntity to YnabCategory
 */
fun com.ynab.receiptscanner.data.local.entity.CategoryEntity.toDomain(): com.ynab.receiptscanner.domain.model.YnabCategory {
    return com.ynab.receiptscanner.domain.model.YnabCategory(
        id = id,
        categoryGroupId = categoryGroupId,
        name = name,
        budgeted = budgeted,
        activity = activity,
        balance = balance
    )
}
