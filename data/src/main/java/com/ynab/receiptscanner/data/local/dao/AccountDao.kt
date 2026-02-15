package com.ynab.receiptscanner.data.local.dao

import androidx.room.*
import com.ynab.receiptscanner.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Account operations
 */
@Dao
interface AccountDao {
    
    @Query("SELECT * FROM accounts WHERE budget_id = :budgetId ORDER BY name ASC")
    fun getAccountsForBudget(budgetId: String): Flow<List<AccountEntity>>
    
    @Query("SELECT * FROM accounts WHERE budget_id = :budgetId AND closed = 0 ORDER BY name ASC")
    fun getActiveAccountsForBudget(budgetId: String): Flow<List<AccountEntity>>
    
    @Query("SELECT * FROM accounts WHERE id = :accountId")
    fun getAccountById(accountId: String): Flow<AccountEntity?>
    
    @Query("SELECT * FROM accounts WHERE id = :accountId")
    suspend fun getAccountByIdSync(accountId: String): AccountEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)
    
    @Delete
    suspend fun deleteAccount(account: AccountEntity)
    
    @Query("DELETE FROM accounts WHERE budget_id = :budgetId")
    suspend fun deleteAccountsForBudget(budgetId: String)
    
    @Query("DELETE FROM accounts")
    suspend fun deleteAllAccounts()
}
