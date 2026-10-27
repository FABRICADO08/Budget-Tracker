package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.dao.LoanDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.entity.Loan
import com.example.data.local.entity.Transaction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * User-scoped local financial state. The UID is supplied by the authenticated app session;
 * UI-provided entity ownership is never trusted on inserts or mutations.
 */
class TransactionViewModel(
    private val transactionDao: TransactionDao,
    private val loanDao: LoanDao? = null,
    private val userId: String = ""
) : ViewModel() {

    val transactions: StateFlow<List<Transaction>> = transactionDao.getAllTransactions(userId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val loans: StateFlow<List<Loan>> = (loanDao?.getAllLoans(userId) ?: emptyFlow())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun insertTransaction(
        amount: Double,
        category: String,
        description: String,
        date: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
        type: String = "expense"
    ) {
        if (userId.isBlank() || !amount.isFinite() || amount <= 0.0 ||
            category.isBlank() || description.isBlank()
        ) return
        viewModelScope.launch {
            transactionDao.insert(
                Transaction(
                    amount = amount,
                    category = category.trim(),
                    date = date,
                    description = description.trim(),
                    type = type,
                    userId = userId
                )
            )
        }
    }

    /** Used by CSV and receipt flows; ownership is overwritten with the authenticated UID. */
    fun insert(transaction: Transaction) {
        if (userId.isBlank() || !transaction.amount.isFinite() || transaction.amount <= 0.0) return
        viewModelScope.launch {
            transactionDao.insert(transaction.copy(userId = userId))
        }
    }

    /**
     * Imports a reviewed batch through Room's list insert, so the database applies it atomically
     * rather than leaving a partial import if one insert fails. Ownership is always the session UID.
     */
    fun insertAll(
        items: List<Transaction>,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        if (userId.isBlank() || items.isEmpty()) {
            onComplete?.invoke(false)
            return
        }
        val ownedItems = items.map { item ->
            item.copy(userId = userId)
        }
        if (ownedItems.any {
                !it.amount.isFinite() || it.amount <= 0.0 ||
                    it.category.isBlank() || it.description.isBlank() ||
                    it.date.isBlank() || it.type !in setOf("income", "expense")
            }
        ) {
            onComplete?.invoke(false)
            return
        }
        viewModelScope.launch {
            val result = runCatching { transactionDao.insertAll(ownedItems) }
            onComplete?.invoke(result.isSuccess)
        }
    }

    fun updateTransaction(transaction: Transaction) {
        if (userId.isBlank() || transaction.amount <= 0.0 || !transaction.amount.isFinite()) return
        viewModelScope.launch {
            transactionDao.updateForUser(
                id = transaction.id,
                amount = transaction.amount,
                category = transaction.category,
                date = transaction.date,
                description = transaction.description,
                type = transaction.type,
                userId = userId
            )
        }
    }

    fun deleteTransaction(transaction: Transaction) = deleteById(transaction.id)

    fun deleteById(id: Long) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            transactionDao.deleteById(id, userId)
        }
    }

    fun insertLoan(
        borrowerName: String,
        amount: Double,
        dueDate: String = "",
        note: String = "",
        lentDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    ) {
        if (userId.isBlank() || borrowerName.isBlank() || !amount.isFinite() || amount <= 0.0) return
        viewModelScope.launch {
            loanDao?.insert(
                Loan(
                    borrowerName = borrowerName.trim(),
                    amount = amount,
                    lentDate = lentDate,
                    dueDate = dueDate.trim(),
                    note = note.trim(),
                    isRepaid = false,
                    userId = userId
                )
            )
        }
    }

    fun markLoanAsRepaid(
        loan: Loan,
        repaidDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    ) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            loanDao?.updateRepaymentStatus(loan.id, userId, true, repaidDate)
        }
    }

    fun unmarkLoanRepaid(loan: Loan) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            loanDao?.updateRepaymentStatus(loan.id, userId, false, "")
        }
    }

    fun toggleLoanRepaid(
        loan: Loan,
        repaidDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    ) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            val newStatus = !loan.isRepaid
            loanDao?.updateRepaymentStatus(
                loan.id,
                userId,
                newStatus,
                if (newStatus) repaidDate else ""
            )
        }
    }

    fun deleteLoan(loan: Loan) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            loanDao?.deleteById(loan.id, userId)
        }
    }

    class Factory(
        private val transactionDao: TransactionDao,
        private val loanDao: LoanDao? = null,
        private val userId: String = ""
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
                return TransactionViewModel(transactionDao, loanDao, userId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
