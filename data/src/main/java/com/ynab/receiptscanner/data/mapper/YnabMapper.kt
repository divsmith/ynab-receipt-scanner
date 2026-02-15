package com.ynab.receiptscanner.data.mapper

import com.ynab.receiptscanner.data.remote.dto.*
import com.ynab.receiptscanner.domain.model.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mapper for converting between YNAB DTOs and domain models
 */
@Singleton
class YnabMapper @Inject constructor() {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    
    /**
     * Convert BudgetDto to YnabBudget domain model
     */
    fun mapBudgetToDomain(dto: BudgetDto): YnabBudget {
        return YnabBudget(
            id = dto.id,
            name = dto.name,
            lastModifiedOn = parseDate(dto.lastModifiedOn),
            accounts = dto.accounts?.map { mapAccountToDomain(it, dto.id) } ?: emptyList(),
            categories = dto.categories?.map { mapCategoryToDomain(it) } ?: emptyList()
        )
    }
    
    /**
     * Convert AccountDto to YnabAccount domain model
     */
    fun mapAccountToDomain(dto: AccountDto, budgetId: String): YnabAccount {
        return YnabAccount(
            id = dto.id,
            budgetId = budgetId,
            name = dto.name,
            type = dto.type,
            balance = dto.balance,
            closed = dto.closed
        )
    }
    
    /**
     * Convert CategoryDto to YnabCategory domain model
     */
    fun mapCategoryToDomain(dto: CategoryDto): YnabCategory {
        return YnabCategory(
            id = dto.id,
            categoryGroupId = dto.categoryGroupId,
            name = dto.name,
            budgeted = dto.budgeted,
            activity = dto.activity,
            balance = dto.balance
        )
    }
    
    /**
     * Convert TransactionDto to YnabTransaction domain model
     */
    fun mapTransactionToDomain(dto: TransactionDto): YnabTransaction {
        return YnabTransaction(
            id = dto.id,
            accountId = dto.accountId,
            categoryId = dto.categoryId,
            date = parseDate(dto.date),
            amount = dto.amount,
            payee = dto.payeeName,
            memo = dto.memo,
            cleared = dto.cleared,
            approved = dto.approved,
            importId = dto.importId
        )
    }
    
    /**
     * Convert YnabTransaction domain model to SaveTransactionDto
     */
    fun mapTransactionToDto(transaction: YnabTransaction): SaveTransactionDto {
        return SaveTransactionDto(
            accountId = transaction.accountId,
            date = formatDate(transaction.date),
            amount = transaction.amount,
            payeeId = null, // We use payee name instead
            payeeName = transaction.payee,
            categoryId = transaction.categoryId,
            memo = transaction.memo,
            cleared = transaction.cleared,
            approved = transaction.approved,
            importId = transaction.importId
        )
    }
    
    /**
     * Create SaveTransactionDto from individual parameters
     */
    fun createSaveTransactionDto(
        accountId: String,
        date: Date,
        amount: Long,
        payeeName: String?,
        categoryId: String?,
        memo: String?,
        cleared: String = "cleared",
        approved: Boolean = false,
        importId: String? = null
    ): SaveTransactionDto {
        return SaveTransactionDto(
            accountId = accountId,
            date = formatDate(date),
            amount = amount,
            payeeId = null,
            payeeName = payeeName,
            categoryId = categoryId,
            memo = memo,
            cleared = cleared,
            approved = approved,
            importId = importId
        )
    }
    
    /**
     * Parse date string to Date object
     */
    private fun parseDate(dateString: String): Date {
        return try {
            if (dateString.contains('T')) {
                // Full datetime format
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(dateString) ?: Date()
            } else {
                // Date only format
                dateFormat.parse(dateString) ?: Date()
            }
        } catch (e: Exception) {
            Date()
        }
    }
    
    /**
     * Format Date to string for API
     */
    private fun formatDate(date: Date): String {
        return dateFormat.format(date)
    }
    
    /**
     * Flatten category groups to list of categories
     */
    fun flattenCategoryGroups(categoryGroups: List<CategoryGroupDto>): List<YnabCategory> {
        return categoryGroups
            .filterNot { it.deleted || it.hidden }
            .flatMap { group ->
                group.categories
                    .filterNot { it.deleted || it.hidden }
                    .map { mapCategoryToDomain(it) }
            }
    }
}
