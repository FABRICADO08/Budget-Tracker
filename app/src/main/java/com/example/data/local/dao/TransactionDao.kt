package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) interface for the [Transaction] entity.
 * Defines comprehensive CRUD operations (Create, Read, Update, Delete)
 * within the Room database for financial management.
 */
@Dao
interface TransactionDao {

    // ==========================================
    // CREATE / INSERT OPERATIONS
    // ==========================================

    /**
     * Inserts a transaction without silently replacing an existing record on ID conflict.
     *
     * @param transaction The [Transaction] to insert.
     * @return The SQLite row ID of the inserted transaction.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: Transaction): Long

    /**
     * Inserts a list of transactions in a single batch operation.
     *
     * @param transactions The list of [Transaction]s to insert.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(transactions: List<Transaction>)

    // ==========================================
    // READ / QUERY OPERATIONS
    // ==========================================

    /**
     * Observes all transactions stored in the database, ordered chronologically (newest first).
     *
     * @return A reactive [Flow] emitting the updated list of transactions whenever data changes.
     */
    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    /**
     * Retrieves a single transaction by its primary key [id].
     *
     * @param id The ID of the transaction to retrieve.
     * @return The matching [Transaction], or null if not found.
     */
    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): Transaction?

    /**
     * Observes transactions recorded for a specific calendar date (formatted as yyyy-MM-dd).
     *
     * @param date The date string to filter by.
     * @return A [Flow] of matching transactions.
     */
    @Query("SELECT * FROM transactions WHERE date = :date ORDER BY id DESC")
    fun getTransactionsByDate(date: String): Flow<List<Transaction>>

    /**
     * Observes transactions categorized under a specific category name.
     *
     * @param category The category name (e.g. "Groceries", "Food & Dining").
     * @return A [Flow] of matching transactions.
     */
    @Query("SELECT * FROM transactions WHERE category = :category ORDER BY date DESC")
    fun getTransactionsByCategory(category: String): Flow<List<Transaction>>

    /**
     * Observes transactions filtered by type (e.g. "expense" vs "income").
     *
     * @param type The transaction type string.
     * @return A [Flow] of matching transactions.
     */
    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")
    fun getTransactionsByType(type: String): Flow<List<Transaction>>

    /**
     * Calculates the total expenditure sum for a specific calendar date.
     *
     * @param date The date string to filter by (yyyy-MM-dd).
     * @return A [Flow] emitting the total expense amount or null if no expenses exist.
     */
    @Query("SELECT SUM(amount) FROM transactions WHERE date = :date AND type = 'expense'")
    fun getTotalExpenseForDate(date: String): Flow<Double?>

    /**
     * Calculates the total expenditure sum for a specific year and month prefix (e.g., '2026-10').
     *
     * @param monthPrefix The prefix (e.g. '2026-10').
     * @return A [Flow] emitting the total expenditure amount for the month.
     */
    @Query("SELECT SUM(amount) FROM transactions WHERE date LIKE :monthPrefix || '%' AND type = 'expense'")
    fun getTotalExpenseForMonth(monthPrefix: String): Flow<Double?>

    // ==========================================
    // UPDATE OPERATIONS
    // ==========================================

    /**
     * Updates an existing transaction entity in the database.
     *
     * @param transaction The [Transaction] containing updated fields.
     */
    @Update
    suspend fun update(transaction: Transaction)

    // ==========================================
    // DELETE OPERATIONS
    // ==========================================

    /**
     * Deletes a specific transaction from the database.
     *
     * @param transaction The [Transaction] entity to delete.
     */
    @Delete
    suspend fun delete(transaction: Transaction)

    /**
     * Deletes a transaction by its primary key [id].
     *
     * @param id The ID of the transaction to delete.
     */
    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Deletes all transactions from the transactions table.
     */
    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}
