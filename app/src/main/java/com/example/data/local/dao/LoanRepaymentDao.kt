package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.LoanRepayment
import kotlinx.coroutines.flow.Flow

/** Repayment reads/deletes must verify the owner through the parent loan row. */
@Dao
interface LoanRepaymentDao {
    @Query("""
        SELECT r.* FROM loan_repayments r
        INNER JOIN loans l ON l.id = r.loan_id
        WHERE r.loan_id = :loanId AND l.user_id = :userId
        ORDER BY r.paid_date DESC, r.id DESC
    """)
    fun observeForLoan(loanId: Long, userId: String): Flow<List<LoanRepayment>>

    @Query("""
        SELECT COALESCE(SUM(r.amount), 0.0) FROM loan_repayments r
        INNER JOIN loans l ON l.id = r.loan_id
        WHERE r.loan_id = :loanId AND l.user_id = :userId
    """)
    fun observeTotalForLoan(loanId: Long, userId: String): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(r.amount), 0.0) FROM loan_repayments r
        INNER JOIN loans l ON l.id = r.loan_id
        WHERE r.loan_id = :loanId AND l.user_id = :userId
    """)
    suspend fun totalForLoan(loanId: Long, userId: String): Double

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(repayment: LoanRepayment): Long

    @Query("""
        SELECT r.* FROM loan_repayments r
        INNER JOIN loans l ON l.id = r.loan_id
        WHERE l.user_id = :userId
        ORDER BY r.loan_id, r.paid_date, r.id
    """)
    suspend fun getAllForUser(userId: String): List<LoanRepayment>

    @Query("""
        DELETE FROM loan_repayments WHERE id = :id AND loan_id IN (
            SELECT id FROM loans WHERE user_id = :userId
        )
    """)
    suspend fun deleteByIdForUser(id: Long, userId: String): Int
}
