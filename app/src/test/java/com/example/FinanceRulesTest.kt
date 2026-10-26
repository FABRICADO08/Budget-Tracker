package com.example

import com.example.data.importer.BankCsvParser
import com.example.domain.finance.BudgetRules
import com.example.domain.finance.RecurringDates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceRulesTest {
    @Test
    fun budgetProgressCountsExpensesOnlyAndNeverReportsNegativeRemaining() {
        val result = BudgetRules.progress(
            limit = 1_000.0,
            expenses = listOf("expense" to 750.0, "income" to 900.0, "expense" to 400.0)
        )
        assertEquals(1_150.0, result.spent, 0.001)
        assertEquals(0.0, result.remaining, 0.001)
        assertEquals(115.0, result.percentUsed, 0.001)
        assertTrue(result.exceeded)
    }

    @Test
    fun budgetThresholdsAreReturnedOncePerCrossedLevel() {
        assertEquals(listOf(70, 90), BudgetRules.crossedThresholds(100.0, 95.0))
        assertEquals(listOf(70, 90, 100), BudgetRules.crossedThresholds(100.0, 100.0))
        assertTrue(BudgetRules.crossedThresholds(100.0, 69.99).isEmpty())
        assertTrue(BudgetRules.crossedThresholds(0.0, 0.0).isEmpty())
    }

    @Test
    fun recurringMonthlyDateClampsEndOfMonth() {
        assertEquals("2026-02-28", RecurringDates.nextDueDate("2026-01-31", RecurringDates.Frequency.MONTHLY))
        assertEquals("2026-10-15", RecurringDates.nextDueDate("2026-07-15", RecurringDates.Frequency.QUARTERLY))
        assertEquals("2027-03-01", RecurringDates.nextDueDate("2027-02-22", RecurringDates.Frequency.WEEKLY))
    }

    @Test
    fun csvParserHandlesQuotedCommasAndSemicolonDecimalComma() {
        val statement = BankCsvParser.parse(
            "Date;Description;Debit;Credit\r\n2026-10-01;\"Cafe, Main Street\";125,50;\r\n"
        )
        assertEquals(';', statement.delimiter)
        assertEquals("Cafe, Main Street", statement.rows.single().values[1])
        assertEquals(125.50, BankCsvParser.parseAmount("R 125,50", decimalComma = true)!!, 0.001)
        assertEquals(1234.56, BankCsvParser.parseAmount("R1,234.56")!!, 0.001)
        assertNull(BankCsvParser.parseAmount("not an amount"))
    }

    @Test
    fun csvParserRejectsUnclosedQuotes() {
        val error = runCatching { BankCsvParser.parse("Date,Description\n2026-10-01,\"unfinished") }.exceptionOrNull()
        assertFalse(error == null)
    }
}
