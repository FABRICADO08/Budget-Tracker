package com.example

import com.example.data.local.entity.Transaction
import com.example.domain.finance.FinancialContextBuilder
import java.time.YearMonth
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialContextBuilderTest {
    @Test
    fun contextAggregatesIncomeExpensesCategoriesAndPreviousMonth() {
        val transactions = listOf(
            transaction(120.50, "Groceries", "2026-10-03", "Supermarket on Strand", "expense"),
            transaction(5.00, "Groceries", "2026-10-07", "Corner shop purchase", "expense"),
            transaction(70.00, "Transport", "2026-10-08", "Ride to work", "expense"),
            transaction(1_000.00, "Salary", "2026-10-01", "Employer payroll memo", "income"),
            transaction(200.00, "Groceries", "2026-09-11", "Last month market purchase", "expense")
        )

        val context = FinancialContextBuilder.build(
            transactions = transactions,
            monthlyBudgetLimit = 500.00,
            currentMonth = YearMonth.of(2026, 10)
        )

        assertTrue(context.contains("Current-month income recorded: R 1000.00"))
        assertTrue(context.contains("Current-month expenses recorded: R 195.50"))
        assertTrue(context.contains("Current-month net cash flow recorded: R 804.50"))
        assertTrue(context.contains("Configured monthly spending cap: R 500.00"))
        assertTrue(context.contains("Cap remaining against recorded expenses: R 304.50"))
        assertTrue(context.contains("Previous-month expenses recorded: R 200.00"))
        assertTrue(context.contains("Groceries: R 125.50"))
        assertTrue(context.contains("Transport: R 70.00"))
        assertFalse(context.contains("Supermarket on Strand"))
        assertFalse(context.contains("Employer payroll memo"))
        assertFalse(context.contains("Corner shop purchase"))
    }

    @Test
    fun incomeIsNotCountedAsSpendingAndInvalidAmountsAreIgnored() {
        val transactions = listOf(
            transaction(900.00, "Salary", "2026-10-01", "salary details", "income"),
            transaction(-10.00, "Other", "2026-10-02", "invalid negative", "expense"),
            transaction(Double.NaN, "Other", "2026-10-03", "invalid NaN", "expense"),
            transaction(25.00, "Food", "2026-10-04", "food details", "expense")
        )

        val context = FinancialContextBuilder.build(
            transactions = transactions,
            monthlyBudgetLimit = 100.00,
            currentMonth = YearMonth.of(2026, 10)
        )

        assertTrue(context.contains("Current-month income recorded: R 900.00"))
        assertTrue(context.contains("Current-month expenses recorded: R 25.00"))
    }

    @Test
    fun contextIsBoundedAndStatesThatItMayOmitCloudOnlyRecords() {
        val transactions = (1..250).map { i ->
            transaction(i.toDouble(), "Category $i", "2026-10-01", "description $i", "expense")
        }

        val context = FinancialContextBuilder.build(
            transactions = transactions,
            monthlyBudgetLimit = 0.0,
            currentMonth = YearMonth.of(2026, 10)
        )

        assertTrue(context.length <= 4_000)
        assertTrue(context.contains("may not include cloud-only records"))
        assertFalse(context.contains("description 1"))
    }

    private fun transaction(
        amount: Double,
        category: String,
        date: String,
        description: String,
        type: String
    ) = Transaction(
        amount = amount,
        category = category,
        date = date,
        description = description,
        type = type,
        userId = "test-user"
    )
}
