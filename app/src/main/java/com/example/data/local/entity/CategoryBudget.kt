package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A persisted category spending limit for one user and calendar month (yyyy-MM). */
@Entity(
    tableName = "category_budgets",
    indices = [Index(value = ["user_id", "category", "month"], unique = true)]
)
data class CategoryBudget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "month") val month: String,
    @ColumnInfo(name = "limit_amount") val limitAmount: Double,
    @ColumnInfo(name = "carry_unused") val carryUnused: Boolean = false,
    @ColumnInfo(name = "is_paused") val isPaused: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
