package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.RecurringOccurrence
import com.example.data.local.entity.RecurringTransaction
import kotlinx.coroutines.flow.Flow

/** User-scoped DAO for recurring schedule data. */
@Dao
interface RecurringTransactionDao {
    @Query("SELECT * FROM recurring_transactions WHERE user_id = :userId ORDER BY is_active DESC, next_due_date ASC")
    fun observeAll(userId: String): Flow<List<RecurringTransaction>>

    @Query("SELECT * FROM recurring_transactions WHERE user_id = :userId ORDER BY created_at, id")
    suspend fun getAllForUser(userId: String): List<RecurringTransaction>

    @Query("""
        SELECT o.* FROM recurring_occurrences o
        INNER JOIN recurring_transactions s ON s.id = o.recurring_id
        WHERE s.user_id = :userId
        ORDER BY o.due_date, o.id
    """)
    suspend fun getAllOccurrencesForUser(userId: String): List<RecurringOccurrence>

    @Query("SELECT * FROM recurring_transactions WHERE user_id = :userId AND is_active = 1 AND next_due_date <= :throughDate ORDER BY next_due_date")
    suspend fun dueThrough(userId: String, throughDate: String): List<RecurringTransaction>

    @Query("SELECT * FROM recurring_transactions WHERE user_id = :userId AND is_active = 1 ORDER BY next_due_date ASC")
    suspend fun getActiveForUser(userId: String): List<RecurringTransaction>

    @Query("SELECT * FROM recurring_transactions WHERE id = :id AND user_id = :userId LIMIT 1")
    suspend fun getScheduleById(id: Long, userId: String): RecurringTransaction?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(schedule: RecurringTransaction): Long

    @Query("""
        UPDATE recurring_transactions SET description = :description, amount = :amount,
            category = :category, type = :type, start_date = :startDate,
            next_due_date = :nextDueDate, frequency = :frequency, end_date = :endDate,
            reminder_days_before = :reminderDaysBefore, is_active = :isActive
        WHERE id = :id AND user_id = :userId
    """)
    suspend fun updateForUser(
        id: Long,
        description: String,
        amount: Double,
        category: String,
        type: String,
        startDate: String,
        nextDueDate: String,
        frequency: String,
        endDate: String?,
        reminderDaysBefore: Int,
        isActive: Boolean,
        userId: String
    ): Int

    @Query("DELETE FROM recurring_transactions WHERE id = :id AND user_id = :userId")
    suspend fun deleteForUser(id: Long, userId: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOccurrenceIfNew(occurrence: RecurringOccurrence): Long

    @Query("""
        SELECT o.* FROM recurring_occurrences o
        INNER JOIN recurring_transactions s ON s.id = o.recurring_id
        WHERE s.user_id = :userId AND o.due_date BETWEEN :fromDate AND :throughDate
        ORDER BY o.due_date
    """)
    fun observeOccurrences(userId: String, fromDate: String, throughDate: String): Flow<List<RecurringOccurrence>>

    @Query("""
        SELECT o.* FROM recurring_occurrences o
        INNER JOIN recurring_transactions s ON s.id = o.recurring_id
        WHERE s.user_id = :userId AND o.recurring_id = :recurringId AND o.due_date = :dueDate
        LIMIT 1
    """)
    suspend fun getOccurrence(recurringId: Long, dueDate: String, userId: String): RecurringOccurrence?

    @Query("""
        SELECT o.* FROM recurring_occurrences o
        INNER JOIN recurring_transactions s ON s.id = o.recurring_id
        WHERE s.user_id = :userId AND o.id = :id LIMIT 1
    """)
    suspend fun getOccurrenceById(id: Long, userId: String): RecurringOccurrence?

    @Query("""
        UPDATE recurring_occurrences SET status = :status, transaction_id = :transactionId
        WHERE id = :id AND recurring_id IN (
            SELECT id FROM recurring_transactions WHERE user_id = :userId
        )
    """)
    suspend fun markOccurrence(id: Long, userId: String, status: String, transactionId: Long?): Int

    /** Compare-and-set so periodic work does not remind for the same occurrence repeatedly. */
    @Query("""
        UPDATE recurring_occurrences SET reminder_sent_at = :sentAt
        WHERE id = :id AND reminder_sent_at IS NULL AND recurring_id IN (
            SELECT id FROM recurring_transactions WHERE user_id = :userId
        )
    """)
    suspend fun markReminderSentIfPending(id: Long, userId: String, sentAt: Long): Int
}
