package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.Loan
import kotlinx.coroutines.flow.Flow

/** All loan reads and mutations are restricted to the authenticated owner's UID. */
@Dao
interface LoanDao {
    @Query("SELECT * FROM loans WHERE user_id = :userId ORDER BY is_repaid ASC, created_at DESC")
    fun getAllLoans(userId: String): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE user_id = :userId ORDER BY is_repaid ASC, created_at DESC")
    suspend fun getAllForBackup(userId: String): List<Loan>

    @Query("SELECT * FROM loans WHERE id = :id AND user_id = :userId LIMIT 1")
    suspend fun getById(id: Long, userId: String): Loan?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(loan: Loan): Long

    @Query("""
        UPDATE loans
        SET borrower_name = :borrowerName, amount = :amount, lent_date = :lentDate,
            due_date = :dueDate, note = :note
        WHERE id = :id AND user_id = :userId
    """)
    suspend fun updateForUser(
        id: Long,
        borrowerName: String,
        amount: Double,
        lentDate: String,
        dueDate: String,
        note: String,
        userId: String
    ): Int

    @Query("DELETE FROM loans WHERE id = :id AND user_id = :userId")
    suspend fun deleteById(id: Long, userId: String): Int

    @Query("UPDATE loans SET is_repaid = :isRepaid, repaid_date = :repaidDate WHERE id = :id AND user_id = :userId")
    suspend fun updateRepaymentStatus(id: Long, userId: String, isRepaid: Boolean, repaidDate: String): Int

    @Query("SELECT COUNT(*) FROM loans WHERE user_id = ''")
    suspend fun countUnowned(): Int

    @Query("UPDATE loans SET user_id = :userId WHERE user_id = ''")
    suspend fun claimUnownedForUser(userId: String): Int
}
