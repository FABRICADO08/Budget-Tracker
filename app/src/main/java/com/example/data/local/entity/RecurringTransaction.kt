package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Schedule definition only; individual occurrences are recorded separately. Dates use yyyy-MM-dd. */
@Entity(tableName = "recurring_transactions", indices = [Index(value = ["is_active", "next_due_date"])])
data class RecurringTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "user_id", defaultValue = "''") val userId: String = "",
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "amount") val amount: Double,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "start_date") val startDate: String,
    @ColumnInfo(name = "next_due_date") val nextDueDate: String,
    @ColumnInfo(name = "frequency") val frequency: String,
    @ColumnInfo(name = "end_date") val endDate: String? = null,
    @ColumnInfo(name = "reminder_days_before") val reminderDaysBefore: Int = 1,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
