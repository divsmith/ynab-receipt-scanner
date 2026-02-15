package com.ynab.receiptscanner.data.local.dao

import androidx.room.*
import com.ynab.receiptscanner.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Category operations
 */
@Dao
interface CategoryDao {
    
    @Query("SELECT * FROM categories WHERE budget_id = :budgetId ORDER BY name ASC")
    fun getCategoriesForBudget(budgetId: String): Flow<List<CategoryEntity>>
    
    @Query("SELECT * FROM categories WHERE budget_id = :budgetId AND category_group_id = :groupId ORDER BY name ASC")
    fun getCategoriesForGroup(budgetId: String, groupId: String): Flow<List<CategoryEntity>>
    
    @Query("SELECT * FROM categories WHERE id = :categoryId")
    fun getCategoryById(categoryId: String): Flow<CategoryEntity?>
    
    @Query("SELECT * FROM categories WHERE id = :categoryId")
    suspend fun getCategoryByIdSync(categoryId: String): CategoryEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)
    
    @Delete
    suspend fun deleteCategory(category: CategoryEntity)
    
    @Query("DELETE FROM categories WHERE budget_id = :budgetId")
    suspend fun deleteCategoriesForBudget(budgetId: String)
    
    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()
}
