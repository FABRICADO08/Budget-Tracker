package com.example.util

import android.content.Context
import android.content.Intent
import com.example.data.local.entity.Transaction
import com.example.data.local.entity.Loan
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StatementExporter {
    fun shareFinancialStatement(
        context: Context,
        transactions: List<Transaction>,
        loans: List<Loan>,
        monthlyBudget: Double
    ) {
        val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val currentYearMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val currentMonthTitle = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())

        val thisMonthExpenses = transactions.filter { it.date.startsWith(currentYearMonth) && it.type == "expense" }
        val thisMonthTotal = thisMonthExpenses.sumOf { it.amount }
        val allExpensesTotal = transactions.filter { it.type == "expense" }.sumOf { it.amount }
        val pendingLoansTotal = loans.filter { !it.isRepaid }.sumOf { it.amount }
        val repaidLoansTotal = loans.filter { it.isRepaid }.sumOf { it.amount }

        val sb = StringBuilder()
        sb.appendLine("==========================================")
        sb.appendLine("   BUDGET TRACKER - FINANCIAL STATEMENT   ")
        sb.appendLine("==========================================")
        sb.appendLine("Generated: $currentDate")
        sb.appendLine("Period:    $currentMonthTitle")
        sb.appendLine()
        sb.appendLine("--- MONTHLY BUDGET & SPENDING ---")
        sb.appendLine("Current Month Spent: R%.2f".format(thisMonthTotal))
        sb.appendLine("Monthly Budget Cap:  R%.2f".format(monthlyBudget))
        if (monthlyBudget > 0) {
            val pct = (thisMonthTotal / monthlyBudget) * 100
            val remaining = monthlyBudget - thisMonthTotal
            sb.appendLine("Budget Utilization:  %.1f%%".format(pct))
            if (remaining >= 0) {
                sb.appendLine("Remaining Budget:    R%.2f".format(remaining))
            } else {
                sb.appendLine("OVER BUDGET BY:      R%.2f".format(-remaining))
            }
        }
        sb.appendLine("All-Time Total Spent: R%.2f".format(allExpensesTotal))
        sb.appendLine()
        sb.appendLine("--- MONEY OWED (LOANS GIVEN) ---")
        sb.appendLine("Pending Return:      R%.2f".format(pendingLoansTotal))
        sb.appendLine("Recovered/Repaid:    R%.2f".format(repaidLoansTotal))
        sb.appendLine()
        sb.appendLine("--- RECENT EXPENSE TRANSACTIONS (${transactions.size} records) ---")
        sb.appendLine("Date       | Category       | Description          | Amount")
        sb.appendLine("---------------------------------------------------------------")
        transactions.take(50).forEach { t ->
            val date = t.date.padEnd(10).take(10)
            val cat = t.category.padEnd(14).take(14)
            val desc = t.description.padEnd(20).take(20)
            val amt = "R%.2f".format(t.amount).padStart(10)
            sb.appendLine("$date | $cat | $desc | $amt")
        }
        if (loans.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("--- LOANS GIVEN (${loans.size} records) ---")
            sb.appendLine("Borrower         | Lent Date  | Due Date   | Status   | Amount")
            sb.appendLine("---------------------------------------------------------------")
            loans.forEach { l ->
                val name = l.borrowerName.padEnd(16).take(16)
                val lDate = l.lentDate.padEnd(10).take(10)
                val dDate = (if (l.dueDate.isNotBlank()) l.dueDate else "N/A").padEnd(10).take(10)
                val status = (if (l.isRepaid) "REPAID" else "PENDING").padEnd(8)
                val amt = "R%.2f".format(l.amount).padStart(8)
                sb.appendLine("$name | $lDate | $dDate | $status | $amt")
            }
        }
        sb.appendLine()
        sb.appendLine("Exported from BudgetTracker Android App")

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "BudgetTracker Statement - $currentMonthTitle")
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Financial Statement"))
    }
}
