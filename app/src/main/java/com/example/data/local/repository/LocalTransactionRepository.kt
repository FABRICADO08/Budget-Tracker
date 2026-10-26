package com.example.data.local.repository

import com.example.data.local.dao.TransactionDao
import com.example.data.local.entity.Transaction
import kotlinx.coroutines.flow.Flow

class LocalTransactionRepository(private val transactionDao: TransactionDao) {
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    fun getTransactionsByDate(date: String): Flow<List<Transaction>> =
        transactionDao.getTransactionsByDate(date)

    fun getTransactionsByCategory(category: String): Flow<List<Transaction>> =
        transactionDao.getTransactionsByCategory(category)

    suspend fun getById(id: Long): Transaction? =
        transactionDao.getTransactionById(id)

    suspend fun insert(transaction: Transaction): Long =
        transactionDao.insert(transaction)

    suspend fun insertAll(transactions: List<Transaction>) =
        transactionDao.insertAll(transactions)

    suspend fun update(transaction: Transaction) =
        transactionDao.update(transaction)

    suspend fun delete(transaction: Transaction) =
        transactionDao.delete(transaction)

    suspend fun deleteById(id: Long) =
        transactionDao.deleteById(id)

    fun getTotalExpenseForDate(date: String): Flow<Double?> =
        transactionDao.getTotalExpenseForDate(date)
}
