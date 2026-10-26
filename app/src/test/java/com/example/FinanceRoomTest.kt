package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.database.AppDatabase
import com.example.data.local.entity.BudgetAlert
import com.example.data.local.entity.CategoryBudget
import com.example.data.local.entity.Loan
import com.example.data.local.entity.LoanRepayment
import com.example.data.local.entity.RecurringOccurrence
import com.example.data.local.entity.RecurringTransaction
import com.example.data.local.entity.Transaction
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
class FinanceRoomTest {
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun categoryBudgetAndAlertPersistAndAlertThresholdIsUnique() = runBlocking {
        val budgetId = database.categoryBudgetDao().insert(
            CategoryBudget(userId = "test-user", category = "Groceries", month = "2026-10", limitAmount = 2500.0)
        )
        assertTrue(budgetId > 0)
        val alert = BudgetAlert(
            budgetId = budgetId,
            month = "2026-10",
            thresholdPercent = 70,
            actualAmount = 1750.0
        )
        assertTrue(database.budgetAlertDao().insertIfNew(alert) > 0)
        assertEquals(-1L, database.budgetAlertDao().insertIfNew(alert))
        assertTrue(database.budgetAlertDao().exists(budgetId, "2026-10", 70))
    }

    @Test
    fun recurringOccurrenceCannotBeInsertedTwiceForSameDueDate() = runBlocking {
        val scheduleId = database.recurringTransactionDao().insert(
            RecurringTransaction(
                description = "Fibre",
                amount = 899.0,
                category = "Bills & Utilities",
                type = "expense",
                startDate = "2026-10-01",
                nextDueDate = "2026-11-01",
                frequency = "MONTHLY"
            )
        )
        val occurrence = RecurringOccurrence(
            recurringId = scheduleId,
            dueDate = "2026-11-01",
            amount = 899.0
        )
        assertTrue(database.recurringTransactionDao().insertOccurrenceIfNew(occurrence) > 0)
        assertEquals(-1L, database.recurringTransactionDao().insertOccurrenceIfNew(occurrence))
    }

    @Test
    fun repaymentsAreSeparateFromOriginalLoanAmount() = runBlocking {
        val loanId = database.loanDao().insert(
            Loan(
                borrowerName = "Test borrower",
                amount = 1000.0,
                lentDate = "2026-09-01",
                dueDate = "2026-10-01"
            )
        )
        database.loanRepaymentDao().insert(
            LoanRepayment(loanId = loanId, amount = 250.0, paidDate = "2026-09-15")
        )
        database.loanRepaymentDao().insert(
            LoanRepayment(loanId = loanId, amount = 300.0, paidDate = "2026-09-25")
        )
        assertEquals(1000.0, database.loanDao().getById(loanId)!!.amount, 0.001)
        assertEquals(550.0, database.loanRepaymentDao().totalForLoan(loanId), 0.001)
        assertEquals(2, database.loanRepaymentDao().observeForLoan(loanId).first().size)
    }

    @Test
    fun migrationFromVersionTwoPreservesExistingFinancialRecords() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "budget_tracker_migration_test.db"
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        val oldDb = SQLiteDatabase.openOrCreateDatabase(file, null)
        oldDb.execSQL(
            """CREATE TABLE transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                amount REAL NOT NULL,
                category TEXT NOT NULL,
                date TEXT NOT NULL,
                description TEXT NOT NULL,
                type TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )"""
        )
        oldDb.execSQL(
            """CREATE TABLE loans (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                borrower_name TEXT NOT NULL,
                amount REAL NOT NULL,
                lent_date TEXT NOT NULL,
                due_date TEXT NOT NULL,
                note TEXT NOT NULL,
                is_repaid INTEGER NOT NULL,
                repaid_date TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )"""
        )
        oldDb.execSQL(
            "INSERT INTO transactions(id, amount, category, date, description, type, created_at) " +
                "VALUES (1, 42.50, 'Groceries', '2026-10-01', 'Existing record', 'expense', 1)"
        )
        oldDb.execSQL(
            "INSERT INTO loans(id, borrower_name, amount, lent_date, due_date, note, is_repaid, repaid_date, created_at) " +
                "VALUES (1, 'Existing borrower', 500.0, '2026-09-01', '2026-10-01', '', 0, '', 1)"
        )
        oldDb.execSQL(
            "INSERT INTO loans(id, borrower_name, amount, lent_date, due_date, note, is_repaid, repaid_date, created_at) " +
                "VALUES (2, 'Legacy repaid borrower', 300.0, '2026-08-01', '2026-09-01', '', 1, '2026-08-25', 2)"
        )
        oldDb.version = 2
        oldDb.close()

        val migrated = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()
        try {
            assertEquals(42.50, migrated.transactionDao().getTransactionById(1L)!!.amount, 0.001)
            assertEquals("Existing borrower", migrated.loanDao().getById(1L)!!.borrowerName)
            assertEquals(300.0, migrated.loanRepaymentDao().totalForLoan(2L), 0.001)
            assertEquals(1, migrated.loanRepaymentDao().observeForLoan(2L).first().size)
            assertTrue(migrated.categoryBudgetDao().observeForMonth("2026-10", "test-user").first().isEmpty())
        } finally {
            migrated.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun reminderCandidatesAreScopedToUser() = runBlocking {
        val dao = database.recurringTransactionDao()
        dao.insert(
            RecurringTransaction(
                userId = "account-a",
                description = "Rent",
                amount = 8000.0,
                category = "Housing",
                type = "expense",
                startDate = "2026-10-01",
                nextDueDate = "2026-11-01",
                frequency = "MONTHLY"
            )
        )
        dao.insert(
            RecurringTransaction(
                userId = "account-b",
                description = "Insurance",
                amount = 1200.0,
                category = "Bills & Utilities",
                type = "expense",
                startDate = "2026-10-01",
                nextDueDate = "2026-11-05",
                frequency = "MONTHLY"
            )
        )

        assertEquals(listOf("Rent"), dao.getActiveForUser("account-a").map { it.description })
        assertTrue(dao.getActiveForUser("unknown-account").isEmpty())
    }

}
