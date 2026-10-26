package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A repayment event. The loan's original principal is never overwritten by repayments. */
@Entity(
    tableName = "loan_repayments",
    foreignKeys = [
        ForeignKey(
            entity = Loan::class,
            parentColumns = ["id"],
            childColumns = ["loan_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["loan_id", "paid_date"])]
)
data class LoanRepayment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "loan_id") val loanId: Long,
    @ColumnInfo(name = "amount") val amount: Double,
    @ColumnInfo(name = "paid_date") val paidDate: String,
    @ColumnInfo(name = "note") val note: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
