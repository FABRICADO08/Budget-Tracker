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

class TransactionViewModel(
    private val transactionDao: TransactionDao,
    private val loanDao: LoanDao? = null
) : ViewModel() {

    // Reactive flow of all transactions from Room
    val transactions: StateFlow<List<Transaction>> = transactionDao.getAllTransactions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // Reactive flow of all loans from Room
    val loans: StateFlow<List<Loan>> = (loanDao?.getAllLoans() ?: emptyFlow())
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
        viewModelScope.launch {
            val transaction = Transaction(
                amount = amount,
                category = category,
                date = date,
                description = description,
                type = type
            )
            transactionDao.insert(transaction)
        }
    }

    fun insert(transaction: Transaction) {
        viewModelScope.launch {
            transactionDao.insert(transaction)
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionDao.update(transaction)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionDao.delete(transaction)
        }
    }

    fun deleteById(id: Long) {
        viewModelScope.launch {
            transactionDao.deleteById(id)
        }
    }

    // --- Loans Management ---

    fun insertLoan(
        borrowerName: String,
        amount: Double,
        dueDate: String = "",
        note: String = "",
        lentDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    ) {
        viewModelScope.launch {
            loanDao?.let { dao ->
                val loan = Loan(
                    borrowerName = borrowerName.trim(),
                    amount = amount,
                    lentDate = lentDate,
                    dueDate = dueDate.trim(),
                    note = note.trim(),
                    isRepaid = false
                )
                dao.insert(loan)
            }
        }
    }

    fun markLoanAsRepaid(
        loan: Loan,
        repaidDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    ) {
        viewModelScope.launch {
            loanDao?.updateRepaymentStatus(loan.id, true, repaidDate)
        }
    }

    fun unmarkLoanRepaid(loan: Loan) {
        viewModelScope.launch {
            loanDao?.updateRepaymentStatus(loan.id, false, "")
        }
    }

    fun toggleLoanRepaid(
        loan: Loan,
        repaidDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    ) {
        viewModelScope.launch {
            loanDao?.let { dao ->
                val newStatus = !loan.isRepaid
                val dateToRecord = if (newStatus) repaidDate else ""
                dao.updateRepaymentStatus(loan.id, newStatus, dateToRecord)
            }
        }
    }

    fun deleteLoan(loan: Loan) {
        viewModelScope.launch {
            loanDao?.delete(loan)
        }
    }

    class Factory(
        private val transactionDao: TransactionDao,
        private val loanDao: LoanDao? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
                return TransactionViewModel(transactionDao, loanDao) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
