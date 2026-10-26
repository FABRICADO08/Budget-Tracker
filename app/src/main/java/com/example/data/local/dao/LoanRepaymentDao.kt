package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.LoanRepayment
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanRepaymentDao {
    @Query("SELECT * FROM loan_repayments WHERE loan_id = :loanId ORDER BY paid_date DESC, id DESC")
    fun observeForLoan(loanId: Long): Flow<List<LoanRepayment>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM loan_repayments WHERE loan_id = :loanId")
    fun observeTotalForLoan(loanId: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM loan_repayments WHERE loan_id = :loanId")
    suspend fun totalForLoan(loanId: Long): Double

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(repayment: LoanRepayment): Long

    @Delete
    suspend fun delete(repayment: LoanRepayment)

    @Query("DELETE FROM loan_repayments WHERE id = :id")
    suspend fun deleteById(id: Long)
}
