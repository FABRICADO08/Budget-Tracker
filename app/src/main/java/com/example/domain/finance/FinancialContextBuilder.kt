package com.example.domain.finance

import com.example.data.local.entity.Transaction
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Creates a minimal, aggregate-only snapshot for optional AI financial insights.
 *
 * It deliberately excludes transaction descriptions, IDs, user IDs, individual transaction dates,
 * merchant names, notes and account credentials. The caller must pass records already scoped to
 * the authenticated user's local repository.
 */
object FinancialContextBuilder {
    private const val MAX_CATEGORIES = 5
    private const val MAX_CONTEXT_LENGTH = 4_000

    fun build(
        transactions: List<Transaction>,
        monthlyBudgetLimit: Double,
        currentMonth: YearMonth = YearMonth.now()
    ): String {
        val valid = transactions.filter { transaction ->
            transaction.amount.isFinite() &&
                transaction.amount > 0.0 &&
                (transaction.type.equals("income", true) || transaction.type.equals("expense", true)) &&
                parseMonth(transaction.date) != null
        }
        val current = valid.filter { parseMonth(it.date) == currentMonth }
        val previousMonth = currentMonth.minusMonths(1)
        val previous = valid.filter { parseMonth(it.date) == previousMonth }

        val currentIncome = current.filter { it.type.equals("income", true) }.sumOf { it.amount }
        val currentExpense = current.filter { it.type.equals("expense", true) }.sumOf { it.amount }
        val previousExpense = previous.filter { it.type.equals("expense", true) }.sumOf { it.amount }
        val previousExpenseCount = previous.count { it.type.equals("expense", true) }
        val categoryTotals = current
            .filter { it.type.equals("expense", true) }
            .groupBy { safeCategory(it.category) }
            .mapValues { (_, items) -> items.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }

        return buildString {
            appendLine("Source: aggregate summary of this signed-in account's locally stored transactions. It may not include cloud-only records.")
            appendLine("Reporting month: ${currentMonth}")
            appendLine("Current-month income recorded: ${zar(currentIncome)}")
            appendLine("Current-month expenses recorded: ${zar(currentExpense)}")
            appendLine("Current-month net cash flow recorded: ${zar(currentIncome - currentExpense)}")
            if (monthlyBudgetLimit.isFinite() && monthlyBudgetLimit > 0.0) {
                appendLine("Configured monthly spending cap: ${zar(monthlyBudgetLimit)}")
                appendLine("Cap remaining against recorded expenses: ${zar(monthlyBudgetLimit - currentExpense)}")
            } else {
                appendLine("Configured monthly spending cap: not available")
            }

            if (previousExpenseCount > 0) {
                val difference = currentExpense - previousExpense
                val trend = when {
                    difference > 0.005 -> "up ${zar(difference)}"
                    difference < -0.005 -> "down ${zar(-difference)}"
                    else -> "unchanged"
                }
                appendLine("Previous-month expenses recorded: ${zar(previousExpense)}")
                appendLine("Month-over-month expense change: $trend")
                if (previousExpense > 0.0) {
                    appendLine("Month-over-month expense change percent: ${String.format(Locale.ROOT, "%.1f%%", difference / previousExpense * 100.0)}")
                }
            } else {
                appendLine("Previous-month expenses: no previous-month expense records available for comparison")
            }

            if (categoryTotals.isEmpty()) {
                appendLine("Current-month spending categories: no expense records")
            } else {
                appendLine("Highest current-month expense categories (aggregated; no individual descriptions):")
                categoryTotals.take(MAX_CATEGORIES).forEach { (category, total) ->
                    appendLine("- $category: ${zar(total)}")
                }
            }

            val trackedMonths = (0L..2L).map { currentMonth.minusMonths(it) }.reversed()
            appendLine("Recorded expenses by month:")
            trackedMonths.forEach { month ->
                val total = valid.asSequence()
                    .filter { parseMonth(it.date) == month && it.type.equals("expense", true) }
                    .sumOf { it.amount }
                appendLine("- $month: ${zar(total)}")
            }
            appendLine("Use recorded totals as facts; explain estimates and assumptions; do not infer unrecorded purchases, guaranteed returns, or balances outside this summary.")
        }.trim().take(MAX_CONTEXT_LENGTH)
    }

    // Category names are user-controlled. Flatten control characters and cap length so they
    // cannot inject extra prompt lines or cause an unexpectedly large outbound context.
    private fun safeCategory(value: String): String =
        value.replace(Regex("[\\r\\n\\t]+"), " ")
            .filterNot { it.isISOControl() }
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(60)
            .ifBlank { "Other" }

    private fun parseMonth(date: String): YearMonth? =
        try {
            if (date.length < 7) null else YearMonth.parse(date.substring(0, 7))
        } catch (_: DateTimeParseException) {
            null
        }

    private fun zar(amount: Double): String =
        "R " + BigDecimal.valueOf(amount)
            .setScale(2, RoundingMode.HALF_UP)
            .toPlainString()
}
