package com.example.data.local.dao

import androidx.room.ColumnInfo

/**
 * Aggregate loan recovery figures for a single authenticated user's loan portfolio.
 *
 * totalRecovered includes the full principal of loans explicitly marked repaid, and otherwise
 * includes recorded repayments capped at the original principal.
 */
data class LoanRecoverySummary(
    @ColumnInfo(name = "total_principal")
    val totalPrincipal: Double = 0.0,
    @ColumnInfo(name = "total_recovered")
    val totalRecovered: Double = 0.0,
    @ColumnInfo(name = "total_outstanding")
    val totalOutstanding: Double = 0.0
)
