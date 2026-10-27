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

    /**
     * Computes principal, recovered and outstanding totals from original loans plus the repayment
     * ledger. Explicitly marked-repaid legacy loans count as fully recovered even if they have no
     * historical repayment rows. Recorded repayments are capped at principal to avoid negative debt.
     */
    @Query("""
        SELECT
            COALESCE(SUM(l.amount), 0.0) AS total_principal,
            COALESCE(SUM(
                CASE
                    WHEN l.is_repaid = 1 THEN l.amount
                    ELSE MIN(COALESCE(r.repaid_amount, 0.0), l.amount)
                END
            ), 0.0) AS total_recovered,
            COALESCE(SUM(
                CASE
                    WHEN l.is_repaid = 1 THEN 0.0
                    ELSE MAX(l.amount - COALESCE(r.repaid_amount, 0.0), 0.0)
                END
            ), 0.0) AS total_outstanding
        FROM loans l
        LEFT JOIN (
            SELECT loan_id, SUM(amount) AS repaid_amount
            FROM loan_repayments
            GROUP BY loan_id
        ) r ON r.loan_id = l.id
        WHERE l.user_id = :userId
    """)
    fun observeRecoverySummaryForUser(userId: String): Flow<LoanRecoverySummary>

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
