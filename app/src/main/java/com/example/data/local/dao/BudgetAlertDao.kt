package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.BudgetAlert
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetAlertDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNew(alert: BudgetAlert): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(alert: BudgetAlert): Long

    @Query("""
        SELECT a.* FROM budget_alerts a
        INNER JOIN category_budgets b ON b.id = a.budget_id
        WHERE b.user_id = :userId
        ORDER BY a.month, a.threshold_percent
    """)
    suspend fun getAllForUser(userId: String): List<BudgetAlert>

    @Query("SELECT * FROM budget_alerts WHERE budget_id = :budgetId AND month = :month ORDER BY threshold_percent")
    fun observeForBudgetMonth(budgetId: Long, month: String): Flow<List<BudgetAlert>>

    @Query("SELECT EXISTS(SELECT 1 FROM budget_alerts WHERE budget_id = :budgetId AND month = :month AND threshold_percent = :threshold)")
    suspend fun exists(budgetId: Long, month: String, threshold: Int): Boolean
}
