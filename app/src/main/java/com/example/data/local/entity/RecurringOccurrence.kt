package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One due-date occurrence. Unique schedule/date prevents duplicate generated transactions. */
@Entity(
    tableName = "recurring_occurrences",
    foreignKeys = [
        ForeignKey(
            entity = RecurringTransaction::class,
            parentColumns = ["id"],
            childColumns = ["recurring_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["recurring_id", "due_date"], unique = true),
        Index(value = ["status", "due_date"])
    ]
)
data class RecurringOccurrence(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "recurring_id") val recurringId: Long,
    @ColumnInfo(name = "due_date") val dueDate: String,
    @ColumnInfo(name = "amount") val amount: Double,
    @ColumnInfo(name = "status") val status: String = "unpaid",
    @ColumnInfo(name = "transaction_id") val transactionId: Long? = null,
    @ColumnInfo(name = "reminder_sent_at") val reminderSentAt: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
