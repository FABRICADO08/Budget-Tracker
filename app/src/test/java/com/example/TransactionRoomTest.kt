package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.dao.TransactionDao
import com.example.data.local.database.AppDatabase
import com.example.data.local.entity.Transaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TransactionRoomTest {

    private lateinit var db: AppDatabase
    private lateinit var transactionDao: TransactionDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        transactionDao = db.transactionDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndRetrieveTransaction() = runBlocking {
        val transaction = Transaction(
            amount = 42.50,
            category = "Groceries",
            date = "2026-10-24",
            description = "Supermarket run for weekly vegetables"
        )
        val id = transactionDao.insert(transaction)
        assertTrue(id > 0)

        val retrieved = transactionDao.getTransactionById(id)
        assertNotNull(retrieved)
        assertEquals(42.50, retrieved!!.amount, 0.001)
        assertEquals("Groceries", retrieved.category)
        assertEquals("2026-10-24", retrieved.date)
        assertEquals("Supermarket run for weekly vegetables", retrieved.description)
        assertEquals("expense", retrieved.type)

        val list = transactionDao.getAllTransactions().first()
        assertEquals(1, list.size)
        assertEquals(id, list[0].id)
    }

    @Test
    fun updateTransaction() = runBlocking {
        val transaction = Transaction(
            amount = 150.00,
            category = "Utilities",
            date = "2026-10-25",
            description = "Electricity bill"
        )
        val id = transactionDao.insert(transaction)
        val inserted = transactionDao.getTransactionById(id)
        assertNotNull(inserted)

        val updated = inserted!!.copy(amount = 180.00, description = "Electricity & Water bill")
        transactionDao.update(updated)

        val retrieved = transactionDao.getTransactionById(id)
        assertNotNull(retrieved)
        assertEquals(180.00, retrieved!!.amount, 0.001)
        assertEquals("Electricity & Water bill", retrieved.description)
    }

    @Test
    fun deleteTransaction() = runBlocking {
        val transaction = Transaction(
            amount = 12.00,
            category = "Transportation",
            date = "2026-10-24",
            description = "Subway ticket"
        )
        val id = transactionDao.insert(transaction)
        val inserted = transactionDao.getTransactionById(id)
        assertNotNull(inserted)

        transactionDao.delete(inserted!!)

        val list = transactionDao.getAllTransactions().first()
        assertTrue(list.isEmpty())
    }

    @Test
    fun deleteByIdAndQueryFiltered() = runBlocking {
        val tx1 = Transaction(amount = 50.0, category = "Food", date = "2026-10-20", description = "Lunch")
        val tx2 = Transaction(amount = 90.0, category = "Food", date = "2026-10-21", description = "Dinner")
        val tx3 = Transaction(amount = 200.0, category = "Transport", date = "2026-10-20", description = "Fuel")

        transactionDao.insertAll(listOf(tx1, tx2, tx3))

        val foodList = transactionDao.getTransactionsByCategory("Food").first()
        assertEquals(2, foodList.size)

        val dateList = transactionDao.getTransactionsByDate("2026-10-20").first()
        assertEquals(2, dateList.size)

        val firstFoodId = foodList[0].id
        transactionDao.deleteById(firstFoodId)

        val afterDelete = transactionDao.getTransactionById(firstFoodId)
        assertNull(afterDelete)

        transactionDao.deleteAll()
        val allAfterClear = transactionDao.getAllTransactions().first()
        assertTrue(allAfterClear.isEmpty())
    }
}
