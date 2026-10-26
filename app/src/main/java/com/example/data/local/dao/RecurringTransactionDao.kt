package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.RecurringOccurrence
import com.example.data.local.entity.RecurringTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {
    @Query("SELECT * FROM recurring_transactions ORDER BY is_active DESC, next_due_date ASC")
    fun observeAll(): Flow<List<RecurringTransaction>>

    @Query("SELECT * FROM recurring_transactions WHERE is_active = 1 AND next_due_date <= :throughDate ORDER BY next_due_date")
    suspend fun dueThrough(throughDate: String): List<RecurringTransaction>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(schedule: RecurringTransaction): Long

    @Update
    suspend fun update(schedule: RecurringTransaction)

    @Delete
    suspend fun delete(schedule: RecurringTransaction)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOccurrenceIfNew(occurrence: RecurringOccurrence): Long

    @Query("SELECT * FROM recurring_occurrences WHERE due_date BETWEEN :fromDate AND :throughDate ORDER BY due_date")
    fun observeOccurrences(fromDate: String, throughDate: String): Flow<List<RecurringOccurrence>>

    @Query("SELECT * FROM recurring_occurrences WHERE recurring_id = :recurringId AND due_date = :dueDate LIMIT 1")
    suspend fun getOccurrence(recurringId: Long, dueDate: String): RecurringOccurrence?

    @Query("UPDATE recurring_occurrences SET status = :status, transaction_id = :transactionId WHERE id = :id")
    suspend fun markOccurrence(id: Long, status: String, transactionId: Long?)
}
