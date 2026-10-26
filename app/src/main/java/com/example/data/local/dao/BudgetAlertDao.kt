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

    @Query("SELECT * FROM budget_alerts WHERE budget_id = :budgetId AND month = :month ORDER BY threshold_percent")
    fun observeForBudgetMonth(budgetId: Long, month: String): Flow<List<BudgetAlert>>

    @Query("SELECT EXISTS(SELECT 1 FROM budget_alerts WHERE budget_id = :budgetId AND month = :month AND threshold_percent = :threshold)")
    suspend fun exists(budgetId: Long, month: String, threshold: Int): Boolean
}
