package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CategoryBudget
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryBudgetDao {
    @Query("SELECT * FROM category_budgets WHERE month = :month ORDER BY category COLLATE NOCASE")
    fun observeForMonth(month: String): Flow<List<CategoryBudget>>

    @Query("SELECT * FROM category_budgets WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CategoryBudget?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(budget: CategoryBudget): Long

    @Update
    suspend fun update(budget: CategoryBudget)

    @Delete
    suspend fun delete(budget: CategoryBudget)

    @Query("SELECT * FROM category_budgets WHERE category = :category AND month = :month LIMIT 1")
    suspend fun find(category: String, month: String): CategoryBudget?
}
