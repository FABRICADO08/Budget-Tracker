package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Persistent threshold notification; the unique index prevents duplicate alerts. */
@Entity(
    tableName = "budget_alerts",
    foreignKeys = [
        ForeignKey(
            entity = CategoryBudget::class,
            parentColumns = ["id"],
            childColumns = ["budget_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["budget_id", "month", "threshold_percent"], unique = true)]
)
data class BudgetAlert(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "budget_id") val budgetId: Long,
    @ColumnInfo(name = "month") val month: String,
    @ColumnInfo(name = "threshold_percent") val thresholdPercent: Int,
    @ColumnInfo(name = "actual_amount") val actualAmount: Double,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
