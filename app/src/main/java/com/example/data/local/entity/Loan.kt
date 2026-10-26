package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loans")
data class Loan(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "borrower_name")
    val borrowerName: String,

    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "lent_date")
    val lentDate: String,

    @ColumnInfo(name = "due_date")
    val dueDate: String = "",

    @ColumnInfo(name = "note")
    val note: String = "",

    @ColumnInfo(name = "is_repaid")
    val isRepaid: Boolean = false,

    @ColumnInfo(name = "repaid_date")
    val repaidDate: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
