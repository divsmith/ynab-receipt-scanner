package com.ynab.receiptscanner.data.local.dao

import androidx.room.*
import com.ynab.receiptscanner.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Budget operations
 */
@Dao
interface BudgetDao {
    
    @Query("SELECT * FROM budgets ORDER BY name ASC")
    fun getAllBudgets(): Flow<List<BudgetEntity>>
    
    @Query("SELECT * FROM budgets WHERE id = :budgetId")
    fun getBudgetById(budgetId: String): Flow<BudgetEntity?>
    
    @Query("SELECT * FROM budgets WHERE id = :budgetId")
    suspend fun getBudgetByIdSync(budgetId: String): BudgetEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<BudgetEntity>)
    
    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)
    
    @Query("DELETE FROM budgets")
    suspend fun deleteAllBudgets()
    
    @Query("DELETE FROM budgets WHERE id = :budgetId")
    suspend fun deleteBudgetById(budgetId: String)
}
