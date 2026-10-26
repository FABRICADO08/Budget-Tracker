package com.example.data.local.repository

import androidx.room.withTransaction
import com.example.data.local.database.AppDatabase
import com.example.data.local.entity.BudgetAlert
import com.example.data.local.entity.CategoryBudget
import com.example.data.local.entity.LoanRepayment
import com.example.data.local.entity.RecurringOccurrence
import com.example.data.local.entity.Transaction
import com.example.domain.finance.BudgetRules
import kotlinx.coroutines.flow.Flow

/**
 * Transactional operations for finance features. The local Room database remains the source
 * of truth for these locally stored records; cloud synchronization is deliberately not implied.
 */
class FinanceRepository(private val database: AppDatabase) {
    private val budgets = database.categoryBudgetDao()
    private val alerts = database.budgetAlertDao()
    private val recurring = database.recurringTransactionDao()
    private val repayments = database.loanRepaymentDao()
    private val loans = database.loanDao()
    private val transactions = database.transactionDao()

    fun observeBudgets(month: String, userId: String = ""): Flow<List<CategoryBudget>> = budgets.observeForMonth(month, userId)

    suspend fun saveBudget(budget: CategoryBudget): Long {
        require(budget.userId.isNotBlank()) { "A signed-in user is required." }
        require(budget.category.isNotBlank()) { "Choose a category." }
        require(budget.month.matches(Regex("\\d{4}-\\d{2}"))) { "Month must use yyyy-MM." }
        require(budget.limitAmount.isFinite() && budget.limitAmount > 0.0) { "Budget must be greater than zero." }
        return if (budget.id == 0L) budgets.insert(budget) else {
            budgets.update(budget)
            budget.id
        }
    }

    suspend fun deleteBudget(budget: CategoryBudget) = budgets.delete(budget)

    /** Inserts each threshold alert once per budget and month, even if this is called repeatedly. */
    suspend fun recordCrossedBudgetAlerts(
        budget: CategoryBudget,
        spent: Double,
        effectiveLimit: Double = budget.limitAmount
    ): List<Int> = database.withTransaction {
            require(budget.id > 0L) { "Save the budget before recording alerts." }
            val thresholds = BudgetRules.crossedThresholds(effectiveLimit, spent)
            thresholds.forEach { threshold ->
                alerts.insertIfNew(
                    BudgetAlert(
                        budgetId = budget.id,
                        month = budget.month,
                        thresholdPercent = threshold,
                        actualAmount = spent
                    )
                )
            }
            thresholds
        }

    fun observeRecurringSchedules(userId: String) = recurring.observeAll(userId)

    suspend fun createRecurringSchedule(
        description: String,
        amount: Double,
        category: String,
        type: String,
        startDate: String,
        nextDueDate: String,
        frequency: String,
        endDate: String? = null,
        reminderDaysBefore: Int = 1,
        userId: String = ""
    ): Long {
        require(userId.isNotBlank()) { "A signed-in user is required." }
        require(description.isNotBlank()) { "Description is required." }
        require(amount.isFinite() && amount > 0.0) { "Amount must be greater than zero." }
        require(category.isNotBlank()) { "Category is required." }
        require(type == "income" || type == "expense") { "Type must be income or expense." }
        require(frequency in setOf("WEEKLY", "BIWEEKLY", "MONTHLY", "QUARTERLY", "ANNUALLY")) {
            "Unsupported recurrence frequency."
        }
        require(reminderDaysBefore in 0..30) { "Reminder lead time must be between 0 and 30 days." }
        require(nextDueDate >= startDate) { "Next due date cannot be before the start date." }
        require(endDate == null || endDate >= nextDueDate) { "End date cannot be before the next due date." }
        return recurring.insert(
            com.example.data.local.entity.RecurringTransaction(
                userId = userId,
                description = description.trim(),
                amount = amount,
                category = category.trim(),
                type = type,
                startDate = startDate,
                nextDueDate = nextDueDate,
                frequency = frequency,
                endDate = endDate,
                reminderDaysBefore = reminderDaysBefore
            )
        )
    }

    /** Creates the associated transaction and marks the occurrence paid atomically and once. */
    suspend fun markRecurringBillPaid(occurrenceId: Long, userId: String, date: String = ""): Boolean =
        database.withTransaction {
            val occurrence = recurring.getOccurrenceById(occurrenceId, userId) ?: return@withTransaction false
            if (occurrence.status == "paid" || occurrence.transactionId != null) return@withTransaction false
            val currentSchedule = recurring.getScheduleById(occurrence.recurringId, userId)
                ?: return@withTransaction false
            val transactionId = transactions.insert(
                Transaction(
                    amount = occurrence.amount,
                    category = currentSchedule.category,
                    date = date.ifBlank { occurrence.dueDate },
                    description = currentSchedule.description,
                    type = currentSchedule.type,
                    userId = currentSchedule.userId
                )
            )
            recurring.markOccurrence(occurrence.id, userId, "paid", transactionId)
            true
        }

    fun observeRepayments(loanId: Long, userId: String): Flow<List<LoanRepayment>> = repayments.observeForLoan(loanId, userId)

    fun observeOutstandingBalance(loanId: Long, userId: String): Flow<Double> =
        kotlinx.coroutines.flow.combine(
            kotlinx.coroutines.flow.flow { emit(loans.getById(loanId, userId)?.amount ?: 0.0) },
            repayments.observeTotalForLoan(loanId, userId)
        ) { principal, paid -> (principal - paid).coerceAtLeast(0.0) }

    suspend fun recordLoanRepayment(
        loanId: Long,
        userId: String,
        amount: Double,
        paidDate: String,
        note: String = ""
    ): Long = database.withTransaction {
        require(userId.isNotBlank()) { "A signed-in user is required." }
        require(amount.isFinite() && amount > 0.0) { "Repayment must be greater than zero." }
        require(paidDate.isNotBlank()) { "Repayment date is required." }
        val loan = loans.getById(loanId, userId) ?: error("Loan was not found for this account.")
        val paidBefore = repayments.totalForLoan(loanId, userId)
        require(paidBefore + amount <= loan.amount + 0.000001) { "Repayment exceeds the outstanding balance." }
        val id = repayments.insert(
            LoanRepayment(loanId = loanId, amount = amount, paidDate = paidDate, note = note.trim())
        )
        if (paidBefore + amount >= loan.amount - 0.000001) {
            loans.updateRepaymentStatus(loanId, userId, true, paidDate)
        }
        id
    }
}
