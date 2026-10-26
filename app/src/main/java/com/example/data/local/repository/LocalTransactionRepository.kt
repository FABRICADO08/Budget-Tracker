package com.example.data.local.repository

import com.example.data.local.dao.TransactionDao
import com.example.data.local.entity.Transaction
import kotlinx.coroutines.flow.Flow

/** Local transaction adapter that never queries or mutates records owned by another account. */
class LocalTransactionRepository(
    private val transactionDao: TransactionDao,
    private val userId: String
) {
    init {
        require(userId.isNotBlank()) { "A signed-in user is required." }
    }

    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions(userId)

    fun getTransactionsByDate(date: String): Flow<List<Transaction>> =
        transactionDao.getTransactionsByDate(date, userId)

    fun getTransactionsByCategory(category: String): Flow<List<Transaction>> =
        transactionDao.getTransactionsByCategory(category, userId)

    suspend fun getById(id: Long): Transaction? =
        transactionDao.getTransactionById(id, userId)

    suspend fun insert(transaction: Transaction): Long =
        transactionDao.insert(transaction.copy(userId = userId))

    suspend fun insertAll(transactions: List<Transaction>) =
        transactionDao.insertAll(transactions.map { it.copy(userId = userId) })

    suspend fun update(transaction: Transaction): Int =
        transactionDao.updateForUser(
            id = transaction.id,
            amount = transaction.amount,
            category = transaction.category,
            date = transaction.date,
            description = transaction.description,
            type = transaction.type,
            userId = userId
        )

    suspend fun delete(transaction: Transaction): Int =
        transactionDao.deleteById(transaction.id, userId)

    suspend fun deleteById(id: Long): Int =
        transactionDao.deleteById(id, userId)

    fun getTotalExpenseForDate(date: String): Flow<Double?> =
        transactionDao.getTotalExpenseForDate(date, userId)
}
