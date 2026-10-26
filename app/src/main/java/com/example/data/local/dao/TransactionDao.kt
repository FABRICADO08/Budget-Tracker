package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * User-scoped data access for transactions. Every read, update, and delete requires a UID.
 * Only count/claim operations can touch unowned legacy rows, and these are used by the
 * explicit one-time legacy-data consent flow.
 */
@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: Transaction): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(transactions: List<Transaction>)

    @Query("SELECT * FROM transactions WHERE user_id = :userId ORDER BY date DESC, id DESC")
    fun getAllTransactions(userId: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id AND user_id = :userId LIMIT 1")
    suspend fun getTransactionById(id: Long, userId: String): Transaction?

    @Query("SELECT * FROM transactions WHERE date = :date AND user_id = :userId ORDER BY id DESC")
    fun getTransactionsByDate(date: String, userId: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE category = :category AND user_id = :userId ORDER BY date DESC, id DESC")
    fun getTransactionsByCategory(category: String, userId: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE type = :type AND user_id = :userId ORDER BY date DESC, id DESC")
    fun getTransactionsByType(type: String, userId: String): Flow<List<Transaction>>

    @Query("SELECT SUM(amount) FROM transactions WHERE date = :date AND type = 'expense' AND user_id = :userId")
    fun getTotalExpenseForDate(date: String, userId: String): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE date LIKE :monthPrefix || '%' AND type = 'expense' AND user_id = :userId")
    fun getTotalExpenseForMonth(monthPrefix: String, userId: String): Flow<Double?>

    @Query("""
        UPDATE transactions
        SET amount = :amount, category = :category, date = :date,
            description = :description, type = :type
        WHERE id = :id AND user_id = :userId
    """)
    suspend fun updateForUser(
        id: Long,
        amount: Double,
        category: String,
        date: String,
        description: String,
        type: String,
        userId: String
    ): Int

    @Query("DELETE FROM transactions WHERE id = :id AND user_id = :userId")
    suspend fun deleteById(id: Long, userId: String): Int

    @Query("DELETE FROM transactions WHERE user_id = :userId")
    suspend fun deleteAll(userId: String): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE user_id = ''")
    suspend fun countUnowned(): Int

    @Query("UPDATE transactions SET user_id = :userId WHERE user_id = ''")
    suspend fun claimUnownedForUser(userId: String): Int
}
