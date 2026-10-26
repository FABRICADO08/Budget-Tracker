package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.dao.LoanDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.database.AppDatabase
import com.example.ui.viewmodel.TransactionViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TransactionViewModelTest {

    private lateinit var db: AppDatabase
    private lateinit var transactionDao: TransactionDao
    private lateinit var loanDao: LoanDao
    private lateinit var viewModel: TransactionViewModel

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        transactionDao = db.transactionDao()
        loanDao = db.loanDao()
        viewModel = TransactionViewModel(transactionDao, loanDao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndObserveTransactions() = runBlocking {
        viewModel.insertTransaction(
            amount = 18.75,
            category = "Food & Dining",
            description = "Lunch bowl",
            date = "2026-10-25"
        )

        val list = transactionDao.getAllTransactions().first()
        assertFalse(list.isEmpty())
        val item = list.first()
        assertEquals(18.75, item.amount, 0.001)
        assertEquals("Food & Dining", item.category)
        assertEquals("Lunch bowl", item.description)

        viewModel.deleteTransaction(item)
        val afterDelete = transactionDao.getAllTransactions().first()
        assertTrue(afterDelete.isEmpty())
    }

    @Test
    fun insertAndMarkLoanAsRepaid() = runBlocking {
        viewModel.insertLoan(
            borrowerName = "Sipho",
            amount = 350.00,
            dueDate = "2026-11-01",
            note = "Taxi fare"
        )

        val loansList = loanDao.getAllLoans().first()
        assertFalse(loansList.isEmpty())
        val loan = loansList.first()
        assertEquals("Sipho", loan.borrowerName)
        assertEquals(350.00, loan.amount, 0.001)
        assertFalse(loan.isRepaid)

        // Mark as brought back
        viewModel.toggleLoanRepaid(loan)
        val updatedLoans = loanDao.getAllLoans().first()
        val updatedLoan = updatedLoans.first { it.id == loan.id }
        assertTrue(updatedLoan.isRepaid)
        assertTrue(updatedLoan.repaidDate.isNotBlank())
    }
}
